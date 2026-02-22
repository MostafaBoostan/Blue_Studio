package com.bluestudio.manager.managers

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import com.bluestudio.manager.model.MicroDevice
import com.bluestudio.manager.model.MicroScript

class TerminalManager(
    private val boardManager: BoardManager
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _output = MutableSharedFlow<String>()
    val output = _output.asSharedFlow()

    init {
        boardManager.addOnDataReceivedListener { text ->
            scope.launch {
                _output.emit(text)
            }
        }
    }

    fun terminateExecution() {
        boardManager.writeCommand(CommandsManager.TERMINATE)
    }

    fun resetDevice(
        microDevice: com.bluestudio.manager.model.MicroDevice,
        onReset: (() -> Unit)? = null
    ) {
        val cmd = if (microDevice.isMicroPython) "machine.reset()" else ""
        boardManager.write(cmd)
        onReset?.invoke()
    }

    fun softResetDevice(onReset: (() -> Unit)? = null) {
        boardManager.writeCommand(CommandsManager.RESET)
        onReset?.invoke()
    }

    fun eval(code: String, onEval: (() -> Unit)? = null) {
        boardManager.write(code.trim())
        onEval?.invoke()
    }

    fun evalMultiLine(code: String, onEval: (() -> Unit)? = null) {
        boardManager.write(code.replace("\n", "\r").trim())
        boardManager.write("\r")
        onEval?.invoke()
    }

    suspend fun executeLocalScript(
        microScript: com.bluestudio.manager.model.MicroScript,
        onClear: () -> Unit
    ) {
        boardManager.writeCommand(CommandsManager.RESET)
        delay(100)
        onClear()
        boardManager.writeCommand(CommandsManager.SILENT_MODE)
        boardManager.writeCommand("print()\r\n${microScript.content}")
        boardManager.writeCommand(CommandsManager.RESET)
        boardManager.writeCommand(CommandsManager.REPL_MODE)
    }

    fun executeScript(microScript: com.bluestudio.manager.model.MicroScript) {
        boardManager.writeCommand(CommandsManager.RESET)
        boardManager.write(CommandsManager.chDir(microScript.scriptDir))
        boardManager.write("import ${microScript.nameWithoutExt}")
        boardManager.writeCommand(CommandsManager.RESET)
        boardManager.writeCommand(CommandsManager.REPL_MODE)
    }

    fun sendCommand(command: String) {
        boardManager.writeCommand(command)
    }

    companion object {
        private const val TAG = "TerminalManager"
    }
}