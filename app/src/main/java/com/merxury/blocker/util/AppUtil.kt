package com.merxury.blocker.util

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

object AppUtil {
    fun hideKeyboard(context: Context, view: View) {
        (context.getSystemService(Activity.INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(view.windowToken, 0)
    }
}