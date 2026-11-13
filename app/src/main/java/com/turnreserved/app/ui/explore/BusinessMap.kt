package com.turnreserved.app.ui.explore

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.turnreserved.app.data.model.Business
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Icon
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

/** Estilo vectorial gratuito de OpenFreeMap (datos OpenStreetMap). Sin API key ni límites. */
const val MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

data class CameraTarget(val lat: Double, val lng: Double, val radiusKm: Double, val id: Long = System.nanoTime())

fun groupColor(group: String?): Color = when (group) {
    "belleza" -> Color(0xFFE8549E)
    "salud" -> Color(0xFF21B38C)
    "legal" -> Color(0xFFD9A633)
    else -> Color(0xFF0069CC)
}

/** Crea y administra un MapView de MapLibre respetando el ciclo de vida de Compose. */
@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply { onCreate(null) }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        // Si ya estamos activos, sincronizar estado
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStart()
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onResume()
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }
    return mapView
}

@Composable
fun BusinessMap(
    businesses: List<Business>,
    selectedId: String?,
    cameraTarget: CameraTarget?,
    onSelect: (String) -> Unit,
    onUserMoved: (lat: Double, lng: Double, radiusKm: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mapView = rememberMapViewWithLifecycle()
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    val markers = remember { mutableMapOf<String, Marker>() }
    val icons = remember { mutableMapOf<String, Icon>() }
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentOnUserMoved by rememberUpdatedState(onUserMoved)

    LaunchedEffect(mapView) {
        mapView.getMapAsync { m ->
            m.setStyle(MAP_STYLE_URL) {
                m.uiSettings.isRotateGesturesEnabled = false
                m.uiSettings.isTiltGesturesEnabled = false
                var userGesture = false
                m.addOnCameraMoveStartedListener { reason ->
                    userGesture = reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE
                }
                m.addOnCameraIdleListener {
                    if (!userGesture) return@addOnCameraIdleListener
                    userGesture = false
                    val target = m.cameraPosition.target ?: return@addOnCameraIdleListener
                    val bounds = m.projection.visibleRegion.latLngBounds
                    val latKm = bounds.latitudeSpan * 111.32
                    val lngKm = bounds.longitudeSpan * 111.32 * cos(Math.toRadians(target.latitude))
                    currentOnUserMoved(target.latitude, target.longitude, min(max(min(latKm, lngKm) / 2, 0.5), 50.0))
                }
                m.setOnMarkerClickListener { marker ->
                    markers.entries.firstOrNull { it.value == marker }?.key?.let { currentOnSelect(it) }
                    false // deja que muestre el título
                }
                map = m
            }
        }
    }

    // Sincronizar marcadores
    LaunchedEffect(map, businesses) {
        val m = map ?: return@LaunchedEffect
        val newIds = businesses.map { it.id }.toSet()
        markers.keys.filter { it !in newIds }.forEach { id -> markers.remove(id)?.let { m.removeMarker(it) } }
        businesses.filter { it.id !in markers }.forEach { b ->
            val icon = icons.getOrPut(b.group ?: "") { markerIcon(context, groupColor(b.group)) }
            markers[b.id] = m.addMarker(
                MarkerOptions().position(LatLng(b.lat, b.lng)).title(b.name).snippet(b.address).icon(icon)
            )
        }
    }

    // Selección desde las tarjetas → mostrar info del marcador
    LaunchedEffect(map, selectedId) {
        val m = map ?: return@LaunchedEffect
        val marker = selectedId?.let { markers[it] } ?: return@LaunchedEffect
        m.selectMarker(marker)
    }

    // Mover cámara
    LaunchedEffect(map, cameraTarget) {
        val m = map ?: return@LaunchedEffect
        val t = cameraTarget ?: return@LaunchedEffect
        val dLat = t.radiusKm / 111.32
        val dLng = t.radiusKm / (111.32 * max(cos(Math.toRadians(t.lat)), 0.01))
        val bounds = LatLngBounds.from(t.lat + dLat, t.lng + dLng, t.lat - dLat, t.lng - dLng)
        m.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 40), 600)
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

/** Pin circular simple del color del rubro (evita depender de assets). */
private fun markerIcon(context: Context, color: Color): Icon {
    val density = context.resources.displayMetrics.density
    val size = (28 * density).toInt()
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val r = size / 2f
    canvas.drawCircle(r, r, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = android.graphics.Color.WHITE })
    canvas.drawCircle(r, r, r - 3 * density, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color.toArgb() })
    canvas.drawCircle(r, r, r / 3.5f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = android.graphics.Color.WHITE })
    return IconFactory.getInstance(context).fromBitmap(bmp)
}
