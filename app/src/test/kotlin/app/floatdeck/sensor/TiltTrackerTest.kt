package app.floatdeck.sensor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

class TiltTrackerTest {
    private val deg = (Math.PI / 180.0).toFloat()

    @Test
    fun `starts centered`() {
        val t = TiltTracker()
        assertEquals(0f, t.normalizedHorizontal)
        assertEquals(0f, t.normalizedVertical)
    }

    @Test
    fun `integrates angular velocity`() {
        val t = TiltTracker(maxAngleDegrees = 15f, recenterSeconds = 0f)
        // 30 deg/s around Y for 0.25s total = 7.5 deg = half of max
        repeat(25) { t.onGyro(0f, 30f * deg, 0.01f) }
        assertEquals(0.5f, t.normalizedHorizontal, 0.01f)
        assertEquals(0f, t.normalizedVertical)
    }

    @Test
    fun `saturates at max angle and unwinds immediately`() {
        val t = TiltTracker(maxAngleDegrees = 10f, recenterSeconds = 0f)
        repeat(100) { t.onGyro(90f * deg, 0f, 0.01f) }
        assertEquals(1f, t.normalizedVertical, 1e-4f)
        // Turning back 5 degrees moves off the limit right away
        repeat(10) { t.onGyro(-50f * deg, 0f, 0.01f) }
        assertEquals(0.5f, t.normalizedVertical, 0.01f)
    }

    @Test
    fun `drifts back to center when held still`() {
        val t = TiltTracker(maxAngleDegrees = 15f, recenterSeconds = 1f)
        repeat(20) { t.onGyro(0f, 30f * deg, 0.01f) }
        val before = abs(t.normalizedHorizontal)
        repeat(500) { t.onGyro(0f, 0f, 0.01f) }
        assertTrue(abs(t.normalizedHorizontal) < before * 0.01f)
    }

    @Test
    fun `ignores bogus time steps`() {
        val t = TiltTracker(recenterSeconds = 0f)
        t.onGyro(1f, 1f, 0f)
        t.onGyro(1f, 1f, -0.1f)
        t.onGyro(1f, 1f, 5f)
        assertEquals(0f, t.normalizedHorizontal)
        assertEquals(0f, t.normalizedVertical)
    }

    @Test
    fun `max angle setter clamps`() {
        val t = TiltTracker()
        t.setMaxAngleDegrees(1000f)
        assertEquals(TiltTracker.MAX_ANGLE_DEG * deg, t.maxAngleRad, 1e-4f)
        t.setMaxAngleDegrees(0f)
        assertEquals(TiltTracker.MIN_ANGLE_DEG * deg, t.maxAngleRad, 1e-4f)
    }
}
