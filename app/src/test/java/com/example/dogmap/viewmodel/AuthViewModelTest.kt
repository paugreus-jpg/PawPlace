package com.example.dogmap.viewmodel

import com.example.dogmap.data.repository.UserRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: AuthViewModel
    private lateinit var mockAuth: FirebaseAuth
    private val mockUserRepository = mockk<UserRepository>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(FirebaseAuth::class)
        mockAuth = mockk(relaxed = true)
        every { FirebaseAuth.getInstance() } returns mockAuth
        viewModel = AuthViewModel(mockUserRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // ── Companion: validEmail ──────────────────────────────────────────────────

    @Test fun `validEmail accepts standard address`() =
        assertTrue(AuthViewModel.validEmail("user@example.com"))

    @Test fun `validEmail accepts subdomain address`() =
        assertTrue(AuthViewModel.validEmail("a@b.co"))

    @Test fun `validEmail accepts plus tag`() =
        assertTrue(AuthViewModel.validEmail("user+tag@example.org"))

    @Test fun `validEmail trims whitespace before checking`() =
        assertTrue(AuthViewModel.validEmail("  user@example.com  "))

    @Test fun `validEmail rejects missing at sign`() =
        assertFalse(AuthViewModel.validEmail("noatsign.com"))

    @Test fun `validEmail rejects missing domain part`() =
        assertFalse(AuthViewModel.validEmail("user@"))

    @Test fun `validEmail rejects single-char TLD`() =
        assertFalse(AuthViewModel.validEmail("a@b.c"))

    @Test fun `validEmail rejects empty string`() =
        assertFalse(AuthViewModel.validEmail(""))

    @Test fun `validEmail rejects whitespace only`() =
        assertFalse(AuthViewModel.validEmail("   "))

    // ── Companion: validPassword ───────────────────────────────────────────────

    @Test fun `validPassword accepts 8-char alphanumeric`() =
        assertTrue(AuthViewModel.validPassword("abcd1234"))

    @Test fun `validPassword accepts password with uppercase`() =
        assertTrue(AuthViewModel.validPassword("Abcdef12"))

    @Test fun `validPassword rejects fewer than 8 chars`() =
        assertFalse(AuthViewModel.validPassword("ab12"))

    @Test fun `validPassword rejects no digits`() =
        assertFalse(AuthViewModel.validPassword("abcdefgh"))

    @Test fun `validPassword rejects no letters`() =
        assertFalse(AuthViewModel.validPassword("12345678"))

    @Test fun `validPassword rejects empty string`() =
        assertFalse(AuthViewModel.validPassword(""))

    @Test fun `validPassword rejects exactly 7 chars with letters and digits`() =
        assertFalse(AuthViewModel.validPassword("abcd123"))

    // ── Companion: validDisplayName ────────────────────────────────────────────

    @Test fun `validDisplayName accepts 2-char name`() =
        assertTrue(AuthViewModel.validDisplayName("Jo"))

    @Test fun `validDisplayName accepts long name`() =
        assertTrue(AuthViewModel.validDisplayName("María José"))

    @Test fun `validDisplayName rejects empty string`() =
        assertFalse(AuthViewModel.validDisplayName(""))

    @Test fun `validDisplayName rejects 1-char name`() =
        assertFalse(AuthViewModel.validDisplayName("J"))

    @Test fun `validDisplayName rejects whitespace only`() =
        assertFalse(AuthViewModel.validDisplayName("  "))

    @Test fun `validDisplayName rejects single space`() =
        assertFalse(AuthViewModel.validDisplayName(" "))

    // ── State update functions ─────────────────────────────────────────────────

    @Test fun `onEmailChange updates email and clears generalError`() = runTest {
        viewModel.onEmailChange("test@test.com")
        assertEquals("test@test.com", viewModel.state.value.email)
        assertNull(viewModel.state.value.generalError)
    }

    @Test fun `onPasswordChange updates password and clears generalError`() = runTest {
        viewModel.onPasswordChange("pass123")
        assertEquals("pass123", viewModel.state.value.password)
        assertNull(viewModel.state.value.generalError)
    }

    @Test fun `onConfirmChange updates confirmPassword and clears generalError`() = runTest {
        viewModel.onConfirmChange("pass123")
        assertEquals("pass123", viewModel.state.value.confirmPassword)
        assertNull(viewModel.state.value.generalError)
    }

    @Test fun `onDisplayNameChange updates displayName and clears generalError`() = runTest {
        viewModel.onDisplayNameChange("Alice")
        assertEquals("Alice", viewModel.state.value.displayName)
        assertNull(viewModel.state.value.generalError)
    }

    // ── Blur functions ─────────────────────────────────────────────────────────

    @Test fun `onEmailBlur marks emailTouched true`() = runTest {
        viewModel.onEmailBlur()
        assertTrue(viewModel.state.value.emailTouched)
    }

    @Test fun `onPasswordBlur marks passwordTouched true`() = runTest {
        viewModel.onPasswordBlur()
        assertTrue(viewModel.state.value.passwordTouched)
    }

    @Test fun `onConfirmBlur marks confirmTouched true`() = runTest {
        viewModel.onConfirmBlur()
        assertTrue(viewModel.state.value.confirmTouched)
    }

    @Test fun `onNameBlur marks nameTouched true`() = runTest {
        viewModel.onNameBlur()
        assertTrue(viewModel.state.value.nameTouched)
    }

    // ── reset ──────────────────────────────────────────────────────────────────

    @Test fun `reset restores state to defaults`() = runTest {
        viewModel.onEmailChange("a@b.com")
        viewModel.onPasswordChange("pass")
        viewModel.onEmailBlur()
        viewModel.reset()
        assertEquals(AuthUiState(), viewModel.state.value)
    }

    // ── emailErrorFor ──────────────────────────────────────────────────────────

    @Test fun `emailErrorFor null when neither touched nor submitted`() {
        assertNull(viewModel.emailErrorFor(AuthUiState(email = "bad")))
    }

    @Test fun `emailErrorFor error when touched with invalid email`() {
        assertEquals(
            "Email no válido",
            viewModel.emailErrorFor(AuthUiState(email = "bad", emailTouched = true))
        )
    }

    @Test fun `emailErrorFor error when submitted with invalid email`() {
        assertEquals(
            "Email no válido",
            viewModel.emailErrorFor(AuthUiState(email = "bad", submitted = true))
        )
    }

    @Test fun `emailErrorFor null when email valid and touched`() {
        assertNull(viewModel.emailErrorFor(AuthUiState(email = "a@b.co", emailTouched = true)))
    }

    // ── passwordErrorFor ───────────────────────────────────────────────────────

    @Test fun `passwordErrorFor null when neither touched nor submitted`() {
        assertNull(viewModel.passwordErrorFor(AuthUiState(password = "bad"), forRegister = true))
    }

    @Test fun `passwordErrorFor register error for weak password when touched`() {
        assertEquals(
            "Mínimo 8 caracteres con letras y números",
            viewModel.passwordErrorFor(AuthUiState(password = "weak", passwordTouched = true), forRegister = true)
        )
    }

    @Test fun `passwordErrorFor register error when submitted with weak password`() {
        assertEquals(
            "Mínimo 8 caracteres con letras y números",
            viewModel.passwordErrorFor(AuthUiState(password = "bad", submitted = true), forRegister = true)
        )
    }

    @Test fun `passwordErrorFor register null for valid password when touched`() {
        assertNull(
            viewModel.passwordErrorFor(AuthUiState(password = "abcd1234", passwordTouched = true), forRegister = true)
        )
    }

    @Test fun `passwordErrorFor login error for blank password when touched`() {
        assertEquals(
            "Contraseña requerida",
            viewModel.passwordErrorFor(AuthUiState(password = "", passwordTouched = true), forRegister = false)
        )
    }

    @Test fun `passwordErrorFor login null for non-blank password when touched`() {
        assertNull(
            viewModel.passwordErrorFor(AuthUiState(password = "anything", passwordTouched = true), forRegister = false)
        )
    }

    // ── confirmErrorFor ────────────────────────────────────────────────────────

    @Test fun `confirmErrorFor null when neither touched nor submitted`() {
        assertNull(viewModel.confirmErrorFor(AuthUiState(password = "a", confirmPassword = "b")))
    }

    @Test fun `confirmErrorFor error when passwords differ and touched`() {
        assertEquals(
            "Las contraseñas no coinciden",
            viewModel.confirmErrorFor(AuthUiState(password = "a", confirmPassword = "b", confirmTouched = true))
        )
    }

    @Test fun `confirmErrorFor error when passwords differ and submitted`() {
        assertEquals(
            "Las contraseñas no coinciden",
            viewModel.confirmErrorFor(AuthUiState(password = "a", confirmPassword = "b", submitted = true))
        )
    }

    @Test fun `confirmErrorFor null when passwords match`() {
        assertNull(
            viewModel.confirmErrorFor(AuthUiState(password = "same", confirmPassword = "same", confirmTouched = true))
        )
    }

    // ── nameErrorFor ───────────────────────────────────────────────────────────

    @Test fun `nameErrorFor null when neither touched nor submitted`() {
        assertNull(viewModel.nameErrorFor(AuthUiState(displayName = "")))
    }

    @Test fun `nameErrorFor error for short name when touched`() {
        assertEquals(
            "Introduce tu nombre",
            viewModel.nameErrorFor(AuthUiState(displayName = "J", nameTouched = true))
        )
    }

    @Test fun `nameErrorFor error when submitted with empty name`() {
        assertEquals(
            "Introduce tu nombre",
            viewModel.nameErrorFor(AuthUiState(displayName = "", submitted = true))
        )
    }

    @Test fun `nameErrorFor null for valid name when touched`() {
        assertNull(viewModel.nameErrorFor(AuthUiState(displayName = "Jo", nameTouched = true)))
    }

    // ── canSubmitLogin ─────────────────────────────────────────────────────────

    @Test fun `canSubmitLogin false when email invalid`() {
        assertFalse(viewModel.canSubmitLogin(AuthUiState(email = "bad", password = "pw")))
    }

    @Test fun `canSubmitLogin false when password blank`() {
        assertFalse(viewModel.canSubmitLogin(AuthUiState(email = "a@b.co", password = "")))
    }

    @Test fun `canSubmitLogin false when loading`() {
        assertFalse(
            viewModel.canSubmitLogin(AuthUiState(email = "a@b.co", password = "pw", isLoading = true))
        )
    }

    @Test fun `canSubmitLogin true when all valid and not loading`() {
        assertTrue(viewModel.canSubmitLogin(AuthUiState(email = "a@b.co", password = "pw")))
    }

    // ── canSubmitRegister ──────────────────────────────────────────────────────

    @Test fun `canSubmitRegister false when email invalid`() {
        assertFalse(
            viewModel.canSubmitRegister(
                AuthUiState(email = "bad", password = "abcd1234", confirmPassword = "abcd1234", displayName = "John")
            )
        )
    }

    @Test fun `canSubmitRegister false when password weak`() {
        assertFalse(
            viewModel.canSubmitRegister(
                AuthUiState(email = "a@b.co", password = "weak", confirmPassword = "weak", displayName = "John")
            )
        )
    }

    @Test fun `canSubmitRegister false when passwords dont match`() {
        assertFalse(
            viewModel.canSubmitRegister(
                AuthUiState(email = "a@b.co", password = "abcd1234", confirmPassword = "different", displayName = "John")
            )
        )
    }

    @Test fun `canSubmitRegister false when name too short`() {
        assertFalse(
            viewModel.canSubmitRegister(
                AuthUiState(email = "a@b.co", password = "abcd1234", confirmPassword = "abcd1234", displayName = "J")
            )
        )
    }

    @Test fun `canSubmitRegister false when loading`() {
        assertFalse(
            viewModel.canSubmitRegister(
                AuthUiState(
                    email = "a@b.co", password = "abcd1234",
                    confirmPassword = "abcd1234", displayName = "John", isLoading = true
                )
            )
        )
    }

    @Test fun `canSubmitRegister true when all valid`() {
        assertTrue(
            viewModel.canSubmitRegister(
                AuthUiState(email = "a@b.co", password = "abcd1234", confirmPassword = "abcd1234", displayName = "John")
            )
        )
    }

    // ── login() early return ───────────────────────────────────────────────────

    @Test fun `login with invalid state sets submitted but skips Firebase call`() = runTest {
        viewModel.login()
        assertTrue(viewModel.state.value.submitted)
        assertFalse(viewModel.state.value.isLoading)
        assertFalse(viewModel.state.value.isAuthenticated)
    }

    // ── login() — success path ─────────────────────────────────────────────────

    @Test fun `login success sets isAuthenticated and clears loading`() = runTest {
        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("pass123")

        val mockTask = mockk<Task<AuthResult>>()
        val mockResult = mockk<AuthResult>()
        val mockUser = mockk<FirebaseUser>()
        every { mockUser.uid } returns "uid123"
        every { mockUser.email } returns "user@example.com"
        every { mockResult.user } returns mockUser
        every { mockAuth.signInWithEmailAndPassword("user@example.com", "pass123") } returns mockTask
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        coEvery { mockTask.await() } returns mockResult

        viewModel.login()

        assertTrue(viewModel.state.value.isAuthenticated)
        assertFalse(viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.generalError)
    }

    // ── login() — error paths via translate() ──────────────────────────────────

    @Test fun `login translate FirebaseAuthInvalidCredentialsException`() = runTest {
        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("wrongpass")
        every { mockAuth.signInWithEmailAndPassword(any(), any()) } throws
            mockk<FirebaseAuthInvalidCredentialsException>(relaxed = true)
        viewModel.login()
        assertEquals("Credenciales inválidas", viewModel.state.value.generalError)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test fun `login translate FirebaseAuthInvalidUserException`() = runTest {
        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("pass123")
        every { mockAuth.signInWithEmailAndPassword(any(), any()) } throws
            mockk<FirebaseAuthInvalidUserException>(relaxed = true)
        viewModel.login()
        assertEquals("Usuario no encontrado", viewModel.state.value.generalError)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test fun `login translate generic exception with message`() = runTest {
        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("pass123")
        every { mockAuth.signInWithEmailAndPassword(any(), any()) } throws RuntimeException("Network failure")
        viewModel.login()
        assertEquals("Network failure", viewModel.state.value.generalError)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test fun `login translate generic exception with null message returns default error`() = runTest {
        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("pass123")
        every { mockAuth.signInWithEmailAndPassword(any(), any()) } throws RuntimeException(null as String?)
        viewModel.login()
        assertEquals("Error desconocido", viewModel.state.value.generalError)
        assertFalse(viewModel.state.value.isLoading)
    }

    // ── register() early return ────────────────────────────────────────────────

    @Test fun `register with invalid state sets submitted but skips Firebase call`() = runTest {
        viewModel.register()
        assertTrue(viewModel.state.value.submitted)
        assertFalse(viewModel.state.value.isLoading)
        assertFalse(viewModel.state.value.isAuthenticated)
    }

    // ── register() — success path ──────────────────────────────────────────────

    @Test fun `register success sets isAuthenticated and clears loading`() = runTest {
        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("validpass1")
        viewModel.onConfirmChange("validpass1")
        viewModel.onDisplayNameChange("John")

        val mockTask = mockk<Task<AuthResult>>()
        val mockResult = mockk<AuthResult>()
        val mockUser = mockk<FirebaseUser>()
        every { mockUser.uid } returns "uid123"
        every { mockResult.user } returns mockUser
        every { mockAuth.createUserWithEmailAndPassword("user@example.com", "validpass1") } returns mockTask
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        coEvery { mockTask.await() } returns mockResult

        viewModel.register()

        assertTrue(viewModel.state.value.isAuthenticated)
        assertFalse(viewModel.state.value.isLoading)
    }

    // ── register() — error paths via translate() ───────────────────────────────

    @Test fun `register translate FirebaseAuthWeakPasswordException`() = runTest {
        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("validpass1")
        viewModel.onConfirmChange("validpass1")
        viewModel.onDisplayNameChange("John")
        every { mockAuth.createUserWithEmailAndPassword(any(), any()) } throws
            mockk<FirebaseAuthWeakPasswordException>(relaxed = true)
        viewModel.register()
        assertEquals("Contraseña demasiado débil", viewModel.state.value.generalError)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test fun `register translate FirebaseAuthUserCollisionException`() = runTest {
        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("validpass1")
        viewModel.onConfirmChange("validpass1")
        viewModel.onDisplayNameChange("John")
        every { mockAuth.createUserWithEmailAndPassword(any(), any()) } throws
            mockk<FirebaseAuthUserCollisionException>(relaxed = true)
        viewModel.register()
        assertEquals("Este email ya está registrado", viewModel.state.value.generalError)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test fun `register translate generic exception with message`() = runTest {
        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("validpass1")
        viewModel.onConfirmChange("validpass1")
        viewModel.onDisplayNameChange("John")
        every { mockAuth.createUserWithEmailAndPassword(any(), any()) } throws RuntimeException("Server error")
        viewModel.register()
        assertEquals("Server error", viewModel.state.value.generalError)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test fun `register translate generic exception with null message returns default error`() = runTest {
        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("validpass1")
        viewModel.onConfirmChange("validpass1")
        viewModel.onDisplayNameChange("John")
        every { mockAuth.createUserWithEmailAndPassword(any(), any()) } throws RuntimeException(null as String?)
        viewModel.register()
        assertEquals("Error desconocido", viewModel.state.value.generalError)
        assertFalse(viewModel.state.value.isLoading)
    }
}
