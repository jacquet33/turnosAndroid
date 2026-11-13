package com.turnreserved.app.data.api

import com.turnreserved.app.data.model.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

class ApiService(private val tokenProvider: () -> String?) {

    companion object {
        private const val BASE_URL = "https://turnreserved.com/api"
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private inline fun <reified T> request(
        path: String,
        method: String = "GET",
        body: String? = null,
        authenticated: Boolean = true
    ): T {
        val url = URL("$BASE_URL$path")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            setRequestProperty("Content-Type", "application/json")
            if (authenticated) {
                tokenProvider()?.let {
                    setRequestProperty("Authorization", "Bearer $it")
                }
            }
            if (body != null) {
                doOutput = true
                outputStream.bufferedWriter().use { it.write(body) }
            }
            connectTimeout = 15000
            readTimeout = 15000
        }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val responseText = stream.bufferedReader().use { it.readText() }

        if (code !in 200..299) {
            val errorMsg = try {
                json.decodeFromString<Map<String, String>>(responseText)["error"] ?: "Error del servidor"
            } catch (_: Exception) { "Error HTTP $code" }
            throw ApiException(errorMsg, code)
        }

        return json.decodeFromString(responseText)
    }

    // Auth
    fun login(email: String, password: String): AuthResponse =
        request("/auth/login", "POST", json.encodeToString(mapOf("email" to email, "password" to password)), false)

    fun register(email: String, phone: String, password: String): RegisterResponse =
        request("/auth/register", "POST", json.encodeToString(mapOf("email" to email, "phone" to phone, "password" to password)), false)

    fun me(): MeResponse = request("/auth/me")

    fun googleAuth(credential: String): AuthResponse =
        request("/auth/google", "POST", json.encodeToString(mapOf("credential" to credential)), false)

    // Services
    fun listServices(): List<Service> = request("/services")

    fun createService(name: String, minAdvanceHours: Int = 0): Service =
        request("/services", "POST", json.encodeToString(mapOf("name" to name, "minAdvanceHours" to minAdvanceHours.toString())))

    fun updateService(id: String, name: String, minAdvanceHours: Int = 0): MessageResponse =
        request("/services/$id", "PUT", json.encodeToString(mapOf("name" to name, "minAdvanceHours" to minAdvanceHours.toString())))

    fun deleteService(id: String): MessageResponse = request("/services/$id", "DELETE")

    fun getSchedule(serviceId: String): ScheduleResponse = request("/services/$serviceId/schedule")

    fun saveSchedule(serviceId: String, schedule: Map<String, DaySchedule>, dayConfig: Map<String, DayConfig>): MessageResponse =
        request("/services/$serviceId/schedule", "PUT",
            json.encodeToString(mapOf("schedule" to json.encodeToString(schedule), "dayConfig" to json.encodeToString(dayConfig))))

    // Bookings
    fun listBookings(date: String? = null, status: String? = null, serviceId: String? = null): List<Booking> {
        val params = mutableListOf<String>()
        date?.let { params.add("date=$it") }
        status?.let { params.add("status=$it") }
        serviceId?.let { params.add("serviceId=$it") }
        val qs = if (params.isEmpty()) "" else "?${params.joinToString("&")}"
        return request("/bookings$qs")
    }

    fun updateBookingStatus(id: String, status: String): StatusUpdateResponse =
        request("/bookings/$id/status", "PATCH", json.encodeToString(mapOf("status" to status)))

    // Public Booking
    fun getBookingPage(linkId: String): BookingPage = request("/book/$linkId", authenticated = false)

    fun createBooking(linkId: String, payload: BookingPayload): BookingCreateResponse =
        request("/book/$linkId", "POST", json.encodeToString(payload), false)

    // Clients
    fun listClients(): List<Client> = request("/clients")

    // Subscription
    fun createPreference(plan: String): PreferenceResponse =
        request("/subscription/create-preference", "POST", json.encodeToString(mapOf("plan" to plan)))

    fun cancelSubscription(): MessageResponse = request("/subscription/cancel", "POST")

    // Explorar (modo cliente, sin auth)
    fun exploreCategories(): List<CategoryGroup> = request("/explore/categories", authenticated = false)

    fun explore(lat: Double, lng: Double, radiusKm: Double, group: String?, category: String?, query: String?): ExploreResponse {
        val params = mutableListOf(
            "lat=${"%.6f".format(Locale.US, lat)}",
            "lng=${"%.6f".format(Locale.US, lng)}",
            "radiusKm=${"%.1f".format(Locale.US, radiusKm)}",
            "limit=100"
        )
        if (category != null) params.add("category=${enc(category)}")
        else if (group != null) params.add("group=${enc(group)}")
        if (!query.isNullOrBlank()) params.add("q=${enc(query.trim())}")
        return request("/explore?${params.joinToString("&")}", authenticated = false)
    }

    fun searchCities(q: String): List<CityResult> = request("/explore/cities?q=${enc(q)}", authenticated = false)

    fun business(publicId: String): Business = request("/explore/business/${enc(publicId)}", authenticated = false)

    // Perfil público del negocio
    fun getProfile(): PublicProfile = request("/profile")

    fun updateProfile(update: ProfileUpdate): PublicProfile =
        request("/profile", "PUT", json.encodeToString(update))

    private fun enc(v: String): String = URLEncoder.encode(v, "UTF-8")
}

class ApiException(message: String, val code: Int = 0) : Exception(message)
