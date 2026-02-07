package micro.repl.ma7moud3ly.screens.macros

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug Macros") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1E1E),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
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
        containerColor = Color(0xFF121212)
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {

            if (macros.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No macros yet. Tap + to add.", color = Color.Gray)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 140.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(macros) { macro ->
                        MacroButton(
                            macro = macro,
                            onClick = {
                                // *** منطق هوشمند ارسال کد ***
                                val cmd = macro.command.trim()

                                if (cmd.contains("\n")) {
                                    // اگر کد چند خطی بود: استفاده از Paste Mode
                                    // \u0005 = Ctrl+E (شروع حالت پیست)
                                    // \u0004 = Ctrl+D (اجرا)
                                    val pasteModePayload = "\u0005" + cmd + "\u0004"
                                    boardManager.writeCommand(pasteModePayload)
                                } else {
                                    // اگر تک خطی بود: ارسال معمولی
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
            onDismiss = { showDialog = false },
            onAdd = { name, code ->
                viewModel.addMacro(name, code)
                showDialog = false
            }
        )
    }
}

@Composable
fun MacroButton(macro: Macro, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C)),
        shape = RoundedCornerShape(12.dp),
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
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = macro.command.lines().firstOrNull()?.take(20) ?: "Empty",
                    color = Color.Gray,
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
fun AddMacroDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Macro") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (e.g. Blink)") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Python Code") },
                    modifier = Modifier.height(150.dp), // ارتفاع بیشتر برای راحتی
                    singleLine = false, // اجازه نوشتن چند خط
                    maxLines = 10
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotEmpty() && code.isNotEmpty()) onAdd(name, code) }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}