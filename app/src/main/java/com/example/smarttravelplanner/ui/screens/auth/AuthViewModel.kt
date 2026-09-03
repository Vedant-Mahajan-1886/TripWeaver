package com.example.smarttravelplanner.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Auth stub — currently bypasses login immediately.
 * Wire real Firebase / OAuth here in a future milestone.
 */
@HiltViewModel
class AuthViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState = _uiState.asStateFlow()

    /** Stub: transitions to Success after a short delay to simulate a network call. */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.LoggingIn
            delay(800)
            _uiState.value = AuthUiState.Success
        }
    }

    /** Stub: Google sign-in will be replaced by real Credential Manager call. */
    fun loginWithGoogle() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.LoggingIn
            delay(600)
            _uiState.value = AuthUiState.Success
        }
    }

    /** Stub: account creation will be wired to auth backend. */
    fun createAccount(fullName: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.LoggingIn
            delay(800)
            _uiState.value = AuthUiState.Success
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
