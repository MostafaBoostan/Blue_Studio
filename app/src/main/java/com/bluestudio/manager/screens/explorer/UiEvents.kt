package com.bluestudio.manager.screens.explorer

import com.bluestudio.manager.model.MicroFile

sealed interface ExplorerEvents {
    data class Run(val file: com.bluestudio.manager.model.MicroFile) : ExplorerEvents
    data class OpenFolder(val file: com.bluestudio.manager.model.MicroFile) : ExplorerEvents
    data class Remove(val file: com.bluestudio.manager.model.MicroFile) : ExplorerEvents
    data class Rename(val file: com.bluestudio.manager.model.MicroFile) : ExplorerEvents
    data class Edit(val file: com.bluestudio.manager.model.MicroFile) : ExplorerEvents
    data class New(val file: com.bluestudio.manager.model.MicroFile) : ExplorerEvents
    data class Export(val file: com.bluestudio.manager.model.MicroFile) : ExplorerEvents
    data object Import : ExplorerEvents
    data object Refresh : ExplorerEvents
    data object Up : ExplorerEvents
}