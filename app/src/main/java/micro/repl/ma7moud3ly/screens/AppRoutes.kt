package micro.repl.ma7moud3ly

import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoutes {
    @Serializable
    data object Home : AppRoutes

    @Serializable
    data class Terminal(val script: String? = null) : AppRoutes

    @Serializable
    data class Editor(val script: String? = null, val blank: Boolean = false) : AppRoutes

    @Serializable
    data object Explorer : AppRoutes

    @Serializable
    data object Scripts : AppRoutes

    @Serializable
    data object Flash : AppRoutes

    @Serializable
    data object Settings : AppRoutes

    @Serializable
    data object Macros : AppRoutes

    @Serializable
    data object Plotter : AppRoutes

    @Serializable
    data object Logger : AppRoutes

    @Serializable
    data object Joystick : AppRoutes

    @Serializable
    data object BlueStudioPro : AppRoutes
}