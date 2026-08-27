package com.turnreserved.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.lifecycle.viewmodel.compose.viewModel
import com.turnreserved.app.ui.auth.AuthViewModel
import com.turnreserved.app.ui.auth.LoginScreen
import com.turnreserved.app.ui.dashboard.DashboardScreen
import com.turnreserved.app.ui.subscription.SubscriptionScreen
import com.turnreserved.app.ui.theme.Primary
import com.turnreserved.app.ui.theme.TurnReservedTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TurnReservedTheme {
                val authViewModel: AuthViewModel = viewModel()
                val state by authViewModel.state.collectAsState()

                when {
                    state.isLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Primary)
                        }
                    }
                    !state.isAuthenticated -> LoginScreen(authViewModel)
                    state.needsSubscription -> SubscriptionScreen(authViewModel)
                    else -> DashboardScreen(authViewModel)
                }
            }
        }
    }
}
