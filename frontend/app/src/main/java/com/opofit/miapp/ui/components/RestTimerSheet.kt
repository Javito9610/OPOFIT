package com.opofit.miapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestTimerSheet(
    visible: Boolean,
    ejercicioNombre: String,
    initialSeconds: Int = 90,
    onDismiss: () -> Unit,
    onSkip: () -> Unit
) {
    if (!visible) return

    var segundosRestantes by remember(ejercicioNombre, initialSeconds) { mutableIntStateOf(initialSeconds) }
    var corriendo by remember { mutableStateOf(true) }

    LaunchedEffect(visible, initialSeconds) {
        segundosRestantes = initialSeconds
        corriendo = true
    }

    LaunchedEffect(visible, corriendo, segundosRestantes) {
        if (!visible || !corriendo) return@LaunchedEffect
        while (segundosRestantes > 0 && corriendo) {
            delay(1000L)
            segundosRestantes -= 1
        }
        if (segundosRestantes <= 0) {
            onDismiss()
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Descanso", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Siguiente: $ejercicioNombre",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onSkip) {
                    Icon(Icons.Filled.Close, contentDescription = "Saltar descanso")
                }
            }

            val mm = segundosRestantes / 60
            val ss = segundosRestantes % 60
            Text(
                text = "%02d:%02d".format(mm, ss),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            CircularProgressIndicator(
                progress = {
                    if (initialSeconds > 0) segundosRestantes.toFloat() / initialSeconds else 0f
                },
                modifier = Modifier.size(72.dp),
                strokeWidth = 6.dp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(60, 90, 120, 180).forEach { s ->
                    FilterChip(
                        selected = segundosRestantes == s,
                        onClick = { segundosRestantes = s },
                        label = { Text("${s}s") }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { segundosRestantes += 15 },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                    Text("+15 s", modifier = Modifier.padding(start = 4.dp))
                }
                Button(
                    onClick = onSkip,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Saltar")
                }
            }
        }
    }
}
