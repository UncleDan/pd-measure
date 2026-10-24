package it.uncledan.pdmeasure.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.uncledan.pdmeasure.R
import it.uncledan.pdmeasure.camera.LiveFrame
import it.uncledan.pdmeasure.measure.PdMath

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
        },
    )
}

/** Testo di guida in base a presenza volto, posizione della testa e distanza stimata. */
@Composable
fun framingHint(frame: LiveFrame?, irisMm: Double): String {
    if (frame == null) return stringResource(R.string.hint_no_face)
    val g = frame.geometry ?: return stringResource(R.string.hint_no_face)
    if (!g.isFrontal) return stringResource(R.string.hint_not_frontal)
    val distance = frame.focalPx?.let { PdMath.distanceMm(it, irisMm, g.irisPx) }
        ?: return stringResource(R.string.hint_ok)
    return when {
        distance > 550.0 -> stringResource(R.string.hint_too_far, fmtCm(distance))
        distance < 250.0 -> stringResource(R.string.hint_too_close, fmtCm(distance))
        else -> stringResource(R.string.hint_ok_distance, fmtCm(distance))
    }
}
