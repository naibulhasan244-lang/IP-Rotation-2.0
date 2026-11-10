package com.yourname.iprotation2.data.network

import android.os.SystemClock
import com.yourname.iprotation2.data.local.ProxyEntity
import com.yourname.iprotation2.data.local.proxyType
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Authenticator
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import org.json.JSONObject

data class IpInfo(
    val ip: String,
    val country: String,
    val countryCode: String,
    val flag: String,
    val pingMs: Long
)

/**
 * Finds the public IP (and country) that the outside world sees for requests
 * made through the given proxy (or directly when proxy == null).
 */
@Singleton
class IpChecker @Inject constructor(private val base: OkHttpClient) {

    suspend fun check(proxy: ProxyEntity?): Result<IpInfo> = withContext(Dispatchers.IO) {
        val client = buildClient(proxy)
        try {
            val info = try {
                fetchPrimary(client)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Network-level failure: proxy is almost surely bad, no need to retry.
                if (e is InterruptedIOException || e is ConnectException || e is UnknownHostException) throw e
                fetchFallback(client)
            }
            Result.success(info)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            client.connectionPool.evictAll()
        }
    }

    private suspend fun fetchPrimary(client: OkHttpClient): IpInfo {
        val start = SystemClock.elapsedRealtime()
        val body = get(client, "https://ipwho.is/")
        val ping = SystemClock.elapsedRealtime() - start
        val json = JSONObject(body)
        if (!json.optBoolean("success", false)) throw IllegalStateException("ipwho.is: ${json.optString("message")}")
        return IpInfo(
            ip = json.getString("ip"),
            country = json.optString("country"),
            countryCode = json.optString("country_code"),
            flag = json.optJSONObject("flag")?.optString("emoji").orEmpty(),
            pingMs = ping
        )
    }

    private suspend fun fetchFallback(client: OkHttpClient): IpInfo {
        val start = SystemClock.elapsedRealtime()
        val body = get(client, "https://api.ipify.org?format=json")
        val ping = SystemClock.elapsedRealtime() - start
        return IpInfo(JSONObject(body).getString("ip"), "", "", "", ping)
    }

    private suspend fun get(client: OkHttpClient, url: String): String {
        val request = Request.Builder().url(url).header("Accept", "application/json").build()
        client.newCall(request).await().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")
            return response.body?.string().orEmpty()
        }
    }

    private fun buildClient(p: ProxyEntity?): OkHttpClient {
        val builder = base.newBuilder()
            .connectTimeout(7, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .callTimeout(12, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)

        if (p == null) return builder.proxy(Proxy.NO_PROXY).build()

        val address = InetSocketAddress.createUnresolved(p.host, p.port)
        val user = p.username
        val pass = p.password
        val type = p.proxyType()

        if (type.isSocks) {
            builder.proxy(Proxy(Proxy.Type.SOCKS, address))
            if (!user.isNullOrEmpty()) SocksAuth.register(p.host, p.port, user, pass.orEmpty())
        } else {
            // HTTP and HTTPS proxies are both used through an HTTP CONNECT proxy.
            builder.proxy(Proxy(Proxy.Type.HTTP, address))
            if (!user.isNullOrEmpty()) {
                builder.proxyAuthenticator(object : Authenticator {
                    override fun authenticate(route: Route?, response: Response): Request? {
                        if (response.request.header("Proxy-Authorization") != null) return null
                        return response.request.newBuilder()
                            .header("Proxy-Authorization", Credentials.basic(user, pass.orEmpty()))
                            .build()
                    }
                })
            }
        }
        return builder.build()
    }
}

private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (!cont.isCancelled) cont.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            cont.resume(response)
        }
    })
    cont.invokeOnCancellation { cancel() }
}
