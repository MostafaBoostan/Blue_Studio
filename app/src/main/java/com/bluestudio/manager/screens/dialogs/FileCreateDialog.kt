package com.bluestudio.manager.screens.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.bluestudio.manager.R
import com.bluestudio.manager.ui.theme.AppTheme
import com.bluestudio.manager.ui.components.MyDialog
import com.bluestudio.manager.model.MicroFile
import com.bluestudio.manager.ui.components.MyDialogState
import com.bluestudio.manager.ui.components.rememberMyDialogState

private val microFile = _root_ide_package_.com.bluestudio.manager.model.MicroFile(
    name = "",
    path = "lib",
    type = _root_ide_package_.com.bluestudio.manager.model.MicroFile.Companion.FILE,
    size = 300000
)


private val microDir = _root_ide_package_.com.bluestudio.manager.model.MicroFile(
    name = "",
    path = "lib",
    type = _root_ide_package_.com.bluestudio.manager.model.MicroFile.Companion.DIRECTORY
)

@Preview
@Composable
private fun FileCreateDialogPreviewLight() {
    _root_ide_package_.com.bluestudio.manager.ui.theme.AppTheme(darkTheme = false) {
        FileCreateDialog(
            microFile = { microFile },
            onOk = {}
        )
    }
}

@Preview
@Composable
private fun FileCreateDialogPreviewDark() {
    _root_ide_package_.com.bluestudio.manager.ui.theme.AppTheme(darkTheme = true) {
        FileCreateDialog(
            microFile = { microFile },
            onOk = {}
        )
    }
}

@Preview
@Composable
private fun FileCreateDialogPreviewLight2() {
    _root_ide_package_.com.bluestudio.manager.ui.theme.AppTheme(darkTheme = false) {
        FileCreateDialog(
            microFile = { microDir },
            onOk = {}
        )
    }
}

@Preview
@Composable
private fun FileCreateDialogPreviewDark2() {
    _root_ide_package_.com.bluestudio.manager.ui.theme.AppTheme(darkTheme = true) {
        FileCreateDialog(
            microFile = { microDir },
            onOk = {}
        )
    }
}

@Composable
fun FileCreateDialog(
    state: com.bluestudio.manager.ui.components.MyDialogState = _root_ide_package_.com.bluestudio.manager.ui.components.rememberMyDialogState(
        visible = true
    ),
    microFile: () -> com.bluestudio.manager.model.MicroFile?,
    onOk: (file: com.bluestudio.manager.model.MicroFile) -> Unit
) {
    val file = microFile() ?: return
    val message = if (file.name.isEmpty()) {
        stringResource(id = R.string.explorer_create) + " " + stringResource(
            id = if (file.isFile) R.string.explorer_file_new
            else R.string.explorer_new_folder
        ) + "~/" + file.path
    } else stringResource(
        id = R.string.explorer_rename_label, file.name
    )

    val name by remember(file) {
        derivedStateOf {
            if (file.name.isNotEmpty()) file.name
            else if (file.isFile) "main.py"
            else "folder"
        }
    }

    _root_ide_package_.com.bluestudio.manager.ui.components.MyDialog(
        state = state,
        dismissOnClickOutside = false
    ) {
        InputDialogContent(
            name = name,
            message = message,
            onDismiss = { state.dismiss() },
            onOk = { fileName ->
                val newFile = _root_ide_package_.com.bluestudio.manager.model.MicroFile(
                    name = fileName,
                    path = file.path,
                    type = if (file.isFile) _root_ide_package_.com.bluestudio.manager.model.MicroFile.Companion.FILE
                    else _root_ide_package_.com.bluestudio.manager.model.MicroFile.Companion.DIRECTORY
                )
                state.dismiss()
                onOk(newFile)
            }
        )
    }
}

