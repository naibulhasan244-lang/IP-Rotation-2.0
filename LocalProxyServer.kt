package com.yourname.iprotation2.vpn

import android.util.Base64
import com.yourname.iprotation2.data.local.ProxyEntity
import com.yourname.iprotation2.data.local.proxyType
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

/**
 * Tiny HTTP/HTTPS proxy that listens on 127.0.0.1 and forwards every connection through
 * the CURRENT rotation proxy (read fresh for each new connection, so rotation needs no reconnect).
 */
class LocalProxyServer(private val upstream: () -> ProxyEntity?) {

    private val pool = Executors.newCachedThreadPool()
    @Volatile private var server: ServerSocket? = null

    val port: Int get() = server?.localPort ?: 0

    fun start() {
        val s = ServerSocket(0, 128, InetAddress.getByName("127.0.0.1"))
        server = s
        pool.execute {
            while (!s.isClosed) {
                try {
                    val c = s.accept()
                    pool.execute { handle(c) }
                } catch (e: IOException) {
                    break
                }
            }
        }
    }

    fun stop() {
        try { server?.close() } catch (_: IOException) {}
        pool.shutdownNow()
    }

    private fun handle(client: Socket) {
        var up: Socket? = null
        try {
            client.tcpNoDelay = true
            val cin = BufferedInputStream(client.getInputStream(), 16 * 1024)
            val cout = client.getOutputStream()
            val head = readHead(cin) ?: return
            val text = String(head, Charsets.ISO_8859_1)
            val lines = text.split("\r\n")
            val first = lines[0].split(" ")
            if (first.size < 3) return
            val method = first[0]
            val target = first[1]
            val version = first[2]

            val proxy = upstream()
            if (proxy == null) {
                cout.write("HTTP/1.1 503 Service Unavailable\r\nContent-Length: 0\r\n\r\n".toByteArray())
                return
            }

            if (method.equals("CONNECT", ignoreCase = true)) {
                val (host, port) = splitHostPort(target, 443)
                up = try {
                    openTunnel(proxy, host, port)
                } catch (e: Exception) {
                    cout.write("HTTP/1.1 502 Bad Gateway\r\nContent-Length: 0\r\n\r\n".toByteArray())
                    return
                }
                cout.write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray())
                cout.flush()
                pipe(client, cin, up)
            } else {
                // Plain HTTP: "GET http://host[:port]/path HTTP/1.1"
                val noScheme = target.removePrefix("http://")
                val slash = noScheme.indexOf('/')
                val hostPort = if (slash >= 0) noScheme.substring(0, slash) else noScheme
                val path = if (slash >= 0) noScheme.substring(slash) else "/"
                val (host, port) = splitHostPort(hostPort, 80)
                up = try {
                    openTunnel(proxy, host, port)
                } catch (e: Exception) {
                    cout.write("HTTP/1.1 502 Bad Gateway\r\nContent-Length: 0\r\n\r\n".toByteArray())
                    return
                }
                val sb = StringBuilder()
                sb.append(method).append(' ').append(path).append(' ').append(version).append("\r\n")
                for (i in 1 until lines.size) {
                    val l = lines[i]
                    if (l.isEmpty()) continue
                    val lower = l.lowercase()
                    if (lower.startsWith("proxy-connection:") || lower.startsWith("connection:")) continue
                    sb.append(l).append("\r\n")
                }
                sb.append("Connection: close\r\n\r\n")
                up.getOutputStream().write(sb.toString().toByteArray(Charsets.ISO_8859_1))
                up.getOutputStream().flush()
                pipe(client, cin, up)
            }
        } catch (_: Exception) {
        } finally {
            closeQuiet(up)
            closeQuiet(client)
        }
    }

    /** Opens a TCP tunnel to host:port through the given upstream proxy. */
    private fun openTunnel(p: ProxyEntity, host: String, port: Int): Socket {
        val type = p.proxyType()
        if (type.isSocks) {
            val s = Socket(Proxy(Proxy.Type.SOCKS, InetSocketAddress.createUnresolved(p.host, p.port)))
            s.connect(InetSocketAddress.createUnresolved(host, port), 10_000)
            return s
        }
        val s = Socket()
        try {
            s.connect(InetSocketAddress(p.host, p.port), 10_000)
            s.soTimeout = 10_000
            val req = StringBuilder()
            req.append("CONNECT $host:$port HTTP/1.1\r\nHost: $host:$port\r\n")
            if (!p.username.isNullOrEmpty()) {
                val cred = "${p.username}:${p.password.orEmpty()}"
                val b64 = Base64.encodeToString(cred.toByteArray(), Base64.NO_WRAP)
                req.append("Proxy-Authorization: Basic $b64\r\n")
            }
            req.append("\r\n")
            s.getOutputStream().write(req.toString().toByteArray())
            s.getOutputStream().flush()
            val resp = readHead(s.getInputStream()) ?: throw IOException("no response from proxy")
            val statusLine = String(resp, Charsets.ISO_8859_1).substringBefore("\r\n")
            if (!statusLine.contains(" 200")) throw IOException("proxy refused: $statusLine")
            s.soTimeout = 0
            return s
        } catch (e: Exception) {
            closeQuiet(s)
            throw e
        }
    }

    private fun pipe(client: Socket, cin: InputStream, up: Socket) {
        val t = pool.submit {
            try { copy(cin, up.getOutputStream()) } catch (_: Exception) {}
            closeQuiet(up); closeQuiet(client)
        }
        try { copy(up.getInputStream(), client.getOutputStream()) } catch (_: Exception) {}
        closeQuiet(up); closeQuiet(client)
        try { t.cancel(true) } catch (_: Exception) {}
    }

    private fun copy(input: InputStream, out: java.io.OutputStream) {
        val buf = ByteArray(16 * 1024)
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            out.write(buf, 0, n)
            out.flush()
        }
    }

    /** Reads bytes up to and including the blank line that ends an HTTP header block. */
    private fun readHead(input: InputStream): ByteArray? {
        val out = ByteArrayOutputStream()
        var matched = 0
        val end = byteArrayOf(13, 10, 13, 10)
        while (out.size() < 32 * 1024) {
            val b = input.read()
            if (b < 0) return null
            out.write(b)
            matched = if (b.toByte() == end[matched]) matched + 1 else if (b.toByte() == end[0]) 1 else 0
            if (matched == 4) return out.toByteArray()
        }
        return null
    }

    private fun splitHostPort(s: String, defaultPort: Int): Pair<String, Int> {
        if (s.startsWith("[")) { // IPv6 literal
            val close = s.indexOf(']')
            val host = s.substring(1, close)
            val port = s.substring(close + 1).removePrefix(":").toIntOrNull() ?: defaultPort
            return host to port
        }
        val i = s.lastIndexOf(':')
        return if (i > 0) s.substring(0, i) to (s.substring(i + 1).toIntOrNull() ?: defaultPort) else s to defaultPort
    }

    private fun closeQuiet(s: Socket?) {
        try { s?.close() } catch (_: Exception) {}
    }
}
