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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
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
    viewModel: MainViewModel,
    boardManager: BoardManager
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val usb = remember { Usb(context) }
    val dfuEngine = remember { DfuSeEngine(usb) }

    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()

    val layoutDirection = if (currentLanguage == "fa") LayoutDirection.Rtl else LayoutDirection.Ltr
    val isFa = currentLanguage == "fa"

    val bgColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F5F5)
    val cardBg = if (isDarkMode) Color(0xFF252525) else Color(0xFFFFFFFF)
    val textColor = if (isDarkMode) Color.White else Color.Black
    val descColor = if (isDarkMode) Color.White.copy(alpha = 0.5f) else Color.Gray

    var logs by remember { mutableStateOf(if(isFa) "> سیستم آماده است.\n" else "> System Ready.\n") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf(if(isFa) "انتخاب فایل فریم‌ور" else "Select Firmware File") }
    var isBusy by remember { mutableStateOf(false) }
    var deviceConnected by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    fun log(msg: String) { logs += "$msg\n" }

    LaunchedEffect(logs) { scrollState.animateScrollTo(scrollState.maxValue) }

    fun toggleConnection() {
        if (deviceConnected) {
            usb.disconnect()
            deviceConnected = false
            log(if(isFa) "> قطع شد." else "> Disconnected.")
        } else {
            val device = usb.findDevice()
            if (device != null) {
                usb.requestPermission(device) { granted ->
                    if (granted) {
                        if (usb.connect(device)) {
                            deviceConnected = true
                            log(if(isFa) "> متصل شد: ${device.deviceName}" else "> Connected: ${device.deviceName}")
                        } else {
                            log(if(isFa) "> اتصال ناموفق بود." else "> Connection failed.")
                        }
                    } else {
                        log(if(isFa) "> مجوز داده نشد." else "> Permission denied.")
                    }
                }
            } else {
                log(if(isFa) "> دستگاه STM32 DFU پیدا نشد." else "> STM32 DFU Device not found.")
            }
        }
    }

    fun startFlash() {
        if (selectedFileUri == null) {
            log(if(isFa) "> خطا: فایلی انتخاب نشده." else "> Error: No file selected.")
            return
        }
        if (!deviceConnected) {
            log(if(isFa) "> خطا: متصل نیست." else "> Error: Not connected.")
            return
        }

        isBusy = true
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val stream = context.contentResolver.openInputStream(selectedFileUri!!)
                val bytes = stream?.readBytes()
                stream?.close()

                if (bytes != null) {
                    withContext(Dispatchers.Main) { log(if(isFa) "> شروع عملیات فلش..." else "> Starting Flash Sequence...") }

                    val success = dfuEngine.flashFirmware(bytes) { msg ->
                        coroutineScope.launch(Dispatchers.Main) { log(msg) }
                    }

                    withContext(Dispatchers.Main) {
                        if (success) log(if(isFa) "\n> [موفق] فریم‌ور آپدیت شد.\n> ریست برد..." else "\n> [SUCCESS] Firmware Updated.\n> Resetting Board...")
                        else log(if(isFa) "\n> [ناموفق] عملیات لغو شد." else "\n> [FAILED] Operation Aborted.")
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
            log(if(isFa) "> خطا: متصل نیست." else "> Error: Not connected.")
            return
        }
        isBusy = true
        coroutineScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { log(if(isFa) "> هشدار: شروع پاکسازی کامل چیپ..." else "> WARNING: Starting Full Chip Erase...") }
            val success = dfuEngine.fullChipErase { msg ->
                coroutineScope.launch(Dispatchers.Main) { log(msg) }
            }
            withContext(Dispatchers.Main) {
                if(success) log(if(isFa) "> [موفق] چیپ پاک شد." else "> [SUCCESS] Chip Erased.")
                else log(if(isFa) "> [ناموفق] خطای پاکسازی." else "> [FAILED] Erase Error.")
            }
            isBusy = false
        }
    }

    fun startOnlineUpgrade() {
        if (!deviceConnected) {
            log(if(isFa) "> خطا: متصل نیست." else "> Error: Not connected.")
            return
        }
        isBusy = true

        coroutineScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { log(if(isFa) "> مرحله ۱/۴: بررسی تنظیمات..." else "> Step 1/4: Checking configuration...") }

                val configUrl = URL("$APP_CONFIG_URL?t=${System.currentTimeMillis()}")
                val jsonContent = configUrl.readText()
                val rootObject = JSONObject(jsonContent)
                val firmwareObject = rootObject.getJSONObject("firmware")
                val downloadUrlString = firmwareObject.getString("url")

                withContext(Dispatchers.Main) { log(if(isFa) "> فریم‌ور پیدا شد! در حال دانلود..." else "> Firmware found! Downloading...") }

                val firmwareUrl = URL(downloadUrlString)
                val firmwareBytes = firmwareUrl.readBytes()

                withContext(Dispatchers.Main) { log(if(isFa) "> دانلود کامل شد (${firmwareBytes.size} بایت)." else "> Download Complete (${firmwareBytes.size} bytes).") }

                withContext(Dispatchers.Main) { log(if(isFa) "> مرحله ۲/۳: پاکسازی چیپ..." else "> Step 2/3: Erasing Chip...") }
                val eraseSuccess = dfuEngine.fullChipErase { msg ->
                    coroutineScope.launch(Dispatchers.Main) { log(msg) }
                }

                if (!eraseSuccess) {
                    withContext(Dispatchers.Main) { log(if(isFa) "> [خطا] پاکسازی ناموفق. توقف." else "> [ERROR] Erase Failed. Stopping.") }
                    isBusy = false
                    return@launch
                }

                withContext(Dispatchers.Main) { log(if(isFa) "> مرحله ۳/۳: فلش فریم‌ور جدید..." else "> Step 3/3: Flashing New Firmware...") }
                val flashSuccess = dfuEngine.flashFirmware(firmwareBytes) { msg ->
                    coroutineScope.launch(Dispatchers.Main) { log(msg) }
                }

                withContext(Dispatchers.Main) {
                    if (flashSuccess) log(if(isFa) "\n> [آپدیت کامل] دستگاه با موفقیت بروزرسانی شد." else "\n> [UPGRADE COMPLETE] Device Updated Successfully.")
                    else log(if(isFa) "\n> [خطا] فلش ناموفق بود." else "\n> [ERROR] Flashing Failed.")
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    log("> [Error]: ${e.localizedMessage}")
                    if (e.message?.contains("JSONObject") == true) {
                        log(if(isFa) "> خطای فرمت JSON. بخش 'firmware' را چک کنید." else "> JSON Format Error. Check 'firmware' section.")
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
            log(if(isFa) "> فایل بارگذاری شد: $fileName" else "> File loaded: $fileName")
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
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
                        text = if(isFa) "فلشر STM32 DFU" else "STM32 DFU FLASHER ",
                        color = textColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
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

                val connectTitle = if (deviceConnected) (if(isFa) "متصل" else "Connected") else (if(isFa) "اتصال" else "Connect")
                val connectDesc = if (deviceConnected) (if(isFa) "برای قطع ضربه بزنید" else "Tap to disconnect") else (if(isFa) "یافتن دستگاه" else "Find Device")

                NeonCard(
                    title = connectTitle,
                    desc = connectDesc,
                    icon = if (deviceConnected) Icons.Default.Usb else Icons.Default.UsbOff,
                    color = if (deviceConnected) NeonGreen else NeonRed,
                    cardBg = cardBg,
                    textColor = textColor,
                    descColor = descColor,
                    onClick = { toggleConnection() }
                )

                NeonCard(
                    title = if(isFa) "آپدیت آنلاین" else "Online Upgrade",
                    desc = if(isFa) "بررسی ابری و فلش" else "Check Cloud & Flash",
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
                    desc = if(selectedFileUri == null) (if(isFa) "برای انتخاب فایل .dfu کلیک کنید" else "Click to browse .dfu files") else (if(isFa) "آماده برای فلش" else "Ready to flash"),
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
                            title = if(isFa) "پاکسازی کامل" else "Full Erase",
                            desc = if(isFa) "پاک کردن چیپ" else "Wipe Chip",
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
                            title = if(isFa) "فلش فایل" else "Flash File",
                            desc = if (isBusy) (if(isFa) "در حال کار..." else "Working...") else (if(isFa) "شروع" else "Start"),
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
                    text = if(isFa) "خروجی ترمینال" else "TERMINAL OUTPUT",
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
                    // برای لاگ‌ها همیشه LTR باشد بهتر است چون انگلیسی هستند
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
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