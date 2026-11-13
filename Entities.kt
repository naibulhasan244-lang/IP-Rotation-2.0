package com.yourname.iprotation2.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.yourname.iprotation2.data.model.ProxyStatus
import com.yourname.iprotation2.data.model.ProxyType

@Entity(
    tableName = "proxies",
    indices = [Index(value = ["host", "port", "type"], unique = true)]
)
data class ProxyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val host: String,
    val port: Int,
    val type: String,
    val username: String? = null,
    val password: String? = null,
    val status: Int = ProxyStatus.UNKNOWN,
    val pingMs: Long = -1,
    val enabled: Boolean = true,
    val lastChecked: Long = 0
)

@Entity(tableName = "ip_history")
data class IpHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ip: String,
    val country: String,
    val flag: String,
    val proxyLabel: String,
    val pingMs: Long,
    val timestamp: Long
)

fun ProxyEntity.proxyType(): ProxyType = ProxyType.fromName(type)

fun ProxyEntity.label(): String = "${proxyType().label} $host:$port"
