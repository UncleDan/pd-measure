package it.uncledan.pdmeasure.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.uncledan.pdmeasure.Prefs
import it.uncledan.pdmeasure.R
import it.uncledan.pdmeasure.measure.Method
import it.uncledan.pdmeasure.measure.PdMeasurement

@Composable
fun ResultScreen(measurement: PdMeasurement, prefs: Prefs, onRepeat: () -> Unit, onHome: () -> Unit) {
    val context = LocalContext.current
    val m = measurement
    val showFar = remember { prefs.farCorrection } && m.pdFarMm != null

    val mainLabel = stringResource(if (showFar) R.string.result_pd_far else R.string.result_pd_measured)
    val mainValue = fmtHalf(m.pd(showFar))
    val monoText = stringResource(R.string.result_mono, fmtHalf(m.monoRight(showFar)), fmtHalf(m.monoLeft(showFar)))
    val nearText = m.distanceMm?.let { stringResource(R.string.result_pd_near, fmt1(m.pdNearMm), fmtCm(it)) }
    val methodText = when (m.method) {
        Method.IRIS -> stringResource(R.string.result_method_iris, m.samples, fmt1(m.referenceMm))
        Method.CARD -> stringResource(R.string.result_method_card)
    }
    val shareText = stringResource(R.string.share_text, mainLabel, mainValue, fmtHalf(m.monoRight(showFar)), fmtHalf(m.monoLeft(showFar)))

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.result_title), style = MaterialTheme.typography.headlineMedium)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(mainLabel, style = MaterialTheme.typography.titleMedium)
                Text("$mainValue mm", fontSize = 48.sp, fontWeight = FontWeight.Bold)
                Text(monoText)
                if (showFar && nearText != null) Text(nearText)
                Text(methodText, style = MaterialTheme.typography.bodySmall)
            }
        }
        Text(stringResource(R.string.result_disclaimer), style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onRepeat, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.repeat)) }
            Button(
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(send, null))
                },
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.share)) }
        }
        TextButton(onClick = onHome) { Text(stringResource(R.string.home)) }
    }
}
