package com.turnreserved.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val email: String,
    val phone: String,
    val plan: String = "trial",
    val subscriptionStatus: String = "trial",
    val trialEndsAt: String? = null,
    val subscriptionEndsAt: String? = null,
    val maxServices: Int = 3
)

@Serializable
data class AuthResponse(val token: String, val user: User)

@Serializable
data class RegisterResponse(val message: String)

@Serializable
data class MeResponse(
    val id: String? = null,
    val plan: String? = null,
    val subscription_status: String? = null,
    val trial_ends_at: String? = null,
    val subscription_ends_at: String? = null,
    val max_services: Int? = null
)

@Serializable
data class Service(
    val id: String,
    val name: String,
    val linkId: String,
    val hasSchedule: Boolean = false,
    val minAdvanceHours: Int = 0,
    val createdAt: String = ""
)

@Serializable
data class DaySchedule(val enabled: Boolean, val slots: List<String> = emptyList())

@Serializable
data class DayConfig(val startTime: String, val endTime: String, val duration: Int)

@Serializable
data class ScheduleResponse(
    val schedule: Map<String, DaySchedule>,
    val dayConfig: Map<String, DayConfig>
)

@Serializable
data class Booking(
    val id: String,
    val serviceId: String,
    val serviceName: String,
    val clientName: String,
    val clientDNI: String,
    val clientPhone: String,
    val date: String,
    val time: String,
    val status: String,
    val createdAt: String
)

@Serializable
data class BookingSlot(val id: String, val date: String, val time: String, val status: String)

@Serializable
data class BookingPage(
    val service: BookingService,
    val schedule: Map<String, DaySchedule>,
    val dayConfig: Map<String, DayConfig>,
    val bookedSlots: List<BookingSlot>
) {
    @Serializable
    data class BookingService(val id: String, val name: String, val minAdvanceHours: Int = 0)
}

@Serializable
data class BookingPayload(val name: String, val dni: String, val phone: String, val date: String, val time: String)

@Serializable
data class BookingCreateResponse(val id: String, val status: String)

@Serializable
data class Client(
    val name: String,
    val phone: String,
    val dni: String,
    val totalBookings: Int,
    val lastBooking: String
)

@Serializable
data class PreferenceResponse(val preferenceId: String, val initPoint: String, val sandboxUrl: String)

@Serializable
data class MessageResponse(val message: String)

@Serializable
data class StatusUpdateResponse(val message: String, val status: String)

@Serializable
data class OfferResponse(val active: Boolean, val expiresAt: String? = null, val discountPct: Int? = null, val months: Int? = null)

data class PlanInfo(val name: String, val price: Int, val maxServices: Int, val days: Int? = null) {
    companion object {
        val plans = mapOf(
            "trial" to PlanInfo("Free Trial", 0, 3, 5),
            "basic" to PlanInfo("Basic", 9, 1),
            "pro" to PlanInfo("Pro", 19, 3),
            "business" to PlanInfo("Business", 39, -1)
        )
    }
}
