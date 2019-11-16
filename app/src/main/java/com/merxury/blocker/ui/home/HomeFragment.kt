package com.merxury.blocker.ui.home

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import com.merxury.blocker.R
import kotlinx.android.synthetic.main.home_fragment.*

class HomeFragment: Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.home_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewPager()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
    }

    private fun setupViewPager() {
        viewpager?.apply {
            adapter = AppListPageAdapter(requireFragmentManager(), lifecycle)
            offscreenPageLimit = 1
            TabLayoutMediator(tabs, viewpager, TabLayoutMediator.TabConfigurationStrategy { tab, position ->
                if (position == 0) {
                    tab.setText(R.string.third_party_app_tab_text)
                } else {
                    tab.setText(R.string.system_app_tab_text)
                }
            }).attach()
        }
    }
}