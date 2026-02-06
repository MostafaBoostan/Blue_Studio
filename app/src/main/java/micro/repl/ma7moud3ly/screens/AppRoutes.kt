package micro.repl.ma7moud3ly

import kotlinx.serialization.Serializable

sealed interface AppRoutes {
    @Serializable
    data object Home : AppRoutes

    @Serializable
    data object Explorer : AppRoutes

    @Serializable
    data object Scripts : AppRoutes

    @Serializable
    data class Terminal(val script: String = "") : AppRoutes

    @Serializable
    data class Editor(val script: String = "", val blank: Boolean = false) : AppRoutes

    @Serializable
    data object Flash : AppRoutes

    @Serializable
    data object Settings : AppRoutes

    // --- Pro Mode Routes ---

    @Serializable
    data object BlueStudioPro : AppRoutes // فقط یک بار باید باشه

    @Serializable
    data object ProDashboard : AppRoutes // فقط یک بار باید باشه
}