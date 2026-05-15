package com.ruthvik.musicplayer

import android.graphics.Color
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

fun ComponentActivity.setupDarkEdgeToEdge() {
    val backgroundDark = ContextCompat.getColor(this, R.color.background_dark)
    enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        navigationBarStyle = SystemBarStyle.dark(backgroundDark)
    )
}

fun View.applySystemBarInsets() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updatePadding(
            left = bars.left,
            top = bars.top,
            right = bars.right,
            bottom = bars.bottom
        )
        insets
    }
}
