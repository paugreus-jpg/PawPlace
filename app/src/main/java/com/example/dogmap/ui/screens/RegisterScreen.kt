// File: app/src/main/java/com/example/dogmap/ui/screens/RegisterScreen.kt
// PawPlace register — same glass card grammar as LoginScreen, four fields.
// AuthViewModel + validators preserved exactly.
package com.example.dogmap.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val factory = LocalViewModelFactory.current
    val viewModel: AuthViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsState()

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val webClientId = stringResource(R.string.google_web_client_id)
    val googleSignInClient = remember(webClientId) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }
    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            viewModel.signInWithGoogle(account.idToken)
        } catch (e: ApiException) {
            viewModel.signInWithGoogle(null)
        }
    }

    LaunchedEffect(state.isAuthenticated) {
        if (state.isAuthenticated) onRegisterSuccess()
    }
    DisposableEffect(Unit) { onDispose { viewModel.reset() } }

    val nameError = viewModel.nameErrorFor(state)
    val emailError = viewModel.emailErrorFor(state)
    val passwordError = viewModel.passwordErrorFor(state, forRegister = true)
    val confirmError = viewModel.confirmErrorFor(state)
    val canSubmit = viewModel.canSubmitRegister(state)

    PawPlaceBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            PawPlaceLogo(size = 60.dp)
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.register_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFF2F6F8),
                letterSpacing = (-0.4).sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.join_community),
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                color = BrandPrimary.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(20.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                GlassTextField(
                    value = state.displayName,
                    onValueChange = viewModel::onDisplayNameChange,
                    label = stringResource(R.string.display_name),
                    leading = { Icon(Icons.Default.Person, null, tint = BrandPrimary) },
                    isError = nameError != null,
                    errorText = nameError,
                )

                Spacer(Modifier.height(10.dp))

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
                                tint = Color(0x99F2F6F8),
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                    keyboardType = KeyboardType.Password,
                    isError = passwordError != null,
                    errorText = passwordError,
                )

                Spacer(Modifier.height(10.dp))

                GlassTextField(
                    value = state.confirmPassword,
                    onValueChange = viewModel::onConfirmChange,
                    label = stringResource(R.string.confirm_password),
                    leading = { Icon(Icons.Default.Lock, null, tint = BrandPrimary) },
                    trailing = {
                        IconButton(onClick = { confirmVisible = !confirmVisible }) {
                            Icon(
                                if (confirmVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                null,
                                tint = Color(0x99F2F6F8),
                            )
                        }
                    },
                    visualTransformation = if (confirmVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                    keyboardType = KeyboardType.Password,
                    isError = confirmError != null,
                    errorText = confirmError,
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
                    onClick = { viewModel.register() },
                    enabled = !state.isLoading,
                    isLoading = state.isLoading,
                    label = stringResource(R.string.register_title),
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    HorizontalDivider(Modifier.weight(1f), color = Color(0x33FFFFFF))
                    Text(
                        text = "  ${stringResource(R.string.or)}  ",
                        color = Color(0x80F2F6F8),
                        fontSize = 12.sp,
                    )
                    HorizontalDivider(Modifier.weight(1f), color = Color(0x33FFFFFF))
                }

                Spacer(Modifier.height(12.dp))

                GoogleSignInButton(
                    onClick = {
                        googleSignInClient.signOut()
                        googleLauncher.launch(googleSignInClient.signInIntent)
                    },
                    isLoading = state.isLoading,
                )

                Spacer(Modifier.height(8.dp))

                TextButton(
                    onClick = onNavigateToLogin,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = Color(0x99F2F6F8))) {
                                append(stringResource(R.string.already_have_account_prefix))
                            }
                            withStyle(
                                SpanStyle(
                                    color = BrandPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            ) { append(stringResource(R.string.login_action)) }
                        },
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}
