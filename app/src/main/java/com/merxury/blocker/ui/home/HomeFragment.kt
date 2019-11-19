package com.merxury.blocker.ui.home

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import com.merxury.blocker.R
import com.merxury.blocker.util.AppUtil
import kotlinx.android.synthetic.main.home_fragment.*

class HomeFragment: Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.home_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSearchBar()
        setupViewPager()
        setupKeyboardListener(getView())
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
    }

    private fun setupSearchBar() = searchBar?.apply {
        addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                (viewpager?.adapter as? AppListPageAdapter)?.updateSearchKey(s?.toString() ?: "")
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }
        })
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

    private fun setupKeyboardListener(view: View?) {
        if (view == null) return
        if (view !is EditText) {
            view.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    AppUtil.hideKeyboard(requireContext(), v)
                }
                false
            }
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val innerView = view.getChildAt(i)
                setupKeyboardListener(innerView)
            }
        }
    }
}