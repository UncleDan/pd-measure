package it.uncledan.pdmeasure.measure

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PdMathTest {

    /** Volto sintetico: pupille a x=200 e x=400 (y=500), iridi di 40 px, ponte al centro. */
    private fun syntheticLandmarks(): List<Pt> {
        val p = MutableList(478) { Pt(300f, 600f) }
        fun iris(center: Int, cx: Float, cy: Float, r: Float) {
            p[center] = Pt(cx, cy)
            p[center + 1] = Pt(cx + r, cy)
            p[center + 2] = Pt(cx, cy - r)
            p[center + 3] = Pt(cx - r, cy)
            p[center + 4] = Pt(cx, cy + r)
        }
        iris(468, 400f, 500f, 20f)
        iris(473, 200f, 500f, 20f)
        p[168] = Pt(300f, 490f)
        p[1] = Pt(300f, 560f)
        p[234] = Pt(150f, 520f)
        p[454] = Pt(450f, 520f)
        return p
    }

    @Test
    fun geometryFromLandmarks() {
        val g = FaceGeometry.fromLandmarks(syntheticLandmarks(), 640, 960)
        assertNotNull(g); g!!
        assertEquals(200f, g.rightPupil.x) // x minore = occhio destro del soggetto
        assertEquals(200.0, g.pdPx, 1e-6)
        assertEquals(40.0, g.irisPx, 1e-6)
        assertEquals(0.0, g.rollDeg, 1e-6)
        assertEquals(1.0, g.yawBalance, 1e-6)
        assertEquals(0.5, g.bridgeFraction, 1e-6)
        assertTrue(g.isFrontal)
    }

    @Test
    fun irisMethod() {
        val g = FaceGeometry.fromLandmarks(syntheticLandmarks(), 640, 960)!!
        val m = PdMath.irisEstimate(g, 11.7, focalPx = 1000.0)
        assertEquals(58.5, m.pdNearMm, 1e-6)
        assertEquals(292.5, m.distanceMm!!, 1e-6)
        assertEquals(58.5 * 306.0 / 292.5, m.pdFarMm!!, 1e-6)
        assertEquals(m.pdNearMm / 2, m.monoRight(false), 1e-6)
    }

    @Test
    fun cardMethodAndCalibration() {
        val g = FaceGeometry.fromLandmarks(syntheticLandmarks(), 640, 960)!!
        val m = PdMath.cardEstimate(g, cardWidthPx = 285.0, focalPx = null)
        assertEquals(200.0 * 85.6 / 285.0, m.pdNearMm, 1e-6)
        assertNull(m.pdFarMm)
        assertEquals(40.0 * 85.6 / 285.0, PdMath.calibratedIrisMm(g, 285.0), 1e-6)
    }

    @Test
    fun medianAndFarFactor() {
        assertEquals(2.0, PdMath.median(listOf(3.0, 1.0, 2.0)), 0.0)
        assertEquals(2.5, PdMath.median(listOf(4.0, 1.0, 2.0, 3.0)), 0.0)
        assertNull(PdMath.farFactor(50.0))
        assertEquals(413.5 / 400.0, PdMath.farFactor(400.0)!!, 1e-9)
    }

    private fun m(pdNear: Double, dist: Double?) =
        PdMeasurement(pdNear, 0.5, dist, Method.IRIS, 45, 11.7)

    @Test
    fun attemptsMeanAndTrim() {
        val three = PdMath.combineAttempts(listOf(m(60.0, null), m(61.0, null), m(62.0, null)))
        assertEquals(61.0, three.pdNearMm, 1e-9)
        assertEquals(2.0, three.spreadMm!!, 1e-9)
        assertEquals(135, three.samples)

        val five = PdMath.combineAttempts(listOf(m(50.0, null), m(60.0, null), m(61.0, null), m(62.0, null), m(70.0, null)))
        assertTrue(five.trimmed)
        assertEquals(61.0, five.pdNearMm, 1e-9) // 50 e 70 esclusi
        assertEquals(20.0, five.spreadMm!!, 1e-9)

        val one = PdMath.combineAttempts(listOf(m(60.0, null)))
        assertNull(one.spreadMm)
    }

    @Test
    fun allPdValues() {
        val set = PdSet.from(m(60.0, 400.0))
        val far = 60.0 * 413.5 / 400.0
        assertEquals(far, set.far.bino, 1e-9)
        assertEquals(far * 388.0 / 413.5, set.reading.bino, 1e-9)
        assertEquals(far * 638.0 / 663.5, set.intermediate.bino, 1e-9)
        assertTrue(set.reading.bino < set.intermediate.bino && set.intermediate.bino < set.far.bino)
        val form = PdTriple(70.4, 35.3, 35.1).forForm()
        assertEquals(70.5, form.bino, 0.0)
        assertEquals(35.5, form.right, 0.0)
        assertEquals(35.0, form.left, 0.0)
    }
}
