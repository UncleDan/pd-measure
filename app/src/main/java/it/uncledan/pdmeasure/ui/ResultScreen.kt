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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.uncledan.pdmeasure.R
import it.uncledan.pdmeasure.measure.Method
import it.uncledan.pdmeasure.measure.PdMeasurement
import it.uncledan.pdmeasure.measure.PdSet
import it.uncledan.pdmeasure.measure.PdTriple
import it.uncledan.pdmeasure.ui.theme.WarnAmber

@Composable
fun ResultScreen(measurement: PdMeasurement, onRepeat: () -> Unit, onHome: () -> Unit) {
    val context = LocalContext.current
    val m = measurement
    val set = PdSet.from(m)
    val far = set.far.forForm()
    val reading = set.reading.forForm()
    val intermediate = set.intermediate.forForm()
    val measured = set.measured.forForm()

    val methodText = when (m.method) {
        Method.IRIS -> stringResource(R.string.result_method_iris, m.samples, fmt1(m.referenceMm))
        Method.CARD -> stringResource(R.string.result_method_card)
    }
    val shareText = stringResource(
        R.string.share_text,
        fmt1(far.bino), fmt1(far.right), fmt1(far.left),
        fmt1(intermediate.bino), fmt1(reading.bino), fmt1(reading.right), fmt1(reading.left),
    )

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.result_title), style = MaterialTheme.typography.headlineMedium)

        // --- Valore principale ---
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.result_main_label), style = MaterialTheme.typography.titleMedium)
                Text("${fmt1(far.bino)} mm", fontSize = 48.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.result_mono, fmt1(far.right), fmt1(far.left)))
                if (!set.farCorrected) {
                    Text(stringResource(R.string.result_not_corrected), color = MaterialTheme.colorScheme.error)
                }
                Consistency(m)
                Text(methodText, style = MaterialTheme.typography.bodySmall)
            }
        }

        // --- Tutti i valori ---
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.table_title), style = MaterialTheme.typography.titleMedium)
                PdRow("", stringResource(R.string.col_bino), stringResource(R.string.col_od), stringResource(R.string.col_os), header = true)
                HorizontalDivider()
                PdRow(stringResource(R.string.row_far), far, bold = true)
                PdRow(stringResource(R.string.row_intermediate), intermediate)
                PdRow(stringResource(R.string.row_reading), reading)
                HorizontalDivider()
                PdRow(
                    set.measuredDistanceMm?.let { stringResource(R.string.row_measured, fmtCm(it)) }
                        ?: stringResource(R.string.row_measured_nodist),
                    measured,
                )
                Text(stringResource(R.string.table_note), style = MaterialTheme.typography.bodySmall)
            }
        }

        // --- Guida ai moduli d'ordine ---
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.form_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.form_single, fmt1(far.bino)), fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.form_dual, fmt1(far.right), fmt1(far.left)), fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.form_progressive))
                Text(stringResource(R.string.form_reading, fmt1(reading.bino), fmt1(reading.right), fmt1(reading.left)))
                Text(stringResource(R.string.form_rounding), style = MaterialTheme.typography.bodySmall)
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

@Composable
private fun Consistency(m: PdMeasurement) {
    val spread = m.spreadMm ?: return
    Text(
        stringResource(R.string.consistency_attempts, m.attemptsMm.size, m.attemptsMm.joinToString(" · ") { fmt1(it) }),
        style = MaterialTheme.typography.bodyMedium,
    )
    val (res, color) = when {
        spread <= 1.0 -> R.string.consistency_good to Color(0xFF2E7D32)
        spread <= 2.0 -> R.string.consistency_ok to WarnAmber
        else -> R.string.consistency_bad to MaterialTheme.colorScheme.error
    }
    Text(stringResource(res, fmt1(spread)), color = color)
    if (m.trimmed) Text(stringResource(R.string.consistency_trimmed), style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun PdRow(label: String, t: PdTriple, bold: Boolean = false) =
    PdRow(label, fmt1(t.bino), fmt1(t.right), fmt1(t.left), bold = bold)

@Composable
private fun PdRow(label: String, a: String, b: String, c: String, header: Boolean = false, bold: Boolean = false) {
    val weight = if (header || bold) FontWeight.Bold else FontWeight.Normal
    val style = if (header) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyLarge
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1.6f), style = style, fontWeight = weight)
        listOf(a, b, c).forEach {
            Text(it, Modifier.weight(1f), style = style, fontWeight = weight, textAlign = TextAlign.End)
        }
    }
}
