package com.turnreserved.app.ui.explore

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.turnreserved.app.data.model.Business
import com.turnreserved.app.ui.booking.PublicBookingScreen
import com.turnreserved.app.ui.theme.*

/** Ícono Material para cada rubro (íconos genéricos que manda el backend). */
fun categoryIcon(icon: String): ImageVector = when (icon) {
    "scissors" -> Icons.Outlined.ContentCut
    "mustache" -> Icons.Outlined.Face
    "hand" -> Icons.Outlined.PanTool
    "sparkles" -> Icons.Outlined.AutoAwesome
    "leaf" -> Icons.Outlined.Spa
    "eye" -> Icons.Outlined.Visibility
    "stethoscope" -> Icons.Outlined.MedicalServices
    "tooth" -> Icons.Outlined.SentimentSatisfied
    "figure" -> Icons.Outlined.AccessibilityNew
    "apple" -> Icons.Outlined.Restaurant
    "brain" -> Icons.Outlined.Psychology
    "briefcase" -> Icons.Outlined.Work
    "family" -> Icons.Outlined.FamilyRestroom
    "shield" -> Icons.Outlined.Shield
    "doc" -> Icons.Outlined.Description
    "pension" -> Icons.Outlined.Elderly
    "scroll" -> Icons.Outlined.HistoryEdu
    "car" -> Icons.Outlined.DirectionsCar
    "stamp" -> Icons.Outlined.Verified
    else -> Icons.Outlined.Place
}

fun groupIcon(group: String?): ImageVector = when (group) {
    "belleza" -> Icons.Outlined.ContentCut
    "salud" -> Icons.Outlined.MedicalServices
    "legal" -> Icons.Outlined.Gavel
    else -> Icons.Outlined.Place
}

private sealed interface ClientRoute {
    data object Map : ClientRoute
    data class Detail(val business: Business) : ClientRoute
    data class Booking(val linkId: String) : ClientRoute
}

/**
 * Modo cliente completo: mapa → ficha del negocio → reserva.
 * [accountLabel]/[onAccount]: "Soy profesional" (abre login) o "Mi panel" (vuelve al dashboard).
 */
@Composable
fun ClientExplore(accountLabel: String, onAccount: () -> Unit, onClose: (() -> Unit)? = null) {
    val vm: ExploreViewModel = viewModel()
    var stack by remember { mutableStateOf(listOf<ClientRoute>(ClientRoute.Map)) }
    fun push(r: ClientRoute) { stack = stack + r }
    fun pop() { stack = stack.dropLast(1) }

    BackHandler(enabled = stack.size > 1 || onClose != null) {
        if (stack.size > 1) pop() else onClose?.invoke()
    }

    when (val route = stack.last()) {
        ClientRoute.Map -> ExploreMapScreen(vm, accountLabel, onAccount) { push(ClientRoute.Detail(it)) }
        is ClientRoute.Detail -> BusinessDetailScreen(route.business, onBack = ::pop) { push(ClientRoute.Booking(it)) }
        is ClientRoute.Booking -> PublicBookingScreen(route.linkId, onBack = ::pop)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExploreMapScreen(
    vm: ExploreViewModel,
    accountLabel: String,
    onAccount: () -> Unit,
    onOpen: (Business) -> Unit
) {
    val state by vm.state.collectAsState()
    var showCitySheet by remember { mutableStateOf(false) }
    var showList by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { vm.start() }
    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        when {
            com.turnreserved.app.util.LocationHelper.hasPermission(ctx) || vm.askedPermission -> vm.start()
            else -> {
                vm.askedPermission = true
                permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Background)) {
        BusinessMap(
            businesses = state.results,
            selectedId = state.selectedId,
            cameraTarget = state.cameraTarget,
            onSelect = vm::select,
            onUserMoved = vm::onUserMoved,
            modifier = Modifier.fillMaxSize()
        )

        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp)) {
            // Header
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(14.dp))
                        .background(Surface.copy(alpha = 0.92f)).clickable { showCitySheet = true }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Search, null, tint = TextPrimary)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Buscar en", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(state.placeLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                            color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (state.isLoading) CircularProgressIndicator(Modifier.size(18.dp), color = Primary, strokeWidth = 2.dp)
                }
                Spacer(Modifier.width(8.dp))
                SquareButton(Icons.Outlined.MyLocation, "Mi ubicación", Surface.copy(alpha = 0.92f)) { vm.centerOnUser() }
                Spacer(Modifier.width(8.dp))
                SquareButton(Icons.Outlined.AccountCircle, accountLabel, Primary, onAccount)
            }

            Spacer(Modifier.height(10.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { MapChip("Todos", Icons.Outlined.GridView, state.selectedGroup == null) { vm.selectGroup(null) } }
                items(state.groups, key = { it.id }) { g ->
                    MapChip(g.name, groupIcon(g.id), state.selectedGroup == g.id) { vm.selectGroup(g.id) }
                }
            }
            if (state.subcategories.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(state.subcategories, key = { it.id }) { c ->
                        MapChip(c.name, categoryIcon(c.icon), state.selectedCategory == c.id, small = true) { vm.selectCategory(c.id) }
                    }
                }
            }
            AnimatedVisibility(state.showSearchHere, Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp)) {
                Button(onClick = vm::searchHere, colors = ButtonDefaults.buttonColors(containerColor = Primary), shape = CircleShape) {
                    Icon(Icons.Outlined.Refresh, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                    Text("Buscar en esta zona", fontWeight = FontWeight.SemiBold)
                }
            }
            state.error?.let { err ->
                Text(err, color = Color.White, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp).clip(RoundedCornerShape(10.dp))
                        .background(Destructive.copy(alpha = 0.9f)).clickable { vm.dismissError() }.padding(10.dp))
            }
        }

        // Tarjetas inferiores
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp)) {
            Row(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(CircleShape)
                    .background(Surface.copy(alpha = 0.92f)).padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val n = state.results.size
                Text(if (n == 0) "Sin resultados en esta zona" else "$n lugar${if (n == 1) "" else "es"} para reservar",
                    color = TextPrimary, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                if (n > 0) Text("Ver lista", color = Primary, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { showList = true })
            }
            if (state.results.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                val listState = rememberLazyListState()
                LaunchedEffect(state.selectedId) {
                    val idx = state.results.indexOfFirst { it.id == state.selectedId }
                    if (idx >= 0) listState.animateScrollToItem(idx)
                }
                LazyRow(state = listState, contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.results, key = { it.id }) { b ->
                        BusinessCard(b, highlighted = b.id == state.selectedId) { onOpen(b) }
                    }
                }
            }
        }
    }

    if (showCitySheet) {
        ModalBottomSheet(onDismissRequest = { showCitySheet = false }, containerColor = Surface) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
                Text("¿Dónde buscás?", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { showCitySheet = false; vm.centerOnUser() }) {
                    Icon(Icons.Outlined.MyLocation, null); Spacer(Modifier.width(8.dp)); Text("Usar mi ubicación")
                }
                OutlinedTextField(value = state.cityQuery, onValueChange = vm::onCityQuery, singleLine = true,
                    label = { Text("Ciudad (ej: Colón, Paraná, Rosario)") }, modifier = Modifier.fillMaxWidth())
                state.cityResults.forEach { c ->
                    Column(Modifier.fillMaxWidth().clickable { vm.chooseCity(c); showCitySheet = false }.padding(vertical = 10.dp)) {
                        Text(c.name, color = TextPrimary)
                        Text(listOfNotNull(c.department, c.province).joinToString(", "),
                            color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = state.query, onValueChange = vm::setQuery, singleLine = true,
                    label = { Text("Nombre del lugar o servicio") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Button(onClick = { vm.search(); showCitySheet = false }, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text("Buscar") }
            }
        }
    }

    if (showList) {
        ModalBottomSheet(onDismissRequest = { showList = false }, containerColor = Surface) {
            LazyColumn(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                items(state.results, key = { it.id }) { b ->
                    Row(Modifier.fillMaxWidth().clickable { showList = false; onOpen(b) }.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(groupIcon(b.group), null, tint = groupColor(b.group))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(b.name, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text(b.fullAddress, color = TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        }
                        b.distanceText?.let { Text(it, color = TextSecondary, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SquareButton(icon: ImageVector, label: String, bg: Color, onClick: () -> Unit) {
    Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = label, tint = Color.White)
    }
}

@Composable
private fun MapChip(title: String, icon: ImageVector, selected: Boolean, small: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.clip(CircleShape).background(if (selected) Primary else Surface.copy(alpha = 0.92f))
            .clickable(onClick = onClick)
            .padding(horizontal = if (small) 10.dp else 14.dp, vertical = if (small) 6.dp else 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(if (small) 14.dp else 18.dp))
        Spacer(Modifier.width(6.dp))
        Text(title, color = Color.White, style = if (small) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun BusinessCard(b: Business, highlighted: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.width(270.dp).clip(RoundedCornerShape(16.dp)).background(Surface.copy(alpha = 0.95f))
            .border(if (highlighted) 2.dp else 1.dp, if (highlighted) Primary else CardBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(groupIcon(b.group), null, tint = groupColor(b.group), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(b.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(4.dp))
        Text(b.fullAddress, color = TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            b.distanceText?.let { Text(it, color = TextSecondary, style = MaterialTheme.typography.labelSmall) }
            Spacer(Modifier.weight(1f))
            Text("Reservar", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(CircleShape).background(Primary).padding(horizontal = 12.dp, vertical = 6.dp))
        }
    }
}

@Composable
private fun BusinessDetailScreen(b: Business, onBack: () -> Unit, onBook: (String) -> Unit) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Surface, Background), radius = 800f))
            .statusBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver", tint = TextPrimary) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(groupColor(b.group)), contentAlignment = Alignment.Center) {
                Icon(groupIcon(b.group), null, tint = Color.White)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(b.name, style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                Text(b.fullAddress, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                b.distanceText?.let { Text("A $it", style = MaterialTheme.typography.bodySmall, color = TextSecondary) }
            }
        }
        if (b.description.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Text(b.description, color = TextSecondary)
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = {
                val uri = Uri.parse("geo:${b.lat},${b.lng}?q=${b.lat},${b.lng}(${Uri.encode(b.name)})")
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
            }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.Directions, null); Spacer(Modifier.width(6.dp)); Text("Cómo llegar")
            }
            b.phone?.let { phone ->
                val digits = phone.filter { it.isDigit() }
                if (digits.length >= 8) {
                    val wa = if (digits.startsWith("54")) digits else "549$digits"
                    OutlinedButton(onClick = {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$wa"))) }
                    }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.Chat, null); Spacer(Modifier.width(6.dp)); Text("WhatsApp")
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("Reservá un turno", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Spacer(Modifier.height(10.dp))
        if (b.services.isEmpty()) Text("Este lugar todavía no tiene servicios disponibles.", color = TextSecondary)
        b.services.forEach { svc ->
            Row(
                Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(14.dp)).background(CardBg)
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp)).clickable { onBook(svc.linkId) }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(svc.name, color = TextPrimary, modifier = Modifier.weight(1f))
                Text("Ver horarios", color = Primary, style = MaterialTheme.typography.labelLarge)
                Icon(Icons.Outlined.ChevronRight, null, tint = TextTertiary)
            }
        }
    }
}
