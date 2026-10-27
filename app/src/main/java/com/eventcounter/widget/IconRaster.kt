package com.eventcounter.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorNode
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath

internal fun ImageVector.raster(sizePx: Int = 96): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.scale(sizePx / viewportWidth, sizePx / viewportHeight)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    drawNode(root, canvas, paint)
    return bitmap
}

private fun drawNode(node: VectorNode, canvas: Canvas, paint: Paint) {
    when (node) {
        is VectorPath -> canvas.drawPath(node.pathData.toPath().asAndroidPath(), paint)
        is VectorGroup -> {
            canvas.save()
            canvas.translate(node.translationX, node.translationY)
            canvas.scale(node.scaleX, node.scaleY, node.pivotX, node.pivotY)
            node.forEach { child -> drawNode(child, canvas, paint) }
            canvas.restore()
        }
    }
}
