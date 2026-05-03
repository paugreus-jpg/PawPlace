// File: app/src/main/java/com/example/dogmap/ui/screens/LoginScreen.kt
// PawPlace login — single glass card on the deep-space gradient.
// Auth logic (AuthViewModel + validators) preserved exactly.
package com.example.dogmap.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dogmap.R
import com.example.dogmap.ui.LocalViewModelFactory
import com.example.dogmap.ui.components.GlassCard
import com.example.dogmap.ui.components.PawPlaceBackground
import com.example.dogmap.ui.components.PawPlaceLogo
import com.example.dogmap.ui.theme.BrandPrimary
import com.example.dogmap.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val factory = LocalViewModelFactory.current
    val viewModel: AuthViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsState()

    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(state.isAuthenticated) {
        if (state.isAuthenticated) onLoginSuccess()
    }
    DisposableEffect(Unit) { onDispose { viewModel.reset() } }

    val emailError = viewModel.emailErrorFor(state)
    val passwordError = viewModel.passwordErrorFor(state, forRegister = false)
    val canSubmit = viewModel.canSubmitLogin(state)

    PawPlaceBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // ── Mark + wordmark ───────────────────────────────────────
            PawPlaceLogo(size = 76.dp)
            Spacer(Modifier.height(14.dp))
            Text(
                text = "PawPlace",
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFF2F6F8),
                letterSpacing = (-0.6).sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "BIENVENIDO DE NUEVO",
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                color = BrandPrimary.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(28.dp))

            // ── Glass auth card ───────────────────────────────────────
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.login_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF2F6F8),
                )
                Spacer(Modifier.height(14.dp))

                GlassTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    label = stringResource(R.string.email_label),
                    leading = { Icon(Icons.Default.Email, null, tint = BrandPrimary) },
                    keyboardType = KeyboardType.Email,
                    isError = emailError != null,
                    errorText = emailError,
                )

                Spacer(Modifier.height(10.dp))

                GlassTextField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = stringResource(R.string.password_label),
                    leading = { Icon(Icons.Default.Lock, null, tint = BrandPrimary) },
                    trailing = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                null,
                                tint = Color(0x99F2F6F8)
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                    keyboardType = KeyboardType.Password,
                    isError = passwordError != null,
                    errorText = passwordError,
                )

                if (state.generalError != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = state.generalError!!,
                        color = Color(0xFFFF9DB1),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Spacer(Modifier.height(20.dp))

                CyanButton(
                    onClick = { viewModel.login() },
                    enabled = canSubmit,
                    isLoading = state.isLoading,
                    label = stringResource(R.string.enter_button),
                )

                Spacer(Modifier.height(8.dp))

                TextButton(
                    onClick = onNavigateToRegister,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = Color(0x99F2F6F8))) {
                                append("¿No tienes cuenta? ")
                            }
                            withStyle(
                                SpanStyle(
                                    color = BrandPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            ) { append("Regístrate") }
                        },
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

/* ─── Local helpers (keep these internal — Login + Register share the look) ─── */

@Composable
internal fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    errorText: String? = null,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = leading,
        trailingIcon = trailing,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        singleLine = singleLine,
        isError = isError,
        supportingText = errorText?.let { { Text(it, color = Color(0xFFFF9DB1)) } },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0x14FFFFFF),
            unfocusedContainerColor = Color(0x0AFFFFFF),
            focusedBorderColor = BrandPrimary,
            unfocusedBorderColor = Color(0x33FFFFFF),
            errorBorderColor = Color(0xFFFF9DB1),
            focusedLabelColor = BrandPrimary,
            unfocusedLabelColor = Color(0x99F2F6F8),
            focusedTextColor = Color(0xFFF2F6F8),
            unfocusedTextColor = Color(0xFFF2F6F8),
            cursorColor = BrandPrimary,
        ),
    )
}

@Composable
internal fun CyanButton(
    onClick: () -> Unit,
    enabled: Boolean,
    isLoading: Boolean,
    label: String,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(BrandPrimary, Color(0xFF5BC2EE))
                ),
                shape = RoundedCornerShape(14.dp),
            ),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color(0x33FFFFFF),
            contentColor = Color(0xFF03161F),
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color(0xFF03161F),
                strokeWidth = 2.dp,
            )
        } else {
            Text(text = label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}
