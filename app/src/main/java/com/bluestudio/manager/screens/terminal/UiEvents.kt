package com.bluestudio.manager.screens.terminal

sealed interface TerminalEvents {
    data object Run : TerminalEvents
    data object Terminate : TerminalEvents
    data object SoftReset : TerminalEvents
    data object Clear : TerminalEvents
    data object MoveUp : TerminalEvents
    data object MoveDown : TerminalEvents
    data object Back : TerminalEvents
}