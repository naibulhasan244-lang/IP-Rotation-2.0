package com.yourname.iprotation2.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProxyDao {
    @Query("SELECT * FROM proxies ORDER BY id ASC")
    fun observeAll(): Flow<List<ProxyEntity>>

    @Query("SELECT * FROM proxies ORDER BY id ASC")
    suspend fun getAll(): List<ProxyEntity>

    @Query("SELECT * FROM proxies WHERE enabled = 1 ORDER BY id ASC")
    suspend fun getEnabled(): List<ProxyEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<ProxyEntity>): List<Long>

    @Query("UPDATE proxies SET status = :status, pingMs = :ping, lastChecked = :ts WHERE id = :id")
    suspend fun updateStatus(id: Long, status: Int, ping: Long, ts: Long)

    @Query("UPDATE proxies SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM proxies WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM proxies WHERE status = 2")
    suspend fun deleteDead()

    @Query("DELETE FROM proxies")
    suspend fun deleteAll()
}

@Dao
interface IpHistoryDao {
    @Query("SELECT * FROM ip_history ORDER BY id DESC")
    fun observeAll(): Flow<List<IpHistoryEntity>>

    @Query("SELECT * FROM ip_history ORDER BY id DESC")
    suspend fun getAll(): List<IpHistoryEntity>

    @Insert
    suspend fun insert(item: IpHistoryEntity)

    @Query("DELETE FROM ip_history")
    suspend fun clear()

    @Query("DELETE FROM ip_history WHERE id NOT IN (SELECT id FROM ip_history ORDER BY id DESC LIMIT 1000)")
    suspend fun trim()
}
