package com.merxury.blocker.ui.home

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class AppListPageAdapter(fm: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fm, lifecycle) {
    private val fragments = mutableListOf<Fragment>()

    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        val fragment = if (position == 0) {
            ListFragment.newInstance(false)
        } else {
            ListFragment.newInstance(true)
        }
        fragments.add(fragment)
        return fragment
    }

    fun updateSearchKey(key: String) {
        fragments.forEach {
            if (it is ListFragment) {
                it.searchForApplication(key)
            }
        }
    }
}