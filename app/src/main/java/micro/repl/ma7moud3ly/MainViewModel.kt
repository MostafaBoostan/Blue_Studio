package micro.repl.ma7moud3ly

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import micro.repl.ma7moud3ly.managers.BoardManager
import micro.repl.ma7moud3ly.managers.TerminalHistoryManager
import micro.repl.ma7moud3ly.model.ConnectionStatus
import micro.repl.ma7moud3ly.model.MicroDevice
import micro.repl.ma7moud3ly.model.MicroFile

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val status = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Connecting)

    val microDevice: MicroDevice? get() = (status.value as? ConnectionStatus.Connected)?.microDevice

    val root = mutableStateOf("/")
    val files = MutableStateFlow<List<MicroFile>>(listOf())

    val terminalInput = mutableStateOf("")
    val terminalOutput = mutableStateOf("")
    val history = TerminalHistoryManager()

    private val _navigateToPro = MutableSharedFlow<Boolean>()
    val navigateToPro = _navigateToPro.asSharedFlow()

    private val _proDeviceId = MutableStateFlow("")
    val proDeviceId = _proDeviceId.asStateFlow()

    val isProMode = _proDeviceId.map { it.isNotEmpty() && it != "Basic" && it != "Unknown" }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun triggerProMode(idName: String) {
        _proDeviceId.value = idName
        viewModelScope.launch {
            _navigateToPro.emit(true)
        }
    }

    fun resetProMode() {
        _proDeviceId.value = ""
    }

    fun onDeviceConnected(boardManager: BoardManager) {
        viewModelScope.launch {
            delay(300)
            if (status.value is ConnectionStatus.Connected) {
                try {
                    val cleanCommand = "\u0003\u0003\r\nprint('#ID:' + BlueCore.DEVICE_ID)\r\n"
                    boardManager.writeCommand(cleanCommand)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private val prefs = application.getSharedPreferences("micro_repl_prefs", Context.MODE_PRIVATE)

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("dark_mode", true))
    val isDarkMode = _isDarkMode.asStateFlow()

    fun setDarkMode(enable: Boolean) {
        _isDarkMode.value = enable
        prefs.edit().putBoolean("dark_mode", enable).apply()
    }

    private val _fontScale = MutableStateFlow(prefs.getFloat("font_scale", 1.0f))
    val fontScale = _fontScale.asStateFlow()

    fun setFontScale(scale: Float) {
        _fontScale.value = scale
        prefs.edit().putFloat("font_scale", scale).apply()
    }
}