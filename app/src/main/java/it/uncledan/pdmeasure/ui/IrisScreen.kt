package it.uncledan.pdmeasure.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableStateListOf
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
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.take

private const val IRIS_SAMPLES = 45

private enum class Phase { IDLE, MEASURING, BETWEEN }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IrisScreen(prefs: Prefs, onResult: (PdMeasurement) -> Unit, onBack: () -> Unit) {
    val source = remember { FrameSource() }
    CameraBinding(source)
    val frame by source.frames.collectAsState()
    val error by source.error.collectAsState()
    val irisMm = remember { prefs.calibratedIrisMm ?: PdMath.DEFAULT_IRIS_MM }

    var target by remember { mutableIntStateOf(prefs.irisAttempts) }
    var phase by remember { mutableStateOf(Phase.IDLE) }
    var count by remember { mutableIntStateOf(0) }
    val done = remember { mutableStateListOf<PdMeasurement>() }

    // Un tentativo = mediana di IRIS_SAMPLES fotogrammi frontali
    LaunchedEffect(phase) {
        if (phase != Phase.MEASURING) return@LaunchedEffect
        val samples = mutableListOf<PdMeasurement>()
        count = 0
        source.frames
            .mapNotNull { f ->
                val g = f?.geometry?.takeIf { it.isFrontal } ?: return@mapNotNull null
                PdMath.irisEstimate(g, irisMm, f.focalPx)
            }
            .take(IRIS_SAMPLES)
            .collect { samples += it; count = samples.size }
        done += PdMath.combine(samples)
        if (done.size >= target) {
            val result = PdMath.combineAttempts(done.toList())
            phase = Phase.IDLE
            onResult(result)
        } else {
            phase = Phase.BETWEEN
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

                if (done.isNotEmpty()) {
                    Text(
                        done.mapIndexed { i, m -> stringResource(R.string.attempt_value, i + 1, fmt1(m.pd(far = true))) }
                            .joinToString("  ·  "),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                when (phase) {
                    Phase.IDLE -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(stringResource(R.string.attempts_label))
                            Prefs.ATTEMPT_CHOICES.forEach { n ->
                                FilterChip(
                                    selected = target == n,
                                    onClick = { target = n; prefs.irisAttempts = n },
                                    label = { Text(n.toString()) },
                                )
                            }
                        }
                        Text(stringResource(R.string.attempts_hint), style = MaterialTheme.typography.bodySmall)
                        Text(stringResource(R.string.iris_instructions), style = MaterialTheme.typography.bodyMedium)
                        Button(
                            onClick = { done.clear(); phase = Phase.MEASURING },
                            enabled = frame?.geometry != null,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.start_measure)) }
                    }
                    Phase.MEASURING -> {
                        LinearProgressIndicator(
                            progress = { count / IRIS_SAMPLES.toFloat() },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(stringResource(R.string.measuring, done.size + 1, target, count, IRIS_SAMPLES))
                        OutlinedButton(
                            onClick = { done.clear(); phase = Phase.IDLE },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.cancel)) }
                    }
                    Phase.BETWEEN -> {
                        Text(stringResource(R.string.between_instructions, done.size + 1, target))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { done.clear(); phase = Phase.IDLE },
                                modifier = Modifier.weight(1f),
                            ) { Text(stringResource(R.string.cancel)) }
                            Button(
                                onClick = { phase = Phase.MEASURING },
                                enabled = frame?.geometry != null,
                                modifier = Modifier.weight(1f),
                            ) { Text(stringResource(R.string.continue_label)) }
                        }
                    }
                }
            }
        }
    }
}
