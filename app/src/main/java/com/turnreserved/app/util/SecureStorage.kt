package com.turnreserved.app.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.turnreserved.app.data.model.User
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class SecureStorage(context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "turnreserved_secure",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var token: String?
        get() = prefs.getString("auth_token", null)
        set(value) = prefs.edit().putString("auth_token", value).apply()

    var user: User?
        get() = prefs.getString("user_data", null)?.let {
            try { json.decodeFromString(it) } catch (_: Exception) { null }
        }
        set(value) = prefs.edit().putString("user_data",
            value?.let { json.encodeToString(it) }
        ).apply()

    fun clear() {
        prefs.edit().clear().apply()
    }
}
