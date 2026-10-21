package pe.amantix.tecoterm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import pe.amantix.tecoterm.ui.diagnostics.DiagnosticsScreen
import pe.amantix.tecoterm.ui.theme.TecoTermTheme

public class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            TecoTermTheme {
                DiagnosticsScreen()
            }
        }
    }
}
