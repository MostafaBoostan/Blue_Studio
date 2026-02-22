package com.bluestudio.manager

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bluestudio.manager.managers.BoardManager
import com.bluestudio.manager.managers.TerminalHistoryManager
import com.bluestudio.manager.model.ConnectionStatus
import com.bluestudio.manager.model.MicroDevice
import com.bluestudio.manager.model.MicroFile
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

// دیتا کلاس ماکرو
data class Macro(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val command: String
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // --- آماده‌سازی ابزارهای ذخیره‌سازی ---
    private val prefs = application.getSharedPreferences("micro_repl_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // کلیدهای ذخیره‌سازی
    private val MACROS_KEY = "saved_macros_list"
    private val DARK_MODE_KEY = "dark_mode"
    private val FONT_SCALE_KEY = "font_scale"
    private val LANGUAGE_KEY = "app_language"

    // --- وضعیت اتصال ---
    val status = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Connecting)

    // دسترسی سریع به دستگاه متصل (اگر متصل باشد)
    val microDevice: MicroDevice?
        get() = (status.value as? ConnectionStatus.Connected)?.microDevice

    // --- فایل منیجر و ترمینال ---
    val root = mutableStateOf("/")
    val files = MutableStateFlow<List<MicroFile>>(emptyList())

    val terminalInput = mutableStateOf("")
    val terminalOutput = mutableStateOf("")
    val history = TerminalHistoryManager()

    // --- Pro Mode (تشخیص دستگاه خاص) ---
    private val _navigateToPro = MutableSharedFlow<Boolean>()
    val navigateToPro = _navigateToPro.asSharedFlow()

    private val _proDeviceId = MutableStateFlow("")
    val proDeviceId = _proDeviceId.asStateFlow()

    val isProMode = _proDeviceId.map { it.isNotEmpty() && it != "Basic" && it != "Unknown" }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    // --- مدیریت ماکروها ---
    private val _macros = MutableStateFlow<List<Macro>>(loadMacrosFromStorage())
    val macros = _macros.asStateFlow()

    fun addMacro(name: String, command: String) {
        val newList = _macros.value.toMutableList().apply {
            add(Macro(name = name, command = command))
        }
        _macros.value = newList
        saveMacrosToStorage(newList)
    }

    fun removeMacro(macro: Macro) {
        val newList = _macros.value.toMutableList().apply {
            remove(macro)
        }
        _macros.value = newList
        saveMacrosToStorage(newList)
    }

    private fun loadMacrosFromStorage(): List<Macro> {
        val json = prefs.getString(MACROS_KEY, null)
        return if (json != null) {
            try {
                val type = object : TypeToken<List<Macro>>() {}.type
                gson.fromJson(json, type)
            } catch (e: Exception) {
                getDefaultMacros()
            }
        } else {
            getDefaultMacros()
        }
    }

    private fun saveMacrosToStorage(list: List<Macro>) {
        val json = gson.toJson(list)
        prefs.edit().putString(MACROS_KEY, json).apply()
    }

    private fun getDefaultMacros(): List<Macro> {
        return listOf(
            Macro(name = "LED On", command = "import machine; machine.Pin(2, machine.Pin.OUT).on()"),
            Macro(name = "LED Off", command = "import machine; machine.Pin(2, machine.Pin.OUT).off()"),
            Macro(name = "Hello", command = "print('Hello from BlueStudio')")
        )
    }

    // --- توابع مربوط به اتصال و شناسایی ---

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
                    // ارسال دستور برای گرفتن ID دستگاه (بدون نمایش در خروجی کاربر)
                    val cleanCommand = "\u0003\u0003\r\nprint('#ID:' + getattr(BlueCore, 'DEVICE_ID', 'Basic'))\r\n"
                    boardManager.writeCommand(cleanCommand)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // --- تنظیمات (Settings) ---

    // 1. Dark Mode
    private val _isDarkMode = MutableStateFlow(prefs.getBoolean(DARK_MODE_KEY, true))
    val isDarkMode = _isDarkMode.asStateFlow()

    fun setDarkMode(enable: Boolean) {
        _isDarkMode.value = enable
        prefs.edit().putBoolean(DARK_MODE_KEY, enable).apply()
    }

    // 2. Font Scale
    private val _fontScale = MutableStateFlow(prefs.getFloat(FONT_SCALE_KEY, 1.0f))
    val fontScale = _fontScale.asStateFlow()

    fun setFontScale(scale: Float) {
        _fontScale.value = scale
        prefs.edit().putFloat(FONT_SCALE_KEY, scale).apply()
    }

    // 3. Language (زبان) - کامل شده با قابلیت ذخیره
    private val _currentLanguage = MutableStateFlow(prefs.getString(LANGUAGE_KEY, "en") ?: "en")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    fun setLanguage(lang: String) {
        _currentLanguage.value = lang
        // ذخیره در حافظه تا بعد از بستن برنامه زبان نپرد
        prefs.edit().putString(LANGUAGE_KEY, lang).apply()
    }
}