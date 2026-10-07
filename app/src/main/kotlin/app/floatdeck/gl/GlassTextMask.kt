package app.floatdeck.gl

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

/**
 * Builds the texture for the glass text layer.
 *
 * Packed RGBA8: R = glyph coverage, G = rounded bevel height (0 at the edge,
 * 1 inside). The shader derives a surface normal from G for refraction and
 * highlights. Alpha is always 255 so no premultiplication touches the data.
 */
object GlassTextMask {
    class Result(
        val buffer: ByteBuffer,
        val width: Int,
        val height: Int,
    )

    /**
     * Renders [text] stretched to exactly [width] x [height] pixels (Bebas is
     * already tall; the stretch lets the user set width and height freely).
     */
    fun build(
        typeface: Typeface,
        text: String,
        width: Int,
        height: Int,
        bevelPx: Int,
    ): Result? {
        if (text.isBlank() || width < 8 || height < 8) return null
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.typeface = typeface
                textSize = REFERENCE_TEXT_SIZE
                color = Color.WHITE
            }
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        if (bounds.width() <= 0 || bounds.height() <= 0) return null

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(width / bounds.width().toFloat(), height / bounds.height().toFloat())
        canvas.drawText(text, -bounds.left.toFloat(), -bounds.top.toFloat(), paint)

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        bitmap.recycle()

        val coverage = FloatArray(pixels.size) { ((pixels[it] ushr 24) and 0xFF) / 255f }
        val bevel = bevelHeights(coverage, width, height, bevelPx)

        val buffer = ByteBuffer.allocateDirect(pixels.size * 4).order(ByteOrder.nativeOrder())
        for (i in pixels.indices) {
            buffer.put((coverage[i] * 255f + 0.5f).toInt().coerceIn(0, 255).toByte())
            buffer.put((bevel[i] * 255f + 0.5f).toInt().coerceIn(0, 255).toByte())
            buffer.put(0)
            buffer.put(0xFF.toByte())
        }
        buffer.position(0)
        return Result(buffer, width, height)
    }

    /**
     * Rounded bevel profile: blur the coverage so it falls to ~0.5 at the glyph
     * edge and reaches 1 about [radius] px inside, remap that to 0..1 and round
     * it with a quarter sine. Pure function for unit tests.
     */
    fun bevelHeights(
        coverage: FloatArray,
        width: Int,
        height: Int,
        radius: Int,
    ): FloatArray {
        val r = radius.coerceAtLeast(1)
        var blurred = boxBlur(coverage, width, height, r)
        blurred = boxBlur(blurred, width, height, r)
        return FloatArray(coverage.size) { i ->
            val t = ((blurred[i] - 0.5f) * 2f).coerceIn(0f, 1f)
            sin(t * (PI / 2).toFloat()) * coverage[i]
        }
    }

    /** Separable box blur with running sums, edges treated as empty. */
    internal fun boxBlur(
        src: FloatArray,
        width: Int,
        height: Int,
        radius: Int,
    ): FloatArray {
        val tmp = FloatArray(src.size)
        val out = FloatArray(src.size)
        val window = (2 * radius + 1).toFloat()
        for (y in 0 until height) {
            val row = y * width
            var sum = 0f
            for (x in -radius..radius) if (x in 0 until width) sum += src[row + x]
            for (x in 0 until width) {
                tmp[row + x] = sum / window
                val add = x + radius + 1
                val remove = x - radius
                if (add < width) sum += src[row + add]
                if (remove >= 0) sum -= src[row + remove]
            }
        }
        for (x in 0 until width) {
            var sum = 0f
            for (y in -radius..radius) if (y in 0 until height) sum += tmp[y * width + x]
            for (y in 0 until height) {
                out[y * width + x] = sum / window
                val add = y + radius + 1
                val remove = y - radius
                if (add < height) sum += tmp[add * width + x]
                if (remove >= 0) sum -= tmp[remove * width + x]
            }
        }
        return out
    }

    private const val REFERENCE_TEXT_SIZE = 400f
}
