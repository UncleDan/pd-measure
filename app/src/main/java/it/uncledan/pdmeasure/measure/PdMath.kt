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
    /** PD (da lontano se disponibile) di ciascun tentativo, se la misura ne combina più d'uno. */
    val attemptsMm: List<Double> = emptyList(),
    /** Con 5 o più tentativi il più alto e il più basso sono esclusi dalla media. */
    val trimmed: Boolean = false,
) {
    /** Scarto tra tentativo massimo e minimo. */
    val spreadMm: Double? get() = if (attemptsMm.size > 1) attemptsMm.max() - attemptsMm.min() else null

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
    /** Distanza apice corneale - lente (vertice) per la centratura delle lenti. */
    const val VERTEX_DISTANCE_MM = 12.0
    const val READING_DISTANCE_MM = 400.0
    const val INTERMEDIATE_DISTANCE_MM = 650.0
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

    /**
     * PD sul piano della lente fissando a distanza D (centratura per vicino/intermedio).
     * Le linee di sguardo partono dai centri di rotazione (distanti PD_lontano) e convergono sul punto:
     * sul piano della lente (vertice v davanti alla cornea) PD = PD_lontano × (D − v) / (D + r)
     */
    fun nearPdAtLensPlane(pdFarMm: Double, distanceMm: Double): Double =
        pdFarMm * (distanceMm - VERTEX_DISTANCE_MM) / (distanceMm + EYE_ROTATION_CENTER_MM)

    /**
     * Combina più tentativi indipendenti: media aritmetica; con 5 o più tentativi
     * si escludono il valore più alto e quello più basso (media troncata).
     */
    fun combineAttempts(attempts: List<PdMeasurement>): PdMeasurement {
        require(attempts.isNotEmpty())
        val values = attempts.map { it.pd(far = true) }
        if (attempts.size == 1) return attempts[0].copy(attemptsMm = values)
        val sorted = attempts.sortedBy { it.pd(far = true) }
        val trim = sorted.size >= 5
        val used = if (trim) sorted.subList(1, sorted.size - 1) else sorted
        val distances = used.mapNotNull { it.distanceMm }
        return PdMeasurement(
            pdNearMm = used.map { it.pdNearMm }.average(),
            bridgeFraction = used.map { it.bridgeFraction }.average(),
            distanceMm = if (distances.isEmpty()) null else distances.average(),
            method = attempts.first().method,
            samples = attempts.sumOf { it.samples },
            referenceMm = attempts.first().referenceMm,
            attemptsMm = values,
            trimmed = trim,
        )
    }

    fun roundHalf(v: Double): Double = Math.round(v * 2.0) / 2.0

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

/** Una PD binoculare con le due mono PD (OD = occhio destro, OS = occhio sinistro). */
data class PdTriple(val bino: Double, val right: Double, val left: Double) {
    /** Valori per i moduli: arrotondati a 0,5 mm, con OD + OS = binoculare. */
    fun forForm(): PdTriple {
        val b = PdMath.roundHalf(bino)
        val r = PdMath.roundHalf(right)
        return PdTriple(b, r, b - r)
    }
}

/** Tutti i valori di PD ricavabili da una misura. */
data class PdSet(
    val far: PdTriple,
    val intermediate: PdTriple,
    val reading: PdTriple,
    val measured: PdTriple,
    val measuredDistanceMm: Double?,
    /** false se manca la distanza: il valore "da lontano" non è corretto per la convergenza. */
    val farCorrected: Boolean,
) {
    companion object {
        fun from(m: PdMeasurement): PdSet {
            val far = m.pdFarMm ?: m.pdNearMm
            fun triple(b: Double) = PdTriple(b, b * m.bridgeFraction, b * (1.0 - m.bridgeFraction))
            return PdSet(
                far = triple(far),
                intermediate = triple(PdMath.nearPdAtLensPlane(far, PdMath.INTERMEDIATE_DISTANCE_MM)),
                reading = triple(PdMath.nearPdAtLensPlane(far, PdMath.READING_DISTANCE_MM)),
                measured = triple(m.pdNearMm),
                measuredDistanceMm = m.distanceMm,
                farCorrected = m.pdFarMm != null,
            )
        }
    }
}
