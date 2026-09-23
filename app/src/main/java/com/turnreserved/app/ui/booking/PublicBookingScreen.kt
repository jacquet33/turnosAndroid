package com.turnreserved.app.ui.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.turnreserved.app.data.api.ApiService
import com.turnreserved.app.data.model.BookingPage
import com.turnreserved.app.data.model.BookingPayload
import com.turnreserved.app.ui.auth.AppTextField
import com.turnreserved.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun PublicBookingScreen(linkId: String, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val api = remember { ApiService { null } }
    var page by remember { mutableStateOf<BookingPage?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var dni by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var success by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(linkId) {
        try { page = withContext(Dispatchers.IO) { api.getBookingPage(linkId) } }
        catch (_: Exception) {}
        isLoading = false
    }

    val availableSlots: List<String> = remember(page, selectedDate) {
        if (page == null || selectedDate.isEmpty()) return@remember emptyList()
        val date = LocalDate.parse(selectedDate)
        val dayName = date.dayOfWeek.name.lowercase()
        val day = page!!.schedule[dayName] ?: return@remember emptyList()
        if (!day.enabled) return@remember emptyList()
        val booked = page!!.bookedSlots
            .filter { it.date == selectedDate && it.status != "cancelled" }
            .map { it.time }.toSet()
        day.slots.filter { it !in booked }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(colors = listOf(Surface, Background), radius = 800f))
    ) {
        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }

            success -> Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Success, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text("¡Turno reservado!", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                Text("Te esperamos el $selectedDate a las $selectedTime", color = TextSecondary)
                Spacer(Modifier.height(24.dp))
                TextButton(onClick = onBack) { Text("Volver", color = Primary) }
            }

            page != null -> Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(page!!.service.name, style = MaterialTheme.typography.headlineSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(20.dp))

                // Date selection (next 14 days)
                Text("Elegí una fecha", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                val today = LocalDate.now()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(200.dp)
                ) {
                    items((0 until 14).toList()) { i ->
                        val date = today.plusDays(i.toLong())
                        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                        val sel = dateStr == selectedDate
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (sel) Primary else CardBg)
                                .border(1.dp, if (sel) Primary else CardBorder, RoundedCornerShape(12.dp))
                                .clickable { selectedDate = dateStr; selectedTime = "" }
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("es")),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (sel) TextPrimary else TextTertiary
                            )
                            Text(
                                "${date.dayOfMonth}",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (sel) TextPrimary else TextSecondary
                            )
                        }
                    }
                }

                // Time slots
                if (selectedDate.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    Text("Elegí un horario", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
                    Spacer(Modifier.height(8.dp))

                    if (availableSlots.isEmpty()) {
                        Text("No hay horarios disponibles para esta fecha", color = TextTertiary)
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.height(((availableSlots.size / 3 + 1) * 48).dp.coerceAtMost(200.dp))
                        ) {
                            items(availableSlots) { slot ->
                                val sel = slot == selectedTime
                                Surface(
                                    onClick = { selectedTime = slot },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (sel) Primary else CardBg
                                ) {
                                    Text(
                                        slot,
                                        modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (sel) TextPrimary else TextSecondary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // Client form
                if (selectedTime.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    Text("Tus datos", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
                    Spacer(Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        AppTextField(value = name, onValueChange = { name = it }, placeholder = "Nombre completo", icon = Icons.Outlined.Person)
                        AppTextField(value = dni, onValueChange = { dni = it }, placeholder = "DNI", icon = Icons.Outlined.Badge, keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        AppTextField(value = phone, onValueChange = { phone = it }, placeholder = "Celular", icon = Icons.Outlined.Phone, keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
                    }

                    if (error.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(error, color = Destructive, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (name.isBlank() || dni.isBlank() || phone.isBlank()) {
                                error = "Completá todos los campos"; return@Button
                            }
                            scope.launch {
                                isSubmitting = true; error = ""
                                try {
                                    withContext(Dispatchers.IO) {
                                        api.createBooking(linkId, BookingPayload(name, dni, phone, selectedDate, selectedTime))
                                    }
                                    success = true
                                } catch (e: Exception) {
                                    error = e.message ?: "Error al reservar"
                                }
                                isSubmitting = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        enabled = !isSubmitting
                    ) {
                        if (isSubmitting) CircularProgressIndicator(Modifier.size(20.dp), color = TextPrimary, strokeWidth = 2.dp)
                        else Text("Reservar turno", fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(Modifier.height(32.dp))
            }

            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Servicio no encontrado", color = TextSecondary)
            }
        }
    }
}
