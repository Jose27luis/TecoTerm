package pe.amantix.tecoterm.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
public fun TecoTermTheme(content: @Composable () -> Unit) {
    val contexto = LocalContext.current
    val esquema =
        if (isSystemInDarkTheme()) {
            dynamicDarkColorScheme(contexto)
        } else {
            dynamicLightColorScheme(contexto)
        }
    MaterialTheme(colorScheme = esquema, content = content)
}
