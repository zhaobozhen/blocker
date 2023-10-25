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

package com.merxury.blocker.feature.ruledetail

import androidx.compose.animation.core.FloatExponentialDecaySpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.merxury.blocker.core.designsystem.component.BlockerAppTopBarMenu
import com.merxury.blocker.core.designsystem.component.BlockerCollapsingTopAppBar
import com.merxury.blocker.core.designsystem.component.BlockerErrorAlertDialog
import com.merxury.blocker.core.designsystem.component.BlockerTab
import com.merxury.blocker.core.designsystem.component.BlockerTabRow
import com.merxury.blocker.core.designsystem.component.DropDownMenuItem
import com.merxury.blocker.core.designsystem.component.MaxToolbarHeight
import com.merxury.blocker.core.designsystem.component.MinToolbarHeight
import com.merxury.blocker.core.designsystem.component.ThemePreviews
import com.merxury.blocker.core.designsystem.icon.BlockerIcons
import com.merxury.blocker.core.designsystem.theme.BlockerTheme
import com.merxury.blocker.core.model.data.ComponentItem
import com.merxury.blocker.core.model.data.GeneralRule
import com.merxury.blocker.core.model.data.IconBasedThemingState
import com.merxury.blocker.core.ui.TabState
import com.merxury.blocker.core.ui.TrackScreenViewEvent
import com.merxury.blocker.core.ui.data.UiMessage
import com.merxury.blocker.core.ui.previewparameter.AppListPreviewParameterProvider
import com.merxury.blocker.core.ui.previewparameter.ComponentListPreviewParameterProvider
import com.merxury.blocker.core.ui.previewparameter.RuleDetailTabStatePreviewParameterProvider
import com.merxury.blocker.core.ui.previewparameter.RuleListPreviewParameterProvider
import com.merxury.blocker.core.ui.rule.RuleDetailTabs
import com.merxury.blocker.core.ui.rule.RuleMatchedApp
import com.merxury.blocker.core.ui.rule.RuleMatchedAppList
import com.merxury.blocker.core.ui.rule.RuleMatchedAppListUiState
import com.merxury.blocker.core.ui.screen.ErrorScreen
import com.merxury.blocker.core.ui.screen.LoadingScreen
import com.merxury.blocker.core.ui.state.toolbar.AppBarAction.MORE
import com.merxury.blocker.core.ui.state.toolbar.AppBarUiState
import com.merxury.blocker.core.ui.state.toolbar.ExitUntilCollapsedState
import com.merxury.blocker.core.ui.state.toolbar.ToolbarState
import com.merxury.blocker.feature.ruledetail.R.string
import com.merxury.blocker.feature.ruledetail.component.RuleDescription
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

@Composable
fun RuleDetailRoute(
    onBackClick: () -> Unit,
    navigateToAppDetail: (String) -> Unit,
    updateIconBasedThemingState: (IconBasedThemingState) -> Unit,
    viewModel: RuleDetailViewModel = hiltViewModel(),
) {
    val ruleInfoUiState by viewModel.ruleInfoUiState.collectAsStateWithLifecycle()
    val tabState by viewModel.tabState.collectAsStateWithLifecycle()
    val errorState by viewModel.errorState.collectAsStateWithLifecycle()
    val appBarUiState by viewModel.appBarUiState.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    RuleDetailScreen(
        ruleInfoUiState = ruleInfoUiState,
        onBackClick = onBackClick,
        tabState = tabState,
        switchTab = viewModel::switchTab,
        appBarUiState = appBarUiState,
        onStopServiceClick = viewModel::stopService,
        onLaunchActivityClick = viewModel::launchActivity,
        onCopyNameClick = { clipboardManager.setText(AnnotatedString(it)) },
        onCopyFullNameClick = { clipboardManager.setText(AnnotatedString(it)) },
        onBlockAllClick = { viewModel.controlAllComponents(it, false) },
        onEnableAllClick = { viewModel.controlAllComponents(it, true) },
        onBlockAllInPageClick = { viewModel.controlAllComponentsInPage(false) },
        onEnableAllInPageClick = { viewModel.controlAllComponentsInPage(true) },
        onSwitch = viewModel::controlComponent,
        navigateToAppDetail = navigateToAppDetail,
        updateIconBasedThemingState = updateIconBasedThemingState,
    )
    if (errorState != null) {
        BlockerErrorAlertDialog(
            title = errorState?.title.orEmpty(),
            text = errorState?.content.orEmpty(),
            onDismissRequest = viewModel::dismissAlert,
        )
    }
    LaunchedEffect(Unit) {
        viewModel.initShizuku()
    }
    DisposableEffect(Unit) {
        onDispose {
            updateIconBasedThemingState(IconBasedThemingState(icon = null, isBasedIcon = false))
        }
    }
}

@Composable
fun RuleDetailScreen(
    modifier: Modifier = Modifier,
    ruleInfoUiState: RuleInfoUiState,
    tabState: TabState<RuleDetailTabs>,
    appBarUiState: AppBarUiState = AppBarUiState(),
    onBackClick: () -> Unit = {},
    switchTab: (RuleDetailTabs) -> Unit = { _ -> },
    onStopServiceClick: (String, String) -> Unit = { _, _ -> },
    onLaunchActivityClick: (String, String) -> Unit = { _, _ -> },
    onCopyNameClick: (String) -> Unit = { _ -> },
    onCopyFullNameClick: (String) -> Unit = { _ -> },
    onBlockAllClick: (List<ComponentItem>) -> Unit = { _ -> },
    onEnableAllClick: (List<ComponentItem>) -> Unit = { _ -> },
    onBlockAllInPageClick: () -> Unit = { },
    onEnableAllInPageClick: () -> Unit = { },
    onSwitch: (String, String, Boolean) -> Unit = { _, _, _ -> },
    navigateToAppDetail: (String) -> Unit = { _ -> },
    updateIconBasedThemingState: (IconBasedThemingState) -> Unit = { _ -> },
) {
    when (ruleInfoUiState) {
        RuleInfoUiState.Loading -> {
            LoadingScreen()
        }

        is RuleInfoUiState.Success -> {
            RuleDetailContent(
                modifier = modifier,
                ruleMatchedAppListUiState = ruleInfoUiState.matchedAppsUiState,
                ruleInfoUiState = ruleInfoUiState,
                onBackClick = onBackClick,
                appBarUiState = appBarUiState,
                tabState = tabState,
                switchTab = switchTab,
                onStopServiceClick = onStopServiceClick,
                onLaunchActivityClick = onLaunchActivityClick,
                onCopyNameClick = onCopyNameClick,
                onCopyFullNameClick = onCopyFullNameClick,
                onBlockAllClick = onBlockAllClick,
                onEnableAllClick = onEnableAllClick,
                onBlockAllInPageClick = onBlockAllInPageClick,
                onEnableAllInPageClick = onEnableAllInPageClick,
                onSwitch = onSwitch,
                navigateToAppDetail = navigateToAppDetail,
                updateIconBasedThemingState = updateIconBasedThemingState,
            )
        }

        is RuleInfoUiState.Error -> {
            ErrorScreen(error = ruleInfoUiState.error)
        }
    }
    TrackScreenViewEvent(screenName = "RuleDetailScreen")
}

@Composable
fun RuleDetailContent(
    modifier: Modifier = Modifier,
    ruleMatchedAppListUiState: RuleMatchedAppListUiState,
    ruleInfoUiState: RuleInfoUiState.Success,
    onBackClick: () -> Unit,
    appBarUiState: AppBarUiState = AppBarUiState(),
    tabState: TabState<RuleDetailTabs>,
    switchTab: (RuleDetailTabs) -> Unit,
    onStopServiceClick: (String, String) -> Unit = { _, _ -> },
    onLaunchActivityClick: (String, String) -> Unit = { _, _ -> },
    onCopyNameClick: (String) -> Unit = { _ -> },
    onCopyFullNameClick: (String) -> Unit = { _ -> },
    onBlockAllClick: (List<ComponentItem>) -> Unit = { _ -> },
    onEnableAllClick: (List<ComponentItem>) -> Unit = { _ -> },
    onBlockAllInPageClick: () -> Unit = { },
    onEnableAllInPageClick: () -> Unit = { },
    onSwitch: (String, String, Boolean) -> Unit = { _, _, _ -> },
    navigateToAppDetail: (String) -> Unit = { _ -> },
    updateIconBasedThemingState: (IconBasedThemingState) -> Unit = { _ -> },
) {
    val listState = rememberLazyListState()
    val systemStatusHeight = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    val toolbarHeightRange = with(LocalDensity.current) {
        MinToolbarHeight.roundToPx() + systemStatusHeight.roundToPx()..MaxToolbarHeight.roundToPx() + systemStatusHeight.roundToPx()
    }
    val toolbarState = rememberToolbarState(toolbarHeightRange)
    val scope = rememberCoroutineScope()
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                toolbarState.scrollTopLimitReached =
                    listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                toolbarState.scrollOffset -= available.y
                return Offset(0f, toolbarState.consumed)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (available.y > 0) {
                    scope.launch {
                        animateDecay(
                            initialValue = toolbarState.height + toolbarState.offset,
                            initialVelocity = available.y,
                            animationSpec = FloatExponentialDecaySpec(),
                        ) { value, _ ->
                            toolbarState.scrollTopLimitReached =
                                listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                            toolbarState.scrollOffset -= (value - (toolbarState.height + toolbarState.offset))
                            if (toolbarState.scrollOffset == 0f) scope.coroutineContext.cancelChildren()
                        }
                    }
                }
                return super.onPostFling(consumed, available)
            }
        }
    }
    updateIconBasedThemingState(
        IconBasedThemingState(
            icon = ruleInfoUiState.ruleIcon,
            isBasedIcon = true,
        ),
    )
    Scaffold(
        topBar = {
            BlockerCollapsingTopAppBar(
                progress = toolbarState.progress,
                onNavigationClick = onBackClick,
                title = ruleInfoUiState.ruleInfo.name,
                subtitle = ruleInfoUiState.ruleInfo.company.toString(),
                summary = "",
                iconSource = ruleInfoUiState.ruleInfo.iconUrl,
                onIconClick = { },
                actions = {
                    RuleDetailAppBarActions(
                        appBarUiState = appBarUiState,
                        blockAllComponents = onBlockAllInPageClick,
                        enableAllComponents = onEnableAllInPageClick,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(LocalDensity.current) { toolbarState.height.toDp() }),
            )
        },
        modifier = modifier.nestedScroll(nestedScrollConnection),
    ) { innerPadding ->
        RuleDetailTabContent(
            modifier = Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .fillMaxSize(),
            ruleMatchedAppListUiState = ruleMatchedAppListUiState,
            ruleInfoUiState = ruleInfoUiState,
            tabState = tabState,
            switchTab = switchTab,
            onStopServiceClick = onStopServiceClick,
            onLaunchActivityClick = onLaunchActivityClick,
            onCopyNameClick = onCopyNameClick,
            onCopyFullNameClick = onCopyFullNameClick,
            onBlockAllClick = onBlockAllClick,
            onEnableAllClick = onEnableAllClick,
            onSwitch = onSwitch,
            navigateToAppDetail = navigateToAppDetail,
        )
    }
}

@Composable
fun RuleDetailAppBarActions(
    appBarUiState: AppBarUiState = AppBarUiState(),
    blockAllComponents: () -> Unit = {},
    enableAllComponents: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val actions = appBarUiState.actions
        if (actions.isEmpty()) return
        if (actions.contains(MORE)) {
            MoreActionMenu(
                blockAllComponents = blockAllComponents,
                enableAllComponents = enableAllComponents,
            )
        }
    }
}

@Composable
fun MoreActionMenu(
    blockAllComponents: () -> Unit,
    enableAllComponents: () -> Unit,
) {
    val items = listOf(
        DropDownMenuItem(
            string.feature_ruledetail_block_all_of_this_page,
            blockAllComponents,
        ),
        DropDownMenuItem(
            string.feature_ruledetail_enable_all_of_this_page,
            enableAllComponents,
        ),
    )
    BlockerAppTopBarMenu(
        menuIcon = BlockerIcons.MoreVert,
        menuIconDesc = com.merxury.blocker.core.ui.R.string.core_ui_more_menu,
        menuList = items,
    )
}

@Composable
private fun rememberToolbarState(toolbarHeightRange: IntRange): ToolbarState {
    return rememberSaveable(saver = ExitUntilCollapsedState.Saver) {
        ExitUntilCollapsedState(heightRange = toolbarHeightRange)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RuleDetailTabContent(
    modifier: Modifier = Modifier,
    ruleMatchedAppListUiState: RuleMatchedAppListUiState,
    ruleInfoUiState: RuleInfoUiState.Success,
    tabState: TabState<RuleDetailTabs>,
    switchTab: (RuleDetailTabs) -> Unit,
    onStopServiceClick: (String, String) -> Unit = { _, _ -> },
    onLaunchActivityClick: (String, String) -> Unit = { _, _ -> },
    onCopyNameClick: (String) -> Unit = { _ -> },
    onCopyFullNameClick: (String) -> Unit = { _ -> },
    navigateToAppDetail: (String) -> Unit = { _ -> },
    onBlockAllClick: (List<ComponentItem>) -> Unit = { _ -> },
    onEnableAllClick: (List<ComponentItem>) -> Unit = { _ -> },
    onSwitch: (String, String, Boolean) -> Unit = { _, _, _ -> },
) {
    val pagerState = rememberPagerState(initialPage = tabState.currentIndex) { tabState.items.size }
    val coroutineScope = rememberCoroutineScope()
    Column(
        modifier = modifier,
    ) {
        BlockerTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            tabState.items.forEachIndexed { index, tabItem ->
                BlockerTab(
                    selected = index == pagerState.currentPage,
                    onClick = {
                        switchTab(tabItem)
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = {
                        Text(
                            text = stringResource(
                                id = tabItem.title,
                            ),
                        )
                    },
                )
            }
        }
        HorizontalPager(
            state = pagerState,
        ) {
            when (it) {
                0 -> RuleMatchedAppList(
                    ruleMatchedAppListUiState = ruleMatchedAppListUiState,
                    onStopServiceClick = onStopServiceClick,
                    onLaunchActivityClick = onLaunchActivityClick,
                    onCopyNameClick = onCopyNameClick,
                    onCopyFullNameClick = onCopyFullNameClick,
                    navigateToAppDetail = navigateToAppDetail,
                    onBlockAllClick = onBlockAllClick,
                    onEnableAllClick = onEnableAllClick,
                    onSwitch = onSwitch,
                )

                1 -> RuleDescription(rule = ruleInfoUiState.ruleInfo)
            }
        }
    }
}

@Composable
@ThemePreviews
fun RuleDetailScreenPreview(
    @PreviewParameter(RuleListPreviewParameterProvider::class)
    ruleList: List<GeneralRule>,
) {
    val components = ComponentListPreviewParameterProvider().values.first()
    val appList = AppListPreviewParameterProvider().values.first()
    val tabState = RuleDetailTabStatePreviewParameterProvider().values.first()

    BlockerTheme {
        Surface {
            RuleDetailScreen(
                ruleInfoUiState = RuleInfoUiState.Success(
                    ruleInfo = ruleList.first(),
                    ruleIcon = null,
                    matchedAppsUiState = RuleMatchedAppListUiState.Success(
                        list = remember {
                            mutableStateListOf(
                                RuleMatchedApp(
                                    app = appList.first(),
                                    componentList = components,
                                ),
                            )
                        },
                    ),
                ),
                tabState = tabState[0],
                appBarUiState = AppBarUiState(
                    actions = listOf(
                        MORE,
                    ),
                ),
            )
        }
    }
}

@Composable
@ThemePreviews
fun RuleDetailScreenSelectedDescriptionPreview(
    @PreviewParameter(RuleListPreviewParameterProvider::class)
    ruleList: List<GeneralRule>,
) {
    val components = ComponentListPreviewParameterProvider().values.first()
    val appList = AppListPreviewParameterProvider().values.first()
    val tabState = RuleDetailTabStatePreviewParameterProvider().values.first()

    BlockerTheme {
        Surface {
            RuleDetailScreen(
                ruleInfoUiState = RuleInfoUiState.Success(
                    ruleInfo = ruleList.first(),
                    ruleIcon = null,
                    matchedAppsUiState = RuleMatchedAppListUiState.Success(
                        list = remember {
                            mutableStateListOf(
                                RuleMatchedApp(
                                    app = appList.first(),
                                    componentList = components,
                                ),
                            )
                        },
                    ),
                ),
                tabState = tabState[1],
            )
        }
    }
}

@Composable
@Preview
fun RuleDetailScreenWithApplicableLoadingPreview(
    @PreviewParameter(RuleListPreviewParameterProvider::class)
    ruleList: List<GeneralRule>,
) {
    val tabState = RuleDetailTabStatePreviewParameterProvider().values.first()

    BlockerTheme {
        Surface {
            RuleDetailScreen(
                ruleInfoUiState = RuleInfoUiState.Success(
                    ruleInfo = ruleList.first(),
                    ruleIcon = null,
                    matchedAppsUiState = RuleMatchedAppListUiState.Loading,
                ),
                tabState = tabState[0],
            )
        }
    }
}

@Composable
@Preview
fun RuleDetailScreenLoadingPreview() {
    val tabState = RuleDetailTabStatePreviewParameterProvider().values.first()
    BlockerTheme {
        Surface {
            RuleDetailScreen(
                ruleInfoUiState = RuleInfoUiState.Loading,
                tabState = tabState[0],
            )
        }
    }
}

@Composable
@Preview
fun RuleDetailScreenErrorPreview() {
    val tabState = RuleDetailTabStatePreviewParameterProvider().values.first()
    BlockerTheme {
        Surface {
            RuleDetailScreen(
                ruleInfoUiState = RuleInfoUiState.Error(
                    error = UiMessage("Error"),
                ),
                tabState = tabState[0],
            )
        }
    }
}
