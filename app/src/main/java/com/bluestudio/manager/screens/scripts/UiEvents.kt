package com.bluestudio.manager.screens.scripts

import com.bluestudio.manager.model.MicroScript

sealed interface ScriptsEvents {
    data class Run(val script: com.bluestudio.manager.model.MicroScript) : ScriptsEvents
    data class Open(val script: com.bluestudio.manager.model.MicroScript) : ScriptsEvents
    data class Delete(val script: com.bluestudio.manager.model.MicroScript) : ScriptsEvents
    data class Rename(val script: com.bluestudio.manager.model.MicroScript) : ScriptsEvents
    data class Share(val script: com.bluestudio.manager.model.MicroScript) : ScriptsEvents
    data object NewScript : ScriptsEvents
    data object Back : ScriptsEvents
}