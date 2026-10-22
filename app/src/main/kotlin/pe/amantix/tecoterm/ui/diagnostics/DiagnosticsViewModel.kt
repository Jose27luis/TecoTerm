package pe.amantix.tecoterm.ui.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pe.amantix.tecoterm.core.security.SecurityDiagnostics
import pe.amantix.tecoterm.core.ssh.crypto.PlatformAlgorithms

public class DiagnosticsViewModel : ViewModel() {
    private val _estado = MutableStateFlow<DiagnosticsState>(DiagnosticsState.Cargando)
    public val estado: StateFlow<DiagnosticsState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val algoritmos = withContext(Dispatchers.Default) { PlatformAlgorithms.detectar() }
            val seguridad = withContext(Dispatchers.IO) { SecurityDiagnostics.comprobar() }
            _estado.value = DiagnosticsState.Listo(algoritmos, seguridad)
        }
    }
}
