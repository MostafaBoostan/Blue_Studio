package com.bluestudio.manager.screens.editor

import android.graphics.Typeface
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import com.bluestudio.manager.MainViewModel
import com.bluestudio.manager.managers.EditorAction
import com.bluestudio.manager.managers.EditorManager
import com.bluestudio.manager.managers.FilesManager
import com.bluestudio.manager.model.EditorState
import com.bluestudio.manager.model.MicroScript
import com.bluestudio.manager.screens.dialogs.FileSaveAsDialog
import com.bluestudio.manager.screens.dialogs.FileSaveDialog
import com.bluestudio.manager.ui.components.rememberMyDialogState

private val NeonGreen = Color(0xFF69F0AE)
// ادیتور همیشه دارک خواهد بود
private val EditorDarkBg = Color(0xFF121212)

@Composable
fun EditorScreen(
    viewModel: com.bluestudio.manager.MainViewModel,
    canRun: () -> Boolean,
    editorState: com.bluestudio.manager.model.EditorState,
    filesManager: com.bluestudio.manager.managers.FilesManager,
    onRemoteRun: (com.bluestudio.manager.model.MicroScript) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var editorManager by remember { mutableStateOf<com.bluestudio.manager.managers.EditorManager?>(null) }
    val saveDialog = _root_ide_package_.com.bluestudio.manager.ui.components.rememberMyDialogState()
    val saveAsNewDialog =
        _root_ide_package_.com.bluestudio.manager.ui.components.rememberMyDialogState()

    // فقط نوار بالا تم را می‌گیرد، ادیتور همیشه مشکی است
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val panelColor = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
    val textColor = if (isDarkMode) Color(0xFFEEEEEE) else Color(0xFF121212)
    val iconColor = if (isDarkMode) Color(0xFFEEEEEE) else Color(0xFF121212)

    LaunchedEffect(canRun()) {
        if (canRun()) editorState.canRun.value = true
    }

    fun initEditor(codeEditor: CodeEditor) {
        editorManager = _root_ide_package_.com.bluestudio.manager.managers.EditorManager(
            context = context,
            coroutineScope = coroutineScope,
            editor = codeEditor,
            editorState = editorState,
            filesManager = filesManager,
            onRun = onRemoteRun,
            afterEdit = onBack
        )
        codeEditor.typefaceText = Typeface.MONOSPACE
        codeEditor.isLineNumberEnabled = true

        // تنظیم رنگ‌های ثابت و پایدار (Dark)
        codeEditor.colorScheme = EditorColorScheme().apply {
            setColor(EditorColorScheme.WHOLE_BACKGROUND, 0xFF121212.toInt())
            setColor(EditorColorScheme.TEXT_NORMAL, 0xFFEEEEEE.toInt())
            setColor(EditorColorScheme.LINE_NUMBER_BACKGROUND, 0xFF1E1E1E.toInt())
            setColor(EditorColorScheme.LINE_NUMBER, 0xFFB0BEC5.toInt())
            setColor(EditorColorScheme.SELECTION_INSERT, 0xFF69F0AE.toInt())
            setColor(EditorColorScheme.SELECTION_HANDLE, 0xFF69F0AE.toInt())
        }
    }

    fun checkAction(action: com.bluestudio.manager.managers.EditorAction) {
        editorManager?.actionAfterSave = action
        if (editorManager?.saveExisting() == true) {
            if (action == _root_ide_package_.com.bluestudio.manager.managers.EditorAction.SaveScript) editorManager?.save {
                Toast.makeText(context, "Saved...", Toast.LENGTH_SHORT).show()
            } else {
                saveDialog.show()
            }
        } else if (editorManager?.saveNew() == true) {
            saveAsNewDialog.show()
        } else {
            editorManager?.actionAfterSave()
        }
    }

    BackHandler {
        checkAction(_root_ide_package_.com.bluestudio.manager.managers.EditorAction.CLoseScript)
    }

    DisposableEffect(LocalLifecycleOwner.current) {
        onDispose {
            editorManager?.release()
        }
    }

    _root_ide_package_.com.bluestudio.manager.screens.dialogs.FileSaveDialog(
        state = saveDialog,
        name = { editorState.title.value },
        onOk = {
            editorManager?.save {
                editorManager?.actionAfterSave()
            }
        },
        onDismiss = {
            editorManager?.actionAfterSave()
        }
    )

    _root_ide_package_.com.bluestudio.manager.screens.dialogs.FileSaveAsDialog(
        state = saveAsNewDialog,
        name = { "main.py" },
        onOk = { name ->
            editorManager?.saveFileAs(name) {
                editorManager?.actionAfterSave()
            }
        },
        onDismiss = {
            editorManager?.actionAfterSave()
        }
    )

    Scaffold(
        containerColor = EditorDarkBg, // پس زمینه کل صفحه همیشه مشکی
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(panelColor) // نوار بالا رنگش عوض می‌شود (شیک‌تر است)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { checkAction(_root_ide_package_.com.bluestudio.manager.managers.EditorAction.CLoseScript) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = iconColor
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = editorState.title.value,
                        color = textColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { checkAction(_root_ide_package_.com.bluestudio.manager.managers.EditorAction.SaveScript) }) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save",
                            tint = NeonGreen
                        )
                    }

                    if (editorState.canRun.value) {
                        IconButton(onClick = { checkAction(_root_ide_package_.com.bluestudio.manager.managers.EditorAction.RunScript) }) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Run",
                                tint = NeonGreen
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(EditorDarkBg)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    CodeEditor(ctx).apply {
                        initEditor(this)
                    }
                }
                // بخش update را حذف کردیم تا دیگر رنگ‌ها را تغییر ندهد
            )
        }
    }
}