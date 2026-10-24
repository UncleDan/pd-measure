package it.uncledan.pdmeasure

import android.content.Context

/** Impostazioni locali (nessun dato lascia il dispositivo). */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("pd_measure", Context.MODE_PRIVATE)

    /** Diametro dell'iride calibrato con la carta, o null se non calibrato. */
    var calibratedIrisMm: Double?
        get() = sp.getFloat(KEY_IRIS, 0f).takeIf { it > 0f }?.toDouble()
        set(value) {
            sp.edit().apply {
                if (value == null) remove(KEY_IRIS) else putFloat(KEY_IRIS, value.toFloat())
            }.apply()
        }

    /** Mostra come valore principale la PD da lontano. */
    var farCorrection: Boolean
        get() = sp.getBoolean(KEY_FAR, true)
        set(value) = sp.edit().putBoolean(KEY_FAR, value).apply()

    private companion object {
        const val KEY_IRIS = "iris_mm"
        const val KEY_FAR = "far_correction"
    }
}
