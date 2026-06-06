package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transfers")
data class TransferEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val fileSize: Long,
    val direction: String, // "SEND" or "RECEIVE"
    val peerName: String, // name of peer or device
    val timestamp: Long = System.currentTimeMillis(),
    val status: String, // "COMPLETED", "FAILED", "IN_PROGRESS", "PENDING"
    val progress: Float = 0f // 0 to 100%
)
