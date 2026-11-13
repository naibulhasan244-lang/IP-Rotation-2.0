package com.yourname.iprotation2.data.network

import java.net.Authenticator
import java.net.PasswordAuthentication
import java.util.concurrent.ConcurrentHashMap

/**
 * JDK-level authenticator used for SOCKS5 username/password auth.
 * Installed once in the Application class via Authenticator.setDefault(SocksAuth).
 */
object SocksAuth : Authenticator() {
    private val credentials = ConcurrentHashMap<String, PasswordAuthentication>()

    fun register(host: String, port: Int, user: String, pass: String) {
        credentials["$host:$port"] = PasswordAuthentication(user, pass.toCharArray())
    }

    override fun getPasswordAuthentication(): PasswordAuthentication? {
        val byName = credentials["${requestingHost}:${requestingPort}"]
        if (byName != null) return byName
        val addr = requestingSite?.hostAddress
        return if (addr != null) credentials["$addr:$requestingPort"] else null
    }
}
