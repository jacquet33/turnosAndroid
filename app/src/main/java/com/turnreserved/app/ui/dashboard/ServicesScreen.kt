package com.turnreserved.app.ui.dashboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.turnreserved.app.data.model.Service
import com.turnreserved.app.ui.auth.AuthViewModel
import com.turnreserved.app.ui.auth.AppTextField
import com.turnreserved.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ServicesScreen(authViewModel: AuthViewModel) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var services by remember { mutableStateOf<List<Service>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var scheduleService by remember { mutableStateOf<Service?>(null) }

    fun load() {
        scope.launch {
            isLoading = true
            try { services = withContext(Dispatchers.IO) { authViewModel.api.listServices() } }
            catch (_: Exception) {}
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Mis Servicios", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            FilledIconButton(
                onClick = { showAddDialog = true },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Primary),
                shape = RoundedCornerShape(10.dp)
            ) { Icon(Icons.Outlined.Add, contentDescription = "Agregar") }
        }
        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (services.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Work, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No tenés servicios creados", color = TextSecondary)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(services, key = { it.id }) { service ->
                    ServiceCard(service, authViewModel, context, onRefresh = { load() }, onSchedule = { scheduleService = service })
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = Surface,
            title = { Text("Nuevo Servicio", color = TextPrimary) },
            text = {
                AppTextField(value = newName, onValueChange = { newName = it }, placeholder = "Nombre del servicio", icon = Icons.Outlined.Work)
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) { authViewModel.api.createService(newName) }
                            newName = ""; showAddDialog = false; load()
                        } catch (_: Exception) {}
                    }
                }) { Text("Crear", color = Primary) }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancelar", color = TextSecondary) } }
        )
    }

    scheduleService?.let { svc ->
        ScheduleConfigScreen(service = svc, authViewModel = authViewModel, onDismiss = { scheduleService = null })
    }
}

@Composable
private fun ServiceCard(service: Service, authViewModel: AuthViewModel, context: Context, onRefresh: () -> Unit, onSchedule: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(service.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            if (service.hasSchedule) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = if (service.hasSchedule) Success else Warning,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            if (service.hasSchedule) "Configurado" else "Sin horarios",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (service.hasSchedule) Success else Warning
                        )
                    }
                }

                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Opciones", tint = TextSecondary)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Configurar horarios") },
                            leadingIcon = { Icon(Icons.Outlined.Schedule, contentDescription = null) },
                            onClick = { onSchedule(); expanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Copiar link") },
                            leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null) },
                            onClick = {
                                val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clip.setPrimaryClip(ClipData.newPlainText("link", "https://turnreserved.com/book/${service.linkId}"))
                                Toast.makeText(context, "Link copiado", Toast.LENGTH_SHORT).show()
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar", color = Destructive) },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = Destructive) },
                            onClick = {
                                scope.launch {
                                    try { withContext(Dispatchers.IO) { authViewModel.api.deleteService(service.id) }; onRefresh() }
                                    catch (_: Exception) {}
                                }
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
