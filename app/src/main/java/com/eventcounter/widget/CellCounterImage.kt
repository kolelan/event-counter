package com.eventcounter.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.eventcounter.domain.EventStatus
import com.eventcounter.domain.cellCounterTextSizeRatio

internal fun cellCounterBitmap(
    label: String,
    status: EventStatus,
    style: DigitStyle,
    sizePx: Int = 256,
    title: String = "",
): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val hasTitle = title.isNotBlank()
    var textSize = sizePx * cellCounterTextSizeRatio(label)
    if (hasTitle) textSize *= 0.82f
    paint.textSize = textSize
    val maxWidth = sizePx * 0.86f
    while (paint.measureText(label) > maxWidth && textSize > sizePx * 0.22f) {
        textSize *= 0.88f
        paint.textSize = textSize
    }
    val centerX = sizePx / 2f
    val baseline = if (hasTitle) sizePx * 0.46f else sizePx / 2f
    val numberBaseline = baseline - (paint.descent() + paint.ascent()) / 2f
    drawContrastText(canvas, paint, label, centerX, numberBaseline, textSize, status, style)
    if (hasTitle) {
        var titleSize = sizePx * 0.15f
        paint.textSize = titleSize
        while (paint.measureText(title) > maxWidth && titleSize > sizePx * 0.09f) {
            titleSize *= 0.9f
            paint.textSize = titleSize
        }
        val titleBaseline = sizePx * 0.86f - (paint.descent() + paint.ascent()) / 2f
        drawContrastText(canvas, paint, title, centerX, titleBaseline, titleSize, status, style)
    }
    return bitmap
}

private fun drawContrastText(
    canvas: Canvas,
    paint: Paint,
    text: String,
    centerX: Float,
    baseline: Float,
    textSize: Float,
    status: EventStatus,
    style: DigitStyle,
) {
    paint.textSize = textSize
    if (style == DigitStyle.PLATE) {
        val textWidth = paint.measureText(text)
        val padX = textSize * 0.28f
        val padY = textSize * 0.16f
        val rect = RectF(
            centerX - textWidth / 2f - padX,
            baseline + paint.ascent() - padY,
            centerX + textWidth / 2f + padX,
            baseline + paint.descent() + padY,
        )
        canvas.drawRoundRect(
            rect,
            textSize * 0.28f,
            textSize * 0.28f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.argb(168, 0, 0, 0) },
        )
    } else {
        paint.style = Paint.Style.FILL
        paint.color = android.graphics.Color.argb(210, 0, 0, 0)
        paint.setShadowLayer(textSize * 0.16f, 0f, textSize * 0.05f, android.graphics.Color.argb(180, 0, 0, 0))
        canvas.drawText(text, centerX, baseline, paint)
        paint.clearShadowLayer()
    }
    paint.style = Paint.Style.FILL
    paint.color = digitColor(status)
    canvas.drawText(text, centerX, baseline, paint)
}

private fun digitColor(status: EventStatus): Int = when (status) {
    EventStatus.YELLOW -> 0xFFFFE08A.toInt()
    EventStatus.RED -> 0xFFFFB4AB.toInt()
    EventStatus.NORMAL -> 0xFFF7FFFC.toInt()
    EventStatus.NONE -> 0xFFF7FFFC.toInt()
}
