package com.eventcounter.ui.icons

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import com.caverock.androidsvg.SVG

fun String.toIconBitmap(sizePx: Int): Bitmap? = runCatching {
    // AndroidSVG treats an 8-digit hex colour as #RRGGBBAA. #AARRGGBB would be invisible.
    val prepared = replace("currentColor", "#000000")
        .replace("width=\"1em\"", "width=\"$sizePx\"")
        .replace("height=\"1em\"", "height=\"$sizePx\"")
    val svg = SVG.getFromString(prepared)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    svg.renderToCanvas(canvas, RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat()))
    bitmap
}.getOrNull()
