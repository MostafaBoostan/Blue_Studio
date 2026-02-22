package com.bluestudio.manager.screens.dialogs

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
private fun ImportScriptDialogPreviewLight() {
    _root_ide_package_.com.bluestudio.manager.ui.theme.AppTheme(darkTheme = false) {
        ImportScriptDialog(
            onOk = {}
        )
    }
}

@Preview
@Composable
private fun ImportScriptDialogPreviewDark() {
    _root_ide_package_.com.bluestudio.manager.ui.theme.AppTheme(darkTheme = true) {
        ImportScriptDialog(
            onOk = {}
        )
    }
}

@Composable
fun ImportScriptDialog(
    state: com.bluestudio.manager.ui.components.MyDialogState = _root_ide_package_.com.bluestudio.manager.ui.components.rememberMyDialogState(
        visible = true
    ),
    onOk: () -> Unit
) {
    _root_ide_package_.com.bluestudio.manager.ui.components.MyDialog(state = state) {
        ApproveDialogContent(
            textAlign = TextAlign.Justify,
            message = stringResource(R.string.explorer_import_msg),
            onOk = {
                state.dismiss()
                onOk()
            },
            onDismiss = { state.dismiss() }
        )
    }
}
