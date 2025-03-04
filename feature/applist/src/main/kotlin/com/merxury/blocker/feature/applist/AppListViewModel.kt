/*
 * Copyright 2023 Blocker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.merxury.blocker.feature.applist

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.merxury.blocker.core.analytics.AnalyticsHelper
import com.merxury.blocker.core.data.respository.app.AppRepository
import com.merxury.blocker.core.data.respository.userdata.UserDataRepository
import com.merxury.blocker.core.dispatchers.BlockerDispatchers.DEFAULT
import com.merxury.blocker.core.dispatchers.BlockerDispatchers.IO
import com.merxury.blocker.core.dispatchers.BlockerDispatchers.MAIN
import com.merxury.blocker.core.dispatchers.Dispatcher
import com.merxury.blocker.core.domain.InitializeDatabaseUseCase
import com.merxury.blocker.core.domain.model.InitializeState
import com.merxury.blocker.core.extension.exec
import com.merxury.blocker.core.extension.getPackageInfoCompat
import com.merxury.blocker.core.model.data.AppItem
import com.merxury.blocker.core.model.data.toAppServiceStatus
import com.merxury.blocker.core.model.preference.AppSorting
import com.merxury.blocker.core.model.preference.AppSorting.FIRST_INSTALL_TIME
import com.merxury.blocker.core.model.preference.AppSorting.LAST_UPDATE_TIME
import com.merxury.blocker.core.model.preference.AppSorting.NAME
import com.merxury.blocker.core.model.preference.SortingOrder
import com.merxury.blocker.core.result.Result
import com.merxury.blocker.core.ui.data.UiMessage
import com.merxury.blocker.core.ui.data.toErrorMessage
import com.merxury.blocker.core.ui.state.AppStateCache
import com.merxury.blocker.core.ui.state.RunningAppCache
import com.merxury.blocker.core.utils.ApplicationUtil
import com.merxury.blocker.core.utils.FileUtils
import com.merxury.blocker.feature.applist.AppListUiState.Initializing
import com.merxury.blocker.feature.applist.AppListUiState.Success
import com.merxury.core.ifw.IIntentFirewall
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AppListViewModel @Inject constructor(
    app: android.app.Application,
    private val pm: PackageManager,
    private val userDataRepository: UserDataRepository,
    private val appRepository: AppRepository,
    private val initializeDatabase: InitializeDatabaseUseCase,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    @Dispatcher(DEFAULT) private val cpuDispatcher: CoroutineDispatcher,
    @Dispatcher(MAIN) private val mainDispatcher: CoroutineDispatcher,
    private val analyticsHelper: AnalyticsHelper,
    private val intentFirewall: IIntentFirewall,
) : AndroidViewModel(app) {
    private val _uiState = MutableStateFlow<AppListUiState>(Initializing())
    val uiState = _uiState.asStateFlow()
    private val _errorState = MutableStateFlow<UiMessage?>(null)
    val errorState = _errorState.asStateFlow()
    private val _warningState = MutableStateFlow<WarningDialogData?>(null)
    val warningState = _warningState.asStateFlow()
    private var _appList = mutableStateListOf<AppItem>()
    private val _appListFlow = MutableStateFlow(_appList)
    private var currentSearchKeyword = ""
    private var loadAppListJob: Job? = null
    private val refreshServiceJobs = SupervisorJob()
    val appListFlow: StateFlow<List<AppItem>>
        get() = _appListFlow
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable)
        _errorState.tryEmit(throwable.toErrorMessage())
    }

    init {
        loadData()
        updateInstalledAppList()
        listenSortingChanges()
        listenShowRunningAppsOnTopChanges()
        listenShowSystemAppsChanges()
    }

    fun loadData() {
        loadAppListJob?.cancel()
        loadAppListJob = viewModelScope.launch(cpuDispatcher + exceptionHandler) {
            // Init DB first to get correct data
            initializeDatabase()
                .takeWhile { it is InitializeState.Initializing }
                .collect {
                    if (it is InitializeState.Initializing) {
                        _uiState.emit(Initializing(it.processingName))
                    }
                }
            appRepository.getApplicationList()
                .onStart {
                    Timber.v("Start loading app list")
                    _uiState.update {
                        if (it is Success) {
                            it.copy(isRefreshing = true)
                        } else {
                            Initializing()
                        }
                    }
                }
                .distinctUntilChanged()
                .collect { list ->
                    Timber.v("App list changed, size ${list.size}")
                    val preference = userDataRepository.userData.first()
                    val sortType = preference.appSorting
                    val sortOrder = preference.appSortingOrder
                    RunningAppCache.refresh(ioDispatcher)
                    _appList = if (preference.showSystemApps) {
                        list
                    } else {
                        list.filterNot { it.isSystem }
                    }.filter {
                        it.label.contains(currentSearchKeyword, true) ||
                            it.packageName.contains(currentSearchKeyword, true)
                    }.map { installedApp ->
                        val packageName = installedApp.packageName
                        AppItem(
                            label = installedApp.label,
                            packageName = packageName,
                            versionName = installedApp.versionName,
                            versionCode = installedApp.versionCode,
                            isSystem = ApplicationUtil.isSystemApp(pm, packageName),
                            isRunning = RunningAppCache.isRunning(packageName),
                            isEnabled = installedApp.isEnabled,
                            firstInstallTime = installedApp.firstInstallTime,
                            lastUpdateTime = installedApp.lastUpdateTime,
                            appServiceStatus = AppStateCache.getOrNull(packageName)
                                ?.toAppServiceStatus(),
                            packageInfo = pm.getPackageInfoCompat(packageName, 0),
                        )
                    }.sortedWith(
                        appComparator(sortType, sortOrder),
                    ).let { sortedList ->
                        if (preference.showRunningAppsOnTop) {
                            sortedList.sortedByDescending { it.isRunning }
                        } else {
                            sortedList
                        }
                    }
                        .toMutableStateList()
                    withContext(mainDispatcher) {
                        refreshServiceJobs.cancelChildren()
                        _appListFlow.value = _appList
                        _uiState.emit(Success(isRefreshing = false))
                    }
                }
        }
    }

    fun filter(keyword: String) {
        currentSearchKeyword = keyword
        loadData()
    }

    private fun appComparator(sortType: AppSorting, sortOrder: SortingOrder): Comparator<AppItem> =
        if (sortOrder == SortingOrder.ASCENDING) {
            when (sortType) {
                NAME -> compareBy { it.label.lowercase() }
                FIRST_INSTALL_TIME -> compareBy { it.firstInstallTime }
                LAST_UPDATE_TIME -> compareBy { it.lastUpdateTime }
            }
        } else {
            when (sortType) {
                NAME -> compareByDescending { it.label.lowercase() }
                FIRST_INSTALL_TIME -> compareByDescending { it.firstInstallTime }
                LAST_UPDATE_TIME -> compareByDescending { it.lastUpdateTime }
            }
        }

    private fun updateInstalledAppList() = viewModelScope.launch {
        appRepository.updateApplicationList().collect {
            if (it is Result.Error) {
                _errorState.emit(it.exception?.toErrorMessage())
            }
        }
    }

    private fun listenSortingChanges() = viewModelScope.launch(cpuDispatcher) {
        userDataRepository.userData
            .distinctUntilChanged()
            .drop(1)
            .collect { userData ->
                val newList = _appList.toMutableList()
                newList.sortWith(appComparator(userData.appSorting, userData.appSortingOrder))
                if (userDataRepository.userData.first().showRunningAppsOnTop) {
                    newList.sortByDescending { it.isRunning }
                }
                withContext(mainDispatcher) {
                    refreshServiceJobs.cancelChildren()
                    _appList = newList.toMutableStateList()
                    _appListFlow.value = _appList
                }
            }
    }

    private fun listenShowRunningAppsOnTopChanges() = viewModelScope.launch {
        userDataRepository.userData
            .map { it.showRunningAppsOnTop }
            .distinctUntilChanged()
            .drop(1)
            .collect { showRunningAppsOnTop ->
                val newList = _appList.toMutableList()
                if (showRunningAppsOnTop) {
                    newList.sortByDescending { it.isRunning }
                } else {
                    val sorting = userDataRepository.userData.first()
                        .appSorting
                    val order = userDataRepository.userData.first()
                        .appSortingOrder
                    newList.sortWith(appComparator(sorting, order))
                }
                withContext(mainDispatcher) {
                    refreshServiceJobs.cancelChildren()
                    _appList = newList.toMutableStateList()
                    _appListFlow.value = _appList
                }
            }
    }

    private fun listenShowSystemAppsChanges() = viewModelScope.launch {
        userDataRepository.userData
            .map { it.showSystemApps }
            .distinctUntilChanged()
            .drop(1)
            .collect { loadData() }
    }

    fun updateServiceStatus(packageName: String, index: Int) {
        viewModelScope.launch(context = refreshServiceJobs + ioDispatcher + exceptionHandler) {
            val userData = userDataRepository.userData.first()
            if (!userData.showServiceInfo) {
                return@launch
            }
            val oldItem = _appList.getOrNull(index) ?: return@launch
            if (oldItem.appServiceStatus != null) {
                // Don't get service info again
                return@launch
            }
            Timber.d("Get service status for $packageName")
            val status = AppStateCache.get(
                getApplication(),
                intentFirewall,
                packageName,
            )
            val newItem = oldItem.copy(appServiceStatus = status.toAppServiceStatus())
            withContext(mainDispatcher) {
                _appList[index] = newItem
            }
        }
    }

    fun dismissErrorDialog() = viewModelScope.launch {
        _errorState.emit(null)
    }

    fun clearData(packageName: String) = viewModelScope.launch {
        val action: () -> Unit = {
            viewModelScope.launch(ioDispatcher + exceptionHandler) {
                Timber.d("Clear data for $packageName")
                "pm clear $packageName".exec(ioDispatcher)
                analyticsHelper.logClearDataClicked()
            }
        }
        val label = appRepository.getApplication(packageName)
            .flowOn(ioDispatcher)
            .first()
            ?.label
            ?: packageName
        val data = WarningDialogData(
            title = label,
            message = R.string.feature_applist_do_you_want_to_clear_data_of_this_app,
            onPositiveButtonClicked = action,
        )
        _warningState.emit(data)
    }

    fun clearCache(packageName: String) = viewModelScope.launch(ioDispatcher + exceptionHandler) {
        val context: Context = getApplication()
        val cacheFolder = context.filesDir
            ?.parentFile
            ?.parentFile
            ?.resolve(packageName)
            ?.resolve("cache")
            ?: run {
                Timber.e("Can't resolve cache path for $packageName")
                return@launch
            }
        Timber.d("Delete cache folder: $cacheFolder")
        FileUtils.delete(cacheFolder.absolutePath, recursively = true, ioDispatcher)
        analyticsHelper.logClearCacheClicked()
    }

    fun uninstall(packageName: String) = viewModelScope.launch {
        val action: () -> Unit = {
            viewModelScope.launch(ioDispatcher + exceptionHandler) {
                Timber.d("Uninstall $packageName")
                "pm uninstall $packageName".exec(ioDispatcher)
                notifyAppUpdated(packageName)
                analyticsHelper.logUninstallAppClicked()
            }
        }
        val label = appRepository.getApplication(packageName)
            .flowOn(ioDispatcher)
            .first()
            ?.label
            ?: packageName
        val data = WarningDialogData(
            title = label,
            message = R.string.feature_applist_do_you_want_to_uninstall_this_app,
            onPositiveButtonClicked = action,
        )
        _warningState.emit(data)
    }

    fun forceStop(packageName: String) = viewModelScope.launch(ioDispatcher + exceptionHandler) {
        "am force-stop $packageName".exec(ioDispatcher)
        RunningAppCache.update(packageName, ioDispatcher)
        val item = _appList.find { it.packageName == packageName }
        if (item != null) {
            val index = _appList.indexOf(item)
            val newItem = item.copy(isRunning = RunningAppCache.isRunning(packageName))
            _appList[index] = newItem
        }
        analyticsHelper.logForceStopClicked()
    }

    fun dismissWarningDialog() = viewModelScope.launch {
        _warningState.emit(null)
    }

    fun enable(packageName: String) = viewModelScope.launch(ioDispatcher + exceptionHandler) {
        "pm enable $packageName".exec(ioDispatcher)
        notifyAppUpdated(packageName)
        analyticsHelper.logEnableAppClicked()
    }

    fun disable(packageName: String) = viewModelScope.launch(ioDispatcher + exceptionHandler) {
        "pm disable $packageName".exec(ioDispatcher)
        notifyAppUpdated(packageName)
        analyticsHelper.logDisableAppClicked()
    }

    private suspend fun notifyAppUpdated(packageName: String) {
        appRepository.updateApplication(packageName).collect {
            if (it is Result.Error) {
                _errorState.emit(it.exception?.toErrorMessage())
            }
        }
    }
}

sealed interface AppListUiState {
    class Initializing(val processingName: String = "") : AppListUiState
    class Error(val error: UiMessage) : AppListUiState
    data class Success(val isRefreshing: Boolean = false) : AppListUiState
}

data class WarningDialogData(
    val title: String,
    val message: Int,
    val onPositiveButtonClicked: () -> Unit,
)
