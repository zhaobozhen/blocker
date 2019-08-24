package com.merxury.blocker.ui.component

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.SparseArray
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import com.elvishew.xlog.XLog
import com.merxury.blocker.R
import com.merxury.blocker.adapter.FragmentAdapter
import com.merxury.blocker.ui.Constants
import com.merxury.blocker.util.AppLauncher
import com.merxury.blocker.util.setupActionBar
import com.merxury.libkit.entity.Application
import kotlinx.android.synthetic.main.activity_component.*
import kotlinx.android.synthetic.main.application_brief_info_layout.*
import moe.shizuku.api.ShizukuClient
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

class ComponentActivity : AppCompatActivity() {

    private lateinit var application: Application
    private lateinit var adapter: FragmentAdapter
    private val logger = XLog.tag("ComponentActivity").build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_component)
        setupActionBar(R.id.component_toolbar) {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }
        getDataFromIntent()
        setupViewPager()
        setupTab()
        showApplicationBriefInfo(application)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            ShizukuClient.REQUEST_CODE_AUTHORIZATION -> {
                if (resultCode == ShizukuClient.AUTH_RESULT_OK) {
                    ShizukuClient.setToken(data)
                } else {
                    logger.d("User denied Shizuku permission")
                }
                return
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            ShizukuClient.REQUEST_CODE_PERMISSION -> if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                ShizukuClient.requestAuthorization(this)
            } else {
                logger.d("User denied Shizuku permission")
            }
        }
    }

    override fun onBackPressed() {
        findViewById<SearchView>(R.id.menu_search)?.let {
            if (!it.isIconified) {
                it.isIconified = true
                it.clearFocus()
                return
            }
        }
        super.onBackPressed()
    }

    private fun setupViewPager() {
        adapter = FragmentAdapter(supportFragmentManager)
        adapter.addFragment(ComponentFragment.newInstance(application.packageName, EComponentType.RECEIVER), getString(R.string.receiver))
        adapter.addFragment(ComponentFragment.newInstance(application.packageName, EComponentType.SERVICE), getString(R.string.service))
        adapter.addFragment(ComponentFragment.newInstance(application.packageName, EComponentType.ACTIVITY), getString(R.string.activity))
        adapter.addFragment(ComponentFragment.newInstance(application.packageName, EComponentType.PROVIDER), getString(R.string.provider))
        component_viewpager.adapter = adapter
    }

    private fun getDataFromIntent() {
        if (intent == null) {
            finish()
        }
        application = intent.getParcelableExtra(Constants.APPLICATION)
    }

    private fun setupTab() {
        component_tabs.setupWithViewPager(component_viewpager)
    }

    private fun showApplicationBriefInfo(application: Application) {
        app_info_app_name.text = getString(R.string.application_label, application.label)
        app_info_app_package_name.text = getString(R.string.package_name, application.packageName)
        app_info_target_sdk_version.text = getString(R.string.target_sdk_version, CODENAME.get(application.targetSdkVersion, UNKNOWN))
        app_info_min_sdk_version.text = getString(R.string.min_sdk_version, CODENAME.get(application.minSdkVersion, UNKNOWN))
        doAsync {
            val icon = application.getApplicationIcon(packageManager)
            uiThread {
                app_info_icon.setImageDrawable(icon)
            }
        }
        app_info_icon.setOnClickListener { AppLauncher.startApplication(this, application.packageName) }
    }

    companion object {
        private val CODENAME: SparseArray<String> = SparseArray(32)
        private const val UNKNOWN = "Unknown"

        init {
            CODENAME.put(1, "Android 1.0")
            CODENAME.put(2, "Android 1.1")
            CODENAME.put(3, "Android 1.5")
            CODENAME.put(4, "Android 1.6")
            CODENAME.put(5, "Android 2.0")
            CODENAME.put(6, "Android 2.0.1")
            CODENAME.put(7, "Android 2.1")
            CODENAME.put(8, "Android 2.2")
            CODENAME.put(9, "Android 2.3")
            CODENAME.put(10, "Android 2.3.3")
            CODENAME.put(11, "Android 3.0")
            CODENAME.put(12, "Android 3.1")
            CODENAME.put(13, "Android 3.2")
            CODENAME.put(14, "Android 4.0.1")
            CODENAME.put(15, "Android 4.0.3")
            CODENAME.put(16, "Android 4.1")
            CODENAME.put(17, "Android 4.2")
            CODENAME.put(18, "Android 4.3")
            CODENAME.put(19, "Android 4.4")
            CODENAME.put(21, "Android 5.0")
            CODENAME.put(22, "Android 5.1")
            CODENAME.put(23, "Android 6.0")
            CODENAME.put(24, "Android 7.0")
            CODENAME.put(25, "Android 7.1")
            CODENAME.put(26, "Android 8.0")
            CODENAME.put(27, "Android 8.1")
            CODENAME.put(28, "Android P")
            // Reference : https://source.android.com/setup/start/build-numbers
        }
    }
}
