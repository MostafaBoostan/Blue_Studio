package micro.repl.ma7moud3ly.screens.macros

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import micro.repl.ma7moud3ly.MainViewModel
import micro.repl.ma7moud3ly.Macro
import micro.repl.ma7moud3ly.managers.BoardManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacrosScreen(
    viewModel: MainViewModel,
    boardManager: BoardManager,
    onBack: () -> Unit
) {
    val macros by viewModel.macros.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    val isDarkMode by viewModel.isDarkMode.collectAsState()

    val bgColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F5F5)
    val topBarColor = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
    val cardColor = if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFFFFFFF)
    val textColor = if (isDarkMode) Color.White else Color.Black
    val iconColor = if (isDarkMode) Color.White else Color.Black

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug Macros", color = textColor) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = iconColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = topBarColor
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = Color(0xFFFBC02D)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Macro", tint = Color.Black)
            }
        },
        containerColor = bgColor
    ) { padding ->
        // اینجا padding اصلی صفحه را اعمال می‌کنیم اما پدینگ لیست را جداگانه میدهیم
        Column(modifier = Modifier.padding(top = padding.calculateTopPadding())) {

            if (macros.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No macros yet. Tap + to add.", color = if (isDarkMode) Color.Gray else Color.DarkGray)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 140.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    // *** اصلاح پدینگ برای جلوگیری از بریده شدن سایه و رفتن زیر دکمه FAB ***
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = 100.dp // فضای کافی برای اسکرول آخر
                    )
                ) {
                    items(macros) { macro ->
                        MacroButton(
                            macro = macro,
                            cardColor = cardColor,
                            textColor = textColor,
                            onClick = {
                                val cmd = macro.command.trim()
                                if (cmd.contains("\n")) {
                                    val pasteModePayload = "\u0005" + cmd + "\u0004"
                                    boardManager.writeCommand(pasteModePayload)
                                } else {
                                    boardManager.writeCommand(cmd + "\r\n")
                                }
                            },
                            onDelete = {
                                viewModel.removeMacro(macro)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        AddMacroDialog(
            isDarkMode = isDarkMode,
            onDismiss = { showDialog = false },
            onAdd = { name, code ->
                viewModel.addMacro(name, code)
                showDialog = false
            }
        )
    }
}

@Composable
fun MacroButton(
    macro: Macro,
    cardColor: Color,
    textColor: Color,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp), // سایه برای لایت مود
        modifier = Modifier
            .height(100.dp)
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(Icons.Default.Code, contentDescription = null, tint = Color(0xFFFBC02D))
                Text(
                    text = macro.name,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = macro.command.lines().firstOrNull()?.take(20) ?: "Empty",
                    color = if (textColor == Color.White) Color.Gray else Color.DarkGray,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun AddMacroDialog(
    isDarkMode: Boolean,
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    val containerColor = if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFFFFFFF)
    val textColor = if (isDarkMode) Color.White else Color.Black

    AlertDialog(
        containerColor = containerColor,
        titleContentColor = textColor,
        textContentColor = textColor,
        onDismissRequest = onDismiss,
        title = { Text("New Macro") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (e.g. Blink)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedLabelColor = textColor,
                        unfocusedLabelColor = Color.Gray
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Python Code") },
                    modifier = Modifier.height(150.dp),
                    singleLine = false,
                    maxLines = 10,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedLabelColor = textColor,
                        unfocusedLabelColor = Color.Gray
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotEmpty() && code.isNotEmpty()) onAdd(name, code) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBC02D), contentColor = Color.Black)
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = textColor)
            }
        }
    )
}