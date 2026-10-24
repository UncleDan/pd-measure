package it.uncledan.pdmeasure.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import it.uncledan.pdmeasure.measure.CropRect
import it.uncledan.pdmeasure.measure.FaceGeometry
import it.uncledan.pdmeasure.measure.Pt
import kotlin.math.min

/** Trasformazione coordinate immagine (non specchiata) → schermo, con ritaglio e specchio. */
class ImageTransform(
    val crop: CropRect,
    val scale: Float,
    val offX: Float,
    val offY: Float,
    val mirror: Boolean,
) {
    val drawWidth: Float get() = crop.width * scale
    val drawHeight: Float get() = crop.height * scale

    fun x(px: Float): Float =
        offX + (if (mirror) crop.left + crop.width - px else px - crop.left) * scale

    fun y(py: Float): Float = offY + (py - crop.top) * scale
    fun toScreen(p: Pt) = Offset(x(p.x), y(p.y))

    /** Spostamento orizzontale sullo schermo → spostamento in pixel immagine. */
    fun imageDx(screenDx: Float): Float = screenDx / scale * (if (mirror) -1f else 1f)

    companion object {
        fun fit(width: Float, height: Float, crop: CropRect, mirror: Boolean): ImageTransform {
            val s = min(width / crop.width, height / crop.height)
            return ImageTransform(crop, s, (width - crop.width * s) / 2f, (height - crop.height * s) / 2f, mirror)
        }
    }
}

/** Disegna un fotogramma (specchiato, come uno specchio) con un overlay in coordinate immagine. */
@Composable
fun FrameView(
    bitmap: Bitmap,
    modifier: Modifier = Modifier,
    crop: CropRect = CropRect.full(bitmap.width, bitmap.height),
    mirror: Boolean = true,
    overlay: DrawScope.(ImageTransform) -> Unit = {},
) {
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    Canvas(modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        val t = ImageTransform.fit(size.width, size.height, crop, mirror)
        val pivot = Offset(t.offX + t.drawWidth / 2f, 0f)
        scale(scaleX = if (mirror) -1f else 1f, scaleY = 1f, pivot = pivot) {
            drawImage(
                image = image,
                srcOffset = IntOffset(crop.left, crop.top),
                srcSize = IntSize(crop.width, crop.height),
                dstOffset = IntOffset(t.offX.toInt(), t.offY.toInt()),
                dstSize = IntSize(t.drawWidth.toInt(), t.drawHeight.toInt()),
            )
        }
        overlay(t)
    }
}

/** Pupille, iridi, linea pupillare e ponte nasale. */
fun DrawScope.drawFace(t: ImageTransform, g: FaceGeometry, color: Color) {
    val r = t.toScreen(g.rightPupil)
    val l = t.toScreen(g.leftPupil)
    val stroke = 2.dp.toPx()
    drawLine(color, r, l, strokeWidth = stroke)
    drawCircle(color, radius = (g.rightIrisPx / 2.0 * t.scale).toFloat(), center = r, style = Stroke(stroke))
    drawCircle(color, radius = (g.leftIrisPx / 2.0 * t.scale).toFloat(), center = l, style = Stroke(stroke))
    drawCircle(color, radius = 2.5.dp.toPx(), center = r)
    drawCircle(color, radius = 2.5.dp.toPx(), center = l)
    drawCircle(Color.White, radius = 3.dp.toPx(), center = t.toScreen(g.bridge))
}
