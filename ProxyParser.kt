package com.yourname.iprotation2.data.parser

import com.yourname.iprotation2.data.model.ParsedProxy
import com.yourname.iprotation2.data.model.ProxyType

/**
 * Parses proxy strings in many common formats:
 *  - ip:port
 *  - ip:port:user:pass
 *  - user:pass:ip:port
 *  - user:pass@ip:port
 *  - scheme://ip:port
 *  - scheme://user:pass@ip:port
 *  - scheme://ip:port:user:pass
 * scheme = http | https | socks4 | socks4a | socks5 | socks5h | socks
 */
object ProxyParser {

    data class ParseResult(val valid: List<ParsedProxy>, val invalid: List<String>)

    private val SCHEME = Regex("^([A-Za-z0-9]+)://(.+)$")
    private val HOST = Regex("^[A-Za-z0-9]([A-Za-z0-9.-]*[A-Za-z0-9])?$")

    fun parseBulk(text: String, defaultType: ProxyType): ParseResult {
        val valid = mutableListOf<ParsedProxy>()
        val invalid = mutableListOf<String>()
        text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .forEach { line ->
                val parsed = parseLine(line, defaultType)
                if (parsed != null) valid.add(parsed) else invalid.add(line)
            }
        return ParseResult(valid, invalid)
    }

    fun parseLine(raw: String, defaultType: ProxyType): ParsedProxy? {
        var line = raw.trim().trimEnd('/')
        if (line.isEmpty()) return null

        var type = defaultType
        val schemeMatch = SCHEME.matchEntire(line)
        if (schemeMatch != null) {
            type = ProxyType.fromScheme(schemeMatch.groupValues[1]) ?: return null
            line = schemeMatch.groupValues[2].trimEnd('/')
        }

        var user: String? = null
        var pass: String? = null
        val host: String
        val portStr: String

        if (line.contains('@')) {
            val auth = line.substringBeforeLast('@')
            val hostPort = line.substringAfterLast('@')
            user = auth.substringBefore(':')
            pass = if (auth.contains(':')) auth.substringAfter(':') else ""
            val idx = hostPort.lastIndexOf(':')
            if (idx <= 0) return null
            host = hostPort.substring(0, idx)
            portStr = hostPort.substring(idx + 1)
        } else {
            val parts = line.split(':')
            when {
                parts.size == 2 -> {
                    host = parts[0]
                    portStr = parts[1]
                }
                parts.size >= 4 -> {
                    if (parts.size == 4 && toPort(parts[1]) == null && toPort(parts[3]) != null) {
                        // user:pass:host:port
                        user = parts[0]
                        pass = parts[1]
                        host = parts[2]
                        portStr = parts[3]
                    } else {
                        // host:port:user:pass (pass may contain ':')
                        host = parts[0]
                        portStr = parts[1]
                        user = parts[2]
                        pass = parts.drop(3).joinToString(":")
                    }
                }
                else -> return null
            }
        }

        val cleanHost = host.trim()
        if (!HOST.matches(cleanHost)) return null
        val port = toPort(portStr.trim()) ?: return null

        return ParsedProxy(
            host = cleanHost,
            port = port,
            type = type,
            username = user?.takeIf { it.isNotEmpty() },
            password = pass?.takeIf { it.isNotEmpty() }
        )
    }

    private fun toPort(s: String): Int? {
        if (s.isEmpty() || !s.all { it.isDigit() }) return null
        return s.toIntOrNull()?.takeIf { it in 1..65535 }
    }
}
