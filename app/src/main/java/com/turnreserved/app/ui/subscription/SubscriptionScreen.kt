package com.turnreserved.app.ui.subscription

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.turnreserved.app.ui.auth.AuthViewModel
import com.turnreserved.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SubscriptionScreen(authViewModel: AuthViewModel) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val plans = listOf(
        Triple("basic", "Basic" to 9, listOf("1 servicio", "Turnos ilimitados", "Link de reserva")),
        Triple("pro", "Pro" to 19, listOf("3 servicios", "Turnos ilimitados", "Link de reserva", "Gestión de clientes")),
        Triple("business", "Business" to 39, listOf("Servicios ilimitados", "Turnos ilimitados", "Todo incluido", "Soporte prioritario"))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(colors = listOf(Surface, Background), radius = 800f))
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))
        Icon(Icons.Outlined.Star, contentDescription = null, tint = Warning, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text("Tu suscripción expiró", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
        Text("Elegí un plan para continuar", color = TextSecondary)
        Spacer(Modifier.height(24.dp))

        plans.forEach { (key, namePrice, features) ->
            val isPopular = key == "pro"
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = if (isPopular) CardDefaults.outlinedCardBorder().copy() else null,
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
            ) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isPopular) {
                        Surface(shape = RoundedCornerShape(50), color = Primary) {
                            Text("POPULAR", modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(namePrice.first, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$${namePrice.second}", style = MaterialTheme.typography.displaySmall, color = TextPrimary)
                        Text("/mes", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                    Spacer(Modifier.height(12.dp))
                    features.forEach { f ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Success, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(f, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    val pref = withContext(Dispatchers.IO) { authViewModel.api.createPreference(key) }
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(pref.initPoint)))
                                } catch (_: Exception) {}
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isPopular) Primary else CardBg)
                    ) { Text("Elegir plan") }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = { authViewModel.logout() }) {
            Text("Cerrar sesión", color = TextSecondary)
        }
    }
}
