package com.android.inputmethodservice

import android.graphics.Color
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.LayoutRes

data class KeyboardViewAttributes(
    val keyBackground: Drawable? = null,
    val verticalCorrectionPx: Int = 0,
    @LayoutRes val previewLayout: Int = 0,
    val previewOffsetPx: Int = 0,
    val previewHeightPx: Int = 80,
    val keyTextSizePx: Int = 48,
    @ColorInt val keyTextColor: Int = Color.parseColor("#FFFFFF"),
    val labelTextSizePx: Int = 14,
    @LayoutRes val popupLayout: Int = 0
)