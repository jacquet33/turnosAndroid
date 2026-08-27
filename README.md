# TurnReserved - Android

App nativa Android (Kotlin/Jetpack Compose) para [TurnReserved](https://turnreserved.com).

## Requisitos

- Android Studio Ladybug (2024.2+)
- Kotlin 2.1+
- Min SDK 28 (Android 9) — soporta últimas 4 versiones
- Target SDK 35

## Arquitectura

- **MVVM** con Jetpack Compose
- **Coroutines + Flow** para async
- **EncryptedSharedPreferences** para almacenamiento seguro
- **Kotlinx Serialization** para JSON
- **Material 3** UI

## Estructura

```
app/src/main/java/com/turnreserved/app/
├── MainActivity.kt
├── data/
│   ├── api/ApiService.kt
│   └── model/Models.kt
├── ui/
│   ├── auth/          # AuthViewModel, LoginScreen
│   ├── dashboard/     # DashboardScreen, Services, Agenda, Clients
│   ├── subscription/  # SubscriptionScreen
│   └── theme/         # Material 3 dark theme
└── util/SecureStorage.kt
```

## Setup

1. Clonar el repo
2. Abrir en Android Studio
3. Sync Gradle
4. Run en emulador o dispositivo (API 28+)

## API

Conecta a `https://turnreserved.com/api`

## Features

- Login / Registro con email
- Dashboard con 3 tabs: Servicios, Agenda, Clientes
- CRUD de servicios con link de booking copiable
- Agenda con filtro por fecha y estado
- Acciones de turno: completar / cancelar
- Búsqueda de clientes
- Suscripciones via MercadoPago (abre browser)
- Deep links: `turnreserved://book/{linkId}`
- Almacenamiento cifrado con EncryptedSharedPreferences
- Network Security Config (HTTPS only)
- Dark theme que replica la web
