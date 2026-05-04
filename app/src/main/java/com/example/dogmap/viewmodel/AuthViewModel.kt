package com.example.dogmap.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dogmap.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val displayName: String = "",
    val emailTouched: Boolean = false,
    val passwordTouched: Boolean = false,
    val confirmTouched: Boolean = false,
    val nameTouched: Boolean = false,
    val submitted: Boolean = false,
    val isLoading: Boolean = false,
    val generalError: String? = null,
    val isAuthenticated: Boolean = false
)

class AuthViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        private val STRONG_PASSWORD_REGEX = Regex("^(?=.*[A-Za-z])(?=.*\\d).{8,}$")

        fun validEmail(email: String) = EMAIL_REGEX.matches(email.trim())
        fun validPassword(pw: String) = STRONG_PASSWORD_REGEX.matches(pw)
        fun validDisplayName(name: String) = name.trim().length >= 2
    }

    fun onEmailChange(v: String) = _state.update { it.copy(email = v, generalError = null) }
    fun onPasswordChange(v: String) = _state.update { it.copy(password = v, generalError = null) }
    fun onConfirmChange(v: String) = _state.update { it.copy(confirmPassword = v, generalError = null) }
    fun onDisplayNameChange(v: String) = _state.update { it.copy(displayName = v, generalError = null) }

    fun onEmailBlur() = _state.update { it.copy(emailTouched = true) }
    fun onPasswordBlur() = _state.update { it.copy(passwordTouched = true) }
    fun onConfirmBlur() = _state.update { it.copy(confirmTouched = true) }
    fun onNameBlur() = _state.update { it.copy(nameTouched = true) }

    fun reset() {
        _state.value = AuthUiState()
    }

    fun emailErrorFor(s: AuthUiState): String? =
        if ((s.emailTouched || s.submitted) && !validEmail(s.email)) "Email no válido" else null

    fun passwordErrorFor(s: AuthUiState, forRegister: Boolean): String? =
        when {
            !(s.passwordTouched || s.submitted) -> null
            forRegister && !validPassword(s.password) -> "Mínimo 8 caracteres con letras y números"
            !forRegister && s.password.isBlank() -> "Contraseña requerida"
            else -> null
        }

    fun confirmErrorFor(s: AuthUiState): String? =
        if ((s.confirmTouched || s.submitted) && s.password != s.confirmPassword)
            "Las contraseñas no coinciden" else null

    fun nameErrorFor(s: AuthUiState): String? =
        if ((s.nameTouched || s.submitted) && !validDisplayName(s.displayName))
            "Introduce tu nombre" else null

    fun canSubmitLogin(s: AuthUiState): Boolean =
        validEmail(s.email) && s.password.isNotBlank() && !s.isLoading

    fun canSubmitRegister(s: AuthUiState): Boolean =
        validEmail(s.email) &&
                validPassword(s.password) &&
                s.password == s.confirmPassword &&
                validDisplayName(s.displayName) &&
                !s.isLoading

    fun login() {
        val s = _state.value
        _state.update { it.copy(submitted = true) }
        if (!canSubmitLogin(s)) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, generalError = null) }
            try {
                val result = auth.signInWithEmailAndPassword(s.email.trim(), s.password).await()
                val uid = result.user?.uid.orEmpty()
                val email = result.user?.email.orEmpty()
                val fallbackName = email.substringBefore("@").ifBlank { "Usuario" }
                userRepository.ensureUserDocument(uid, email, fallbackName)
                _state.update { it.copy(isLoading = false, isAuthenticated = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, generalError = translate(e)) }
            }
        }
    }

    fun register() {
        val s = _state.value
        _state.update { it.copy(submitted = true) }
        if (!canSubmitRegister(s)) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, generalError = null) }
            try {
                val result = auth.createUserWithEmailAndPassword(s.email.trim(), s.password).await()
                val uid = result.user?.uid.orEmpty()
                userRepository.ensureUserDocument(uid, s.email.trim(), s.displayName.trim())
                _state.update { it.copy(isLoading = false, isAuthenticated = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, generalError = translate(e)) }
            }
        }
    }

    fun signInWithGoogle(idToken: String?) {
        if (idToken == null) {
            _state.update { it.copy(generalError = "Error de Google Sign-In") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, generalError = null) }
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val result = auth.signInWithCredential(credential).await()
                val uid = result.user?.uid.orEmpty()
                val email = result.user?.email.orEmpty()
                val displayName = result.user?.displayName.orEmpty()
                    .ifBlank { email.substringBefore("@") }
                userRepository.ensureUserDocument(uid, email, displayName)
                _state.update { it.copy(isLoading = false, isAuthenticated = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, generalError = translate(e)) }
            }
        }
    }

    private fun translate(e: Exception): String = when (e) {
        is FirebaseAuthWeakPasswordException -> "Contraseña demasiado débil"
        is FirebaseAuthInvalidCredentialsException -> "Credenciales inválidas"
        is FirebaseAuthInvalidUserException -> "Usuario no encontrado"
        is FirebaseAuthUserCollisionException -> "Este email ya está registrado"
        else -> e.localizedMessage ?: "Error desconocido"
    }
}
