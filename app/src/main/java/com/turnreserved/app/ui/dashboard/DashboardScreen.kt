package com.turnreserved.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.turnreserved.app.ui.auth.AuthViewModel
import com.turnreserved.app.ui.theme.*

enum class DashTab(val label: String, val icon: ImageVector) {
    Services("Servicios", Icons.Outlined.Work),
    Agenda("Agenda", Icons.Outlined.CalendarMonth),
    Clients("Clientes", Icons.Outlined.People)
}

@Composable
fun DashboardScreen(authViewModel: AuthViewModel) {
    var selectedTab by remember { mutableStateOf(DashTab.Services) }
    val user = authViewModel.state.collectAsState().value.user

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(colors = listOf(Surface, Background), radius = 800f))
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("TurnReserved", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                user?.let {
                    Text(
                        if (it.plan == "trial") "Prueba gratuita" else "Plan ${it.plan}",
                        style = MaterialTheme.typography.labelSmall, color = TextTertiary
                    )
                }
            }
            IconButton(onClick = { authViewModel.logout() }) {
                Icon(Icons.Outlined.Logout, contentDescription = "Salir", tint = TextSecondary)
            }
        }

        // Content
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                DashTab.Services -> ServicesScreen(authViewModel)
                DashTab.Agenda -> AgendaScreen(authViewModel)
                DashTab.Clients -> ClientsScreen(authViewModel)
            }
        }

        // Bottom nav
        NavigationBar(containerColor = Surface.copy(alpha = 0.8f)) {
            DashTab.entries.forEach { tab ->
                NavigationBarItem(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                    label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Primary,
                        selectedTextColor = Primary,
                        unselectedIconColor = TextTertiary,
                        unselectedTextColor = TextTertiary,
                        indicatorColor = Primary.copy(alpha = 0.12f)
                    )
                )
            }
        }
    }
}
