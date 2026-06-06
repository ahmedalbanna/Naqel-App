package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.database.TransferEntity
import com.example.data.model.FileType
import com.example.data.model.LocalFile
import com.example.data.model.PredefinedFiles
import com.example.ui.ConnectionState
import com.example.ui.P2PDevice
import com.example.ui.Screen
import com.example.ui.ShareViewModel
import com.example.ui.theme.ElectroCyan
import com.example.ui.theme.LaserMagenta
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.DarkSlate
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SpeedGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.HighPowerBlue
import com.example.ui.theme.TextLight
import com.example.ui.theme.GlassBorderColor
import kotlin.random.Random
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch
import java.io.InputStream
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {
    private var sensorManager: SensorManager? = null
    private var shakeListener: SensorEventListener? = null
    private var lastAcceleration = 0f
    private var currentAcceleration = 0f
    private var shakeThreshold = 14f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        currentAcceleration = SensorManager.GRAVITY_EARTH
        lastAcceleration = SensorManager.GRAVITY_EARTH

        setContent {
            MyApplicationTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    MainScreenContent(
                        onSetupShake = { onShakeDetected ->
                            setupShakeSensor(onShakeDetected)
                        },
                        onDisableShake = {
                            disableShakeSensor()
                        }
                    )
                }
            }
        }
    }

    private fun setupShakeSensor(onShake: () -> Unit) {
        shakeListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                
                lastAcceleration = currentAcceleration
                currentAcceleration = sqrt(x * x + y * y + z * z)
                val delta = currentAcceleration - lastAcceleration
                val accelerationOffset = delta * 0.9f
                
                if (accelerationOffset > shakeThreshold) {
                    onShake()
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager?.registerListener(
            shakeListener,
            sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    private fun disableShakeSensor() {
        shakeListener?.let {
            sensorManager?.unregisterListener(it)
        }
        shakeListener = null
    }

    override fun onPause() {
        super.onPause()
        disableShakeSensor()
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreenContent(
    viewModel: ShareViewModel = viewModel(),
    onSetupShake: (() -> Unit) -> Unit,
    onDisableShake: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Manage runtime permissions
    val permissionsToRequest = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.CAMERA
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.READ_MEDIA_IMAGES)
            add(Manifest.permission.READ_MEDIA_VIDEO)
            add(Manifest.permission.READ_MEDIA_AUDIO)
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    val permissionState = rememberMultiplePermissionsState(permissionsToRequest)

    LaunchedEffect(Unit) {
        if (!permissionState.allPermissionsGranted) {
            permissionState.launchMultiplePermissionRequest()
        }
    }

    // Handle Shake sensing registration
    LaunchedEffect(viewModel.isShakingDetectActive) {
        if (viewModel.isShakingDetectActive) {
            onSetupShake {
                viewModel.connectToDevice(
                    P2PDevice("جهاز اهتزاز مباشر 📱", "192.168.49.50", false),
                    asSender = true
                )
                Toast.makeText(context, "تم التقاط الاهتزاز! جاري الربط...", Toast.LENGTH_SHORT).show()
                onDisableShake()
            }
        } else {
            onDisableShake()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (viewModel.transferState == null && 
                viewModel.currentScreen != Screen.SENDER && 
                viewModel.currentScreen != Screen.RECEIVER &&
                viewModel.currentScreen != Screen.FILE_PICKER
            ) {
                NaqelBottomNavigation(
                    currentScreen = viewModel.currentScreen,
                    onNavigate = { viewModel.currentScreen = it }
                )
            }
        },
        contentWindowInsets = WindowInsets.navigationBars
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkSlate)
                .padding(innerPadding)
        ) {
            // Draw premium soft blurred mesh circles in background
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Top-right soft blue blob (300dp-equivalent responsive radial gradient)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x66BFDBFE), Color.Transparent),
                        center = Offset(size.width * 1.0f, size.height * 0.05f),
                        radius = size.width * 0.75f
                    ),
                    center = Offset(size.width * 1.0f, size.height * 0.05f),
                    radius = size.width * 0.75f
                )
                
                // Bottom-left soft indigo blob
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x55C7D2FE), Color.Transparent),
                        center = Offset(size.width * 0.0f, size.height * 0.85f),
                        radius = size.width * 0.65f
                    ),
                    center = Offset(size.width * 0.0f, size.height * 0.85f),
                    radius = size.width * 0.65f
                )
            }
            // Live Transfer Overlay (Highest priority to block UI and display precise stats)
            val activeTransfer = viewModel.transferState
            if (activeTransfer != null) {
                ActiveTransferScreen(
                    state = activeTransfer,
                    connectedDevice = viewModel.connectedDevice
                )
            } else {
                when (viewModel.currentScreen) {
                    Screen.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        onRequestPermissions = { permissionState.launchMultiplePermissionRequest() }
                    )
                    Screen.FILE_PICKER -> FilePickerScreen(viewModel = viewModel)
                    Screen.SENDER -> SenderScreen(viewModel = viewModel)
                    Screen.RECEIVER -> ReceiverScreen(viewModel = viewModel)
                    Screen.HISTORY -> HistoryScreen(viewModel = viewModel)
                    Screen.HELP -> HelpScreen()
                }
            }
        }
    }
}

// Custom Bottom Navigation
@Composable
fun NaqelBottomNavigation(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit
) {
    NavigationBar(
        containerColor = SlateCard,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("main_bottom_navigation")
    ) {
        NavigationBarItem(
            selected = currentScreen == Screen.DASHBOARD,
            onClick = { onNavigate(Screen.DASHBOARD) },
            icon = { Icon(Icons.Filled.Share, contentDescription = "الرئيسية") },
            label = { Text("الرئيسية", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = ElectroCyan,
                selectedTextColor = ElectroCyan,
                indicatorColor = Color(0xFF1E293B)
            )
        )
        NavigationBarItem(
            selected = currentScreen == Screen.HISTORY,
            onClick = { onNavigate(Screen.HISTORY) },
            icon = { Icon(Icons.Outlined.History, contentDescription = "السجل") },
            label = { Text("سجل النقل", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = ElectroCyan,
                selectedTextColor = ElectroCyan,
                indicatorColor = Color(0xFF1E293B)
            )
        )
        NavigationBarItem(
            selected = currentScreen == Screen.HELP,
            onClick = { onNavigate(Screen.HELP) },
            icon = { Icon(Icons.Outlined.HelpOutline, contentDescription = "كيفية الاستخدام") },
            label = { Text("تعليمات", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = ElectroCyan,
                selectedTextColor = ElectroCyan,
                indicatorColor = Color(0xFF1E293B)
            )
        )
    }
}

// ---------------- DASHBOARD COMPOSABLE -----------------
@Composable
fun DashboardScreen(
    viewModel: ShareViewModel,
    onRequestPermissions: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val history by viewModel.allTransfers.collectAsState()

    // File picker launcher for actual local external files
    val localFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val contentResolver = context.contentResolver
            val pickedList = uris.mapNotNull { uri ->
                var name = "ملف_سريع_${Random.nextInt(10, 99)}"
                var size = 1024L
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIndex != -1) name = cursor.getString(nameIndex)
                        if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                    }
                }
                val extension = name.substringAfterLast(".", "bin")
                val type = when (extension.lowercase()) {
                    "jpg", "jpeg", "png", "webp", "gif" -> FileType.IMAGE
                    "mp4", "mkv", "mov", "avi" -> FileType.VIDEO
                    "mp3", "wav", "m4a", "ogg" -> FileType.AUDIO
                    "pdf", "docx", "xlsx", "txt", "zip" -> FileType.DOCUMENT
                    "apk" -> FileType.APP
                    else -> FileType.DOCUMENT
                }
                
                LocalFile(
                    id = uri.toString(),
                    name = name,
                    size = size,
                    type = type,
                    uriString = uri.toString(),
                    extension = extension
                )
            }
            // Add picked files to state and switch screen to pick categories
            viewModel.selectedFiles.value = viewModel.selectedFiles.value + pickedList
            viewModel.currentScreen = Screen.FILE_PICKER
            Toast.makeText(context, "تم إدراج ${pickedList.size} ملف من الذاكرة", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // App title & Logo
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Pulsing geometric logo representing Wi-Fi beam
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(ElectroCyan.copy(alpha = 0.2f), Color.Transparent)))
                            .border(1.5.dp, ElectroCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WifiTethering,
                            contentDescription = "قناة ناقل",
                            modifier = Modifier.size(36.dp),
                            tint = ElectroCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "ناقِل",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextLight
                    )
                    Text(
                        text = "مشاركة ونقل الملفات بأقصى سرعة واي فاي مباشرة",
                        fontSize = 13.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        }

        // Active Device info Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlassBorderColor, RoundedCornerShape(24.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEEF2FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Smartphone, contentDescription = "جهازك", tint = ElectroCyan)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("الجهاز النشط", fontSize = 12.sp, color = TextMuted)
                            Text(viewModel.deviceName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextLight)
                        }
                    }
                    IconButton(
                        onClick = {
                            viewModel.deviceName = "جهاز_أندرويد_${Random.nextInt(1000, 9999)}"
                        }
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "تغيير الاسم", tint = TextMuted)
                    }
                }
            }
        }

        // Action Core buttons (Two giant grids: SEND / RECEIVE)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Send File (إرسال)
                Button(
                    onClick = { viewModel.currentScreen = Screen.FILE_PICKER },
                    shape = RoundedCornerShape(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectroCyan),
                    modifier = Modifier
                        .weight(1f)
                        .height(145.dp)
                        .testTag("send_button"),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.FileUpload, 
                                contentDescription = "إرسال", 
                                tint = Color.White, 
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("إرسال", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                // Receive File (استقبال)
                Button(
                    onClick = { 
                        viewModel.generateQR()
                        viewModel.currentScreen = Screen.RECEIVER 
                    },
                    shape = RoundedCornerShape(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HighPowerBlue),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E7FF)),
                    modifier = Modifier
                        .weight(1f)
                        .height(145.dp)
                        .testTag("receive_button"),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEEF2FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.FileDownload, 
                                contentDescription = "استقبال", 
                                tint = ElectroCyan, 
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("استقبال", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextLight)
                    }
                }
            }
        }

        // Storage External File picker Trigger
        item {
            Card(
                onClick = { localFilePicker.launch("*/*") },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ElectroCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = "مستند خارجي", tint = ElectroCyan)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("اختر ملفات من ذاكرة الهاتف 📂", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ElectroCyan)
                }
            }
        }

        // Secondary Utilities Card (Shake to Connect / Reset permissions)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "الاقتران الفوري الذكي",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    // Shake row option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (viewModel.isShakingDetectActive) Color(0xFF0369A1) else Color(0xFF334155).copy(alpha = 0.4f))
                            .clickable {
                                if (viewModel.isShakingDetectActive) {
                                    viewModel.isShakingDetectActive = false
                                } else {
                                    viewModel.simulateShakeConnect()
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Vibration, 
                                contentDescription = "اهتزاز", 
                                tint = if (viewModel.isShakingDetectActive) Color.White else ElectroCyan
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("الربط بالاهتزاز (Shake Connection)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                Text("هز كلا الهاتفين معاً للربط الفوري", fontSize = 11.sp, color = TextMuted)
                            }
                        }
                        if (viewModel.isShakingDetectActive) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("اضغط للتفعيل", fontSize = 11.sp, color = ElectroCyan)
                        }
                    }
                }
            }
        }

        // Mini statistics or Mini History log (Conforms to persistence rule)
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("العمليات الأخيرة", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    TextButton(onClick = { viewModel.currentScreen = Screen.HISTORY }) {
                        Text("عرض الكل", color = ElectroCyan, fontSize = 13.sp)
                    }
                }
                
                if (history.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(SlateCard, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("لا يوجد سجل نقل حالي", color = TextMuted, fontSize = 12.sp)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        history.take(2).forEach { transfer ->
                            TransferHistoryItem(transfer)
                        }
                    }
                }
            }
        }
    }
}

// ---------------- FILE PICKER SCREEN -----------------
@Composable
fun FilePickerScreen(viewModel: ShareViewModel) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf<FileType?>(null) }
    
    val allFiles = PredefinedFiles.sampleFiles
    val filteredFiles = remember(selectedCategory) {
        if (selectedCategory == null) allFiles else allFiles.filter { it.type == selectedCategory }
    }

    val selectedList = viewModel.selectedFiles.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("file_picker_container")
    ) {
        // Upper Title block
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { viewModel.currentScreen = Screen.DASHBOARD }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع", tint = TextLight)
            }
            Text("مستكشف الملفات", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextLight)
            TextButton(onClick = { viewModel.clearSelections() }) {
                Text("إلغاء الكل", color = LaserMagenta, fontSize = 13.sp)
            }
        }

        // Category horizontally scrollable chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val categories = listOf(
                null to "الكل 📁",
                FileType.IMAGE to "صور 🖼️",
                FileType.VIDEO to "فيديو 🎥",
                FileType.APP to "تطبيقات 📲",
                FileType.DOCUMENT to "كتب 📄",
                FileType.AUDIO to "صوتيات 🎵"
            )

            // Dynamic filter row
            categories.forEach { (type, label) ->
                FilterChip(
                    selected = selectedCategory == type,
                    onClick = { selectedCategory = type },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectroCyan,
                        selectedLabelColor = Color.White,
                        containerColor = SlateCard,
                        labelColor = TextMuted
                    ),
                    modifier = Modifier.padding(2.dp)
                )
            }
        }

        // File list inside LazyColumn
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredFiles) { file ->
                val isSelected = selectedList.contains(file)
                val itemBgColor = if (isSelected) ElectroCyan.copy(alpha = 0.08f) else SlateCard
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(itemBgColor)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) ElectroCyan.copy(alpha = 0.5f) else GlassBorderColor,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { viewModel.toggleFileSelection(file) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // File category avatar/icon representation
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFEEF2FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            val icon = when (file.type) {
                                FileType.IMAGE -> Icons.Filled.Image
                                FileType.VIDEO -> Icons.Filled.VideoFile
                                FileType.APP -> Icons.Filled.Android
                                FileType.DOCUMENT -> Icons.Filled.Description
                                FileType.AUDIO -> Icons.Filled.AudioFile
                            }
                            Icon(icon, contentDescription = file.type.name, tint = ElectroCyan)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                file.name, 
                                fontSize = 14.sp, 
                                fontWeight = FontWeight.Bold, 
                                color = TextLight,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text("${file.sizeFormatted} • ${file.extension}", fontSize = 11.sp, color = TextMuted)
                        }
                    }
                    // Selection checkmark indicator
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) ElectroCyan else Color(0xFFE2E8F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(Icons.Filled.Check, contentDescription = "تم الاختيار", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Sticky Bottom Send/Trigger Action sheet
        if (selectedList.isNotEmpty()) {
            val totalSize = selectedList.sumOf { it.size }
            val totalSizeStr = remember(totalSize) {
                val mb = totalSize / (1024.0 * 1024.0)
                if (mb >= 1024.0) String.format("%.2f جيجابايت", mb / 1024.0) else String.format("%.1f ميجابايت", mb)
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlassBorderColor, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("تم تحديد ${selectedList.size} ملف", fontSize = 13.sp, color = TextMuted)
                        Text(totalSizeStr, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ElectroCyan)
                    }
                    Button(
                        onClick = { viewModel.currentScreen = Screen.SENDER },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectroCyan),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("proceed_send_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("إرسال الآن ⚡", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ---------------- SENDER SCREEN (WIFI P2P DISCOVERY & CONNECT) -----------------
@Composable
fun SenderScreen(viewModel: ShareViewModel) {
    val context = LocalContext.current
    var isCameraActive by remember { mutableStateOf(false) }

    // Start Scanning automatically
    LaunchedEffect(Unit) {
        viewModel.startDiscovery()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("sender_panel")
    ) {
        // App bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { 
                viewModel.stopDiscovery()
                viewModel.currentScreen = Screen.FILE_PICKER 
            }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع", tint = TextLight)
            }
            Text("البحث عن أجهزة استقبال", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextLight)
            IconButton(onClick = { viewModel.startDiscovery() }) {
                Icon(Icons.Filled.Refresh, contentDescription = "تحديث الاكتشاف", tint = ElectroCyan)
            }
        }

        if (isCameraActive) {
            // Live scanning viewfinder
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                // Interactive Cam analyzer Simulator
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.QrCodeScanner, contentDescription = "كاميرا", tint = ElectroCyan, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("كاميرا المعاينة والمسح نشطة 📷", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("وجه الكاميرا نحو كود المستقبل للمزامنة والسحب", fontSize = 11.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Direct click link to simulate scan results instantly
                    Button(
                        onClick = { 
                            isCameraActive = false
                            viewModel.connectViaQR("NaqelShare|GuestPass|192.168.49.12|8888")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SpeedGreen)
                    ) {
                        Text("محاكاة التقاط الرمز فوراً (للتجربة)", color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { isCameraActive = false }) {
                        Text("إلغاء الكاميرا", color = LaserMagenta)
                    }
                }
            }
        } else {
            // Dynamic Sonar radar pulsing
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                RadarSonarAnimation()
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Radar, contentDescription = "رادار", tint = ElectroCyan, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("جاري اكتشاف الأجهزة عبر الواي فاي المباشر...", fontSize = 12.sp, color = TextMuted)
                }
            }

            // Option to connect by custom scan QR
            Card(
                onClick = { isCameraActive = true },
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .border(1.dp, GlassBorderColor, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = "مسح الكود", tint = ElectroCyan)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("مسح كود المستقبل 🤳", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextLight)
                            Text("اقتران فوري بدون انتظار البحث اللاسلكي", fontSize = 11.sp, color = TextMuted)
                        }
                    }
                    Icon(Icons.Filled.ArrowForward, contentDescription = "فتح", tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Discovered Peers list view
            Text(
                "الأجهزة المكتشفة القريبة",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextLight,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            val devicesList = viewModel.discoveredDevices.value
            if (devicesList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = ElectroCyan, strokeWidth = 2.dp, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("بانتظار ظهور الأجهزة المجاورة...", fontSize = 12.sp, color = TextMuted)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(devicesList) { device ->
                        Card(
                            onClick = { viewModel.connectToDevice(device, asSender = true) },
                            colors = CardDefaults.cardColors(containerColor = SlateCard),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, GlassBorderColor, RoundedCornerShape(16.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(ElectroCyan.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Smartphone, contentDescription = "قرص اتصال", tint = ElectroCyan)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(device.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextLight)
                                        Text(device.ipAddress, fontSize = 11.sp, color = TextMuted)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (device.isGroupOwner) {
                                        Text("رئيسي ⭐", fontSize = 10.sp, color = ElectroCyan, modifier = Modifier.padding(end = 6.dp))
                                    }
                                    Icon(Icons.Filled.FlashOn, contentDescription = "اتصال", tint = SpeedGreen)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- RECEIVER SCREEN (STANDBY SERVER MODE) -----------------
@Composable
fun ReceiverScreen(viewModel: ShareViewModel) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("receiver_panel")
    ) {
        // App bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { viewModel.currentScreen = Screen.DASHBOARD }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع", tint = TextLight)
            }
            Text("قناة الاستقبال النشطة", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextLight)
            Box(Modifier.size(40.dp))
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Hotspot/SSID Info card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GlassBorderColor, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("بيانات البث المباشر (P2P Hotspot)", fontSize = 12.sp, color = TextMuted)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("شبكة ناقل: Naqel_Hotspot_Express", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextLight)
                        Text("الرقم السري: NaqelPass_1234", fontSize = 12.sp, color = ElectroCyan)
                    }
                }
            }

            // Premium custom simulated QR code
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .size(240.dp)
                        .border(1.dp, GlassBorderColor, RoundedCornerShape(24.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        QRStylizedCanvas(payload = viewModel.qrCodePayload)
                    }
                }
            }

            // Instructions text
            item {
                Text(
                    text = "امسح رمز الاستقرار هذا من الهاتف المرسل لتأسيس الاتصال المقبسي الآمن عالي السرعة تلقائياً",
                    fontSize = 13.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            // Quick instant fallback simulator trigger
            item {
                Button(
                    onClick = {
                        // Triggers instant simulated receiver pipeline connected to a virtual sender
                        viewModel.connectedDevice = P2PDevice("حافظ الرميح 📱", "192.168.49.5", false)
                        viewModel.connectToDevice(
                            P2PDevice("حافظ الرميح 📱", "192.168.49.5", false),
                            asSender = false
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HighPowerBlue),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E7FF)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "محاكاة", tint = ElectroCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("البدء الفوري (محاكاة استقبال ملفات ومحرر)", color = TextLight, fontWeight = FontWeight.Bold)
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ---------------- TRANSIT PROGRESS SCREEN (SENDER OR RECEIVER PIPELINE) -----------------
@Composable
fun ActiveTransferScreen(
    state: com.example.ui.TransferProgressState,
    connectedDevice: P2PDevice?
) {
    val progressPercent = state.progress.toInt()
    val isSending = state.direction == "SEND"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSlate)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Role Banner indicator with glowing halo
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSending) LaserMagenta.copy(alpha = 0.15f) else SpeedGreen.copy(alpha = 0.15f))
                    .border(1.dp, if (isSending) LaserMagenta else SpeedGreen, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isSending) "جاري إرسال الملفات" else "جاري استقبال الملفات",
                    color = if (isSending) LaserMagenta else SpeedGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Giant fluid animated round progress tracker with custom speeds
            Box(
                modifier = Modifier.size(170.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer circle tracker background
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color(0xFF1E293B),
                        style = Stroke(width = 14.dp.toPx())
                    )
                }
                
                // Infinite smooth brush tracer progress
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = if (isSending) LaserMagenta else ElectroCyan,
                        startAngle = -90f,
                        sweepAngle = (state.progress / 100f) * 360f,
                        useCenter = false,
                        style = Stroke(width = 14.dp.toPx())
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$progressPercent%",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = state.transferSpeed,
                        fontSize = 14.sp,
                        color = SpeedGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // File indices & Meta name
            Text(
                "ملف ${state.index} من أصل ${state.totalCount}",
                fontSize = 13.sp,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                state.fileName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // Peer active device info block
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("الطرف المتصل", fontSize = 11.sp, color = TextMuted)
                        Text(connectedDevice?.name ?: "جهاز أندرويد", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Icon(Icons.Filled.Link, contentDescription = "رابط نشط", tint = ElectroCyan)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            Text("يرجى إبقاء التطبيق مفتوحاً وعدم تدوير الشاشة لضمان أقصى كفاءة للمقبس", fontSize = 11.sp, color = TextMuted, textAlign = TextAlign.Center)
        }
    }
}

// ---------------- HISTORY LOG SCREEN -----------------
@Composable
fun HistoryScreen(viewModel: ShareViewModel) {
    val historyList by viewModel.allTransfers.collectAsState()
    
    val totalSent = remember(historyList) {
        historyList.filter { it.direction == "SEND" && it.status == "COMPLETED" }.sumOf { it.fileSize }
    }
    val totalReceived = remember(historyList) {
        historyList.filter { it.direction == "RECEIVE" && it.status == "COMPLETED" }.sumOf { it.fileSize }
    }

    val totalSentStr = formatSizeHelper(totalSent)
    val totalRecStr = formatSizeHelper(totalReceived)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("history_panel")
    ) {
        // App bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("سجل عمليات النقل والمشاركة", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextLight)
            TextButton(onClick = { viewModel.clearHistory() }) {
                Text("تفريغ", color = LaserMagenta, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Aggregate statistics ribbon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Total Sent Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, GlassBorderColor, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("إجمالي المرسل 📤", fontSize = 11.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(totalSentStr, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ElectroCyan)
                }
            }

            // Total Received Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, GlassBorderColor, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("إجمالي المستلم 📥", fontSize = 11.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(totalRecStr, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SpeedGreen)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // History items lazy vertical list (Database persisted reactive model)
        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.History, contentDescription = "خال", tint = TextMuted, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("لا يوجد سجلات حتى الآن", fontSize = 13.sp, color = TextMuted)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(historyList) { transfer ->
                    TransferHistoryItem(transfer)
                }
            }
        }
    }
}

// Single Transfer Row Composable
@Composable
fun TransferHistoryItem(transfer: TransferEntity) {
    val isSend = transfer.direction == "SEND"
    val sizeStr = formatSizeHelper(transfer.fileSize)
    
    // Formatting date helper
    val timeFormatted = remember(transfer.timestamp) {
        val date = java.util.Date(transfer.timestamp)
        val format = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
        format.format(date)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SlateCard)
            .border(1.dp, GlassBorderColor, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isSend) LaserMagenta.copy(alpha = 0.1f) else SpeedGreen.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSend) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                    contentDescription = transfer.direction,
                    tint = if (isSend) LaserMagenta else SpeedGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    transfer.fileName, 
                    fontSize = 13.sp, 
                    fontWeight = FontWeight.Bold, 
                    color = TextLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${transfer.peerName} • $sizeStr • $timeFormatted",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }
        
        // Status Chip
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (transfer.status == "COMPLETED") SpeedGreen.copy(alpha = 0.15f) else LaserMagenta.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (transfer.status == "COMPLETED") "مكتمل" else "جاري",
                color = if (transfer.status == "COMPLETED") SpeedGreen else LaserMagenta,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ---------------- HELP SCREEN -----------------
@Composable
fun HelpScreen() {
    val faqs = listOf(
        FAQItem(
            "كيف يعمل نقل ناقل بدون إنترنت؟ 🛰️",
            "يقوم التطبيق بالاستفادة من تقنية Wi-Fi Direct (الواي فاي المباشر الخفي) أو تشغيل نقطة اتصال محلية (P2P Hotspot). يتم تأسيس شبكة مصغرة بين الهاتفين لتسريع النقل عبر الـ Sockets العادية مع تفادي الراوتر المنزلي تماماً للتحرك بأي مكان."
        ),
        FAQItem(
            "ما هي السرعة الجبرية التي يوفرها؟ ⚡",
            "بما أن النقل مباشر ولا يخضع لسرعة خط الإنترنت الخارجي، فإنه يعتمد كلياً على فئة شريحة الواي فاي بجهازك لسرعة مذهلة تتجاوز غالباً 40 ميجابايت بالثانية (أسرع ٥٠ مرة من البلوتوث التقليدي)."
        ),
        FAQItem(
            "لماذا يطلب صلاحية الموقع الجغرافي والكاميرا؟ 🕵️",
            "يفرض نظام تشغيل أندرويد صلاحية الموقع الجغرافي للسماح للهاتف بالبحث ومسح شبكات الواي فاي والاقتران عن قرب. بينما نحتاج للكاميرا لغرض مسح رمز QR وبدء الاقتران المباشر بلحظة."
        ),
        FAQItem(
            "هل يدعم النقل مع أنظمة أخرى كالحاسوب؟ 💻",
            "نعم، مستقبلاً سيدعم التطبيق تشغيل سيرفر Ktor ويب خفيف، لتبادل الملفات بفتح عنوان السيرفر والتحميل من أي جهاز."
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("قاعدة المعرفة والتعليمات", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextLight)
            Text("احصل على إجابات سريعة لحلول ومعضلات نقل الملفات اللاسلكية", fontSize = 11.sp, color = TextMuted)
            Spacer(modifier = Modifier.height(6.dp))
        }

        items(faqs) { faq ->
            var isExpanded by remember { mutableStateOf(false) }
            Card(
                onClick = { isExpanded = !isExpanded },
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlassBorderColor, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(faq.question, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextLight, modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = "توسيع",
                            tint = ElectroCyan
                        )
                    }
                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(faq.answer, fontSize = 12.sp, color = TextMuted, lineHeight = 18.sp)
                    }
                }
            }
        }
    }
}

data class FAQItem(val question: String, val answer: String)

// ---------------- CUSTOM COMPOSABLE GRAPHICS Helpers -----------------

// Pulsing circular sonar for Wi-Fi P2P device scans
@Composable
fun RadarSonarAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "sonar")
    val pulseRatio by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radiusPulse"
    )
    val opacityRatio by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "opacityPulse"
    )

    Canvas(modifier = Modifier.size(240.dp)) {
        drawCircle(
            color = ElectroCyan.copy(alpha = opacityRatio),
            radius = size.width / 2 * pulseRatio,
            style = Stroke(width = 2.dp.toPx())
        )
        drawCircle(
            color = ElectroCyan.copy(alpha = opacityRatio * 0.4f),
            radius = size.width / 2 * (pulseRatio * 0.7f),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

// Gorgeous stylized Canvas QR Code
@Composable
fun QRStylizedCanvas(payload: String) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val totalGrid = 15
        val cellSize = size.width / totalGrid
        
        // Draw 3 primary corner finder patterns (Classic QR signature)
        drawCornerFinder(0f, 0f, cellSize) // top left
        drawCornerFinder((totalGrid - 5) * cellSize, 0f, cellSize) // top right
        drawCornerFinder(0f, (totalGrid - 5) * cellSize, cellSize) // bottom left

        // Populate randomized central blocks for highly realistic representation
        val randomBits = listOf(
            2 to 2, 4 to 3, 2 to 6, 8 to 4, 9 to 9, 11 to 2, 6 to 11, 13 to 14,
            7 to 7, 7 to 8, 8 to 7, 10 to 11, 12 to 12, 11 to 14, 5 to 5,
            6 to 6, 8 to 11, 10 to 3, 14 to 8, 12 to 6, 12 to 10, 11 to 7
        )

        for ((col, row) in randomBits) {
            drawRect(
                color = TextLight,
                topLeft = Offset(col * cellSize, row * cellSize),
                size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCornerFinder(
    x: Float,
    y: Float,
    cellSize: Float
) {
    val finderPixels = 5
    val length = finderPixels * cellSize
    
    // Outer big black/light box
    drawRect(
        color = ElectroCyan,
        topLeft = Offset(x, y),
        size = androidx.compose.ui.geometry.Size(length, length)
    )
    
    // Middle background mask box
    drawRect(
        color = SlateCard,
        topLeft = Offset(x + cellSize, y + cellSize),
        size = androidx.compose.ui.geometry.Size(length - cellSize * 2, length - cellSize * 2)
    )
    
    // Core center anchor
    drawRect(
        color = ElectroCyan,
        topLeft = Offset(x + cellSize * 1.5f, y + cellSize * 1.5f),
        size = androidx.compose.ui.geometry.Size(length - cellSize * 3, length - cellSize * 3)
    )
}

// Storage formatting helper
private fun formatSizeHelper(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format("%.2f جيجابايت", gb)
        mb >= 1.0 -> String.format("%.2f ميجابايت", mb)
        else -> String.format("%.1f كيلوبايت", kb)
    }
}
