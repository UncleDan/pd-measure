package it.uncledan.pdmeasure.measure

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/** Punto in pixel dell'immagine NON specchiata della fotocamera. */
data class Pt(val x: Float, val y: Float)

fun dist(a: Pt, b: Pt): Double = hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble())

/** Ritaglio rettangolare dell'immagine, in pixel interi. */
data class CropRect(val left: Int, val top: Int, val width: Int, val height: Int) {
    companion object {
        fun full(width: Int, height: Int) = CropRect(0, 0, width, height)
    }
}

/**
 * Geometria del volto in un fotogramma, in pixel.
 * "Destro"/"sinistro" sono riferiti al SOGGETTO (come per l'ottico).
 */
data class FaceGeometry(
    val rightPupil: Pt,
    val leftPupil: Pt,
    val bridge: Pt,
    val rightIrisPx: Double,
    val leftIrisPx: Double,
    val rollDeg: Double,
    val yawBalance: Double,
    val imageWidth: Int,
    val imageHeight: Int,
) {
    val pdPx: Double get() = dist(rightPupil, leftPupil)
    val irisPx: Double get() = (rightIrisPx + leftIrisPx) / 2.0

    /** Posizione del ponte nasale proiettata sulla linea pupillare: 0 = pupilla destra, 1 = sinistra. */
    val bridgeFraction: Double
        get() {
            val dx = (leftPupil.x - rightPupil.x).toDouble()
            val dy = (leftPupil.y - rightPupil.y).toDouble()
            val len2 = dx * dx + dy * dy
            if (len2 <= 0.0) return 0.5
            val t = ((bridge.x - rightPupil.x) * dx + (bridge.y - rightPupil.y) * dy) / len2
            return t.coerceIn(0.3, 0.7)
        }

    /** Testa dritta (rollio) e rivolta alla fotocamera (imbardata). */
    val isFrontal: Boolean
        get() = abs(rollDeg) <= MAX_ROLL_DEG && yawBalance in MIN_YAW..MAX_YAW

    companion object {
        const val LANDMARK_COUNT = 478
        const val MAX_ROLL_DEG = 5.0
        const val MIN_YAW = 0.85
        const val MAX_YAW = 1.0 / 0.85

        // Indici MediaPipe Face Mesh (478 punti, con iridi)
        private const val IRIS_A_CENTER = 468 // seguito da 469..472 sul bordo
        private const val IRIS_B_CENTER = 473 // seguito da 474..477 sul bordo
        private const val NOSE_BRIDGE = 168
        private const val NOSE_TIP = 1
        private const val FACE_EDGE_A = 234
        private const val FACE_EDGE_B = 454

        fun fromLandmarks(p: List<Pt>, width: Int, height: Int): FaceGeometry? {
            if (p.size < LANDMARK_COUNT) return null
            val a = p[IRIS_A_CENTER]
            val b = p[IRIS_B_CENTER]
            val diaA = irisDiameter(p, IRIS_A_CENTER + 1)
            val diaB = irisDiameter(p, IRIS_B_CENTER + 1)
            if (diaA <= 0.0 || diaB <= 0.0) return null

            // Nell'immagine non specchiata della frontale l'occhio DESTRO del soggetto ha x minore.
            val aIsRight = a.x < b.x
            val rp = if (aIsRight) a else b
            val lp = if (aIsRight) b else a
            val rd = if (aIsRight) diaA else diaB
            val ld = if (aIsRight) diaB else diaA

            val roll = Math.toDegrees(atan2((lp.y - rp.y).toDouble(), (lp.x - rp.x).toDouble()))

            val nose = p[NOSE_TIP]
            val e1 = p[FACE_EDGE_A].x
            val e2 = p[FACE_EDGE_B].x
            val spanLow = nose.x - min(e1, e2)
            val spanHigh = max(e1, e2) - nose.x
            if (spanLow <= 0f || spanHigh <= 0f) return null

            return FaceGeometry(
                rightPupil = rp,
                leftPupil = lp,
                bridge = p[NOSE_BRIDGE],
                rightIrisPx = rd,
                leftIrisPx = ld,
                rollDeg = roll,
                yawBalance = (spanLow / spanHigh).toDouble(),
                imageWidth = width,
                imageHeight = height,
            )
        }

        /** I 4 punti del bordo iride sono in sequenza: 1-3 e 2-4 sono diametri; si usa quello più orizzontale. */
        private fun irisDiameter(p: List<Pt>, first: Int): Double {
            val h13 = abs(p[first].x - p[first + 2].x)
            val h24 = abs(p[first + 1].x - p[first + 3].x)
            return if (h13 >= h24) dist(p[first], p[first + 2]) else dist(p[first + 1], p[first + 3])
        }
    }
}
