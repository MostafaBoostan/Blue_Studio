package micro.repl.ma7moud3ly.screens

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
import micro.repl.ma7moud3ly.AppRoutes
import micro.repl.ma7moud3ly.MainViewModel
import micro.repl.ma7moud3ly.managers.BoardManager
import micro.repl.ma7moud3ly.managers.FilesManager
import micro.repl.ma7moud3ly.managers.TerminalManager
import micro.repl.ma7moud3ly.model.ConnectionStatus
import micro.repl.ma7moud3ly.model.EditorState
import micro.repl.ma7moud3ly.model.asMicroScript
import micro.repl.ma7moud3ly.screens.editor.EditorScreen
import micro.repl.ma7moud3ly.screens.explorer.FilesExplorerScreen
import micro.repl.ma7moud3ly.screens.flash.FlashScreen
import micro.repl.ma7moud3ly.screens.home.HomeScreen
import micro.repl.ma7moud3ly.screens.pro.BlueStudioProScreen
import micro.repl.ma7moud3ly.screens.scripts.ScriptsScreen
import micro.repl.ma7moud3ly.screens.settings.SettingsScreen
import micro.repl.ma7moud3ly.screens.terminal.TerminalScreen

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

    // دریافت مسیر فعلی برای جلوگیری از پرش
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    // دریافت نام مسیر به صورت ایمن (با توجه به اینکه از Type-Safe استفاده میکنیم)
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(Unit) {
        viewModel.status.collect { status ->
            if (status is ConnectionStatus.Connected) {
                canRun = true
            } else {
                canRun = false
                if (status !is ConnectionStatus.Connecting) {

                    viewModel.resetProMode()

                    // فقط اگر در Home نیستیم، برگردیم عقب
                    // این جلوی رفرش شدن بیخود صفحه هوم و لگ زدن رو میگیره
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
                // همیشه نویگیت کن چون دستور جدید اومده
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
                openSettings = { navController.navigate(AppRoutes.Settings) }
            )
        }

        composable<AppRoutes.Terminal> { backStackEntry ->
            val terminal: AppRoutes.Terminal = backStackEntry.toRoute()
            val microScript = remember { terminal.script.asMicroScript() }
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
            val editorState = remember { EditorState(editor.script.asMicroScript(), editor.blank) }
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
                openTerminal = { microScript -> navController.navigate(AppRoutes.Terminal(microScript.asJson)) },
                openEditor = { microScript -> navController.navigate(AppRoutes.Editor(microScript.asJson)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable<AppRoutes.Scripts> {
            ScriptsScreen(
                viewModel = viewModel,
                onOpenLocalScript = { microScript -> navController.navigate(AppRoutes.Editor(microScript.asJson)) },
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