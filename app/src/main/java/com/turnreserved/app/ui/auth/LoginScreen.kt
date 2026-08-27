package com.turnreserved.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import com.turnreserved.app.ui.theme.*

@Composable
fun LoginScreen(authViewModel: AuthViewModel) {
    var isSignup by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var success by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(
                colors = listOf(Surface, Background),
                radius = 800f
            ))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(60.dp))

            // Logo
            Icon(
                Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text("TurnReserved", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Text(
                if (isSignup) "Creá tu cuenta" else "Iniciá sesión",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(Modifier.height(32.dp))

            // Form card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg)
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppTextField(
                        value = email, onValueChange = { email = it },
                        placeholder = "Email", icon = Icons.Outlined.Email,
                        keyboardType = KeyboardType.Email
                    )

                    if (isSignup) {
                        AppTextField(
                            value = phone, onValueChange = { phone = it },
                            placeholder = "Celular", icon = Icons.Outlined.Phone,
                            keyboardType = KeyboardType.Phone
                        )
                    }

                    AppTextField(
                        value = password, onValueChange = { password = it },
                        placeholder = "Contraseña", icon = Icons.Outlined.Lock,
                        isPassword = !showPassword,
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = null, tint = TextSecondary
                                )
                            }
                        }
                    )

                    if (error.isNotEmpty()) {
                        Text(error, color = Destructive, style = MaterialTheme.typography.bodySmall)
                    }
                    if (success.isNotEmpty()) {
                        Text(success, color = Success, style = MaterialTheme.typography.bodySmall)
                    }

                    Button(
                        onClick = {
                            error = ""; success = ""; isLoading = true
                            if (isSignup) {
                                authViewModel.register(email, phone, password,
                                    onSuccess = {
                                        isLoading = false; isSignup = false
                                        success = "✅ Cuenta creada. Tenés 5 días de prueba gratuita."
                                    },
                                    onError = { isLoading = false; error = it }
                                )
                            } else {
                                authViewModel.login(email, password) { isLoading = false; error = it }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        enabled = !isLoading
                    ) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = TextPrimary, strokeWidth = 2.dp)
                        else Text(if (isSignup) "Crear cuenta" else "Iniciar sesión")
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            TextButton(onClick = { isSignup = !isSignup; error = ""; success = "" }) {
                Text(
                    if (isSignup) "¿Ya tenés cuenta? Iniciar sesión" else "¿No tenés cuenta? Crear cuenta",
                    color = Primary
                )
            }
        }
    }
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = TextTertiary) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp)) },
        trailingIcon = trailingIcon,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedBorderColor = Primary,
            unfocusedBorderColor = CardBorder,
            cursorColor = Primary,
            focusedContainerColor = CardBg,
            unfocusedContainerColor = CardBg
        ),
        modifier = Modifier.fillMaxWidth()
    )
}
