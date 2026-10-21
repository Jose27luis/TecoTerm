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
            is DiagnosticsState.Listo -> ListaAlgoritmos(actual.algoritmos, margenes)
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
private fun ListaAlgoritmos(
    algoritmos: List<AlgorithmAvailability>,
    margenes: PaddingValues,
) {
    val porCategoria = algoritmos.groupBy { it.algoritmo.categoria }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = margenes,
    ) {
        porCategoria.forEach { (categoria, deCategoria) ->
            item(key = categoria.name) {
                Text(
                    text = stringResource(nombreCategoria(categoria)),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
                )
            }
            items(deCategoria, key = { it.algoritmo.name }) { disponibilidad ->
                FilaAlgoritmo(disponibilidad)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun FilaAlgoritmo(disponibilidad: AlgorithmAvailability) {
    val colorEstado =
        if (disponibilidad.disponible) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.error
        }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(10.dp).background(colorEstado, CircleShape),
        )
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(
                text = disponibilidad.algoritmo.nombre,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = textoEstado(disponibilidad),
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
