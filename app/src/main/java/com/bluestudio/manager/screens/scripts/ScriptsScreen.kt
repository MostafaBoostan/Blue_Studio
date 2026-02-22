package com.bluestudio.manager.screens.scripts

import android.util.Log
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
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
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNewScript: () -> Unit,
    onOpenLocalScript: (MicroScript) -> Unit
) {
    val context = LocalContext.current
    val scriptsManager = remember { ScriptsManager(context) }

    // دیالوگ‌ها
    val renameFileDialog = rememberMyDialogState()
    val deleteFileDialog = rememberMyDialogState()

    var selectedScript by remember { mutableStateOf<MicroScript?>(null) }
    val scripts = remember { scriptsManager.scripts }

    // وضعیت زبان و تم
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val isFa = currentLanguage == "fa"
    val layoutDirection = if (isFa) LayoutDirection.Rtl else LayoutDirection.Ltr

    // رنگ‌ها
    val bgColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F5F5)
    val cardBg = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
    val textColor = if (isDarkMode) Color(0xFFEEEEEE) else Color(0xFF121212)
    val textGray = if (isDarkMode) Color(0xFFB0BEC5) else Color(0xFF757575)

    FileRenameDialog(
        state = renameFileDialog,
        name = { selectedScript?.name.orEmpty() },
        onOk = { newName ->
            selectedScript?.let { scriptsManager.renameScript(it, newName) }
        }
    )

    FileDeleteDialog(
        state = deleteFileDialog,
        name = { selectedScript?.name.orEmpty() },
        onOk = {
            selectedScript?.let { scriptsManager.deleteScript(it) }
        }
    )

    fun readLocalScript(script: MicroScript) {
        try {
            val content = scriptsManager.read(script.file)
            script.content = content
            Log.v(TAG, script.toString())
            onOpenLocalScript(script)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // اعمال جهت (RTL/LTR)
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        ScriptsScreenContent(
            scripts = { scripts },
            isFa = isFa,
            bgColor = bgColor,
            cardBg = cardBg,
            textColor = textColor,
            textGray = textGray,
            uiEvents = { event ->
                when (event) {
                    is ScriptScreenEvent.Back -> onBack()
                    is ScriptScreenEvent.NewScript -> onNewScript()
                    is ScriptScreenEvent.Open -> readLocalScript(event.script)
                    is ScriptScreenEvent.Share -> scriptsManager.shareScript(event.script)
                    is ScriptScreenEvent.Delete -> {
                        selectedScript = event.script
                        deleteFileDialog.show()
                    }
                    is ScriptScreenEvent.Rename -> {
                        selectedScript = event.script
                        renameFileDialog.show()
                    }
                    is ScriptScreenEvent.Run -> {}
                }
            }
        )
    }
}

@Composable
fun ScriptsScreenContent(
    scripts: () -> List<MicroScript>,
    isFa: Boolean,
    bgColor: Color,
    cardBg: Color,
    textColor: Color,
    textGray: Color,
    uiEvents: (ScriptScreenEvent) -> Unit
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
                IconButton(onClick = { uiEvents(ScriptScreenEvent.Back) }) {
                    // آیکون در RTL خودکار برعکس می‌شود
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = NeonGreen
                    )
                }
                Text(
                    text = if(isFa) "اسکریپت‌های من" else "My Scripts",
                    color = textColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                // Spacer برای تراز تقریبی
                Spacer(modifier = Modifier.size(48.dp))
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { uiEvents(ScriptScreenEvent.NewScript) },
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
                    text = if(isFa) "اسکریپتی یافت نشد" else "No scripts found",
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
                        isFa = isFa,
                        cardBg = cardBg,
                        textColor = textColor,
                        textGray = textGray,
                        onOpen = { uiEvents(ScriptScreenEvent.Open(it)) },
                        onRename = { uiEvents(ScriptScreenEvent.Rename(it)) },
                        onDelete = { uiEvents(ScriptScreenEvent.Delete(it)) },
                        onShare = { uiEvents(ScriptScreenEvent.Share(it)) }
                    )
                }
            }
        }
    }
}

@Composable
fun ScriptItemView(
    script: MicroScript,
    isFa: Boolean,
    cardBg: Color,
    textColor: Color,
    textGray: Color,
    onOpen: (MicroScript) -> Unit,
    onRename: (MicroScript) -> Unit,
    onDelete: (MicroScript) -> Unit,
    onShare: (MicroScript) -> Unit
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
            // *** نام اسکریپت همیشه LTR باشد ***
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
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
        }

        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Menu",
                    tint = textGray
                )
            }

            // منوها راست‌چین می‌شوند (اگر زبان فارسی باشد)
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                offset = DpOffset((-10).dp, 0.dp),
                modifier = Modifier.background(cardBg)
            ) {
                DropdownMenuItem(
                    text = { Text(if (isFa) "تغییر نام" else "Rename", color = textColor) },
                    onClick = {
                        expanded = false
                        onRename(script)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Edit, null, tint = textColor)
                    }
                )

                DropdownMenuItem(
                    text = { Text(if (isFa) "اشتراک‌گذاری" else "Share", color = textColor) },
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
                    text = { Text(if (isFa) "حذف" else "Delete", color = Color(0xFFFF5252)) },
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

// *** تغییر نام کلاس برای جلوگیری از تداخل با UIEvents.kt ***
sealed class ScriptScreenEvent {
    object Back : ScriptScreenEvent()
    object NewScript : ScriptScreenEvent()
    data class Open(val script: MicroScript) : ScriptScreenEvent()
    data class Share(val script: MicroScript) : ScriptScreenEvent()
    data class Delete(val script: MicroScript) : ScriptScreenEvent()
    data class Rename(val script: MicroScript) : ScriptScreenEvent()
    data class Run(val script: MicroScript) : ScriptScreenEvent()
}