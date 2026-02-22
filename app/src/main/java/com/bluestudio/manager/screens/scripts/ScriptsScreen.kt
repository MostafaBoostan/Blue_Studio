package com.bluestudio.manager.screens.scripts

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.bluestudio.manager.MainViewModel
import com.bluestudio.manager.managers.ScriptsManager
import com.bluestudio.manager.model.MicroScript
import com.bluestudio.manager.screens.dialogs.FileDeleteDialog
import com.bluestudio.manager.screens.dialogs.FileRenameDialog
import com.bluestudio.manager.ui.components.rememberMyDialogState

private const val TAG = "ScriptsScreen"
private val NeonGreen = Color(0xFF69F0AE)

@Composable
fun ScriptsScreen(
    viewModel: com.bluestudio.manager.MainViewModel,
    onBack: () -> Unit,
    onNewScript: () -> Unit,
    onOpenLocalScript: (com.bluestudio.manager.model.MicroScript) -> Unit
) {
    val context = LocalContext.current
    val scriptsManager = remember {
        _root_ide_package_.com.bluestudio.manager.managers.ScriptsManager(
            context
        )
    }
    val renameFileDialog =
        _root_ide_package_.com.bluestudio.manager.ui.components.rememberMyDialogState()
    val deleteFileDialog =
        _root_ide_package_.com.bluestudio.manager.ui.components.rememberMyDialogState()
    var selectedScript by remember { mutableStateOf<com.bluestudio.manager.model.MicroScript?>(null) }
    val scripts = remember { scriptsManager.scripts }

    val isDarkMode by viewModel.isDarkMode.collectAsState()

    val bgColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F5F5)
    val cardBg = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
    val textColor = if (isDarkMode) Color(0xFFEEEEEE) else Color(0xFF121212)
    val textGray = if (isDarkMode) Color(0xFFB0BEC5) else Color(0xFF757575)

    _root_ide_package_.com.bluestudio.manager.screens.dialogs.FileRenameDialog(
        state = renameFileDialog,
        name = { selectedScript?.name.orEmpty() },
        onOk = { newName ->
            scriptsManager.renameScript(selectedScript!!, newName)
        }
    )

    _root_ide_package_.com.bluestudio.manager.screens.dialogs.FileDeleteDialog(
        state = deleteFileDialog,
        name = { selectedScript?.name.orEmpty() },
        onOk = {
            scriptsManager.deleteScript(selectedScript!!)
        }
    )

    fun readLocalScript(script: com.bluestudio.manager.model.MicroScript) {
        try {
            val content = scriptsManager.read(script.file)
            script.content = content
            Log.v(TAG, script.toString())
            onOpenLocalScript(script)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    ScriptsScreenContent(
        scripts = { scripts },
        bgColor = bgColor,
        cardBg = cardBg,
        textColor = textColor,
        textGray = textGray,
        uiEvents = {
            when (it) {
                is ScriptsEvents.Back -> onBack()
                is ScriptsEvents.NewScript -> onNewScript()
                is ScriptsEvents.Open -> readLocalScript(it.script)
                is ScriptsEvents.Share -> scriptsManager.shareScript(it.script)
                is ScriptsEvents.Delete -> {
                    selectedScript = it.script
                    deleteFileDialog.show()
                }

                is ScriptsEvents.Rename -> {
                    selectedScript = it.script
                    renameFileDialog.show()
                }

                is ScriptsEvents.Run -> {}
            }
        }
    )
}

@Composable
fun ScriptsScreenContent(
    scripts: () -> List<com.bluestudio.manager.model.MicroScript>,
    bgColor: Color,
    cardBg: Color,
    textColor: Color,
    textGray: Color,
    uiEvents: (ScriptsEvents) -> Unit
) {
    Scaffold(
        containerColor = bgColor,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardBg)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { uiEvents(ScriptsEvents.Back) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = NeonGreen
                    )
                }
                Text(
                    text = "My Scripts",
                    color = textColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.size(48.dp))
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { uiEvents(ScriptsEvents.NewScript) },
                containerColor = NeonGreen,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Script")
            }
        }
    ) { padding ->
        val scriptList = scripts()
        if (scriptList.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    tint = textGray.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No scripts found",
                    color = textGray,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(scriptList) { script ->
                    ScriptItemView(
                        script = script,
                        cardBg = cardBg,
                        textColor = textColor,
                        textGray = textGray,
                        onOpen = { uiEvents(ScriptsEvents.Open(it)) },
                        onRename = { uiEvents(ScriptsEvents.Rename(it)) },
                        onDelete = { uiEvents(ScriptsEvents.Delete(it)) },
                        onShare = { uiEvents(ScriptsEvents.Share(it)) }
                    )
                }
            }
        }
    }
}

@Composable
fun ScriptItemView(
    script: com.bluestudio.manager.model.MicroScript,
    cardBg: Color,
    textColor: Color,
    textGray: Color,
    onOpen: (com.bluestudio.manager.model.MicroScript) -> Unit,
    onRename: (com.bluestudio.manager.model.MicroScript) -> Unit,
    onDelete: (com.bluestudio.manager.model.MicroScript) -> Unit,
    onShare: (com.bluestudio.manager.model.MicroScript) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .border(1.dp, textColor.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .clickable { onOpen(script) }
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
                imageVector = Icons.Default.Code,
                contentDescription = null,
                tint = NeonGreen,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = script.name,
                color = textColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
                DropdownMenuItem(
                    text = { Text("Rename", color = textColor) },
                    onClick = {
                        expanded = false
                        onRename(script)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Edit, null, tint = textColor)
                    }
                )

                DropdownMenuItem(
                    text = { Text("Share", color = textColor) },
                    onClick = {
                        expanded = false
                        onShare(script)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Share, null, tint = textColor)
                    }
                )

                HorizontalDivider(color = textGray.copy(alpha = 0.2f))

                DropdownMenuItem(
                    text = { Text("Delete", color = Color(0xFFFF5252)) },
                    onClick = {
                        expanded = false
                        onDelete(script)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Delete, null, tint = Color(0xFFFF5252))
                    }
                )
            }
        }
    }
}