package com.turnreserved.app.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.turnreserved.app.data.api.ApiService
import com.turnreserved.app.data.model.User
import com.turnreserved.app.util.SecureStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AuthState(
    val user: User? = null,
    val isAuthenticated: Boolean = false,
    val isLoading: Boolean = true,
    val needsSubscription: Boolean = false
)

class AuthViewModel(app: Application) : AndroidViewModel(app) {

    private val storage = SecureStorage(app)
    val api = ApiService { storage.token }

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init { checkSession() }

    fun checkSession() {
        viewModelScope.launch {
            val token = storage.token
            val savedUser = storage.user
            if (token == null || savedUser == null) {
                _state.value = AuthState(isLoading = false)
                return@launch
            }

            try {
                val fresh = withContext(Dispatchers.IO) { api.me() }
                if (!fresh.id.isNullOrEmpty()) {
                    val updated = savedUser.copy(
                        plan = fresh.plan ?: savedUser.plan,
                        subscriptionStatus = fresh.subscription_status ?: savedUser.subscriptionStatus,
                        trialEndsAt = fresh.trial_ends_at,
                        subscriptionEndsAt = fresh.subscription_ends_at,
                        maxServices = fresh.max_services ?: savedUser.maxServices
                    )
                    storage.user = updated
                    val needsSub = updated.subscriptionStatus in listOf("expired", "cancelled")
                    _state.value = AuthState(updated, true, false, needsSub)
                } else {
                    logout()
                }
            } catch (_: Exception) {
                _state.value = AuthState(savedUser, true, false)
            }
        }
    }

    fun login(email: String, password: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val resp = withContext(Dispatchers.IO) { api.login(email, password) }
                handleAuth(resp.token, resp.user)
            } catch (e: Exception) {
                onError(e.message ?: "Error al iniciar sesión")
            }
        }
    }

    fun register(email: String, phone: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { api.register(email, phone, password) }
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Error al registrarse")
            }
        }
    }

    fun logout() {
        storage.clear()
        _state.value = AuthState(isLoading = false)
    }

    private fun handleAuth(token: String, user: User) {
        storage.token = token
        storage.user = user
        val needsSub = user.subscriptionStatus in listOf("expired", "cancelled")
        _state.value = AuthState(user, true, false, needsSub)
    }
}
