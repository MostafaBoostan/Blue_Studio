package com.bluestudio.manager.screens.terminal

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.bluestudio.manager.MainViewModel
import com.bluestudio.manager.R
import com.bluestudio.manager.managers.BoardManager
import com.bluestudio.manager.managers.CommandsManager
import com.bluestudio.manager.managers.TerminalManager
import com.bluestudio.manager.model.MicroScript

private val NeonGreen = Color(0xFF69F0AE)
private val NeonRed = Color(0xFFFF5252)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    microScript: MicroScript,
    viewModel: MainViewModel,
    boardManager: BoardManager,
    terminalManager: TerminalManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var terminalInput by remember { viewModel.terminalInput }
    var terminalOutput by remember { viewModel.terminalOutput }
    var fontSize by remember { mutableStateOf(14.sp) }

    val scrollState = rememberScrollState()
    val buttonsScrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }

    // وضعیت زبان و تم
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val isFa = currentLanguage == "fa"
    val layoutDirection = if (isFa) LayoutDirection.Rtl else LayoutDirection.Ltr

    // رنگ‌ها
    val bgColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFFFFFFF)
    val panelBg = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)
    val textColor = if (isDarkMode) Color.White else Color.Black
    val terminalTextColor = if (isDarkMode) NeonGreen else Color(0xFF006400)
    val iconColor = if (isDarkMode) Color.White else Color.Black
    val buttonColor = if (isDarkMode) Color.Gray else Color.DarkGray

    // متن‌ها
    val titleText = if (microScript.name.isNotEmpty()) microScript.name else (if (isFa) "ترمینال" else "Terminal")
    val terminateMsg = if (isFa) "توقف اجرا" else context.getString(R.string.terminal_terminate_msg)
    val softResetMsg = if (isFa) "ریست نرم انجام شد" else context.getString(R.string.terminal_soft_reset_msg)

    fun onRun() {
        val code = terminalInput
        if (code.isNotEmpty()) {
            viewModel.history.push(code)
            coroutineScope.launch {
                if (code.contains("\n").not()) terminalManager.eval(code)
                else terminalManager.evalMultiLine(code)
                terminalInput = ""
            }
        }
    }

    fun executeScript() {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                if (microScript.isLocal) {
                    terminalManager.executeLocalScript(
                        microScript = microScript,
                        onClear = {
                            terminalInput = ""
                            terminalOutput = ""
                        }
                    )
                } else {
                    terminalManager.executeScript(microScript)
                }
            }
        }
    }

    fun onTerminate(showMessage: Boolean = false) {
        terminalManager.terminateExecution()
        if (showMessage) Toast.makeText(
            context,
            terminateMsg,
            Toast.LENGTH_SHORT
        ).show()
    }

    fun onSoftReset() {
        terminalManager.softResetDevice {
            Toast.makeText(
                context,
                softResetMsg,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.terminalOutput.value = ""
        terminalManager.terminateExecution()
        delay(300)
        if (microScript.hasContent) {
            executeScript()
        } else {
            boardManager.writeCommand(CommandsManager.REPL_MODE)
        }
    }

    LaunchedEffect(terminalOutput) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    DisposableEffect(LocalLifecycleOwner.current) {
        onDispose {
            viewModel.terminalOutput.value = ""
            viewModel.terminalInput.value = ""
            terminalManager.terminateExecution()
        }
    }

    BackHandler(enabled = true, onBack)

    // اعمال جهت (RTL/LTR)
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            containerColor = bgColor,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = titleText,
                            color = textColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            // آیکون در RTL خودکار برعکس می‌شود
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = iconColor
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            viewModel.terminalOutput.value = ""
                            viewModel.terminalInput.value = ""
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear", tint = NeonRed)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = panelBg
                    )
                )
            }
        ) { padding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // نوار دکمه‌های کنترلی (بالای ترمینال)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(panelBg)
                        .border(
                            width = 1.dp,
                            color = if(isDarkMode) Color.DarkGray else Color.LightGray,
                            shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                        )
                        .horizontalScroll(buttonsScrollState)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // دکمه‌ها را به ترتیب LTR می‌چینیم تا ترتیب منطقی حفظ شود
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        ControlBtn("-", buttonColor) {
                            if (fontSize.value > 10) fontSize = (fontSize.value - 2).sp
                        }
                        ControlBtn("+", buttonColor) {
                            if (fontSize.value < 30) fontSize = (fontSize.value + 2).sp
                        }
                        ControlBtn("▲", if(isDarkMode) NeonGreen else Color(0xFF006400)) {
                            viewModel.history.up()?.let { terminalInput = it }
                        }
                        ControlBtn("▼", if(isDarkMode) NeonGreen else Color(0xFF006400)) {
                            viewModel.history.down()?.let { terminalInput = it }
                        }
                        ControlBtn("CTRL-C", NeonRed) { onTerminate(true) }
                        ControlBtn("CTRL-D", Color(0xFFFFAB40)) { onSoftReset() }
                    }
                }

                // محیط اصلی ترمینال
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(bgColor)
                        .verticalScroll(scrollState)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            focusRequester.requestFocus()
                        }
                        .padding(16.dp)
                ) {
                    // *** بسیار مهم: ترمینال همیشه باید LTR باشد ***
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Column {
                            Text(
                                text = terminalOutput,
                                color = terminalTextColor,
                                fontSize = fontSize,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = (fontSize.value + 6).sp
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = ">>> ",
                                    color = terminalTextColor,
                                    fontSize = fontSize,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )

                                BasicTextField(
                                    value = terminalInput,
                                    onValueChange = { terminalInput = it },
                                    textStyle = TextStyle(
                                        color = terminalTextColor,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = fontSize
                                    ),
                                    cursorBrush = SolidColor(terminalTextColor),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(onSend = { onRun() }),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(150.dp))
                }
            }
        }
    }
}

@Composable
fun ControlBtn(text: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}