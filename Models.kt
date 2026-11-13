package com.yourname.iprotation2.data.model

enum class ProxyType(val label: String) {
    HTTP("HTTP"),
    HTTPS("HTTPS"),
    SOCKS4("SOCKS4"),
    SOCKS5("SOCKS5");

    val isSocks: Boolean get() = this == SOCKS4 || this == SOCKS5

    companion object {
        fun fromScheme(scheme: String): ProxyType? = when (scheme.lowercase()) {
            "http" -> HTTP
            "https" -> HTTPS
            "socks4", "socks4a" -> SOCKS4
            "socks5", "socks5h", "socks" -> SOCKS5
            else -> null
        }

        fun fromName(name: String?): ProxyType = entries.firstOrNull { it.name == name } ?: HTTP
    }
}

object ProxyStatus {
    const val UNKNOWN = 0
    const val WORKING = 1
    const val DEAD = 2
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class RotationOrder { SEQUENTIAL, RANDOM }

data class ParsedProxy(
    val host: String,
    val port: Int,
    val type: ProxyType,
    val username: String? = null,
    val password: String? = null
)

data class AppSettings(
    val intervalSec: Int = 10,
    val randomOrder: Boolean = false,
    val skipDead: Boolean = true,
    val retryCount: Int = 3,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val defaultType: ProxyType = ProxyType.HTTP,
    val overlayEnabled: Boolean = true
)
