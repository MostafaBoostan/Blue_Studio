package micro.repl.ma7moud3ly.screens.explorer

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import micro.repl.ma7moud3ly.MainViewModel
import micro.repl.ma7moud3ly.R
import micro.repl.ma7moud3ly.managers.FilesManager
import micro.repl.ma7moud3ly.managers.TerminalManager
import micro.repl.ma7moud3ly.model.EditorMode
import micro.repl.ma7moud3ly.model.MicroFile
import micro.repl.ma7moud3ly.model.MicroScript
import micro.repl.ma7moud3ly.screens.dialogs.FileCreateDialog
import micro.repl.ma7moud3ly.screens.dialogs.FileDeleteDialog
import micro.repl.ma7moud3ly.screens.dialogs.FileRenameDialog
import micro.repl.ma7moud3ly.screens.dialogs.ImportScriptDialog
import micro.repl.ma7moud3ly.ui.components.rememberMyDialogState
import java.io.File

// رنگ ثابت برای برندینگ
private val NeonGreen = Color(0xFF69F0AE)

// اکستنشن اصلاح شده
private val MicroFile.isDirectory: Boolean get() = !this.isFile

@Composable
fun FilesExplorerScreen(
    viewModel: MainViewModel,
    terminalManager: TerminalManager,
    filesManager: FilesManager?,
    openTerminal: (MicroScript) -> Unit,
    openEditor: (MicroScript) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val root by remember { viewModel.root }
    val coroutineScope = rememberCoroutineScope()
    val files = viewModel.files.collectAsState()
    var selectedFile by remember { mutableStateOf<MicroFile?>(null) }

    // دریافت وضعیت تم
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    // تعریف رنگ‌های داینامیک
    val bgColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F5F5)
    val cardBg = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
    val textColor = if (isDarkMode) Color(0xFFEEEEEE) else Color(0xFF121212)
    val textGray = if (isDarkMode) Color(0xFFB0BEC5) else Color(0xFF757575)

    val importScriptDialog = rememberMyDialogState()
    val deleteFileDialog = rememberMyDialogState()
    val createFileDialog = rememberMyDialogState()
    val renameFileDialog = rememberMyDialogState()

    val isMicroPython = viewModel.microDevice?.isMicroPython == true
    val filesPicker = rememberFilesPickerResult()

    LaunchedEffect(Unit) {
        terminalManager.terminateExecution()
        filesManager?.listDir(viewModel.root.value)
    }

    fun onUp() {
        if (root.isEmpty() || root == "/") {
            onBack()
            return
        }
        val newRoot = File(root).parent ?: "/"
        viewModel.root.value = newRoot
        filesManager?.listDir(newRoot)
    }

    BackHandler { onUp() }

    fun onRun(file: MicroFile) {
        filesManager?.read(file.fullPath, onRead = { content ->
            val script = MicroScript(
                path = file.fullPath,
                content = content,
                editorMode = EditorMode.REMOTE
            )
            coroutineScope.launch { openTerminal(script) }
        })
    }

    fun onEdit(file: MicroFile) {
        filesManager?.read(file.fullPath, onRead = { content ->
            val script = MicroScript(
                path = file.fullPath,
                content = content,
                editorMode = EditorMode.REMOTE
            )
            coroutineScope.launch { openEditor(script) }
        })
    }

    fun importFile(fileName: String, byteArray: ByteArray) {
        filesManager?.writeBinary(
            path = "$root/$fileName",
            bytes = byteArray,
            onSave = {
                coroutineScope.launch {
                    filesManager.listDir()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Saved to $root", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    fun onOpenFolder(file: MicroFile) {
        viewModel.root.value = file.fullPath
        filesManager?.listDir(file.fullPath)
    }

    fun onRefresh() {
        Toast.makeText(context, context.getText(R.string.explorer_refresh), Toast.LENGTH_SHORT).show()
        filesManager?.listDir()
    }

    FileDeleteDialog(
        state = deleteFileDialog,
        name = { selectedFile?.name.orEmpty() },
        onOk = { filesManager?.remove(selectedFile!!) }
    )

    FileCreateDialog(
        state = createFileDialog,
        microFile = { selectedFile },
        onOk = { file -> filesManager?.new(file) }
    )

    FileRenameDialog(
        state = renameFileDialog,
        name = { selectedFile?.name.orEmpty() },
        onOk = { newName ->
            val dst = MicroFile(
                name = newName,
                path = selectedFile!!.path,
                type = if (selectedFile!!.isFile) MicroFile.FILE else MicroFile.DIRECTORY
            )
            filesManager?.rename(src = selectedFile!!, dst = dst)
        }
    )

    ImportScriptDialog(
        state = importScriptDialog,
        onOk = { filesPicker.pickFile(::importFile) }
    )

    ExplorerScreenContent(
        files = { files.value },
        root = { root },
        isMicroPython = isMicroPython,
        bgColor = bgColor,
        cardBg = cardBg,
        textColor = textColor,
        textGray = textGray,
        uiEvents = { event ->
            when (event) {
                is ExplorerEvents.OpenFolder -> onOpenFolder(event.file)
                is ExplorerEvents.Edit -> onEdit(event.file)
                is ExplorerEvents.Run -> onRun(event.file)
                is ExplorerEvents.Refresh -> onRefresh()
                is ExplorerEvents.Up -> onUp()
                is ExplorerEvents.Import -> importScriptDialog.show()
                is ExplorerEvents.Export -> { }
                is ExplorerEvents.Rename -> {
                    selectedFile = event.file
                    renameFileDialog.show()
                }
                is ExplorerEvents.Remove -> {
                    selectedFile = event.file
                    deleteFileDialog.show()
                }
                is ExplorerEvents.New -> {
                    selectedFile = event.file
                    createFileDialog.show()
                }
            }
        }
    )
}

@Composable
fun ExplorerScreenContent(
    files: () -> List<MicroFile>,
    root: () -> String,
    isMicroPython: Boolean,
    bgColor: Color,
    cardBg: Color,
    textColor: Color,
    textGray: Color,
    uiEvents: (ExplorerEvents) -> Unit
) {
    Scaffold(
        containerColor = bgColor,
        topBar = {
            ExplorerTopBar(
                currentPath = root(),
                cardBg = cardBg,
                textColor = textColor,
                textGray = textGray,
                onBack = { uiEvents(ExplorerEvents.Up) },
                onRefresh = { uiEvents(ExplorerEvents.Refresh) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { uiEvents(ExplorerEvents.New(MicroFile(root(), "", MicroFile.FILE))) },
                containerColor = NeonGreen,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "New File")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val fileList = files()

            if (fileList.isEmpty()) {
                EmptyStateView(cardBg, textGray)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(fileList) { file ->
                        FileItemView(
                            file = file,
                            isMicroPython = isMicroPython,
                            cardBg = cardBg,
                            textColor = textColor,
                            textGray = textGray,
                            onFolderClick = { uiEvents(ExplorerEvents.OpenFolder(it)) },
                            onFileClick = { uiEvents(ExplorerEvents.Edit(it)) },
                            onRun = { uiEvents(ExplorerEvents.Run(it)) },
                            onRename = { uiEvents(ExplorerEvents.Rename(it)) },
                            onDelete = { uiEvents(ExplorerEvents.Remove(it)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExplorerTopBar(
    currentPath: String,
    cardBg: Color,
    textColor: Color,
    textGray: Color,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBg)
            .padding(top = 8.dp, bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = NeonGreen
                )
            }
            Text(
                text = "File Manager",
                style = androidx.compose.ui.text.TextStyle(
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace
                )
            )
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = NeonGreen
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if(textColor == Color.White) Color.Black.copy(alpha = 0.3f) else Color.LightGray.copy(alpha = 0.3f))
                .border(1.dp, NeonGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (currentPath == "/") Icons.Default.Home else Icons.Default.SdStorage,
                contentDescription = null,
                tint = textGray,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = currentPath.ifEmpty { "/" },
                color = NeonGreen,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun FileItemView(
    file: MicroFile,
    isMicroPython: Boolean,
    cardBg: Color,
    textColor: Color,
    textGray: Color,
    onFolderClick: (MicroFile) -> Unit,
    onFileClick: (MicroFile) -> Unit,
    onRun: (MicroFile) -> Unit,
    onRename: (MicroFile) -> Unit,
    onDelete: (MicroFile) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val icon = if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile
    val iconTint = if (file.isDirectory) Color(0xFFFFC107) else NeonGreen

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .border(1.dp, textColor.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .clickable {
                if (file.isDirectory) onFolderClick(file) else onFileClick(file)
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if(textColor == Color.White) Color.Black.copy(alpha = 0.3f) else Color.LightGray.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                color = textColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (file.isFile && (file.name.endsWith(".py") || file.name.endsWith(".txt")) && isMicroPython) {
            IconButton(onClick = { onRun(file) }) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Run",
                    tint = NeonGreen
                )
            }
        }

        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Menu",
                    tint = textGray
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                offset = DpOffset((-10).dp, 0.dp),
                modifier = Modifier.background(cardBg)
            ) {
                if (file.isFile) {
                    DropdownMenuItem(
                        text = { Text("Edit", color = textColor) },
                        onClick = {
                            expanded = false
                            onFileClick(file)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, null, tint = NeonGreen)
                        }
                    )
                    HorizontalDivider(color = textGray.copy(alpha = 0.2f))
                }

                DropdownMenuItem(
                    text = { Text("Rename", color = textColor) },
                    onClick = {
                        expanded = false
                        onRename(file)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Edit, null, tint = textColor)
                    }
                )

                DropdownMenuItem(
                    text = { Text("Delete", color = Color(0xFFFF5252)) },
                    onClick = {
                        expanded = false
                        onDelete(file)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Delete, null, tint = Color(0xFFFF5252))
                    }
                )
            }
        }
    }
}

@Composable
fun EmptyStateView(cardBg: Color, textGray: Color) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(cardBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = textGray.copy(alpha = 0.5f),
                modifier = Modifier.size(50.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Directory is empty",
            color = textGray,
            fontSize = 16.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}