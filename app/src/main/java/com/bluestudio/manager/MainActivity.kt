/*
 * Created by Mahmoud Aly - engma7moud3ly@gmail.com
 * Project Micro REPL - https://github.com/Ma7moud3ly/micro-repl
 * Copyright (c) 2023 . MIT license.
 *
 */

package com.bluestudio.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.bluestudio.manager.managers.BoardManager
import com.bluestudio.manager.managers.FilesManager
import com.bluestudio.manager.managers.TerminalManager
import com.bluestudio.manager.model.ConnectionStatus
import com.bluestudio.manager.screens.RootGraph
import com.bluestudio.manager.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    private lateinit var boardManager: com.bluestudio.manager.managers.BoardManager
    private lateinit var terminalManager: com.bluestudio.manager.managers.TerminalManager
    private lateinit var filesManager: com.bluestudio.manager.managers.FilesManager
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        boardManager = _root_ide_package_.com.bluestudio.manager.managers.BoardManager(
            context = this,
            onStatusChanges = { status ->
                // اول وضعیت را به ویومدل می‌دهیم
                viewModel.status.value = status

                // *** تغییر مهم: اجرا در لحظه کانکت شدن (چه بار اول، چه بار دهم) ***
                if (status is com.bluestudio.manager.model.ConnectionStatus.Connected) {
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
                        val cleanId =
                            extractedId.takeWhile { it.isLetterOrDigit() || it == ' ' || it == '_' }

                        // شرط ورود به حالت پرو
                        if (cleanId.isNotEmpty() && cleanId != "Basic") {
                            viewModel.triggerProMode(cleanId)
                        }
                    }
                }
            }
        )

        terminalManager =
            _root_ide_package_.com.bluestudio.manager.managers.TerminalManager(boardManager)
        filesManager = _root_ide_package_.com.bluestudio.manager.managers.FilesManager(
            boardManager = boardManager,
            onUpdateFiles = { viewModel.files.value = it }
        )

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val fontScale by viewModel.fontScale.collectAsState()

            _root_ide_package_.com.bluestudio.manager.ui.theme.AppTheme(
                darkTheme = isDarkMode,
                darkStatusBar = true
            ) {
                val currentDensity = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(currentDensity.density, fontScale = fontScale)
                ) {
                    _root_ide_package_.com.bluestudio.manager.screens.RootGraph(
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