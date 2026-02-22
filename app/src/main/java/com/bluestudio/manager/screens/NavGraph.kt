package com.bluestudio.manager.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.bluestudio.manager.AppRoutes
import com.bluestudio.manager.MainViewModel
import com.bluestudio.manager.managers.BoardManager
import com.bluestudio.manager.managers.FilesManager
import com.bluestudio.manager.managers.TerminalManager
import com.bluestudio.manager.model.ConnectionStatus
import com.bluestudio.manager.model.EditorState
import com.bluestudio.manager.model.asMicroScript
import com.bluestudio.manager.screens.editor.EditorScreen
import com.bluestudio.manager.screens.explorer.FilesExplorerScreen
import com.bluestudio.manager.screens.flash.FlashScreen
import com.bluestudio.manager.screens.home.HomeScreen
import com.bluestudio.manager.screens.joystick.JoystickScreen
import com.bluestudio.manager.screens.logger.LoggerScreen
import com.bluestudio.manager.screens.macros.MacrosScreen
import com.bluestudio.manager.screens.plotter.PlotterScreen
import com.bluestudio.manager.screens.pro.BlueStudioProScreen
import com.bluestudio.manager.screens.scripts.ScriptsScreen
import com.bluestudio.manager.screens.settings.SettingsScreen
import com.bluestudio.manager.screens.terminal.TerminalScreen

@Composable
fun RootGraph(
    viewModel: MainViewModel,
    boardManager: BoardManager,
    filesManager: FilesManager,
    terminalManager: TerminalManager,
    navController: NavHostController = rememberNavController(),
) {
    var canRun by remember { mutableStateOf(false) }
    val proDeviceIdName by viewModel.proDeviceId.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    LaunchedEffect(Unit) {
        viewModel.status.collect { status ->
            if (status is ConnectionStatus.Connected) {
                canRun = true
            } else {
                canRun = false
                if (status !is ConnectionStatus.Connecting) {
                    viewModel.resetProMode()
                    if (navController.previousBackStackEntry != null) {
                        navController.navigate(AppRoutes.Home) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigateToPro.collect { shouldNavigate ->
            if (shouldNavigate) {
                navController.navigate(AppRoutes.BlueStudioPro) {
                    popUpTo(AppRoutes.Home) { inclusive = true }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppRoutes.Home
    ) {
        composable<AppRoutes.Home> {
            HomeScreen(
                viewModel = viewModel,
                boardManager = boardManager,
                terminalManager = terminalManager,
                openExplorer = { navController.navigate(AppRoutes.Explorer) },
                openTerminal = { navController.navigate(AppRoutes.Terminal()) },
                openEditor = { navController.navigate(AppRoutes.Editor()) },
                openScripts = { navController.navigate(AppRoutes.Scripts) },
                openFlasher = { navController.navigate(AppRoutes.Flash) },
                openSettings = { navController.navigate(AppRoutes.Settings) },
                openMacros = { navController.navigate(AppRoutes.Macros) },
                openPlotter = { navController.navigate(AppRoutes.Plotter) },
                openLogger = { navController.navigate(AppRoutes.Logger) },
                openJoystick = { navController.navigate(AppRoutes.Joystick) }
            )
        }

        composable<AppRoutes.Joystick> {
            JoystickScreen(
                viewModel = viewModel,
                terminalManager = terminalManager,
                onBack = { navController.popBackStack() }
            )
        }

        composable<AppRoutes.Plotter> {
            PlotterScreen(
                viewModel = viewModel,
                terminalManager = terminalManager,
                onBack = { navController.popBackStack() }
            )
        }

        composable<AppRoutes.Logger> {
            LoggerScreen(
                viewModel = viewModel,
                terminalManager = terminalManager,
                onBack = { navController.popBackStack() }
            )
        }

        composable<AppRoutes.Macros> {
            MacrosScreen(
                viewModel = viewModel,
                boardManager = boardManager,
                onBack = { navController.popBackStack() }
            )
        }

        composable<AppRoutes.Terminal> { backStackEntry ->
            val terminal: AppRoutes.Terminal = backStackEntry.toRoute()
            val microScript = remember { (terminal.script ?: "").asMicroScript() }
            TerminalScreen(
                microScript = microScript,
                viewModel = viewModel,
                terminalManager = terminalManager,
                boardManager = boardManager,
                onBack = { navController.popBackStack() }
            )
        }

        composable<AppRoutes.Editor> { backStackEntry ->
            val editor: AppRoutes.Editor = backStackEntry.toRoute()
            val editorState = remember {
                EditorState(
                    (editor.script ?: "").asMicroScript(),
                    editor.blank
                )
            }
            EditorScreen(
                viewModel = viewModel,
                canRun = { canRun },
                editorState = editorState,
                filesManager = filesManager,
                onRemoteRun = { s -> navController.navigate(AppRoutes.Terminal(s.asJson)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable<AppRoutes.Explorer> {
            FilesExplorerScreen(
                filesManager = filesManager,
                viewModel = viewModel,
                terminalManager = terminalManager,
                openTerminal = { microScript ->
                    navController.navigate(
                        AppRoutes.Terminal(
                            microScript.asJson
                        )
                    )
                },
                openEditor = { microScript -> navController.navigate(AppRoutes.Editor(microScript.asJson)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable<AppRoutes.Scripts> {
            ScriptsScreen(
                viewModel = viewModel,
                onOpenLocalScript = { microScript ->
                    navController.navigate(
                        AppRoutes.Editor(
                            microScript.asJson
                        )
                    )
                },
                onNewScript = { navController.navigate(AppRoutes.Editor(blank = true)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable<AppRoutes.Flash> {
            FlashScreen(
                viewModel = viewModel,
                boardManager = boardManager
            )
        }

        composable<AppRoutes.Settings> {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable<AppRoutes.BlueStudioPro> {
            BlueStudioProScreen(
                viewModel = viewModel,
                deviceIdName = proDeviceIdName,
                onFinished = {
                    navController.navigate(AppRoutes.Home) {
                        popUpTo(AppRoutes.Home) { inclusive = true }
                    }
                }
            )
        }
    }
}