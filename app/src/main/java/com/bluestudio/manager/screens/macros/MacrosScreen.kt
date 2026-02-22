package com.bluestudio.manager.screens.macros

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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bluestudio.manager.MainViewModel
import com.bluestudio.manager.Macro
import com.bluestudio.manager.managers.BoardManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacrosScreen(
    viewModel: MainViewModel,
    boardManager: BoardManager,
    onBack: () -> Unit
) {
    val macros by viewModel.macros.collectAsState()

    // وضعیت برای نمایش دیالوگ
    var showAddDialog by remember { mutableStateOf(false) }
    var macroToEdit by remember { mutableStateOf<Macro?>(null) }

    // وضعیت زبان و تم
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val isFa = currentLanguage == "fa"
    val layoutDirection = if (isFa) LayoutDirection.Rtl else LayoutDirection.Ltr

    // رنگ‌ها
    val bgColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F5F5)
    val topBarColor = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
    val cardColor = if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFFFFFFF)
    val textColor = if (isDarkMode) Color.White else Color.Black
    val iconColor = if (isDarkMode) Color.White else Color.Black
    val accentColor = Color(0xFFFBC02D)

    // تابع ذخیره (برای ادیت یا افزودن)
    fun onSaveMacro(name: String, code: String) {
        if (macroToEdit != null) {
            // حالت ویرایش: حذف قدیمی و افزودن جدید
            viewModel.removeMacro(macroToEdit!!)
            viewModel.addMacro(name, code)
            macroToEdit = null
        } else {
            // حالت افزودن
            viewModel.addMacro(name, code)
            showAddDialog = false
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (isFa) "ماکروها" else "Debug Macros", color = textColor) },
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
                    onClick = { showAddDialog = true },
                    containerColor = accentColor
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Macro", tint = Color.Black)
                }
            },
            containerColor = bgColor
        ) { padding ->
            Column(modifier = Modifier.padding(top = padding.calculateTopPadding())) {

                if (macros.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            if (isFa) "هنوز ماکرویی نیست. + را بزنید." else "No macros yet. Tap + to add.",
                            color = if (isDarkMode) Color.Gray else Color.DarkGray
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 150.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 100.dp
                        )
                    ) {
                        items(macros) { macro ->
                            MacroButton(
                                macro = macro,
                                cardColor = cardColor,
                                textColor = textColor,
                                isFa = isFa,
                                onClick = {
                                    val cmd = macro.command.trim()
                                    if (cmd.contains("\n")) {
                                        val pasteModePayload = "\u0005" + cmd + "\u0004"
                                        boardManager.writeCommand(pasteModePayload)
                                    } else {
                                        boardManager.writeCommand(cmd + "\r\n")
                                    }
                                },
                                onEdit = {
                                    macroToEdit = macro
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

        // دیالوگ افزودن / ویرایش
        if (showAddDialog || macroToEdit != null) {
            MacroDialog(
                isDarkMode = isDarkMode,
                isFa = isFa,
                initialName = macroToEdit?.name ?: "",
                initialCode = macroToEdit?.command ?: "",
                isEditMode = macroToEdit != null,
                onDismiss = {
                    showAddDialog = false
                    macroToEdit = null
                },
                onConfirm = { name, code ->
                    onSaveMacro(name, code)
                }
            )
        }
    }
}

@Composable
fun MacroButton(
    macro: Macro,
    cardColor: Color,
    textColor: Color,
    isFa: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier
            .height(110.dp)
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // آیکون کد
                Icon(Icons.Default.Code, contentDescription = null, tint = Color(0xFFFBC02D))

                // نام ماکرو
                Text(
                    text = macro.name,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1
                )

                // پیش‌نمایش کد (همیشه LTR باشد چون کد است)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        text = macro.command.lines().firstOrNull()?.take(20) ?: "Empty",
                        color = if (textColor == Color.White) Color.Gray else Color.DarkGray,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }

            // دکمه‌های عملیاتی (ویرایش و حذف)
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                // دکمه ویرایش
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = if(isFa) Color.Gray else Color.Gray, // رنگ خاکستری برای ادیت
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // دکمه حذف
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Red.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MacroDialog(
    isDarkMode: Boolean,
    isFa: Boolean,
    initialName: String,
    initialCode: String,
    isEditMode: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var code by remember { mutableStateOf(initialCode) }

    val containerColor = if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFFFFFFF)
    val textColor = if (isDarkMode) Color.White else Color.Black

    val titleText = if (isEditMode) (if (isFa) "ویرایش ماکرو" else "Edit Macro") else (if (isFa) "ماکرو جدید" else "New Macro")
    val nameLabel = if (isFa) "نام (مثلا: چشمک زن)" else "Name (e.g. Blink)"
    val codeLabel = if (isFa) "کد پایتون" else "Python Code"
    val btnText = if (isEditMode) (if (isFa) "ذخیره" else "Save") else (if (isFa) "افزودن" else "Add")
    val cancelText = if (isFa) "لغو" else "Cancel"

    AlertDialog(
        containerColor = containerColor,
        titleContentColor = textColor,
        textContentColor = textColor,
        onDismissRequest = onDismiss,
        title = { Text(titleText, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(nameLabel) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedLabelColor = textColor,
                        unfocusedLabelColor = Color.Gray,
                        cursorColor = Color(0xFFFBC02D),
                        focusedBorderColor = Color(0xFFFBC02D)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // ورودی کد همیشه LTR باشد
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text(codeLabel) },
                        modifier = Modifier.height(150.dp).fillMaxWidth(),
                        singleLine = false,
                        maxLines = 10,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedLabelColor = textColor,
                            unfocusedLabelColor = Color.Gray,
                            cursorColor = Color(0xFFFBC02D),
                            focusedBorderColor = Color(0xFFFBC02D)
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotEmpty() && code.isNotEmpty()) onConfirm(name, code) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBC02D), contentColor = Color.Black)
            ) {
                Text(btnText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(cancelText, color = textColor)
            }
        }
    )
}