package com.opofit.miapp.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.responsemodels.RecuperarPasswordRequest
import com.opofit.miapp.data.responsemodels.ResetPasswordRequest
import com.opofit.miapp.ui.components.OpoFitLogo
import com.opofit.miapp.utils.ApiErrorParser
import kotlinx.coroutines.launch

/**
 * Recuperación de contraseña por código (email). Dos pasos:
 *  1. El usuario introduce su email → recibe un código de 6 dígitos por correo.
 *  2. Introduce el código + la nueva contraseña → se restablece.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit,
    onPasswordReset: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var paso by remember { mutableStateOf(1) }
    var email by remember { mutableStateOf("") }
    var codigo by remember { mutableStateOf("") }
    var nuevaPass by remember { mutableStateOf("") }
    var passVisible by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    var esError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recuperar contraseña") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            OpoFitLogo(size = 72.dp)
            Icon(
                if (paso == 1) Icons.Filled.LockReset else Icons.Filled.MarkEmailRead,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )

            if (paso == 1) {
                Text(
                    "Introduce tu email y te enviaremos un código de 6 dígitos para restablecer tu contraseña.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        loading = true; msg = ""; esError = false
                        scope.launch {
                            try {
                                val resp = RetrofitClient.authApi.recuperarPassword(
                                    RecuperarPasswordRequest(email.trim())
                                )
                                msg = resp.msg ?: "Si el email está registrado, recibirás un código."
                                esError = false
                                paso = 2
                            } catch (e: Exception) {
                                msg = ApiErrorParser.message(e); esError = true
                            } finally { loading = false }
                        }
                    },
                    enabled = email.isNotBlank() && !loading,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    if (loading) CircularProgressIndicator(Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    else Text("Enviar código", fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    "Revisa tu correo e introduce el código de 6 dígitos junto con tu nueva contraseña.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                OutlinedTextField(
                    value = codigo,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) codigo = it },
                    label = { Text("Código (6 dígitos)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nuevaPass,
                    onValueChange = { nuevaPass = it },
                    label = { Text("Nueva contraseña") },
                    singleLine = true,
                    visualTransformation = if (passVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                    isError = nuevaPass.isNotEmpty() && nuevaPass.length < 6,
                    supportingText = {
                        if (nuevaPass.isNotEmpty() && nuevaPass.length < 6)
                            Text("Mínimo 6 caracteres", color = MaterialTheme.colorScheme.error)
                    },
                    trailingIcon = {
                        TextButton(onClick = { passVisible = !passVisible }) {
                            Text(if (passVisible) "Ocultar" else "Ver")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        loading = true; msg = ""; esError = false
                        scope.launch {
                            try {
                                val resp = RetrofitClient.authApi.resetPassword(
                                    ResetPasswordRequest(email.trim(), codigo.trim(), nuevaPass)
                                )
                                if (resp.ok) {
                                    msg = resp.msg ?: "Contraseña actualizada."
                                    esError = false
                                    onPasswordReset()
                                } else {
                                    msg = resp.msg ?: "No se pudo restablecer"; esError = true
                                }
                            } catch (e: Exception) {
                                msg = ApiErrorParser.message(e); esError = true
                            } finally { loading = false }
                        }
                    },
                    enabled = codigo.length == 6 && nuevaPass.length >= 6 && !loading,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    if (loading) CircularProgressIndicator(Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    else Text("Restablecer contraseña", fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = { paso = 1; msg = "" }) {
                    Text("¿No te llegó? Volver a enviar")
                }
            }

            if (msg.isNotBlank()) {
                Text(
                    msg,
                    color = if (esError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
