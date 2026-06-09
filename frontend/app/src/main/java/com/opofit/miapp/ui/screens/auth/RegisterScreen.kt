package com.opofit.miapp.ui.screens.auth

import com.opofit.miapp.ui.components.ElevatedCard
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opofit.miapp.data.api.RetrofitClient
import com.opofit.miapp.data.local.TokenManager
import com.opofit.miapp.data.responsemodels.Oposicion
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.opofit.miapp.R
import com.opofit.miapp.ui.viewmodels.AuthViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var genero by remember { mutableStateOf("") }
    var peso by remember { mutableStateOf("") }
    var altura by remember { mutableStateOf("") }
    var imc by remember { mutableStateOf("0.00") }
    var oposicionId by remember { mutableStateOf<Int?>(null) }
    var soloFitness by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var pendingFirebaseIdToken by remember { mutableStateOf<String?>(null) }

    val uiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val scope = rememberCoroutineScope()
    val firebaseAuth = remember { FirebaseAuth.getInstance() }

    var oposiciones by remember { mutableStateOf<List<Oposicion>>(emptyList()) }
    var errorOposiciones by remember { mutableStateOf("") }
    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    .getResult(ApiException::class.java)
                val token = account.idToken
                if (token != null) {
                    scope.launch {
                        try {
                            val credential = GoogleAuthProvider.getCredential(token, null)
                            firebaseAuth.signInWithCredential(credential).await()
                            val idToken = firebaseAuth.currentUser?.getIdToken(true)?.await()?.token
                            if (!idToken.isNullOrBlank()) {
                                viewModel.clearError()
                                localError = ""
                                pendingFirebaseIdToken = idToken
                                account.email?.let { email = it }
                                val fromGoogle =
                                    account.givenName?.takeIf { it.isNotBlank() }
                                        ?: account.displayName?.takeIf { it.isNotBlank() }
                                if (!fromGoogle.isNullOrBlank()) nombre = fromGoogle
                            } else {
                                viewModel.setError("No se pudo obtener el token de Firebase")
                            }
                        } catch (e: Exception) {
                            viewModel.setError("Error al registrar con Firebase: ${e.message ?: "desconocido"}")
                        }
                    }
                } else {
                    viewModel.setError("No se pudo obtener el token de Google")
                }
            } catch (e: ApiException) {
                viewModel.setError(GoogleSignInHelper.getErrorMessage(e))
            }
        }
    }

    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            pendingFirebaseIdToken = null
            onRegisterSuccess()
            viewModel.clearSuccessFlag()
        }
    }

    LaunchedEffect(Unit) {
        try {
            val resp = RetrofitClient.oposicionesApi.getOposiciones("")
            if (resp.ok) {
                oposiciones = resp.data ?: emptyList()
            } else {
                errorOposiciones = "No se pudieron cargar las oposiciones"
            }
        } catch (_: Exception) {
            errorOposiciones = "No se pudieron cargar las oposiciones"
        }
    }

    fun calcularIMC() {
        val pesoFloat = peso.toFloatOrNull() ?: 0f
        val alturaFloat = altura.toFloatOrNull() ?: 0f
        if (pesoFloat > 0 && alturaFloat > 0) {
            val alturaMetros = alturaFloat / 100f
            imc = String.format("%.2f", pesoFloat / (alturaMetros * alturaMetros))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text("🏋️", fontSize = 36.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Crear cuenta",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Completa tus datos para empezar",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Datos personales",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (pendingFirebaseIdToken != null) {
                        Text(
                            text = "Registro con Google: completa género, peso, altura y oposición; revisa el nombre si hace falta.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        placeholder = { Text("tu@email.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = pendingFirebaseIdToken == null && !uiState.isLoading,
                        readOnly = pendingFirebaseIdToken != null,
                        shape = MaterialTheme.shapes.medium
                    )

                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre completo") },
                        placeholder = { Text("Tu nombre") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !uiState.isLoading,
                        shape = MaterialTheme.shapes.medium
                    )

                    
                    var expandedGenero by remember { mutableStateOf(false) }
                    val generosDisponibles = listOf("HOMBRE", "MUJER")

                    ExposedDropdownMenuBox(
                        expanded = expandedGenero,
                        onExpandedChange = { if (!uiState.isLoading) expandedGenero = it }
                    ) {
                        OutlinedTextField(
                            value = genero,
                            onValueChange = {},
                            label = { Text("Género") },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            enabled = !uiState.isLoading,
                            shape = MaterialTheme.shapes.medium,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedGenero) }
                        )
                        ExposedDropdownMenu(
                            expanded = expandedGenero,
                            onDismissRequest = { expandedGenero = false }
                        ) {
                            generosDisponibles.forEach { opcion ->
                                DropdownMenuItem(
                                    text = { Text(opcion) },
                                    onClick = { genero = opcion; expandedGenero = false }
                                )
                            }
                        }
                    }

                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = peso,
                            onValueChange = { peso = it; calcularIMC() },
                            label = { Text("Peso (kg)") },
                            placeholder = { Text("70") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            enabled = !uiState.isLoading,
                            shape = MaterialTheme.shapes.medium
                        )
                        OutlinedTextField(
                            value = altura,
                            onValueChange = { altura = it; calcularIMC() },
                            label = { Text("Altura (cm)") },
                            placeholder = { Text("175") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            enabled = !uiState.isLoading,
                            shape = MaterialTheme.shapes.medium
                        )
                    }

                    
                    if (imc != "0.00") {
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "IMC calculado: $imc",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    
                    androidx.compose.material3.FilterChip(
                        selected = soloFitness,
                        onClick = { soloFitness = !soloFitness; if (soloFitness) oposicionId = null },
                        label = { Text("Solo entrenar (sin oposición)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!soloFitness) {
                    var expandedOpo by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedOpo,
                        onExpandedChange = { if (!uiState.isLoading) expandedOpo = it }
                    ) {
                        val selectedLabel = oposiciones.firstOrNull { it.id_oposicion == oposicionId }?.nombre ?: ""
                        OutlinedTextField(
                            value = selectedLabel,
                            onValueChange = {},
                            label = { Text("Oposición") },
                            placeholder = { Text("Selecciona tu oposición") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            singleLine = true,
                            enabled = !uiState.isLoading,
                            readOnly = true,
                            shape = MaterialTheme.shapes.medium,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedOpo) },
                            isError = errorOposiciones.isNotEmpty()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedOpo,
                            onDismissRequest = { expandedOpo = false }
                        ) {
                            oposiciones.forEach { opo ->
                                DropdownMenuItem(
                                    text = { Text(opo.nombre) },
                                    onClick = {
                                        oposicionId = opo.id_oposicion
                                        expandedOpo = false
                                    }
                                )
                            }
                        }
                    }
                    if (errorOposiciones.isNotEmpty()) {
                        Text(
                            text = errorOposiciones,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    }

                    if (pendingFirebaseIdToken == null) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Text(
                            text = "Seguridad",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Contraseña") },
                            placeholder = { Text("Mínimo 6 caracteres") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !uiState.isLoading,
                            visualTransformation = if (passwordVisible)
                                VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(
                                    onClick = { passwordVisible = !passwordVisible },
                                    enabled = !uiState.isLoading
                                ) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = "Mostrar contraseña",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            shape = MaterialTheme.shapes.medium
                        )

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirmar contraseña") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !uiState.isLoading,
                            visualTransformation = if (confirmPasswordVisible)
                                VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(
                                    onClick = { confirmPasswordVisible = !confirmPasswordVisible },
                                    enabled = !uiState.isLoading
                                ) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = "Mostrar contraseña",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            shape = MaterialTheme.shapes.medium
                        )
                    }

                    
                    val errorMessage = localError.ifEmpty { uiState.error }
                    if (errorMessage.isNotEmpty()) {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    
                    Button(
                        onClick = {
                            val token = pendingFirebaseIdToken
                            localError = when {
                                nombre.isBlank() -> "El nombre es obligatorio"
                                email.isBlank() -> "El email es obligatorio"
                                genero.isBlank() -> "Debes seleccionar un género"
                                peso.isBlank() || peso.toDoubleOrNull() == null -> "Introduce un peso válido (kg)"
                                altura.isBlank() || altura.toDoubleOrNull() == null -> "Introduce una altura válida (cm)"
                                !soloFitness && oposicionId == null -> "Selecciona una oposición"
                                token == null && password.length < 6 -> "La contraseña debe tener al menos 6 caracteres"
                                token == null && password != confirmPassword -> "Las contraseñas no coinciden"
                                else -> ""
                            }
                            if (localError.isEmpty()) {
                                if (token != null) {
                                    viewModel.registerWithFirebase(
                                        idToken = token,
                                        nombre = nombre.trim(),
                                        genero = genero,
                                        peso = peso.toDouble(),
                                        altura = altura.toDouble(),
                                        oposicionesId = oposicionId!!
                                    )
                                } else {
                                    viewModel.register(
                                        nombre = nombre,
                                        email = email,
                                        password = password,
                                        genero = genero,
                                        peso = peso.toDouble(),
                                        altura = altura.toDouble(),
                                        oposiciones_id = if (soloFitness) null else oposicionId,
                                        modoUso = if (soloFitness) "FITNESS" else "OPOSITOR"
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !uiState.isLoading,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                if (pendingFirebaseIdToken != null) "Completar registro con Google"
                                else "Crear cuenta",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    if (pendingFirebaseIdToken != null) {
                        TextButton(
                            onClick = {
                                pendingFirebaseIdToken = null
                                scope.launch {
                                    runCatching {
                                        firebaseAuth.signOut()
                                        googleSignInClient.signOut().await()
                                    }
                                    viewModel.clearError()
                                    localError = ""
                                }
                            },
                            enabled = !uiState.isLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            com.opofit.miapp.ui.components.ButtonText("Usar email en su lugar")
                        }
                    }

                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Divider(modifier = Modifier.weight(1f))
                        Text(
                            text = if (pendingFirebaseIdToken != null) "  o cambia la cuenta Google  "
                            else "  o regístrate con  ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Divider(modifier = Modifier.weight(1f))
                    }

                    OutlinedButton(
                        onClick = {
                            googleSignInClient.signOut().addOnCompleteListener {
                                googleSignInLauncher.launch(googleSignInClient.signInIntent)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !uiState.isLoading,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            if (pendingFirebaseIdToken != null) "🔐  Elegir otra cuenta de Google"
                            else "🔐  Registrarse con Google",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "¿Ya tienes cuenta? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(
                    onClick = onNavigateToLogin,
                    enabled = !uiState.isLoading
                ) {
                    Text(
                        "Inicia sesión",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
