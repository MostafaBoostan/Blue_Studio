package com.bluestudio.manager.screens.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bluestudio.manager.R
import com.bluestudio.manager.ui.components.MyButton
import com.bluestudio.manager.ui.components.MyDialog
import com.bluestudio.manager.ui.components.MyDialogState
import com.bluestudio.manager.ui.components.rememberMyDialogState
import com.bluestudio.manager.ui.theme.AppTheme

@Preview
@Composable
private fun FileDeleteDialogPreviewLight() {
    _root_ide_package_.com.bluestudio.manager.ui.theme.AppTheme(darkTheme = false) {
        FileDeleteDialog(
            name = { "script.py" },
            onOk = {}
        )
    }
}

@Preview
@Composable
private fun FileDeleteDialogPreviewDark() {
    _root_ide_package_.com.bluestudio.manager.ui.theme.AppTheme(darkTheme = true) {
        FileDeleteDialog(
            name = { "script.py" },
            onOk = {}
        )
    }
}

@Composable
fun FileDeleteDialog(
    name: () -> String,
    state: com.bluestudio.manager.ui.components.MyDialogState = _root_ide_package_.com.bluestudio.manager.ui.components.rememberMyDialogState(
        visible = true
    ),
    onOk: () -> Unit
) {
    _root_ide_package_.com.bluestudio.manager.ui.components.MyDialog(state) {
        ApproveDialogContent(
            message = stringResource(R.string.editor_msg_delete, name()),
            onOk = {
                state.dismiss()
                onOk()
            },
            onDismiss = { state.dismiss() }
        )
    }
}

@Composable
internal fun ApproveDialogContent(
    message: String,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    textAlign: TextAlign = TextAlign.Center,
    onOk: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = style,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = textAlign
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            _root_ide_package_.com.bluestudio.manager.ui.components.MyButton(
                text = stringResource(id = R.string.dialog_yes),
                modifier = Modifier.weight(0.4f),
                onClick = onOk
            )
            _root_ide_package_.com.bluestudio.manager.ui.components.MyButton(
                text = stringResource(id = R.string.dialog_no),
                background = MaterialTheme.colorScheme.secondary,
                color = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.weight(0.4f),
                onClick = onDismiss
            )
        }
    }
}