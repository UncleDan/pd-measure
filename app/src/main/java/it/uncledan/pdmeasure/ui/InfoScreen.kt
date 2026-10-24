package it.uncledan.pdmeasure.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.uncledan.pdmeasure.BuildConfig
import it.uncledan.pdmeasure.R

const val REPO_URL = "https://github.com/UncleDan/pd-measure"

@Composable
fun InfoScreen(onBack: () -> Unit) {
    val uri = LocalUriHandler.current
    Scaffold(topBar = { BackTopBar(stringResource(R.string.info_title), onBack) }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.app_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.info_version, BuildConfig.VERSION_NAME))
            Text(stringResource(R.string.info_how_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.info_how_body))
            Text(stringResource(R.string.info_privacy_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.info_privacy_body))
            Text(stringResource(R.string.result_disclaimer), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.info_author))
            TextButton(onClick = { uri.openUri(REPO_URL) }) { Text(REPO_URL) }
        }
    }
}
