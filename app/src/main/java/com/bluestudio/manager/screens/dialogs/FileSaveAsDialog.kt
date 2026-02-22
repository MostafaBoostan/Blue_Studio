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
private fun FileSaveAsDialogPreviewLight() {
    _root_ide_package_.com.bluestudio.manager.ui.theme.AppTheme(darkTheme = false) {
        FileSaveAsDialog(
            name = { "main.py" },
            onOk = {}
        )
    }
}

@Preview
@Composable
private fun FileSaveAsDialogPreviewDark() {
    _root_ide_package_.com.bluestudio.manager.ui.theme.AppTheme(darkTheme = true) {
        FileSaveAsDialog(
            name = { "main.py" },
            onOk = {}
        )
    }
}

@Composable
fun FileSaveAsDialog(
    name: () -> String,
    state: com.bluestudio.manager.ui.components.MyDialogState = _root_ide_package_.com.bluestudio.manager.ui.components.rememberMyDialogState(
        visible = true
    ),
    onOk: (String) -> Unit,
    onDismiss: () -> Unit = {}
) {
    _root_ide_package_.com.bluestudio.manager.ui.components.MyDialog(
        state = state,
        dismissOnClickOutside = false
    ) {
        InputDialogContent(
            name = name(),
            message = stringResource(R.string.editor_msg_save),
            onDismiss = {
                state.dismiss()
                onDismiss()
            },
            onOk = {
                state.dismiss()
                onOk(it)
            },
        )
    }
}
