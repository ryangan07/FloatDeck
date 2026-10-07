package app.floatdeck.data

import android.content.SharedPreferences

/**
 * Tunables for the depth (two-layer parallax) wallpaper mode.
 *
 * Stored in the same "settings_prefs" SharedPreferences the wallpaper service
 * already listens to, so slider changes apply live.
 */
data class DepthSettings(
    /** Tilt angle (degrees) at which the parallax reaches its maximum. */
    val maxAngleDegrees: Float = DEFAULT_MAX_ANGLE,
    /** Foreground (subject) travel, percent of screen width. */
    val foregroundPercent: Float = DEFAULT_FOREGROUND_PERCENT,
    /** Background travel, percent of screen width. */
    val backgroundPercent: Float = DEFAULT_BACKGROUND_PERCENT,
    /** Base zoom of both layers, percent (100 = fit exactly). */
    val zoomPercent: Float = DEFAULT_ZOOM_PERCENT,
    /** Flip the parallax direction. */
    val invert: Boolean = false,
) {
    fun save(prefs: SharedPreferences) {
        prefs
            .edit()
            .putFloat(KEY_MAX_ANGLE, maxAngleDegrees)
            .putFloat(KEY_FOREGROUND, foregroundPercent)
            .putFloat(KEY_BACKGROUND, backgroundPercent)
            .putFloat(KEY_ZOOM, zoomPercent)
            .putBoolean(KEY_INVERT, invert)
            .apply()
    }

    companion object {
        const val PREFS_NAME = "settings_prefs"

        const val KEY_MAX_ANGLE = "depth_max_angle"
        const val KEY_FOREGROUND = "depth_foreground_percent"
        const val KEY_BACKGROUND = "depth_background_percent"
        const val KEY_ZOOM = "depth_zoom_percent"
        const val KEY_INVERT = "depth_invert"

        val KEYS = setOf(KEY_MAX_ANGLE, KEY_FOREGROUND, KEY_BACKGROUND, KEY_ZOOM, KEY_INVERT)

        const val DEFAULT_MAX_ANGLE = 15f
        const val DEFAULT_FOREGROUND_PERCENT = 1.2f
        const val DEFAULT_BACKGROUND_PERCENT = 0.5f
        const val DEFAULT_ZOOM_PERCENT = 104f

        /** Built-in template used when nothing has been selected yet. */
        const val DEFAULT_TEMPLATE_ID = "shida"

        fun load(prefs: SharedPreferences): DepthSettings =
            DepthSettings(
                maxAngleDegrees = prefs.getFloat(KEY_MAX_ANGLE, DEFAULT_MAX_ANGLE),
                foregroundPercent = prefs.getFloat(KEY_FOREGROUND, DEFAULT_FOREGROUND_PERCENT),
                backgroundPercent = prefs.getFloat(KEY_BACKGROUND, DEFAULT_BACKGROUND_PERCENT),
                zoomPercent = prefs.getFloat(KEY_ZOOM, DEFAULT_ZOOM_PERCENT),
                invert = prefs.getBoolean(KEY_INVERT, false),
            )
    }
}
