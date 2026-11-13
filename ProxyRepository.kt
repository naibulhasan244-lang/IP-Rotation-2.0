package com.yourname.iprotation2.data

import com.yourname.iprotation2.data.local.ProxyDao
import com.yourname.iprotation2.data.local.ProxyEntity
import com.yourname.iprotation2.data.model.ProxyStatus
import com.yourname.iprotation2.data.model.ProxyType
import com.yourname.iprotation2.data.network.IpChecker
import com.yourname.iprotation2.data.parser.ProxyParser
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

data class AddResult(val added: Int, val duplicates: Int, val invalid: Int)

@Singleton
class ProxyRepository @Inject constructor(
    private val dao: ProxyDao,
    private val checker: IpChecker
) {
    val proxies = dao.observeAll()

    suspend fun addBulk(text: String, defaultType: ProxyType): AddResult {
        val parsed = ProxyParser.parseBulk(text, defaultType)
        val entities = parsed.valid.map {
            ProxyEntity(
                host = it.host,
                port = it.port,
                type = it.type.name,
                username = it.username,
                password = it.password
            )
        }
        val ids = dao.insertAll(entities)
        val added = ids.count { it != -1L }
        return AddResult(added, entities.size - added, parsed.invalid.size)
    }

    /** Health check: returns true if the proxy works and updates its status in the DB. */
    suspend fun testProxy(p: ProxyEntity): Boolean {
        val result = checker.check(p)
        val now = System.currentTimeMillis()
        return result.fold(
            onSuccess = {
                dao.updateStatus(p.id, ProxyStatus.WORKING, it.pingMs, now)
                true
            },
            onFailure = {
                dao.updateStatus(p.id, ProxyStatus.DEAD, -1, now)
                false
            }
        )
    }

    suspend fun testAll(onProgress: (done: Int, total: Int) -> Unit) = coroutineScope {
        val list = dao.getAll()
        val total = list.size
        val done = AtomicInteger(0)
        val semaphore = Semaphore(5)
        onProgress(0, total)
        list.map { p ->
            async {
                semaphore.withPermit { testProxy(p) }
                onProgress(done.incrementAndGet(), total)
            }
        }.awaitAll()
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) = dao.setEnabled(id, enabled)
    suspend fun delete(id: Long) = dao.delete(id)
    suspend fun deleteDead() = dao.deleteDead()
    suspend fun deleteAll() = dao.deleteAll()
}
