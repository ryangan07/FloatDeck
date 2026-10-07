package app.floatdeck.sensor

import kotlin.math.exp

/**
 * Relative tilt tracker for the depth (parallax) wallpaper.
 *
 * Integrates gyroscope angular velocity into two tilt angles and leaks them
 * back toward zero over [recenterSeconds]. The neutral position is therefore
 * whatever angle the phone is being held at, like iOS motion effects, rather
 * than "lying flat". Angles saturate at [maxAngleRad], so tilting further does
 * not build up an offset that would then need to be unwound.
 *
 * Pure math, no Android dependencies, so it can be unit tested.
 */
class TiltTracker(
    maxAngleDegrees: Float = 15f,
    var recenterSeconds: Float = 2.5f,
) {
    @Volatile
    var maxAngleRad: Float = Math.toRadians(maxAngleDegrees.toDouble()).toFloat()

    /** Rotation about the device X axis (top edge toward/away from the user), radians. */
    var angleX = 0f
        private set

    /** Rotation about the device Y axis (left/right turn), radians. */
    var angleY = 0f
        private set

    fun setMaxAngleDegrees(degrees: Float) {
        maxAngleRad = Math.toRadians(degrees.coerceIn(MIN_ANGLE_DEG, MAX_ANGLE_DEG).toDouble()).toFloat()
    }

    /**
     * Feeds one gyroscope sample.
     *
     * @param wx angular velocity about X (rad/s)
     * @param wy angular velocity about Y (rad/s)
     * @param dt seconds since the previous sample; ignored when out of range
     */
    fun onGyro(
        wx: Float,
        wy: Float,
        dt: Float,
    ) {
        if (dt <= 0f || dt > MAX_DT_SECONDS) return
        val limit = maxAngleRad
        val decay = if (recenterSeconds > 0f) exp(-dt / recenterSeconds) else 1f
        angleX = ((angleX + wx * dt) * decay).coerceIn(-limit, limit)
        angleY = ((angleY + wy * dt) * decay).coerceIn(-limit, limit)
    }

    /** Horizontal tilt in [-1, 1] (driven by rotation about Y). */
    val normalizedHorizontal: Float
        get() = if (maxAngleRad > 0f) (angleY / maxAngleRad).coerceIn(-1f, 1f) else 0f

    /** Vertical tilt in [-1, 1] (driven by rotation about X). */
    val normalizedVertical: Float
        get() = if (maxAngleRad > 0f) (angleX / maxAngleRad).coerceIn(-1f, 1f) else 0f

    fun reset() {
        angleX = 0f
        angleY = 0f
    }

    companion object {
        const val MIN_ANGLE_DEG = 3f
        const val MAX_ANGLE_DEG = 45f

        /** Larger gaps (sensor paused, screen off) are dropped rather than integrated. */
        private const val MAX_DT_SECONDS = 0.25f
    }
}
