package it.uncledan.pdmeasure

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import it.uncledan.pdmeasure.ui.PdMeasureApp
import it.uncledan.pdmeasure.ui.theme.PdMeasureTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            PdMeasureTheme { PdMeasureApp() }
        }
    }
}
