package com.bluestudio.manager.screens.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
private fun FileRenameDialogPreviewLight() {
    _root_ide_package_.com.bluestudio.manager.ui.theme.AppTheme(darkTheme = false) {
        FileRenameDialog(
            name = { "main.py" },
            onOk = {}
        )
    }
}

@Preview
@Composable
private fun FileRenameDialogPreviewDark() {
    _root_ide_package_.com.bluestudio.manager.ui.theme.AppTheme(darkTheme = true) {
        FileRenameDialog(
            name = { "main.py" },
            onOk = {}
        )
    }
}

@Composable
fun FileRenameDialog(
    name: () -> String,
    state: com.bluestudio.manager.ui.components.MyDialogState = _root_ide_package_.com.bluestudio.manager.ui.components.rememberMyDialogState(
        visible = true
    ),
    onOk: (String) -> Unit
) {
    _root_ide_package_.com.bluestudio.manager.ui.components.MyDialog(state) {
        InputDialogContent(
            name = name(),
            message = stringResource(R.string.explorer_rename_label, name()),
            onDismiss = { state.dismiss() },
            onOk = {
                state.dismiss()
                onOk(it)
            }
        )
    }
}

@Composable
internal fun InputDialogContent(
    name: String,
    message: String,
    onDismiss: () -> Unit,
    onOk: (String) -> Unit
) {
    var fileName by remember { mutableStateOf(name) }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center, maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        BasicTextField(
            value = fileName,
            onValueChange = { fileName = it },
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White
                )
                .padding(vertical = 16.dp, horizontal = 8.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            _root_ide_package_.com.bluestudio.manager.ui.components.MyButton(
                text = stringResource(id = R.string.dialog_ok),
                modifier = Modifier.weight(0.4f),
                onClick = { onOk(fileName) }
            )
            _root_ide_package_.com.bluestudio.manager.ui.components.MyButton(
                text = stringResource(id = R.string.dialog_cancel),
                background = MaterialTheme.colorScheme.secondary,
                color = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.weight(0.4f),
                onClick = onDismiss
            )
        }
    }
}