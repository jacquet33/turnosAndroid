package com.turnreserved.app.ui.explore

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.turnreserved.app.data.api.ApiService
import com.turnreserved.app.data.model.Business
import com.turnreserved.app.data.model.BusinessCategory
import com.turnreserved.app.data.model.CategoryGroup
import com.turnreserved.app.data.model.CityResult
import com.turnreserved.app.util.LocationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ExploreState(
    val groups: List<CategoryGroup> = emptyList(),
    val results: List<Business> = emptyList(),
    val selectedId: String? = null,
    val selectedGroup: String? = null,
    val selectedCategory: String? = null,
    val query: String = "",
    val centerLat: Double = -34.6037,   // CABA por defecto
    val centerLng: Double = -58.3816,
    val radiusKm: Double = 10.0,
    val placeLabel: String = "Cerca tuyo",
    val cameraTarget: CameraTarget? = null,
    val showSearchHere: Boolean = false,
    val cityQuery: String = "",
    val cityResults: List<CityResult> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val subcategories: List<BusinessCategory>
        get() = groups.firstOrNull { it.id == selectedGroup }?.categories ?: emptyList()
}

class ExploreViewModel(app: Application) : AndroidViewModel(app) {

    private val api = ApiService { null }
    private val _state = MutableStateFlow(ExploreState())
    val state: StateFlow<ExploreState> = _state.asStateFlow()

    private var searchJob: Job? = null
    private var cityJob: Job? = null
    private var pending: Triple<Double, Double, Double>? = null
    private var started = false
    /** Para no volver a pedir el permiso cada vez que se vuelve al mapa */
    var askedPermission = false

    /** Llamar después de pedir (o no) el permiso de ubicación. */
    fun start() {
        if (started) return
        started = true
        viewModelScope.launch {
            val groups = runCatching { withContext(Dispatchers.IO) { api.exploreCategories() } }.getOrDefault(emptyList())
            _state.update { it.copy(groups = groups) }
            val loc = LocationHelper.currentLocation(getApplication<Application>())
            if (loc != null) moveTo(loc.latitude, loc.longitude, 5.0, "Cerca tuyo")
            else moveTo(_state.value.centerLat, _state.value.centerLng, 10.0, "Elegí tu ciudad")
        }
    }

    fun centerOnUser() {
        viewModelScope.launch {
            val loc = LocationHelper.currentLocation(getApplication<Application>())
            if (loc != null) moveTo(loc.latitude, loc.longitude, 5.0, "Cerca tuyo")
            else _state.update { it.copy(error = "Activá la ubicación o buscá tu ciudad.") }
        }
    }

    fun chooseCity(city: CityResult) {
        _state.update { it.copy(cityQuery = "", cityResults = emptyList()) }
        moveTo(city.lat, city.lng, 8.0, city.label)
    }

    private fun moveTo(lat: Double, lng: Double, radiusKm: Double, label: String) {
        _state.update {
            it.copy(centerLat = lat, centerLng = lng, radiusKm = radiusKm, placeLabel = label,
                showSearchHere = false, cameraTarget = CameraTarget(lat, lng, radiusKm))
        }
        search()
    }

    /** El usuario movió el mapa: ofrecer "Buscar en esta zona" (estilo Mercado Libre). */
    fun onUserMoved(lat: Double, lng: Double, radiusKm: Double) {
        pending = Triple(lat, lng, radiusKm)
        val s = _state.value
        val results = FloatArray(1)
        android.location.Location.distanceBetween(s.centerLat, s.centerLng, lat, lng, results)
        val movedKm = results[0] / 1000.0
        _state.update {
            it.copy(showSearchHere = movedKm > s.radiusKm * 0.3 || kotlin.math.abs(radiusKm - s.radiusKm) > s.radiusKm * 0.5)
        }
    }

    fun searchHere() {
        val (lat, lng, r) = pending ?: return
        _state.update { it.copy(centerLat = lat, centerLng = lng, radiusKm = r, placeLabel = "Esta zona", showSearchHere = false) }
        search()
    }

    fun selectGroup(id: String?) {
        _state.update { it.copy(selectedGroup = if (it.selectedGroup == id) null else id, selectedCategory = null) }
        search()
    }

    fun selectCategory(id: String) {
        _state.update { it.copy(selectedCategory = if (it.selectedCategory == id) null else id) }
        search()
    }

    fun select(id: String?) = _state.update { it.copy(selectedId = id) }
    fun setQuery(q: String) = _state.update { it.copy(query = q) }
    fun dismissError() = _state.update { it.copy(error = null) }

    fun search() {
        searchJob?.cancel()
        val s = _state.value
        searchJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val res = withContext(Dispatchers.IO) {
                    api.explore(s.centerLat, s.centerLng, s.radiusKm, s.selectedGroup, s.selectedCategory, s.query)
                }
                _state.update { cur ->
                    cur.copy(
                        results = res.results,
                        selectedId = cur.selectedId?.takeIf { id -> res.results.any { it.id == id } },
                        isLoading = false, error = null
                    )
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "No se pudo buscar") }
            }
        }
    }

    fun onCityQuery(q: String) {
        _state.update { it.copy(cityQuery = q) }
        cityJob?.cancel()
        if (q.trim().length < 2) { _state.update { it.copy(cityResults = emptyList()) }; return }
        cityJob = viewModelScope.launch {
            delay(350) // debounce
            val list = runCatching { withContext(Dispatchers.IO) { api.searchCities(q.trim()) } }.getOrDefault(emptyList())
            _state.update { it.copy(cityResults = list) }
        }
    }
}
