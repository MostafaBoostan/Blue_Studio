package micro.repl.ma7moud3ly

import kotlinx.serialization.Serializable

sealed class AppRoutes {
    @Serializable
    object Home

    @Serializable
    data class Terminal(val script: String = "")

    @Serializable
    data class Editor(val script: String = "", val blank: Boolean = false)

    @Serializable
    object Explorer

    @Serializable
    object Scripts

    @Serializable
    object Flash

    @Serializable
    object Settings

    @Serializable
    object BlueStudioPro

    @Serializable
    object Macros
}