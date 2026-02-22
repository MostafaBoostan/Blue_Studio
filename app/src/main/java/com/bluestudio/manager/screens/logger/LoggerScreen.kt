package com.bluestudio.manager.screens.logger

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.bluestudio.manager.MainViewModel
import com.bluestudio.manager.managers.TerminalManager
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoggerScreen(
    viewModel: com.bluestudio.manager.MainViewModel,
    terminalManager: com.bluestudio.manager.managers.TerminalManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    val bgColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F5F5)
    val cardColor = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
    val textColor = if (isDarkMode) Color.White else Color.Black
    val logTextColor = if (isDarkMode) Color(0xFFFFCC80) else Color(0xFFE65100)
    val accentColor = Color(0xFFFF9800)
    val borderColor = if (isDarkMode) Color(0xFF333333) else Color(0xFFDDDDDD)

    var isRecording by remember { mutableStateOf(false) }
    var terminalInput by remember { mutableStateOf("") }

    val logs = remember { mutableStateListOf<Pair<String, String>>() }
    val listState = rememberLazyListState()

    DisposableEffect(Unit) {
        onDispose {
            terminalManager.sendCommand("\u0003")
        }
    }

    LaunchedEffect(Unit) {
        terminalManager.output.collect { text ->
            if (text.isNotEmpty()) {
                val lines = text.split("\n")
                val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())

                for (line in lines) {
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty() && !trimmed.startsWith(">>>")) {
                        if (isRecording) {
                            logs.add(timestamp to trimmed)
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    fun sendCommand() {
        if (terminalInput.isNotEmpty()) {
            val cmd = terminalInput
            if (cmd.contains("\n")) {
                terminalManager.sendCommand("\u0005" + cmd + "\u0004")
            } else {
                terminalManager.sendCommand(cmd + "\r\n")
            }
            terminalInput = ""
            if (!isRecording) isRecording = true
        }
    }

    fun saveAndShare() {
        if (logs.isEmpty()) {
            Toast.makeText(context, "No data to save!", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val fileName = "log_${System.currentTimeMillis()}.csv"
            val file = File(context.getExternalFilesDir(null), fileName)
            val writer = FileWriter(file)

            writer.append("Timestamp,Value\n")

            logs.forEach { (time, value) ->
                val cleanValue = value.replace(",", ".")
                writer.append("$time,$cleanValue\n")
            }
            writer.flush()
            writer.close()

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share CSV via"))

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error saving file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Data Logger", color = textColor, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.background(accentColor.copy(0.2f), RoundedCornerShape(4.dp)).padding(horizontal=4.dp)) {
                            Text("CSV", color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        terminalManager.sendCommand("\u0003")
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = textColor)
                    }
                },
                actions = {
                    IconButton(onClick = { logs.clear() }) {
                        Icon(Icons.Default.Delete, "Clear", tint = textColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
            )
        },
        containerColor = bgColor
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                border = BorderStroke(1.dp, borderColor)
            ) {
                if (logs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No Data Recorded", color = Color.Gray, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Run a script below to start logging", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                Text("TIME (HH:mm:ss.SSS)", modifier = Modifier.weight(0.5f), color = Color.Gray, fontSize = 10.sp)
                                Text("VALUE", modifier = Modifier.weight(0.5f), color = Color.Gray, fontSize = 10.sp)
                            }
                            HorizontalDivider(color = borderColor)
                        }
                        items(logs) { (time, value) ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text(time, modifier = Modifier.weight(0.5f), color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                Text(value, modifier = Modifier.weight(0.5f), color = logTextColor, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().height(50.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { isRecording = !isRecording },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) Color(0xFFFF5252) else accentColor
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isRecording) "STOP REC" else "START REC",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = { saveAndShare() },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    colors = ButtonDefaults.buttonColors(containerColor = cardColor),
                    border = BorderStroke(1.dp, accentColor),
                    shape = RoundedCornerShape(12.dp),
                    enabled = logs.isNotEmpty()
                ) {
                    Icon(Icons.Default.Share, null, tint = accentColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("EXPORT CSV", color = accentColor, fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(cardColor, RoundedCornerShape(12.dp))
                        .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    if (terminalInput.isEmpty()) {
                        Text("Paste logging script here...", color = Color.Gray, fontSize = 12.sp)
                    }
                    BasicTextField(
                        value = terminalInput,
                        onValueChange = { terminalInput = it },
                        textStyle = TextStyle(
                            color = textColor,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        ),
                        cursorBrush = SolidColor(accentColor),
                        singleLine = false,
                        keyboardOptions = KeyboardOptions.Default,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Button(
                    onClick = { sendCommand() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    modifier = Modifier
                        .width(70.dp)
                        .fillMaxHeight()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Run",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            "RUN",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}