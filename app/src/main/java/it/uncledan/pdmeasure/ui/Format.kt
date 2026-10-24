package it.uncledan.pdmeasure.ui

import java.util.Locale
import kotlin.math.roundToInt

fun fmt1(v: Double): String = String.format(Locale.getDefault(), "%.1f", v)

/** Arrotonda al mezzo millimetro, come si usa per la PD. */
fun fmtHalf(v: Double): String = fmt1((v * 2.0).roundToInt() / 2.0)

fun fmtCm(mm: Double): String = (mm / 10.0).roundToInt().toString()
