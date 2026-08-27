package com.turnreserved.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.turnreserved.app.data.model.Client
import com.turnreserved.app.ui.auth.AuthViewModel
import com.turnreserved.app.ui.auth.AppTextField
import com.turnreserved.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ClientsScreen(authViewModel: AuthViewModel) {
    val scope = rememberCoroutineScope()
    var clients by remember { mutableStateOf<List<Client>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var search by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try { clients = withContext(Dispatchers.IO) { authViewModel.api.listClients() } }
        catch (_: Exception) {}
        isLoading = false
    }

    val filtered = if (search.isBlank()) clients else {
        val q = search.lowercase()
        clients.filter { it.name.lowercase().contains(q) || it.phone.contains(q) || it.dni.contains(q) }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Clientes", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Surface(shape = RoundedCornerShape(50), color = Primary.copy(alpha = 0.15f)) {
                Text("${clients.size}", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall, color = Primary)
            }
        }
        Spacer(Modifier.height(12.dp))

        AppTextField(value = search, onValueChange = { search = it }, placeholder = "Buscar cliente...", icon = Icons.Outlined.Search)

        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.People, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(if (search.isBlank()) "Sin clientes aún" else "Sin resultados", color = TextSecondary)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered) { client ->
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardBg)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(44.dp).clip(CircleShape).background(Primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(client.name.take(1).uppercase(), style = MaterialTheme.typography.titleMedium, color = Primary)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(client.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text("📞 ${client.phone}  ·  DNI: ${client.dni}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${client.totalBookings}", style = MaterialTheme.typography.titleMedium, color = Primary)
                                Text("turnos", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                            }
                        }
                    }
                }
            }
        }
    }
}
