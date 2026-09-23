package com.turnreserved.app.ui.dashboard

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
import androidx.compose.ui.unit.dp
import com.turnreserved.app.data.model.DayConfig
import com.turnreserved.app.data.model.DaySchedule
import com.turnreserved.app.data.model.Service
import com.turnreserved.app.ui.auth.AuthViewModel
import com.turnreserved.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val DAYS = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
private val DAY_LABELS = mapOf(
    "monday" to "Lunes", "tuesday" to "Martes", "wednesday" to "Miércoles",
    "thursday" to "Jueves", "friday" to "Viernes", "saturday" to "Sábado", "sunday" to "Domingo"
)
private val DURATIONS = listOf(15, 20, 30, 45, 60)
private val HOURS = (6..22).flatMap { h -> listOf(0, 30).map { m -> "%02d:%02d".format(h, m) } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleConfigScreen(service: Service, authViewModel: AuthViewModel, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var schedule by remember { mutableStateOf<MutableMap<String, DaySchedule>>(mutableMapOf()) }
    var dayConfig by remember { mutableStateOf<MutableMap<String, DayConfig>>(mutableMapOf()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    LaunchedEffect(service.id) {
        try {
            val res = withContext(Dispatchers.IO) { authViewModel.api.getSchedule(service.id) }
            schedule = res.schedule.toMutableMap()
            dayConfig = res.dayConfig.toMutableMap()
        } catch (_: Exception) {
            DAYS.forEach { day ->
                schedule[day] = DaySchedule(enabled = false, slots = emptyList())
                dayConfig[day] = DayConfig(startTime = "09:00", endTime = "18:00", duration = 30)
            }
        }
        isLoading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Background,
        sheetMaxWidth = 480.dp
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text("Horarios: ${service.name}", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(Modifier.height(16.dp))

            if (isLoading) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    items(DAYS) { day ->
                        val enabled = schedule[day]?.enabled ?: false
                        val config = dayConfig[day] ?: DayConfig("09:00", "18:00", 30)

                        DayCard(
                            dayName = DAY_LABELS[day] ?: day,
                            enabled = enabled,
                            startTime = config.startTime,
                            endTime = config.endTime,
                            duration = config.duration,
                            onToggle = { on ->
                                schedule = schedule.toMutableMap().apply {
                                    put(day, DaySchedule(enabled = on, slots = schedule[day]?.slots ?: emptyList()))
                                }
                            },
                            onStartChange = { t ->
                                dayConfig = dayConfig.toMutableMap().apply {
                                    put(day, config.copy(startTime = t))
                                }
                            },
                            onEndChange = { t ->
                                dayConfig = dayConfig.toMutableMap().apply {
                                    put(day, config.copy(endTime = t))
                                }
                            },
                            onDurationChange = { d ->
                                dayConfig = dayConfig.toMutableMap().apply {
                                    put(day, config.copy(duration = d))
                                }
                            }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        scope.launch {
                            isSaving = true
                            try {
                                withContext(Dispatchers.IO) {
                                    authViewModel.api.saveSchedule(service.id, schedule, dayConfig)
                                }
                                saved = true
                                kotlinx.coroutines.delay(2000)
                                saved = false
                            } catch (_: Exception) {}
                            isSaving = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (saved) Success else Primary),
                    enabled = !isSaving
                ) {
                    if (isSaving) CircularProgressIndicator(Modifier.size(20.dp), color = TextPrimary, strokeWidth = 2.dp)
                    else if (saved) Text("✓ Guardado")
                    else Text("Guardar horarios")
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DayCard(
    dayName: String,
    enabled: Boolean,
    startTime: String,
    endTime: String,
    duration: Int,
    onToggle: (Boolean) -> Unit,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit,
    onDurationChange: (Int) -> Unit
) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardBg)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(dayName, style = MaterialTheme.typography.titleSmall, color = TextPrimary, modifier = Modifier.weight(1f))
                Switch(
                    checked = enabled, onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(checkedTrackColor = Primary, checkedThumbColor = TextPrimary)
                )
            }

            if (enabled) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeDropdown("Inicio", startTime, onStartChange, Modifier.weight(1f))
                    TimeDropdown("Fin", endTime, onEndChange, Modifier.weight(1f))
                    DurationDropdown(duration, onDurationChange, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TimeDropdown(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Box {
            Surface(
                onClick = { expanded = true },
                shape = RoundedCornerShape(8.dp),
                color = CardBg
            ) {
                Text(value, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                HOURS.forEach { h ->
                    DropdownMenuItem(text = { Text(h) }, onClick = { onChange(h); expanded = false })
                }
            }
        }
    }
}

@Composable
private fun DurationDropdown(value: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        Text("Duración", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Box {
            Surface(
                onClick = { expanded = true },
                shape = RoundedCornerShape(8.dp),
                color = CardBg
            ) {
                Text("${value}m", modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DURATIONS.forEach { d ->
                    DropdownMenuItem(text = { Text("$d min") }, onClick = { onChange(d); expanded = false })
                }
            }
        }
    }
}
