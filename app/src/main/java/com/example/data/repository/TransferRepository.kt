package com.example.data.repository

import com.example.data.database.TransferDao
import com.example.data.database.TransferEntity
import kotlinx.coroutines.flow.Flow

class TransferRepository(private val transferDao: TransferDao) {
    val allTransfers: Flow<List<TransferEntity>> = transferDao.getAllTransfers()

    suspend fun insertTransfer(transfer: TransferEntity): Long {
        return transferDao.insertTransfer(transfer)
    }

    suspend fun updateTransfer(transfer: TransferEntity) {
        transferDao.updateTransfer(transfer)
    }

    suspend fun deleteTransferById(id: Long) {
        transferDao.deleteTransferById(id)
    }

    suspend fun clearAll() {
        transferDao.clearAll()
    }
}
