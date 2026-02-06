/*
 * Created by Mahmoud Aly - engma7moud3ly@gmail.com
 * Project Micro REPL - https://github.com/Ma7moud3ly/micro-repl
 * Copyright (c) 2023 . MIT license.
 *
 */

package micro.repl.ma7moud3ly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import micro.repl.ma7moud3ly.managers.BoardManager
import micro.repl.ma7moud3ly.managers.FilesManager
import micro.repl.ma7moud3ly.managers.TerminalManager
import micro.repl.ma7moud3ly.model.ConnectionStatus
import micro.repl.ma7moud3ly.screens.RootGraph
import micro.repl.ma7moud3ly.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    private lateinit var boardManager: BoardManager
    private lateinit var terminalManager: TerminalManager
    private lateinit var filesManager: FilesManager
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        boardManager = BoardManager(
            context = this,
            onStatusChanges = { status ->
                // اول وضعیت را به ویومدل می‌دهیم
                viewModel.status.value = status

                // *** تغییر مهم: اجرا در لحظه کانکت شدن (چه بار اول، چه بار دهم) ***
                if (status is ConnectionStatus.Connected) {
                    // بلافاصله درخواست شناسایی را ارسال کن
                    viewModel.onDeviceConnected(boardManager)
                }
            },
            onReceiveData = { data: String, clear: Boolean ->
                runOnUiThread {
                    // نمایش دیتا در ترمینال
                    if (clear) viewModel.terminalOutput.value = ""
                    else if (viewModel.terminalOutput.value.length > 10000)
                        viewModel.terminalOutput.value = data
                    else viewModel.terminalOutput.value += data

                    // *** بررسی پاسخ آیدی ***
                    if (data.contains("#ID:")) {
                        val extractedId = data.substringAfter("#ID:").trim()

                        // پاکسازی کاراکترهای عجیب و غریب
                        val cleanId = extractedId.takeWhile { it.isLetterOrDigit() || it == ' ' || it == '_' }

                        // شرط ورود به حالت پرو
                        if (cleanId.isNotEmpty() && cleanId != "Basic") {
                            viewModel.triggerProMode(cleanId)
                        }
                    }
                }
            }
        )

        terminalManager = TerminalManager(boardManager)
        filesManager = FilesManager(
            boardManager = boardManager,
            onUpdateFiles = { viewModel.files.value = it }
        )

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val fontScale by viewModel.fontScale.collectAsState()

            AppTheme(
                darkTheme = isDarkMode,
                darkStatusBar = true
            ) {
                val currentDensity = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(currentDensity.density, fontScale = fontScale)
                ) {
                    RootGraph(
                        viewModel = viewModel,
                        boardManager = boardManager,
                        terminalManager = terminalManager,
                        filesManager = filesManager
                    )
                }
            }
        }
    }
}