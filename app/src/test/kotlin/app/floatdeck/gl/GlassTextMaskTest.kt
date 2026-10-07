package app.floatdeck.gl

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GlassTextMaskTest {
    @Test
    fun `box blur preserves a constant field away from the edges`() {
        val w = 20
        val h = 20
        val src = FloatArray(w * h) { 1f }
        val out = GlassTextMask.boxBlur(src, w, h, 2)
        assertEquals(1f, out[10 * w + 10], 1e-5f)
        // Corner sees empty space outside the image
        assertTrue(out[0] < 1f)
    }

    @Test
    fun `bevel is zero outside, rises from the edge and is flat deep inside`() {
        val w = 60
        val h = 60
        // Solid square from 10..49
        val cov = FloatArray(w * h) { i -> if (i % w in 10..49 && i / w in 10..49) 1f else 0f }
        val bevel = GlassTextMask.bevelHeights(cov, w, h, 4)
        val row = 30 * w
        assertEquals(0f, bevel[row + 5], 1e-5f)
        assertTrue(bevel[row + 10] < 0.2f, "edge should be low: ${bevel[row + 10]}")
        assertTrue(bevel[row + 12] > bevel[row + 10])
        assertEquals(1f, bevel[row + 30], 1e-4f)
    }
}
