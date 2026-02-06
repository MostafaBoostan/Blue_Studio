package micro.repl.ma7moud3ly.screens.home

import android.app.Activity
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import micro.repl.ma7moud3ly.MainViewModel
import micro.repl.ma7moud3ly.managers.BoardManager
import micro.repl.ma7moud3ly.managers.TerminalManager
import micro.repl.ma7moud3ly.model.ConnectionStatus

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
    openSettings: () -> Unit
) {
    val activity = LocalActivity.current as Activity

    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isProMode by viewModel.isProMode.collectAsState()
    val proName by viewModel.proDeviceId.collectAsState()

    val bgColor = if (isProMode) Color.Transparent else if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F5F5)
    val itemBg = if (isDarkMode) Color(0xFF252525) else Color(0xFFFFFFFF)
    val textColor = if (isDarkMode) Color.White else Color.Black
    val descColor = if (isDarkMode) Color.White.copy(alpha = 0.5f) else Color.Gray

    val proHeaderColor = if (isDarkMode) Color.Cyan else Color(0xFF0D47A1)
    val proTitleColor = if (isDarkMode) Color.Yellow else Color(0xFFE65100)

    val status by viewModel.status.collectAsState()
    val isConnected = status is ConnectionStatus.Connected
    val isConnecting = status is ConnectionStatus.Connecting

    val deviceName = if (isConnected) viewModel.microDevice?.usbDevice?.productName ?: "Unknown Device" else "No Device"

    val statusText = when(status) {
        is ConnectionStatus.Connected -> "Connected"
        is ConnectionStatus.Connecting -> "Connecting..."
        else -> "Disconnected"
    }

    fun onConnectClick() {
        if (isConnected || isConnecting) {
            boardManager?.onDisconnectDevice()
        } else {
            boardManager?.detectUsbDevices()
        }
    }

    fun onSoftReset() {
        if (isConnected) {
            terminalManager?.softResetDevice {
                activity.runOnUiThread {
                    Toast.makeText(activity, "Soft Reset Sent (Ctrl+D)", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(activity, "Not Connected", Toast.LENGTH_SHORT).show()
        }
    }

    fun onHelp() {
        try {
            val url = "https://bluewaverobotics.ir"
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
            activity.startActivity(intent)
        } catch (e: Exception) { e.printStackTrace() }
    }

    val connectTitle = when {
        isConnecting -> "Wait..."
        isConnected -> "Disconnect"
        else -> "Connect"
    }

    val connectColor = when {
        isConnecting -> Color.Gray
        isConnected -> Color(0xFFFF5252)
        else -> Color(0xFF448AFF)
    }

    val menuItems = listOf(
        MenuItem(
            title = connectTitle,
            desc = if (isConnecting) "Processing..." else "USB & Bluetooth",
            icon = if (isConnected) Icons.Default.Close else Icons.Default.Add,
            color = connectColor,
            onClick = { onConnectClick() }
        ),
        MenuItem(
            title = "Terminal",
            desc = "REPL Access",
            icon = Icons.Default.List,
            color = if (isConnected) Color(0xFF69F0AE) else Color.Gray,
            onClick = { if (isConnected) openTerminal() else Toast.makeText(activity, "Connect First!", Toast.LENGTH_SHORT).show() }
        ),
        MenuItem(
            title = "File Manager",
            desc = "Internal & SD",
            icon = Icons.Default.Home,
            color = if (isConnected) Color(0xFFFFAB40) else Color.Gray,
            onClick = { if (isConnected) openExplorer() else Toast.makeText(activity, "Connect First!", Toast.LENGTH_SHORT).show() }
        ),
        MenuItem(
            title = "Scripts",
            desc = "Run .py scripts",
            icon = Icons.Default.Edit,
            color = Color(0xFFE040FB),
            onClick = { openScripts() }
        ),
        MenuItem(
            title = "Firmware",
            desc = "Flash .dfu file",
            icon = Icons.Default.Build,
            color = Color(0xFF00E5FF),
            onClick = { openFlasher() }
        ),
        MenuItem(
            title = "Soft Reset",
            desc = "CTRL + D",
            icon = Icons.Default.Refresh,
            color = Color(0xFFFF5252),
            onClick = { onSoftReset() }
        ),
        MenuItem(
            title = "Settings",
            desc = "App Config",
            icon = Icons.Default.Settings,
            color = Color(0xFFFF4081),
            onClick = { openSettings() }
        ),
        MenuItem(
            title = "Help",
            desc = "Documentation",
            icon = Icons.Default.Info,
            color = Color(0xFF64FFDA),
            onClick = { onHelp() }
        )
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .statusBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if(isProMode) "BLUE CORE PRO" else "MicroBluePy Manager",
                    color = if(isProMode) proHeaderColor else textColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                StatusPill(isConnected)
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {

            if (isProMode) {
                val brush = if (isDarkMode) {
                    Brush.verticalGradient(listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)))
                } else {
                    Brush.verticalGradient(listOf(Color(0xFFE3F2FD), Color(0xFFBBDEFB), Color(0xFF90CAF9)))
                }
                Box(modifier = Modifier.fillMaxSize().background(brush))
            } else {
                Box(modifier = Modifier.fillMaxSize().background(bgColor))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(15.dp),
                    horizontalArrangement = Arrangement.spacedBy(15.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {

                    item(span = { GridItemSpan(2) }) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF263238))
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                            ) {
                                Image(
                                    painter = painterResource(id = micro.repl.ma7moud3ly.R.drawable.header_bg),
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .size(200.dp)
                                        .padding(end = 8.dp)
                                        .alpha(0.8f)
                                )

                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color(0xFF121212),
                                                    Color(0xFF121212).copy(alpha = 0.6f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                )

                                Column(
                                    modifier = Modifier
                                        .padding(20.dp)
                                        .align(Alignment.CenterStart)
                                ) {
                                    Text(
                                        text = if(isProMode) "PRO DEVICE DETECTED" else "TARGET DEVICE",
                                        color = if(isProMode) Color.Cyan else Color(0xFF69F0AE),
                                        fontSize = 12.sp,
                                        letterSpacing = 1.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if(isProMode) proName else deviceName,
                                        color = if(isProMode) Color.Yellow else Color.White,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if(isConnected) Color.Green else Color.Red)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = statusText,
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(25.dp))

                            if (isProMode) {
                                Text(
                                    text = "PRO TOOLS",
                                    color = proHeaderColor,
                                    fontSize = 14.sp,
                                    letterSpacing = 1.2.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            } else {
                                Text(
                                    text = "TOOLS",
                                    color = descColor,
                                    fontSize = 14.sp,
                                    letterSpacing = 1.2.sp
                                )
                                Spacer(modifier = Modifier.height(0.dp))
                            }
                        }
                    }

                    if (isProMode) {
                        item(span = { GridItemSpan(2) }) { ProHomeButton("Plotter", Color(0xFFD32F2F), isDarkMode) {} }
                        item(span = { GridItemSpan(2) }) { ProHomeButton("Joystick", Color(0xFF1976D2), isDarkMode) {} }
                        item(span = { GridItemSpan(2) }) { ProHomeButton("Logger", Color(0xFF388E3C), isDarkMode) {} }
                        item(span = { GridItemSpan(2) }) { ProHomeButton("Macros", Color(0xFFFBC02D), isDarkMode) {} }

                        item(span = { GridItemSpan(2) }) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 10.dp),
                                color = if(isDarkMode) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.1f)
                            )
                        }
                    }

                    items(menuItems) { item ->
                        MenuCard(item, itemBg, textColor, descColor)
                    }

                    item(span = { GridItemSpan(2) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Copyright © 2026 BlueWave",
                                color = descColor,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProHomeButton(text: String, color: Color, isDarkMode: Boolean, onClick: () -> Unit) {
    val icon = when (text) {
        "Plotter" -> Icons.Default.DateRange
        "Joystick" -> Icons.Default.PlayArrow
        "Logger" -> Icons.Default.Share
        "Macros" -> Icons.Default.Star
        else -> Icons.Default.Settings
    }

    val containerColor = if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFFFFFFF)
    val contentColor = if (isDarkMode) Color.White else Color.Black

    Card(
        modifier = Modifier
            .height(90.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(color)
                    .align(Alignment.CenterStart)
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 20.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = text,
                        color = contentColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pro Feature",
                        color = contentColor.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatusPill(isConnected: Boolean) {
    val color = if (isConnected) Color.Green else Color.Red
    val text = if (isConnected) "Connected" else "Offline"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.2f))
            .border(1.dp, color, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, color = color, fontSize = 12.sp)
    }
}

@Composable
fun MenuCard(item: MenuItem, cardBg: Color, titleColor: Color, descColor: Color) {
    val alpha = if (item.title == "Wait...") 0.5f else 1f

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(15.dp))
            .background(cardBg.copy(alpha = alpha))
            .border(1.dp, item.color.copy(alpha = 0.3f), RoundedCornerShape(15.dp))
            .clickable(enabled = item.title != "Wait...") { item.onClick() }
            .padding(16.dp)
            .height(100.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(item.color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(item.icon, contentDescription = null, tint = item.color)
        }

        Column {
            Text(
                text = item.title,
                color = titleColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = item.desc,
                color = descColor,
                fontSize = 12.sp
            )
        }
    }
}

data class MenuItem(
    val title: String,
    val desc: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)