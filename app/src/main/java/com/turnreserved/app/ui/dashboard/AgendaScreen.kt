package com.turnreserved.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.turnreserved.app.data.model.Booking
import com.turnreserved.app.ui.auth.AuthViewModel
import com.turnreserved.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun AgendaScreen(authViewModel: AuthViewModel) {
    val scope = rememberCoroutineScope()
    var bookings by remember { mutableStateOf<List<Booking>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var filterStatus by remember { mutableStateOf<String?>(null) }
    val dateStr = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

    fun load() {
        scope.launch {
            isLoading = true
            try { bookings = withContext(Dispatchers.IO) { authViewModel.api.listBookings(date = dateStr, status = filterStatus) } }
            catch (_: Exception) {}
            isLoading = false
        }
    }

    LaunchedEffect(selectedDate, filterStatus) { load() }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Text("Agenda", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Spacer(Modifier.height(12.dp))

        // Date row
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val today = LocalDate.now()
            items(14) { i ->
                val date = today.plusDays(i.toLong())
                val sel = date == selectedDate
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (sel) Primary else CardBg)
                        .clickable { selectedDate = date }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(date.dayOfWeek.name.take(3), style = MaterialTheme.typography.labelSmall, color = if (sel) TextPrimary else TextTertiary)
                    Text("${date.dayOfMonth}", style = MaterialTheme.typography.titleMedium, color = if (sel) TextPrimary else TextSecondary)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Status chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val filters = listOf(null to "Todos", "pending" to "Pendientes", "completed" to "Completados", "cancelled" to "Cancelados")
            items(filters) { (status, label) ->
                FilterChipView(label = label, selected = filterStatus == status) { filterStatus = status }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (bookings.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No hay turnos para esta fecha", color = TextSecondary)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(bookings.sortedBy { it.time }, key = { it.id }) { booking ->
                    BookingCard(booking, authViewModel) { load() }
                }
            }
        }
    }
}

@Composable
private fun BookingCard(booking: Booking, authViewModel: AuthViewModel, onRefresh: () -> Unit) {
    val scope = rememberCoroutineScope()
    val statusColor = when (booking.status) {
        "pending" -> Warning; "completed" -> Success; else -> Destructive
    }
    val statusLabel = when (booking.status) {
        "pending" -> "Pendiente"; "completed" -> "Completado"; else -> "Cancelado"
    }

    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardBg)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(booking.clientName, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Text(booking.serviceName, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
                Text(booking.time, style = MaterialTheme.typography.titleMedium, color = Primary)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("📞 ${booking.clientPhone}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Text("🪪 ${booking.clientDNI}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(50), color = statusColor.copy(alpha = 0.15f)) {
                    Text(statusLabel, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall, color = statusColor)
                }
                Spacer(Modifier.weight(1f))
                if (booking.status == "pending") {
                    IconButton(onClick = {
                        scope.launch {
                            try { withContext(Dispatchers.IO) { authViewModel.api.updateBookingStatus(booking.id, "completed") }; onRefresh() }
                            catch (_: Exception) {}
                        }
                    }) { Icon(Icons.Outlined.CheckCircle, contentDescription = "Completar", tint = Success) }

                    IconButton(onClick = {
                        scope.launch {
                            try { withContext(Dispatchers.IO) { authViewModel.api.updateBookingStatus(booking.id, "cancelled") }; onRefresh() }
                            catch (_: Exception) {}
                        }
                    }) { Icon(Icons.Outlined.Cancel, contentDescription = "Cancelar", tint = Destructive) }
                }
            }
        }
    }
}

@Composable
fun FilterChipView(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) Primary else CardBg
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) TextPrimary else TextSecondary
        )
    }
}
