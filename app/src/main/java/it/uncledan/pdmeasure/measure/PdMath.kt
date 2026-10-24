package it.uncledan.pdmeasure.measure

enum class Method { IRIS, CARD }

/** Risultato di una misura. Le distanze sono in millimetri. */
data class PdMeasurement(
    /** PD alla distanza di misura (occhi convergenti sulla fotocamera). */
    val pdNearMm: Double,
    /** Posizione del ponte nasale tra le pupille (0 = destra, 1 = sinistra). */
    val bridgeFraction: Double,
    /** Distanza stimata occhi-fotocamera, se disponibile. */
    val distanceMm: Double?,
    val method: Method,
    val samples: Int,
    /** Riferimento usato: diametro iride (metodo iride) o larghezza carta (metodo carta). */
    val referenceMm: Double,
) {
    val farFactor: Double? get() = distanceMm?.let { PdMath.farFactor(it) }
    val pdFarMm: Double? get() = farFactor?.let { pdNearMm * it }

    fun pd(far: Boolean): Double = if (far) pdFarMm ?: pdNearMm else pdNearMm
    fun monoRight(far: Boolean): Double = pd(far) * bridgeFraction
    fun monoLeft(far: Boolean): Double = pd(far) * (1.0 - bridgeFraction)
}

object PdMath {
    /** Formato ISO/IEC 7810 ID-1 (carte di credito, bancomat, tessere). */
    const val CARD_WIDTH_MM = 85.60
    /** Diametro orizzontale medio dell'iride visibile (HVID) negli adulti. */
    const val DEFAULT_IRIS_MM = 11.7
    /** Distanza approssimativa del centro di rotazione dietro l'apice corneale. */
    const val EYE_ROTATION_CENTER_MM = 13.5
    /** Intervallo plausibile per la calibrazione dell'iride. */
    val PLAUSIBLE_IRIS_MM = 10.2..13.2
    /** Intervallo di distanza in cui la correzione vicino→lontano è affidabile. */
    val VALID_DISTANCE_MM = 150.0..1200.0

    /** Modello pinhole: distanza = focale(px) × dimensione reale / dimensione in pixel. */
    fun distanceMm(focalPx: Double, realMm: Double, sizePx: Double): Double = focalPx * realMm / sizePx

    /**
     * Fattore di conversione PD da vicino → PD da lontano.
     * Fissando a distanza D, ogni pupilla ruota verso il naso attorno al centro di rotazione (r):
     * PD_lontano = PD_vicino × (D + r) / D
     */
    fun farFactor(distanceMm: Double): Double? =
        if (distanceMm in VALID_DISTANCE_MM) (distanceMm + EYE_ROTATION_CENTER_MM) / distanceMm else null

    fun irisEstimate(g: FaceGeometry, irisMm: Double, focalPx: Double?): PdMeasurement {
        val mmPerPx = irisMm / g.irisPx
        return PdMeasurement(
            pdNearMm = g.pdPx * mmPerPx,
            bridgeFraction = g.bridgeFraction,
            distanceMm = focalPx?.let { distanceMm(it, irisMm, g.irisPx) },
            method = Method.IRIS,
            samples = 1,
            referenceMm = irisMm,
        )
    }

    fun cardEstimate(g: FaceGeometry, cardWidthPx: Double, focalPx: Double?): PdMeasurement {
        val mmPerPx = CARD_WIDTH_MM / cardWidthPx
        return PdMeasurement(
            pdNearMm = g.pdPx * mmPerPx,
            bridgeFraction = g.bridgeFraction,
            distanceMm = focalPx?.let { distanceMm(it, CARD_WIDTH_MM, cardWidthPx) },
            method = Method.CARD,
            samples = 1,
            referenceMm = CARD_WIDTH_MM,
        )
    }

    /** Diametro reale dell'iride ricavato dalla carta, per calibrare il metodo iride. */
    fun calibratedIrisMm(g: FaceGeometry, cardWidthPx: Double): Double = g.irisPx * CARD_WIDTH_MM / cardWidthPx

    /** Combina più campioni con la mediana (robusta a fotogrammi anomali). */
    fun combine(list: List<PdMeasurement>): PdMeasurement {
        require(list.isNotEmpty())
        val distances = list.mapNotNull { it.distanceMm }
        return PdMeasurement(
            pdNearMm = median(list.map { it.pdNearMm }),
            bridgeFraction = median(list.map { it.bridgeFraction }),
            distanceMm = if (distances.isEmpty()) null else median(distances),
            method = list.first().method,
            samples = list.size,
            referenceMm = list.first().referenceMm,
        )
    }

    fun median(values: List<Double>): Double {
        require(values.isNotEmpty())
        val s = values.sorted()
        val n = s.size
        return if (n % 2 == 1) s[n / 2] else (s[n / 2 - 1] + s[n / 2]) / 2.0
    }
}

/** Posizione iniziale delle linee della carta e ritaglio ingrandito attorno a fronte e occhi. */
object CardGuess {
    data class Initial(val xA: Float, val xB: Float, val crop: CropRect)

    fun initial(g: FaceGeometry): Initial {
        val iw = g.imageWidth.toFloat()
        val ih = g.imageHeight.toFloat()
        val cardPx = (PdMath.CARD_WIDTH_MM / (PdMath.DEFAULT_IRIS_MM / g.irisPx)).toFloat()
        val cx = g.bridge.x
        val cropW = minOf(iw, cardPx * 1.7f)
        val cropH = minOf(ih, cropW)
        val left = (cx - cropW / 2f).coerceIn(0f, iw - cropW)
        val top = (g.bridge.y - cropH * 0.72f).coerceIn(0f, ih - cropH)
        return Initial(
            xA = (cx - cardPx / 2f).coerceIn(0f, iw),
            xB = (cx + cardPx / 2f).coerceIn(0f, iw),
            crop = CropRect(left.toInt(), top.toInt(), cropW.toInt(), cropH.toInt()),
        )
    }
}
