package com.example.scanmecalculator

import android.app.Activity
import android.os.Build
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import androidx.core.view.DisplayCutoutCompat
import androidx.core.view.ViewCompat
import androidx.core.view.updatePadding

object Helper {
    fun transparentStatusBar(ctx: Activity) {
        val w: Window = ctx.window
        w.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
    }

    fun isSupport(versionCode : Int): Boolean {
        return Build.VERSION.SDK_INT >= versionCode
    }

    fun adjustSafeAreaPadding(rootView: View, targetView : View){
        Log.d(TAG, "DISPLAY " + "bakla")
//        ViewCompat.setOnApplyWindowInsetsListener(
//            rootView
//        ) { v, insets ->
//            val displayCutout: DisplayCutoutCompat? = insets.displayCutout
//            if (displayCutout != null) {
//                val safeArea = displayCutout.safeInsetTop
//                targetView.setPadding(0, safeArea, 0, 0)
//            }
//            Log.d(TAG, "DISPLAY " + displayCutout)
//            insets
//        }
        rootView.rootView.setOnApplyWindowInsetsListener { _, insets ->
            val systemBarsInsetsBottom = if (isSupport(Build.VERSION_CODES.R)){
                insets.getInsets(WindowInsets.Type.systemBars()).top
            }else{
                insets.systemWindowInsetTop
            }

            if (systemBarsInsetsBottom > 0){
                targetView.updatePadding(top = systemBarsInsetsBottom)
            }
            insets
        }
    }
}