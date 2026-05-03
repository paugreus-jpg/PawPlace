package com.example.dogmap.viewmodel

import org.junit.Assert.*
import org.junit.Test

class AuthUiStateTest {

    @Test fun `default values are all empty or false`() {
        val state = AuthUiState()
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertEquals("", state.confirmPassword)
        assertEquals("", state.displayName)
        assertFalse(state.emailTouched)
        assertFalse(state.passwordTouched)
        assertFalse(state.confirmTouched)
        assertFalse(state.nameTouched)
        assertFalse(state.submitted)
        assertFalse(state.isLoading)
        assertNull(state.generalError)
        assertFalse(state.isAuthenticated)
    }

    @Test fun `constructor stores custom values`() {
        val state = AuthUiState(
            email = "a@b.com",
            password = "pw",
            confirmPassword = "pw",
            displayName = "Alice",
            emailTouched = true,
            passwordTouched = true,
            confirmTouched = true,
            nameTouched = true,
            submitted = true,
            isLoading = true,
            generalError = "Some error",
            isAuthenticated = true
        )
        assertEquals("a@b.com", state.email)
        assertEquals("pw", state.password)
        assertEquals("pw", state.confirmPassword)
        assertEquals("Alice", state.displayName)
        assertTrue(state.emailTouched)
        assertTrue(state.passwordTouched)
        assertTrue(state.confirmTouched)
        assertTrue(state.nameTouched)
        assertTrue(state.submitted)
        assertTrue(state.isLoading)
        assertEquals("Some error", state.generalError)
        assertTrue(state.isAuthenticated)
    }

    @Test fun `copy changes only email`() {
        val original = AuthUiState(email = "old@a.com", password = "pw")
        val copy = original.copy(email = "new@a.com")
        assertEquals("new@a.com", copy.email)
        assertEquals("pw", copy.password)
    }

    @Test fun `copy clears generalError`() {
        val original = AuthUiState(generalError = "error")
        val copy = original.copy(generalError = null)
        assertNull(copy.generalError)
    }

    @Test fun `equality based on all fields`() {
        val a = AuthUiState(email = "a@b.com", password = "pw", submitted = true)
        val b = AuthUiState(email = "a@b.com", password = "pw", submitted = true)
        assertEquals(a, b)
    }

    @Test fun `inequality when email differs`() {
        val a = AuthUiState(email = "a@b.com")
        val b = AuthUiState(email = "x@y.com")
        assertNotEquals(a, b)
    }

    @Test fun `inequality when isLoading differs`() {
        val a = AuthUiState(isLoading = true)
        val b = AuthUiState(isLoading = false)
        assertNotEquals(a, b)
    }

    @Test fun `hashCode consistent with equality`() {
        val a = AuthUiState(email = "test@test.com", isAuthenticated = true)
        val b = AuthUiState(email = "test@test.com", isAuthenticated = true)
        assertEquals(a.hashCode(), b.hashCode())
    }
}
