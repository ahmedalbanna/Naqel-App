package com.example.ui

import android.app.Application
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.TransferEntity
import com.example.data.model.LocalFile
import com.example.data.model.PredefinedFiles
import com.example.data.repository.TransferRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.ServerSocket
import java.net.Socket
import kotlin.random.Random

class ShareViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransferRepository
    val allTransfers: StateFlow<List<TransferEntity>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TransferRepository(database.transferDao())
        allTransfers = repository.allTransfers.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    // UI Navigation State
    var currentScreen by mutableStateOf(Screen.DASHBOARD)

    // Selection State
    var selectedFiles = mutableStateOf<Set<LocalFile>>(emptySet())

    // Wifi P2P / Network State
    var wifiP2pSupported by mutableStateOf(false)
    var isDiscoveryActive by mutableStateOf(false)
    var discoveredDevices = mutableStateOf<List<P2PDevice>>(emptyList())
    var connectedDevice by mutableStateOf<P2PDevice?>(null)
    
    // Connection Info
    var connectionState by mutableStateOf(ConnectionState.DISCONNECTED)
    var qrCodePayload by mutableStateOf("")

    // Active Transfer State
    var transferState by mutableStateOf<TransferProgressState?>(null)

    // Server socket reference (real p2p)
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var transferJob: Job? = null

    // For hardware shake to connect simulation
    var isShakingDetectActive by mutableStateOf(false)

    // User preference values (Mock / local metadata)
    var deviceName by mutableStateOf("جهاز_أندرويد_${Random.nextInt(100, 999)}")

    fun toggleFileSelection(file: LocalFile) {
        val currentSet = selectedFiles.value.toMutableSet()
        if (currentSet.contains(file)) {
            currentSet.remove(file)
        } else {
            currentSet.add(file)
        }
        selectedFiles.value = currentSet
    }

    fun clearSelections() {
        selectedFiles.value = emptySet()
    }

    // Start discovering nearby peers
    fun startDiscovery() {
        isDiscoveryActive = true
        connectionState = ConnectionState.SEARCHING
        
        viewModelScope.launch {
            // Simulated nearby devices for instant feedback (even in emulator)
            delay(1500)
            discoveredDevices.value = listOf(
                P2PDevice("جهاز سامسونج S24", "192.168.49.23", true),
                P2PDevice("شاومي مي 13", "192.168.49.45", false),
                P2PDevice("آيباد سليمان", "192.168.49.199", false)
            )
        }
    }

    fun stopDiscovery() {
        isDiscoveryActive = false
        if (connectionState == ConnectionState.SEARCHING) {
            connectionState = ConnectionState.DISCONNECTED
        }
    }

    fun connectToDevice(device: P2PDevice, asSender: Boolean) {
        viewModelScope.launch {
            connectionState = ConnectionState.CONNECTING
            delay(2000)
            connectedDevice = device
            connectionState = ConnectionState.CONNECTED
            
            if (asSender) {
                // If Sender, immediately start transferring the selected files
                startTransferSequence(asSender = true)
            } else {
                // If receiver, standby to stream
                startTransferSequence(asSender = false)
            }
        }
    }

    // Generate QR payload representing connection configuration
    fun generateQR() {
        // Formulate connection schema: SSID|PASSWORD|IP|PORT
        qrCodePayload = "NaqelShare|WifiPass_${Random.nextInt(1000, 9999)}|192.168.49.1|8888"
    }

    // Trigger QR scan connection
    fun connectViaQR(scannedPayload: String) {
        val parts = scannedPayload.split("|")
        if (parts.size >= 4) {
            val ssid = parts[0]
            val pass = parts[1]
            val ip = parts[2]
            val port = parts[3]
            
            viewModelScope.launch {
                connectionState = ConnectionState.CONNECTING
                delay(1500)
                connectedDevice = P2PDevice("جهاز المستقبل (عبر QR)", ip, true)
                connectionState = ConnectionState.CONNECTED
                
                // Immediately stream files
                startTransferSequence(asSender = true)
            }
        }
    }

    // Trigger shake to connect logic
    fun simulateShakeConnect() {
        isShakingDetectActive = true
        viewModelScope.launch {
            delay(2000) // simulated delay while detecting device shake in same interval
            isShakingDetectActive = false
            connectToDevice(P2PDevice("جهاز مهتز قريب 📱", "192.168.49.77", true), asSender = true)
        }
    }

    // High fidelity stream transfer logic
    private fun startTransferSequence(asSender: Boolean) {
        transferJob?.cancel()
        transferJob = viewModelScope.launch(Dispatchers.Default) {
            val peerName = connectedDevice?.name ?: "جهاز مجهول"
            
            if (asSender) {
                val filesToSend = selectedFiles.value.toList()
                if (filesToSend.isEmpty()) return@launch
                
                // Loop files
                for ((index, file) in filesToSend.withIndex()) {
                    val entity = TransferEntity(
                        fileName = file.name,
                        fileSize = file.size,
                        direction = "SEND",
                        peerName = peerName,
                        status = "IN_PROGRESS"
                    )
                    
                    var databaseId: Long = 0
                    withContext(Dispatchers.Main) {
                        databaseId = repository.insertTransfer(entity)
                    }

                    // Simulated Real Time Streaming with throughput stats
                    var sentBytes = 0L
                    val totalBytes = file.size
                    val startTime = System.currentTimeMillis()
                    
                    while (sentBytes < totalBytes) {
                        delay(200)
                        
                        // Dynamic transfer speed fluctuate (15MB/s to 45MB/s)
                        val speedMBs = Random.nextDouble(18.4, 48.7)
                        val bytesSentThisStep = (speedMBs * 1024 * 1024 * 0.2).toLong()
                        sentBytes = (sentBytes + bytesSentThisStep).coerceAtMost(totalBytes)
                        
                        val progress = (sentBytes.toFloat() / totalBytes) * 100f
                        
                        withContext(Dispatchers.Main) {
                            transferState = TransferProgressState(
                                fileName = file.name,
                                fileSize = totalBytes,
                                direction = "SEND",
                                progress = progress,
                                transferSpeed = String.format("%.1f م.ب/ث", speedMBs),
                                index = index + 1,
                                totalCount = filesToSend.size
                            )
                        }

                        // update in database occasionally or save for end
                    }

                    // Complete
                    withContext(Dispatchers.Main) {
                        repository.insertTransfer(
                            entity.copy(id = databaseId, status = "COMPLETED", progress = 100f)
                        )
                    }
                }
                
                // Finished all transfers
                withContext(Dispatchers.Main) {
                    transferState = null
                    clearSelections()
                    connectionState = ConnectionState.DISCONNECTED
                    connectedDevice = null
                    currentScreen = Screen.HISTORY
                }

            } else {
                // RECEIVER MODE
                // Let's simulated 2 beautiful incoming files
                val incomingCount = 2
                val fileNames = listOf("فيديو_المشروع.mp4", "تحديث_مستندات.pdf")
                val fileSizes = listOf(68200000L, 12500000L)

                for (idx in 0 until incomingCount) {
                    val name = fileNames[idx]
                    val size = fileSizes[idx]
                    
                    val entity = TransferEntity(
                        fileName = name,
                        fileSize = size,
                        direction = "RECEIVE",
                        peerName = peerName,
                        status = "IN_PROGRESS"
                    )
                    
                    var databaseId: Long = 0
                    withContext(Dispatchers.Main) {
                        databaseId = repository.insertTransfer(entity)
                    }

                    var recBytes = 0L
                    while (recBytes < size) {
                        delay(200)
                        val speedMBs = Random.nextDouble(22.0, 50.3)
                        val bytesReceivedThisStep = (speedMBs * 1024 * 1024 * 0.2).toLong()
                        recBytes = (recBytes + bytesReceivedThisStep).coerceAtMost(size)
                        
                        val progress = (recBytes.toFloat() / size) * 100f
                        
                        withContext(Dispatchers.Main) {
                            transferState = TransferProgressState(
                                fileName = name,
                                fileSize = size,
                                direction = "RECEIVE",
                                progress = progress,
                                transferSpeed = String.format("%.1f م.ب/ث", speedMBs),
                                index = idx + 1,
                                totalCount = incomingCount
                            )
                        }
                    }

                    withContext(Dispatchers.Main) {
                        repository.insertTransfer(
                            entity.copy(id = databaseId, status = "COMPLETED", progress = 100f)
                        )
                    }
                }

                withContext(Dispatchers.Main) {
                    transferState = null
                    connectionState = ConnectionState.DISCONNECTED
                    connectedDevice = null
                    currentScreen = Screen.HISTORY
                }
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    override fun onCleared() {
        super.onCleared()
        transferJob?.cancel()
        serverSocket?.close()
        clientSocket?.close()
    }
}

enum class Screen {
    DASHBOARD,
    SENDER,
    RECEIVER,
    FILE_PICKER,
    HISTORY,
    HELP
}

enum class ConnectionState {
    DISCONNECTED,
    SEARCHING,
    CONNECTING,
    CONNECTED
}

data class P2PDevice(
    val name: String,
    val ipAddress: String,
    val isGroupOwner: Boolean
)

data class TransferProgressState(
    val fileName: String,
    val fileSize: Long,
    val direction: String,
    val progress: Float,
    val transferSpeed: String,
    val index: Int,
    val totalCount: Int
)
