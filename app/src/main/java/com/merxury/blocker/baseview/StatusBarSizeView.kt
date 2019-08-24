package com.merxury.blocker.baseview

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.View

class StatusBarSizeView : View {

    constructor(context: Context) : super(context) {
        this.init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        this.init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        this.init()
    }

    private fun init() {
        if (heightSize != 0) {
            return
        }
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            (context as? Activity)?.window?.decorView?.setOnApplyWindowInsetsListener { _, windowInsets ->
                heightSize = windowInsets.systemWindowInsetTop
                windowInsets
            }
        } else {
            // Placeholder for kitkat devices
        }

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (h != 0 || heightSize == 0) {
            return
        }
        postDelayed({
            applyHeight(heightSize)
        }, 0)
    }

    private fun applyHeight(height: Int) {
        val lp = this.layoutParams
        lp.height = height
        this.layoutParams = lp
    }

    companion object {
        var heightSize: Int = 0
    }
}