package it.uncledan.pdmeasure.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val Teal = Color(0xFF00796B)
val TealLight = Color(0xFF4DB6AC)
val OkGreen = Color(0xFF00E676)
val WarnAmber = Color(0xFFFFC400)

@Composable
fun PdMeasureTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme(primary = TealLight, secondary = TealLight)
        else -> lightColorScheme(primary = Teal, secondary = Teal)
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
