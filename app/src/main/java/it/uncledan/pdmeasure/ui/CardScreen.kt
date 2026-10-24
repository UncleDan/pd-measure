package it.uncledan.pdmeasure.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import it.uncledan.pdmeasure.Prefs
import it.uncledan.pdmeasure.R
import it.uncledan.pdmeasure.camera.FrameSource
import it.uncledan.pdmeasure.camera.LiveFrame
import it.uncledan.pdmeasure.measure.CardGuess
import it.uncledan.pdmeasure.measure.FaceGeometry
import it.uncledan.pdmeasure.measure.PdMath
import it.uncledan.pdmeasure.measure.PdMeasurement
import it.uncledan.pdmeasure.ui.theme.OkGreen
import it.uncledan.pdmeasure.ui.theme.WarnAmber
import kotlin.math.abs

@Composable
fun CardScreen(prefs: Prefs, onResult: (PdMeasurement) -> Unit, onBack: () -> Unit) {
    var captured by remember { mutableStateOf<LiveFrame?>(null) }
    val frame = captured
    val geometry = frame?.geometry
    if (frame == null || geometry == null) {
        CardCapture(prefs, onCaptured = { captured = it }, onBack = onBack)
    } else {
        CardAdjust(frame, geometry, prefs, onRetake = { captured = null }, onResult = onResult, onBack = onBack)
    }
}

@Composable
private fun CardCapture(prefs: Prefs, onCaptured: (LiveFrame) -> Unit, onBack: () -> Unit) {
    val source = remember { FrameSource() }
    CameraBinding(source)
    val frame by source.frames.collectAsState()
    val error by source.error.collectAsState()
    val irisMm = remember { prefs.calibratedIrisMm ?: PdMath.DEFAULT_IRIS_MM }

    Scaffold(topBar = { BackTopBar(stringResource(R.string.method_card_title), onBack) }) { padding ->
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
                Text(stringResource(R.string.card_instructions), style = MaterialTheme.typography.bodyMedium)
                Button(
                    onClick = { frame?.let(onCaptured) },
                    enabled = frame?.geometry?.isFrontal == true,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.capture)) }
            }
        }
    }
}

@Composable
private fun CardAdjust(
    frame: LiveFrame,
    g: FaceGeometry,
    prefs: Prefs,
    onRetake: () -> Unit,
    onResult: (PdMeasurement) -> Unit,
    onBack: () -> Unit,
) {
    val initial = remember(frame) { CardGuess.initial(g) }
    var xA by remember(frame) { mutableFloatStateOf(initial.xA) }
    var xB by remember(frame) { mutableFloatStateOf(initial.xB) }
    var selected by remember(frame) { mutableIntStateOf(0) }
    var useForCalibration by remember { mutableStateOf(true) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val mirror = true
    val transform = if (canvasSize.width > 0 && canvasSize.height > 0) {
        ImageTransform.fit(canvasSize.width.toFloat(), canvasSize.height.toFloat(), initial.crop, mirror)
    } else null
    val transformState = rememberUpdatedState(transform)
    val maxX = g.imageWidth.toFloat()

    fun move(line: Int, dImage: Float) {
        if (line == 0) xA = (xA + dImage).coerceIn(0f, maxX) else xB = (xB + dImage).coerceIn(0f, maxX)
    }

    fun nearest(screenX: Float): Int {
        val t = transformState.value ?: return selected
        return if (abs(t.x(xA) - screenX) <= abs(t.x(xB) - screenX)) 0 else 1
    }

    val cardPx = abs(xB - xA).toDouble()
    val valid = cardPx > g.pdPx * 0.6
    val estimate = if (valid) PdMath.cardEstimate(g, cardPx, frame.focalPx) else null
    val irisCal = if (valid) PdMath.calibratedIrisMm(g, cardPx) else null
    val calPlausible = irisCal != null && irisCal in PdMath.PLAUSIBLE_IRIS_MM

    Scaffold(topBar = { BackTopBar(stringResource(R.string.method_card_title), onBack) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black)
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(frame) {
                        detectTapGestures { pos -> selected = nearest(pos.x) }
                    }
                    .pointerInput(frame) {
                        detectDragGestures(
                            onDragStart = { pos -> selected = nearest(pos.x) },
                            onDrag = { change, amount ->
                                change.consume()
                                transformState.value?.let { t -> move(selected, t.imageDx(amount.x)) }
                            },
                        )
                    }
            ) {
                FrameView(frame.bitmap, Modifier.fillMaxSize(), crop = initial.crop, mirror = mirror) { t ->
                    drawFace(t, g, OkGreen)
                    val top = t.offY
                    val bottom = t.offY + t.drawHeight
                    listOf(xA, xB).forEachIndexed { i, x ->
                        val sx = t.x(x)
                        val active = i == selected
                        drawLine(
                            color = if (active) Color(0xFFFF6D00) else Color.Cyan,
                            start = Offset(sx, top),
                            end = Offset(sx, bottom),
                            strokeWidth = if (active) 3.dp.toPx() else 2.dp.toPx(),
                        )
                    }
                }
            }
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.card_adjust_instructions), style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val leftDesc = stringResource(R.string.nudge_left)
                    val rightDesc = stringResource(R.string.nudge_right)
                    // Mezzo pixel immagine per tocco, nella direzione vista sullo schermo
                    FilledTonalButton(
                        onClick = { move(selected, transformState.value?.imageDx(-1f)?.let { 0.5f * kotlin.math.sign(it) } ?: 0f) },
                        modifier = Modifier.semantics { contentDescription = leftDesc },
                    ) { Text("◀") }
                    Text(
                        stringResource(if (selected == 0) R.string.line_first else R.string.line_second),
                        Modifier.weight(1f),
                    )
                    FilledTonalButton(
                        onClick = { move(selected, transformState.value?.imageDx(1f)?.let { 0.5f * kotlin.math.sign(it) } ?: 0f) },
                        modifier = Modifier.semantics { contentDescription = rightDesc },
                    ) { Text("▶") }
                }
                Text(
                    if (estimate != null) stringResource(R.string.card_preview, fmt1(estimate.pdNearMm)) else "—",
                    style = MaterialTheme.typography.titleLarge,
                )
                if (irisCal != null) {
                    if (calPlausible) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = useForCalibration, onCheckedChange = { useForCalibration = it })
                            Text(stringResource(R.string.calibrate_iris, fmt1(irisCal)))
                        }
                    } else {
                        Text(
                            stringResource(R.string.calibration_out_of_range, fmt1(irisCal)),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onRetake, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.retake))
                    }
                    Button(
                        onClick = {
                            estimate?.let { e ->
                                if (calPlausible && useForCalibration) prefs.calibratedIrisMm = irisCal
                                onResult(e)
                            }
                        },
                        enabled = estimate != null,
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.confirm)) }
                }
            }
        }
    }
}
