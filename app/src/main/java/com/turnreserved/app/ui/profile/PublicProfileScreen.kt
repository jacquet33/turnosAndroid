package com.turnreserved.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.turnreserved.app.data.api.ApiService
import com.turnreserved.app.data.model.CategoryGroup
import com.turnreserved.app.data.model.ProfileUpdate
import com.turnreserved.app.data.model.PublicProfile
import com.turnreserved.app.ui.auth.AppTextField
import com.turnreserved.app.ui.explore.MAP_STYLE_URL
import com.turnreserved.app.ui.explore.categoryIcon
import com.turnreserved.app.ui.explore.rememberMapViewWithLifecycle
import com.turnreserved.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap

/** Pestaña "Perfil" del panel: datos públicos y ubicación en el mapa de la app. */
@Composable
fun PublicProfileScreen(api: ApiService) {
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<PublicProfile?>(null) }
    var groups by remember { mutableStateOf<List<CategoryGroup>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var messageIsError by remember { mutableStateOf(false) }
    // Centro del mapa y si el dueño lo movió a mano
    var pin by remember { mutableStateOf<LatLng?>(null) }
    var pinAdjusted by remember { mutableStateOf(false) }
    var recenter by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        try {
            profile = withContext(Dispatchers.IO) { api.getProfile() }
            groups = runCatching { withContext(Dispatchers.IO) { api.exploreCategories() } }.getOrDefault(emptyList())
            recenter++
        } catch (e: Exception) {
            message = e.message; messageIsError = true
        }
        isLoading = false
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Primary) }
        return
    }
    val p = profile ?: run {
        Box(Modifier.fillMaxSize().padding(20.dp)) { Text(message ?: "No se pudo cargar el perfil", color = Destructive) }
        return
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardBg)
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Aparecer en el mapa", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text("Los clientes te encuentran por rubro y ciudad en la app",
                    color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = p.isListed, onCheckedChange = { profile = p.copy(isListed = it) },
                colors = SwitchDefaults.colors(checkedTrackColor = Primary))
        }

        SectionTitle("Tu negocio")
        AppTextField(p.businessName, { profile = p.copy(businessName = it) }, "Nombre del negocio", Icons.Outlined.Storefront)
        Spacer(Modifier.height(10.dp))
        CategoryPicker(groups, p.category) { profile = p.copy(category = it) }
        Spacer(Modifier.height(10.dp))
        AppTextField(p.description, { profile = p.copy(description = it.take(500)) },
            "Descripción breve (qué hacés, especialidades)", Icons.Outlined.Notes)
        Spacer(Modifier.height(10.dp))
        AppTextField(p.publicPhone, { profile = p.copy(publicPhone = it) }, "WhatsApp público (opcional)",
            Icons.Outlined.Chat, KeyboardType.Phone)

        SectionTitle("Ubicación")
        AppTextField(p.address, { profile = p.copy(address = it) }, "Calle y número", Icons.Outlined.Place)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) { AppTextField(p.city, { profile = p.copy(city = it) }, "Ciudad", Icons.Outlined.LocationCity) }
            Box(Modifier.weight(1f)) { AppTextField(p.province, { profile = p.copy(province = it) }, "Provincia", Icons.Outlined.Map) }
        }
        Spacer(Modifier.height(10.dp))
        PinMap(
            initial = p.lat?.let { lat -> p.lng?.let { LatLng(lat, it) } },
            recenterKey = recenter,
            onCenterChanged = { center, byUser ->
                pin = center
                if (byUser) pinAdjusted = true
            },
            modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp))
        )
        Text(
            if (pinAdjusted) "Usaremos la posición del pin."
            else "Guardá para ubicar la dirección automáticamente, o mové el mapa para ajustar el pin.",
            color = TextSecondary, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 6.dp)
        )

        message?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = if (messageIsError) Destructive else Success, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                scope.launch {
                    isSaving = true; message = null
                    try {
                        val updated = withContext(Dispatchers.IO) {
                            api.updateProfile(ProfileUpdate(
                                businessName = p.businessName, category = p.category, description = p.description,
                                address = p.address, city = p.city, province = p.province,
                                publicPhone = p.publicPhone, isListed = p.isListed,
                                lat = if (pinAdjusted) pin?.latitude else null,
                                lng = if (pinAdjusted) pin?.longitude else null
                            ))
                        }
                        profile = updated
                        pinAdjusted = false
                        recenter++
                        if (updated.geocodeWarning != null) { message = updated.geocodeWarning; messageIsError = true }
                        else { message = if (updated.isListed) "¡Listo! Ya aparecés en el mapa." else "Perfil guardado."; messageIsError = false }
                    } catch (e: Exception) {
                        message = e.message; messageIsError = true
                    }
                    isSaving = false
                }
            },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            if (isSaving) CircularProgressIndicator(Modifier.size(20.dp), color = TextPrimary, strokeWidth = 2.dp)
            else Text("Guardar perfil", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = TextSecondary, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 20.dp, bottom = 10.dp))
}

@Composable
private fun CategoryPicker(groups: List<CategoryGroup>, selected: String?, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val name = groups.flatMap { it.categories }.firstOrNull { it.id == selected }?.name
    Box {
        Row(
            Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(12.dp)).background(CardBg)
                .border(1.dp, CardBorder, RoundedCornerShape(12.dp)).clickable { open = true }.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Sell, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(name ?: "Elegí tu rubro", color = if (name == null) TextTertiary else TextPrimary, modifier = Modifier.weight(1f))
            Icon(Icons.Outlined.ExpandMore, null, tint = TextSecondary)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.background(Surface)) {
            groups.forEach { g ->
                Text(g.name, color = TextSecondary, style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                g.categories.forEach { c ->
                    DropdownMenuItem(
                        text = { Text(c.name, color = TextPrimary) },
                        leadingIcon = { Icon(categoryIcon(c.icon), null, tint = TextSecondary) },
                        onClick = { onSelect(c.id); open = false }
                    )
                }
            }
        }
    }
}

/** Mapa con pin fijo al centro: el dueño mueve el mapa para ubicar su local. */
@Composable
private fun PinMap(
    initial: LatLng?,
    recenterKey: Int,
    onCenterChanged: (LatLng, Boolean) -> Unit,
    modifier: Modifier
) {
    val mapView = rememberMapViewWithLifecycle()
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    val currentOnCenter by rememberUpdatedState(onCenterChanged)

    LaunchedEffect(mapView) {
        mapView.getMapAsync { m ->
            m.setStyle(MAP_STYLE_URL) {
                m.uiSettings.isRotateGesturesEnabled = false
                m.uiSettings.isTiltGesturesEnabled = false
                var byUser = false
                m.addOnCameraMoveStartedListener { reason ->
                    byUser = reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE
                }
                m.addOnCameraIdleListener {
                    m.cameraPosition.target?.let { currentOnCenter(it, byUser) }
                    byUser = false
                }
                map = m
            }
        }
    }

    LaunchedEffect(map, recenterKey) {
        val m = map ?: return@LaunchedEffect
        val target = initial ?: LatLng(-34.6037, -58.3816)
        m.moveCamera(CameraUpdateFactory.newLatLngZoom(target, if (initial != null) 16.0 else 11.0))
    }

    Box(modifier) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        Icon(Icons.Outlined.Place, contentDescription = null, tint = Destructive,
            modifier = Modifier.align(Alignment.Center).offset(y = (-18).dp).size(40.dp))
    }
}
