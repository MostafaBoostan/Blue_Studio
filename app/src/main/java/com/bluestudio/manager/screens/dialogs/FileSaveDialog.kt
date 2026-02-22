package com.bluestudio.manager.screens.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.bluestudio.manager.R
import com.bluestudio.manager.ui.components.MyDialog
import com.bluestudio.manager.ui.components.MyDialogState
import com.bluestudio.manager.ui.components.rememberMyDialogState
import com.bluestudio.manager.ui.theme.AppTheme

@Preview
@Composable
private fun FileSaveDialogPreviewLight() {
    _root_ide_package_.com.bluestudio.manager.ui.theme.AppTheme(darkTheme = false) {
        FileSaveDialog(
            name = { "script.py" },
            onOk = {}
        )
    }
}

@Preview
@Composable
private fun FileSaveDialogPreviewDark() {
    _root_ide_package_.com.bluestudio.manager.ui.theme.AppTheme(darkTheme = true) {
        FileSaveDialog(
            name = { "script.py" },
            onOk = {}
        )
    }
}

@Preview
@Composable
private fun FileSaveDialogPreviewLight2() {
    _root_ide_package_.com.bluestudio.manager.ui.theme.AppTheme(darkTheme = false) {
        FileSaveDialog(
            name = { "" },
            onOk = {}
        )
    }
}

@Preview
@Composable
private fun FileSaveDialogPreviewDark2() {
    _root_ide_package_.com.bluestudio.manager.ui.theme.AppTheme(darkTheme = true) {
        FileSaveDialog(
            name = { "" },
            onOk = {}
        )
    }
}

@Composable
fun FileSaveDialog(
    name: () -> String,
    state: com.bluestudio.manager.ui.components.MyDialogState = _root_ide_package_.com.bluestudio.manager.ui.components.rememberMyDialogState(
        visible = true
    ),
    onOk: () -> Unit,
    onDismiss: () -> Unit = {}
) {
    _root_ide_package_.com.bluestudio.manager.ui.components.MyDialog(
        state = state,
        dismissOnClickOutside = false
    ) {
        val fileName = name()
        ApproveDialogContent(
            message = if (fileName.isEmpty())
                stringResource(R.string.editor_msg_save)
            else stringResource(
                R.string.editor_msg_save_changes,
                fileName
            ),
            onOk = {
                state.dismiss()
                onOk()
            },
            onDismiss = {
                state.dismiss()
                onDismiss()
            }
        )
    }
}
