package it.uncledan.pdmeasure.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import it.uncledan.pdmeasure.Prefs
import it.uncledan.pdmeasure.R
import it.uncledan.pdmeasure.measure.Method
import it.uncledan.pdmeasure.measure.PdMeasurement

private sealed interface Screen {
    data object Home : Screen
    data object Iris : Screen
    data object Card : Screen
    data object Info : Screen
    data class Result(val measurement: PdMeasurement) : Screen
}

@Composable
fun PdMeasureApp() {
    val context = LocalContext.current
    val prefs = remember { Prefs(context.applicationContext) }
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var hasCamera by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCamera = granted }

    val goHome = { screen = Screen.Home }
    BackHandler(enabled = screen != Screen.Home) { goHome() }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (val s = screen) {
            Screen.Home -> HomeScreen(
                prefs = prefs,
                onIris = { screen = Screen.Iris },
                onCard = { screen = Screen.Card },
                onInfo = { screen = Screen.Info },
            )
            Screen.Iris -> CameraGate(hasCamera, { permissionLauncher.launch(Manifest.permission.CAMERA) }, goHome) {
                IrisScreen(prefs, onResult = { screen = Screen.Result(it) }, onBack = goHome)
            }
            Screen.Card -> CameraGate(hasCamera, { permissionLauncher.launch(Manifest.permission.CAMERA) }, goHome) {
                CardScreen(prefs, onResult = { screen = Screen.Result(it) }, onBack = goHome)
            }
            Screen.Info -> InfoScreen(onBack = goHome)
            is Screen.Result -> ResultScreen(
                measurement = s.measurement,
                prefs = prefs,
                onRepeat = {
                    screen = if (s.measurement.method == Method.CARD) Screen.Card else Screen.Iris
                },
                onHome = goHome,
            )
        }
    }
}

@Composable
private fun CameraGate(
    granted: Boolean,
    onRequest: () -> Unit,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (granted) {
        content()
        return
    }
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.permission_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.permission_text), textAlign = TextAlign.Center)
        Button(onClick = onRequest) { Text(stringResource(R.string.permission_grant)) }
        TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
    }
}
