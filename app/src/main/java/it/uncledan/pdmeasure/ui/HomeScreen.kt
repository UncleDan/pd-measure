package it.uncledan.pdmeasure.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.uncledan.pdmeasure.Prefs
import it.uncledan.pdmeasure.R
import it.uncledan.pdmeasure.measure.PdMath

@Composable
fun HomeScreen(prefs: Prefs, onIris: () -> Unit, onCard: () -> Unit, onInfo: () -> Unit) {
    var iris by remember { mutableStateOf(prefs.calibratedIrisMm) }

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.app_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.home_subtitle), style = MaterialTheme.typography.bodyLarge)

        MethodCard(stringResource(R.string.method_iris_title), stringResource(R.string.method_iris_desc), onIris)
        MethodCard(stringResource(R.string.method_card_title), stringResource(R.string.method_card_desc), onCard)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val cal = iris
                Text(
                    if (cal == null) stringResource(R.string.calibration_none, fmt1(PdMath.DEFAULT_IRIS_MM))
                    else stringResource(R.string.calibration_value, fmt1(cal))
                )
                if (cal != null) {
                    TextButton(onClick = { prefs.calibratedIrisMm = null; iris = null }) {
                        Text(stringResource(R.string.calibration_reset))
                    }
                }
            }
        }

        TextButton(onClick = onInfo) { Text(stringResource(R.string.info_title)) }
    }
}

@Composable
private fun MethodCard(title: String, description: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
