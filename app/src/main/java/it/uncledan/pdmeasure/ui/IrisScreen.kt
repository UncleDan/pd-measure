package it.uncledan.pdmeasure.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.uncledan.pdmeasure.Prefs
import it.uncledan.pdmeasure.R
import it.uncledan.pdmeasure.camera.FrameSource
import it.uncledan.pdmeasure.measure.PdMath
import it.uncledan.pdmeasure.measure.PdMeasurement
import it.uncledan.pdmeasure.ui.theme.OkGreen
import it.uncledan.pdmeasure.ui.theme.WarnAmber

private const val IRIS_SAMPLES = 45

@Composable
fun IrisScreen(prefs: Prefs, onResult: (PdMeasurement) -> Unit, onBack: () -> Unit) {
    val source = remember { FrameSource() }
    CameraBinding(source)
    val frame by source.frames.collectAsState()
    val error by source.error.collectAsState()
    val irisMm = remember { prefs.calibratedIrisMm ?: PdMath.DEFAULT_IRIS_MM }
    var measuring by remember { mutableStateOf(false) }
    var count by remember { mutableIntStateOf(0) }

    LaunchedEffect(measuring) {
        if (!measuring) return@LaunchedEffect
        val samples = mutableListOf<PdMeasurement>()
        count = 0
        source.frames.collect { f ->
            val g = f?.geometry ?: return@collect
            if (!g.isFrontal) return@collect
            samples += PdMath.irisEstimate(g, irisMm, f.focalPx)
            count = samples.size
            if (samples.size >= IRIS_SAMPLES) {
                measuring = false
                onResult(PdMath.combine(samples))
            }
        }
    }

    Scaffold(topBar = { BackTopBar(stringResource(R.string.method_iris_title), onBack) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth().background(Color.Black)) {
                val f = frame
                if (f == null) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                } else {
                    FrameView(f.bitmap, Modifier.fillMaxSize()) { t ->
                        f.geometry?.let { drawFace(t, it, if (it.isFrontal) OkGreen else WarnAmber) }
                    }
                }
            }
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                error?.let { Text(stringResource(R.string.camera_error, it), color = MaterialTheme.colorScheme.error) }
                Text(framingHint(frame, irisMm), style = MaterialTheme.typography.titleMedium)
                if (measuring) {
                    LinearProgressIndicator(progress = { count / IRIS_SAMPLES.toFloat() }, modifier = Modifier.fillMaxWidth())
                    Text(stringResource(R.string.measuring, count, IRIS_SAMPLES))
                    OutlinedButton(onClick = { measuring = false }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.cancel))
                    }
                } else {
                    Text(stringResource(R.string.iris_instructions), style = MaterialTheme.typography.bodyMedium)
                    Button(
                        onClick = { measuring = true },
                        enabled = frame?.geometry != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.start_measure)) }
                }
            }
        }
    }
}
