package com.bluestudio.manager.screens.home

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.bluestudio.manager.MainViewModel
import com.bluestudio.manager.managers.BoardManager
import com.bluestudio.manager.managers.TerminalManager
import com.bluestudio.manager.model.ConnectionStatus
import org.json.JSONObject
import java.net.URL

private const val APP_CONFIG_URL = "https://bluewaverobotics.ir/app_config.json"

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    boardManager: BoardManager?,
    terminalManager: TerminalManager?,
    openTerminal: () -> Unit,
    openEditor: () -> Unit,
    openScripts: () -> Unit,
    openExplorer: () -> Unit,
    openFlasher: () -> Unit,
    openSettings: () -> Unit,
    openMacros: () -> Unit,
    openPlotter: () -> Unit,
    openLogger: () -> Unit,
    openJoystick: () -> Unit
) {
    val activity = LocalActivity.current as Activity
    val coroutineScope = rememberCoroutineScope()

    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isProMode by viewModel.isProMode.collectAsState()
    val proName by viewModel.proDeviceId.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()

    val layoutDirection = if (currentLanguage == "fa") LayoutDirection.Rtl else LayoutDirection.Ltr
    val isFa = currentLanguage == "fa"

    val bgColor = if (isProMode) Color.Transparent else if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F5F5)
    val itemBg = if (isDarkMode) Color(0xFF252525) else Color(0xFFFFFFFF)
    val textColor = if (isDarkMode) Color.White else Color.Black
    val descColor = if (isDarkMode) Color.White.copy(alpha = 0.5f) else Color.Gray
    val proHeaderColor = if (isDarkMode) Color.Cyan else Color(0xFF0D47A1)

    val status by viewModel.status.collectAsState()
    val isConnected = status is ConnectionStatus.Connected
    val isConnecting = status is ConnectionStatus.Connecting

    val deviceNameText = if (isConnected) viewModel.microDevice?.usbDevice?.productName ?: (if (isFa) "دستگاه ناشناس" else "Unknown Device") else (if (isFa) "بدون دستگاه" else "No Device")

    val statusText = when(status) {
        is ConnectionStatus.Connected -> if (isFa) "متصل" else "Connected"
        is ConnectionStatus.Connecting -> if (isFa) "در حال اتصال..." else "Connecting..."
        else -> if (isFa) "قطع" else "Disconnected"
    }

    fun onConnectClick() {
        if (isConnected || isConnecting) boardManager?.onDisconnectDevice() else boardManager?.detectUsbDevices()
    }

    fun onSoftReset() {
        if (isConnected) {
            terminalManager?.softResetDevice {
                activity.runOnUiThread {
                    Toast.makeText(activity, if (isFa) "ریست نرم انجام شد (Ctrl+D)" else "Soft Reset Sent (Ctrl+D)", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(activity, if (isFa) "متصل نیستید" else "Not Connected", Toast.LENGTH_SHORT).show()
        }
    }

    fun onHelp() {
        Toast.makeText(activity, if (isFa) "دریافت مستندات..." else "Fetching Documentation...", Toast.LENGTH_SHORT).show()
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val configUrl = URL("$APP_CONFIG_URL?t=${System.currentTimeMillis()}")
                val jsonContent = configUrl.readText()
                val rootObject = JSONObject(jsonContent)
                val linksObject = rootObject.optJSONObject("links")
                val docUrl = linksObject?.optString("documentation", "https://bluewaverobotics.ir")
                    ?: "https://bluewaverobotics.ir"

                withContext(Dispatchers.Main) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(docUrl))
                    activity.startActivity(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://bluewaverobotics.ir"))
                    activity.startActivity(intent)
                }
            }
        }
    }

    val connectTitle = when {
        isConnecting -> if (isFa) "صبر کنید..." else "Wait..."
        isConnected -> if (isFa) "قطع اتصال" else "Disconnect"
        else -> if (isFa) "اتصال" else "Connect"
    }

    val connectColor = when {
        isConnecting -> Color.Gray
        isConnected -> Color(0xFFFF5252)
        else -> Color(0xFF448AFF)
    }

    val connectDesc = if (isConnecting) (if (isFa) "پردازش..." else "Processing...") else (if (isFa) "USB و بلوتوث" else "USB & Bluetooth")
    val connectMsg = if (isFa) "ابتدا متصل شوید!" else "Connect First!"

    val menuItems = listOf(
        MenuItem(connectTitle, connectDesc, if (isConnected) Icons.Default.Close else Icons.Default.Add, connectColor) { onConnectClick() },
        MenuItem(if (isFa) "ترمینال" else "Terminal", if (isFa) "دسترسی REPL" else "REPL Access", Icons.Default.List, if (isConnected) Color(0xFF69F0AE) else Color.Gray) { if (isConnected) openTerminal() else Toast.makeText(activity, connectMsg, Toast.LENGTH_SHORT).show() },
        MenuItem(if (isFa) "مدیریت فایل" else "File Manager", if (isFa) "داخلی و SD" else "Internal & SD", Icons.Default.Home, if (isConnected) Color(0xFFFFAB40) else Color.Gray) { if (isConnected) openExplorer() else Toast.makeText(activity, connectMsg, Toast.LENGTH_SHORT).show() },
        MenuItem(if (isFa) "اسکریپت‌ها" else "Scripts", if (isFa) "اجرای اسکریپت .py" else "Run .py scripts", Icons.Default.Edit, Color(0xFFE040FB)) { openScripts() },
        MenuItem(if (isFa) "فریم‌ور" else "Firmware", if (isFa) "فلش فایل .dfu" else "Flash .dfu file", Icons.Default.Build, Color(0xFF00E5FF)) { openFlasher() },
        MenuItem(if (isFa) "ریست نرم" else "Soft Reset", "CTRL + D", Icons.Default.Refresh, Color(0xFFFF5252)) { onSoftReset() },
        MenuItem(if (isFa) "تنظیمات" else "Settings", if (isFa) "پیکربندی برنامه" else "App Config", Icons.Default.Settings, Color(0xFFFF4081)) { openSettings() },
        MenuItem(if (isFa) "راهنما" else "Help", if (isFa) "مستندات" else "Documentation", Icons.Default.Info, Color(0xFF64FFDA)) { onHelp() }
    )

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp).statusBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if(isProMode) "BLUE CORE PRO" else (if (isFa) "مدیریت میکرو بلو پای" else "MicroBluePy Manager"),
                        color = if(isProMode) proHeaderColor else textColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    StatusPill(isConnected, if (isFa) "متصل" else "Connected", if (isFa) "آفلاین" else "Offline")
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize()) {
                if (isProMode) {
                    val brush = if (isDarkMode) Brush.verticalGradient(listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))) else Brush.verticalGradient(listOf(Color(0xFFE3F2FD), Color(0xFFBBDEFB), Color(0xFF90CAF9)))
                    Box(modifier = Modifier.fillMaxSize().background(brush))
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(bgColor))
                }

                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(15.dp),
                        horizontalArrangement = Arrangement.spacedBy(15.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        item(span = { GridItemSpan(2) }) {
                            Column {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFF263238)).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                                ) {
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                        Image(
                                            painter = painterResource(id = com.bluestudio.manager.R.drawable.header_bg),
                                            contentDescription = null,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.align(Alignment.CenterEnd).size(200.dp).padding(end = 8.dp).alpha(0.8f)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier.matchParentSize().background(brush = Brush.horizontalGradient(colors = listOf(Color(0xFF121212), Color(0xFF121212).copy(alpha = 0.6f), Color.Transparent)))
                                    )
                                    Column(modifier = Modifier.padding(20.dp).align(Alignment.CenterStart)) {
                                        Text(text = if(isProMode) (if(isFa) "دستگاه پرو شناسایی شد" else "PRO DEVICE DETECTED") else (if(isFa) "دستگاه هدف" else "TARGET DEVICE"), color = if(isProMode) Color.Cyan else Color(0xFF69F0AE), fontSize = 12.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                            Text(text = if(isProMode) proName else deviceNameText, color = if(isProMode) Color.Yellow else Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if(isConnected) Color.Green else Color.Red))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(text = statusText, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(25.dp))
                                if (isProMode) {
                                    Text(text = if(isFa) "ابزارهای پرو" else "PRO TOOLS", color = proHeaderColor, fontSize = 14.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(10.dp))
                                } else {
                                    Text(text = if(isFa) "ابزارها" else "TOOLS", color = descColor, fontSize = 14.sp, letterSpacing = 1.2.sp)
                                    Spacer(modifier = Modifier.height(0.dp))
                                }
                            }
                        }

                        if (isProMode) {
                            item(span = { GridItemSpan(2) }) { ProHomeButton(if (isFa) "پلاتر" else "Plotter", Color(0xFFD32F2F), isDarkMode, isFa) { openPlotter() } }

                            item(span = { GridItemSpan(2) }) {
                                ProHomeButton(if (isFa) "جوی‌استیک" else "Joystick", Color(0xFF1976D2), isDarkMode, isFa) { openJoystick() }
                            }

                            item(span = { GridItemSpan(2) }) { ProHomeButton(if (isFa) "لاگر" else "Logger", Color(0xFF388E3C), isDarkMode, isFa) { openLogger() } }
                            item(span = { GridItemSpan(2) }) { ProHomeButton(if (isFa) "ماکروها" else "Macros", Color(0xFFFBC02D), isDarkMode, isFa) { openMacros() } }
                            item(span = { GridItemSpan(2) }) { HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = if(isDarkMode) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.1f)) }
                        }

                        items(menuItems) { item -> MenuCard(item, itemBg, textColor, descColor) }

                        item(span = { GridItemSpan(2) }) {
                            Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp), contentAlignment = Alignment.Center) {
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(text = "Copyright © 2026 BlueWave", color = descColor, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProHomeButton(text: String, color: Color, isDarkMode: Boolean, isFa: Boolean, onClick: () -> Unit) {
    val icon = when {
        text.contains("Plotter") || text.contains("پلاتر") -> Icons.Default.DateRange
        text.contains("Joystick") || text.contains("جوی‌استیک") -> Icons.Default.PlayArrow
        text.contains("Logger") || text.contains("لاگر") -> Icons.Default.Share
        text.contains("Macros") || text.contains("ماکروها") -> Icons.Default.Star
        else -> Icons.Default.Settings
    }
    val containerColor = if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFFFFFFF)
    val contentColor = if (isDarkMode) Color.White else Color.Black
    Card(
        modifier = Modifier.height(90.dp).fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxHeight().width(6.dp).background(color).align(Alignment.CenterStart))
            Row(modifier = Modifier.fillMaxSize().padding(start = 20.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.Center) {
                    Text(text = text, color = contentColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(text = if(isFa) "قابلیت پرو" else "Pro Feature", color = contentColor.copy(alpha = 0.5f), fontSize = 10.sp)
                }
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
fun StatusPill(isConnected: Boolean, connectedText: String, disconnectedText: String) {
    val color = if (isConnected) Color.Green else Color.Red
    val text = if (isConnected) connectedText else disconnectedText
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(color.copy(alpha = 0.2f)).border(1.dp, color, RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, color = color, fontSize = 12.sp)
    }
}

@Composable
fun MenuCard(item: MenuItem, cardBg: Color, titleColor: Color, descColor: Color) {
    val alpha = if (item.title == "Wait..." || item.title == "صبر کنید...") 0.5f else 1f
    Column(modifier = Modifier.clip(RoundedCornerShape(15.dp)).background(cardBg.copy(alpha = alpha)).border(1.dp, item.color.copy(alpha = 0.3f), RoundedCornerShape(15.dp)).clickable(enabled = alpha == 1f) { item.onClick() }.padding(16.dp).height(100.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(item.color.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(item.icon, contentDescription = null, tint = item.color)
        }
        Column {
            Text(text = item.title, color = titleColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(text = item.desc, color = descColor, fontSize = 12.sp)
        }
    }
}

data class MenuItem(val title: String, val desc: String, val icon: ImageVector, val color: Color, val onClick: () -> Unit)