package com.bluestudio.manager.screens.flash

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.bluestudio.manager.MainViewModel
import com.bluestudio.manager.managers.BoardManager
import com.bluestudio.manager.dfu.DfuSeEngine
import com.bluestudio.manager.dfu.Usb
import org.json.JSONObject
import java.net.URL

private const val APP_CONFIG_URL = "https://bluewaverobotics.ir/app_config.json"

private val NeonCyan = Color(0xFF00E5FF)
private val NeonGreen = Color(0xFF69F0AE)
private val NeonRed = Color(0xFFFF5252)
private val NeonYellow = Color(0xFFFFAB40)
private val DangerRed = Color(0xFFD50000)
private val UpgradePurple = Color(0xFFD500F9)

@Composable
fun FlashScreen(
    viewModel: com.bluestudio.manager.MainViewModel,
    boardManager: com.bluestudio.manager.managers.BoardManager
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val usb = remember { _root_ide_package_.com.bluestudio.manager.dfu.Usb(context) }
    val dfuEngine = remember { _root_ide_package_.com.bluestudio.manager.dfu.DfuSeEngine(usb) }

    val isDarkMode by viewModel.isDarkMode.collectAsState()

    val bgColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F5F5)
    val cardBg = if (isDarkMode) Color(0xFF252525) else Color(0xFFFFFFFF)
    val textColor = if (isDarkMode) Color.White else Color.Black
    val descColor = if (isDarkMode) Color.White.copy(alpha = 0.5f) else Color.Gray

    var logs by remember { mutableStateOf("> System Ready.\n") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("Select Firmware File") }
    var isBusy by remember { mutableStateOf(false) }
    var deviceConnected by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    fun log(msg: String) { logs += "$msg\n" }

    LaunchedEffect(logs) { scrollState.animateScrollTo(scrollState.maxValue) }

    fun toggleConnection() {
        if (deviceConnected) {
            usb.disconnect()
            deviceConnected = false
            log("> Disconnected.")
        } else {
            val device = usb.findDevice()
            if (device != null) {
                usb.requestPermission(device) { granted ->
                    if (granted) {
                        if (usb.connect(device)) {
                            deviceConnected = true
                            log("> Connected: ${device.deviceName}")
                        } else {
                            log("> Connection failed.")
                        }
                    } else {
                        log("> Permission denied.")
                    }
                }
            } else {
                log("> STM32 DFU Device not found.")
            }
        }
    }

    fun startFlash() {
        if (selectedFileUri == null) {
            log("> Error: No file selected.")
            return
        }
        if (!deviceConnected) {
            log("> Error: Not connected.")
            return
        }

        isBusy = true
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val stream = context.contentResolver.openInputStream(selectedFileUri!!)
                val bytes = stream?.readBytes()
                stream?.close()

                if (bytes != null) {
                    withContext(Dispatchers.Main) { log("> Starting Flash Sequence...") }

                    val success = dfuEngine.flashFirmware(bytes) { msg ->
                        coroutineScope.launch(Dispatchers.Main) { log(msg) }
                    }

                    withContext(Dispatchers.Main) {
                        if (success) log("\n> [SUCCESS] Firmware Updated.\n> Resetting Board...")
                        else log("\n> [FAILED] Operation Aborted.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { log("> Error: ${e.message}") }
            }
            isBusy = false
        }
    }

    fun startMassErase() {
        if (!deviceConnected) {
            log("> Error: Not connected.")
            return
        }
        isBusy = true
        coroutineScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { log("> WARNING: Starting Full Chip Erase...") }
            val success = dfuEngine.fullChipErase { msg ->
                coroutineScope.launch(Dispatchers.Main) { log(msg) }
            }
            withContext(Dispatchers.Main) {
                if(success) log("> [SUCCESS] Chip Erased.")
                else log("> [FAILED] Erase Error.")
            }
            isBusy = false
        }
    }

    fun startOnlineUpgrade() {
        if (!deviceConnected) {
            log("> Error: Not connected.")
            return
        }
        isBusy = true

        coroutineScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { log("> Step 1/4: Checking configuration...") }

                val configUrl = URL("$APP_CONFIG_URL?t=${System.currentTimeMillis()}")
                val jsonContent = configUrl.readText()
                val rootObject = JSONObject(jsonContent)
                val firmwareObject = rootObject.getJSONObject("firmware")
                val downloadUrlString = firmwareObject.getString("url")

                withContext(Dispatchers.Main) { log("> Firmware found! Downloading...") }

                val firmwareUrl = URL(downloadUrlString)
                val firmwareBytes = firmwareUrl.readBytes()

                withContext(Dispatchers.Main) { log("> Download Complete (${firmwareBytes.size} bytes).") }

                withContext(Dispatchers.Main) { log("> Step 2/3: Erasing Chip...") }
                val eraseSuccess = dfuEngine.fullChipErase { msg ->
                    coroutineScope.launch(Dispatchers.Main) { log(msg) }
                }

                if (!eraseSuccess) {
                    withContext(Dispatchers.Main) { log("> [ERROR] Erase Failed. Stopping.") }
                    isBusy = false
                    return@launch
                }

                withContext(Dispatchers.Main) { log("> Step 3/3: Flashing New Firmware...") }
                val flashSuccess = dfuEngine.flashFirmware(firmwareBytes) { msg ->
                    coroutineScope.launch(Dispatchers.Main) { log(msg) }
                }

                withContext(Dispatchers.Main) {
                    if (flashSuccess) log("\n> [UPGRADE COMPLETE] Device Updated Successfully.")
                    else log("\n> [ERROR] Flashing Failed.")
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    log("> [Error]: ${e.localizedMessage}")
                    if (e.message?.contains("JSONObject") == true) {
                        log("> JSON Format Error. Check 'firmware' section.")
                    }
                    e.printStackTrace()
                }
            } finally {
                isBusy = false
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedFileUri = it
            fileName = getFileName(context, it)
            log("> File loaded: $fileName")
        }
    }

    Scaffold(
        containerColor = bgColor,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STM32 DFU FLASHER ",
                    color = textColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (deviceConnected) NeonGreen else NeonRed)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            NeonCard(
                title = if (deviceConnected) "Connected" else "Connect",
                desc = if (deviceConnected) "Tap to disconnect" else "Find Device",
                icon = if (deviceConnected) Icons.Default.Usb else Icons.Default.UsbOff,
                color = if (deviceConnected) NeonGreen else NeonRed,
                cardBg = cardBg,
                textColor = textColor,
                descColor = descColor,
                onClick = { toggleConnection() }
            )

            NeonCard(
                title = "Online Upgrade",
                desc = "Check Cloud & Flash",
                icon = Icons.Default.CloudDownload,
                color = UpgradePurple,
                cardBg = cardBg,
                textColor = textColor,
                descColor = descColor,
                onClick = { if (!isBusy) startOnlineUpgrade() }
            )

            Divider(color = descColor.copy(alpha = 0.2f), thickness = 1.dp)

            NeonCard(
                title = fileName,
                desc = if(selectedFileUri == null) "Click to browse .dfu files" else "Ready to flash",
                icon = Icons.Default.FolderOpen,
                color = NeonCyan,
                cardBg = cardBg,
                textColor = textColor,
                descColor = descColor,
                onClick = { launcher.launch("*/*") }
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    NeonCard(
                        title = "Full Erase",
                        desc = "Wipe Chip",
                        icon = Icons.Default.DeleteForever,
                        color = DangerRed,
                        cardBg = cardBg,
                        textColor = textColor,
                        descColor = descColor,
                        onClick = { if (!isBusy) startMassErase() }
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    NeonCard(
                        title = "Flash File",
                        desc = if (isBusy) "Working..." else "Start",
                        icon = Icons.Default.Bolt,
                        color = if (isBusy) Color.Gray else NeonYellow,
                        cardBg = cardBg,
                        textColor = textColor,
                        descColor = descColor,
                        onClick = { if (!isBusy) startFlash() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "TERMINAL OUTPUT",
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Color(0xFF000000))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(15.dp))
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.verticalScroll(scrollState)) {
                    Text(
                        text = logs,
                        color = NeonGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun NeonCard(
    title: String,
    desc: String,
    icon: ImageVector,
    color: Color,
    cardBg: Color,
    textColor: Color,
    descColor: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(cardBg)
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(15.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column {
            Text(
                text = title,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = desc,
                color = descColor,
                fontSize = 12.sp
            )
        }
    }
}

fun getFileName(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) result = cursor.getString(index)
            }
        } finally { cursor?.close() }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) result = result?.substring(cut + 1)
    }
    return result ?: "Unknown File"
}