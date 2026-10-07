package app.floatdeck.data

import android.content.SharedPreferences

/**
 * Static "liquid glass" word drawn between the background and the subject in
 * depth mode. Stored in the same SharedPreferences the wallpaper service watches.
 */
data class GlassTextSettings(
    val enabled: Boolean = true,
    val text: String = DEFAULT_TEXT,
    /** Top edge, percent of screen height. */
    val topPercent: Float = 30f,
    /** Text height, percent of screen height. */
    val heightPercent: Float = 17f,
    /** Text width, percent of screen width. */
    val widthPercent: Float = 84f,
    /** White mixed into the glass, percent (0 = perfectly clear). */
    val whitePercent: Float = 10f,
    /** Refraction strength (0 ~ 30, 12 matches the approved mock-up). */
    val refraction: Float = 12f,
    /** Also show on the home screen (otherwise lock screen only, like iOS). */
    val showOnHome: Boolean = false,
) {
    fun save(prefs: SharedPreferences) {
        prefs
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putString(KEY_TEXT, text)
            .putFloat(KEY_TOP, topPercent)
            .putFloat(KEY_HEIGHT, heightPercent)
            .putFloat(KEY_WIDTH, widthPercent)
            .putFloat(KEY_WHITE, whitePercent)
            .putFloat(KEY_REFRACTION, refraction)
            .putBoolean(KEY_SHOW_ON_HOME, showOnHome)
            .apply()
    }

    companion object {
        const val DEFAULT_TEXT = "SHIDA"

        const val KEY_ENABLED = "glass_enabled"
        const val KEY_TEXT = "glass_text"
        const val KEY_TOP = "glass_top_percent"
        const val KEY_HEIGHT = "glass_height_percent"
        const val KEY_WIDTH = "glass_width_percent"
        const val KEY_WHITE = "glass_white_percent"
        const val KEY_REFRACTION = "glass_refraction"
        const val KEY_SHOW_ON_HOME = "glass_show_on_home"

        val KEYS =
            setOf(
                KEY_ENABLED,
                KEY_TEXT,
                KEY_TOP,
                KEY_HEIGHT,
                KEY_WIDTH,
                KEY_WHITE,
                KEY_REFRACTION,
                KEY_SHOW_ON_HOME,
            )

        fun load(prefs: SharedPreferences): GlassTextSettings {
            val d = GlassTextSettings()
            return GlassTextSettings(
                enabled = prefs.getBoolean(KEY_ENABLED, d.enabled),
                text = prefs.getString(KEY_TEXT, d.text) ?: d.text,
                topPercent = prefs.getFloat(KEY_TOP, d.topPercent),
                heightPercent = prefs.getFloat(KEY_HEIGHT, d.heightPercent),
                widthPercent = prefs.getFloat(KEY_WIDTH, d.widthPercent),
                whitePercent = prefs.getFloat(KEY_WHITE, d.whitePercent),
                refraction = prefs.getFloat(KEY_REFRACTION, d.refraction),
                showOnHome = prefs.getBoolean(KEY_SHOW_ON_HOME, d.showOnHome),
            )
        }
    }
}
