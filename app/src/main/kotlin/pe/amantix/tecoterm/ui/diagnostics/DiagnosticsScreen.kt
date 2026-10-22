package pe.amantix.tecoterm.ui.diagnostics

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pe.amantix.tecoterm.R
import pe.amantix.tecoterm.core.ssh.crypto.AlgorithmAvailability
import pe.amantix.tecoterm.core.ssh.crypto.AlgorithmCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun DiagnosticsScreen(viewModel: DiagnosticsViewModel = viewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.diagnostics_titulo)) }) },
    ) { margenes ->
        when (val actual = estado) {
            DiagnosticsState.Cargando -> CargandoAlgoritmos(margenes)
            is DiagnosticsState.Listo -> ListaDiagnostico(actual, margenes)
        }
    }
}

@Composable
private fun CargandoAlgoritmos(margenes: PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().padding(margenes),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.diagnostics_cargando),
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun ListaDiagnostico(
    diagnostico: DiagnosticsState.Listo,
    margenes: PaddingValues,
) {
    val porCategoria = diagnostico.algoritmos.groupBy { it.algoritmo.categoria }
    val seguridad = diagnostico.seguridad
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = margenes,
    ) {
        item(key = CLAVE_SEGURIDAD) {
            Encabezado(stringResource(R.string.diagnostics_almacen_seguro))
            FilaEstado(
                titulo = stringResource(R.string.diagnostics_keystore),
                correcto = seguridad.keystoreFunciona,
                detalle =
                    stringResource(
                        if (seguridad.keystoreFunciona) R.string.diagnostics_funciona else R.string.diagnostics_no_funciona,
                    ),
            )
            HorizontalDivider()
            FilaEstado(
                titulo = stringResource(R.string.diagnostics_strongbox),
                correcto = seguridad.strongBox,
                detalle =
                    stringResource(
                        if (seguridad.strongBox) R.string.diagnostics_disponible else R.string.diagnostics_strongbox_ausente,
                    ),
            )
            HorizontalDivider()
        }
        porCategoria.forEach { (categoria, deCategoria) ->
            item(key = categoria.name) {
                Encabezado(stringResource(nombreCategoria(categoria)))
            }
            items(deCategoria, key = { it.algoritmo.name }) { disponibilidad ->
                FilaEstado(
                    titulo = disponibilidad.algoritmo.nombre,
                    correcto = disponibilidad.disponible,
                    detalle = textoEstado(disponibilidad),
                    monoespaciado = true,
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun Encabezado(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun FilaEstado(
    titulo: String,
    correcto: Boolean,
    detalle: String,
    monoespaciado: Boolean = false,
) {
    val colorEstado = if (correcto) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(10.dp).background(colorEstado, CircleShape),
        )
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(
                text = titulo,
                fontFamily = if (monoespaciado) FontFamily.Monospace else FontFamily.Default,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = detalle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun textoEstado(disponibilidad: AlgorithmAvailability): String =
    if (disponibilidad.disponible) {
        stringResource(R.string.diagnostics_disponible)
    } else {
        stringResource(
            R.string.diagnostics_no_disponible,
            disponibilidad.primitivasFaltantes.joinToString { it.nombre },
        )
    }

@StringRes
private fun nombreCategoria(categoria: AlgorithmCategory): Int =
    when (categoria) {
        AlgorithmCategory.INTERCAMBIO_CLAVES -> R.string.categoria_intercambio_claves
        AlgorithmCategory.CLAVE_HOST -> R.string.categoria_clave_host
        AlgorithmCategory.CIFRADO -> R.string.categoria_cifrado
        AlgorithmCategory.MAC -> R.string.categoria_mac
    }

private const val CLAVE_SEGURIDAD: String = "almacen-seguro"
