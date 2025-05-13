package com.example.obd_iiservice

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat

class ButtonConnect : AppCompatButton {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    private var txtColor : Int = 0
    private var enabledBackground : Drawable
    private var disabledBackground : Drawable

    init {
        txtColor = ContextCompat.getColor(context, android.R.color.background_light)
        enabledBackground = ContextCompat.getDrawable(context, R.drawable.bg_button_connect) as Drawable
        disabledBackground = ContextCompat.getDrawable(context, R.drawable.bg_button_disconnect) as Drawable
    }

    override fun onDraw(canvas: Canvas) {

        super.onDraw(canvas)
        background = if (isEnabled) enabledBackground else disabledBackground
        setTextColor(txtColor)
        textSize = 12f
        gravity = Gravity.CENTER
//        text = if (isEnabled) "Connect" else "Disconnect"
    }
}