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

    /** Numero di tentativi del metodo iride (1, 3 o 5). */
    var irisAttempts: Int
        get() = sp.getInt(KEY_ATTEMPTS, 3).takeIf { it in ATTEMPT_CHOICES } ?: 3
        set(value) = sp.edit().putInt(KEY_ATTEMPTS, value).apply()

    companion object {
        val ATTEMPT_CHOICES = listOf(1, 3, 5)
        private const val KEY_IRIS = "iris_mm"
        private const val KEY_ATTEMPTS = "iris_attempts"
    }
}
