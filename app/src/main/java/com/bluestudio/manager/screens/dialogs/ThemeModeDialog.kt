package com.bluestudio.manager.screens.dialogs

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.bluestudio.manager.R
import com.bluestudio.manager.ui.components.MyDialog
import com.bluestudio.manager.ui.components.MyDialogState
import com.bluestudio.manager.ui.components.rememberMyDialogState
import com.bluestudio.manager.ui.theme.AppTheme

@Preview
@Composable
private fun ThemeModeDialogPreviewLight() {
    val darkMode = false
    _root_ide_package_.com.bluestudio.manager.ui.theme.AppTheme(darkTheme = darkMode) {
        ThemeModeDialog(
            isDark = darkMode,
            onOk = {}
        )
    }
}

@Preview
@Composable
private fun ThemeModeDialogPreviewDark() {
    val darkMode = true
    _root_ide_package_.com.bluestudio.manager.ui.theme.AppTheme(darkTheme = darkMode) {
        ThemeModeDialog(
            isDark = darkMode,
            onOk = {}
        )
    }
}

@Composable
fun ThemeModeDialog(
    state: com.bluestudio.manager.ui.components.MyDialogState = _root_ide_package_.com.bluestudio.manager.ui.components.rememberMyDialogState(
        visible = true
    ),
    isDark: Boolean,
    onOk: () -> Unit
) {
    _root_ide_package_.com.bluestudio.manager.ui.components.MyDialog(state = state) {
        val newMode = stringResource(
            if (isDark) R.string.config_light_mode
            else R.string.config_light_mode
        )
        ApproveDialogContent(
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Justify,
            message = stringResource(R.string.editor_msg_mode, newMode),
            onOk = {
                state.dismiss()
                onOk()
            },
            onDismiss = { state.dismiss() }
        )
    }
}
