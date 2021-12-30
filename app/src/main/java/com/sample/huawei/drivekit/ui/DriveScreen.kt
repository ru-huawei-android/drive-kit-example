package com.sample.huawei.drivekit.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sample.huawei.drivekit.DisplayMode

@Composable
fun DriveScreen(
    mode: DisplayMode = DisplayMode.Upload,
    name: String,
    folders: List<String>,
    onFolderClick: (Int) -> Unit = { },
    files: List<String> = emptyList(),
    onFileClick: (Int) -> Unit = { },
    onBack: () -> Unit = { },
    onSubmitFolder: () -> Unit = { },
    onCreateNewFolder: (String) -> Unit = { },
    onActionDone: () -> Unit,
    progress: Float? = null
) {
    Box {
        Column(
            modifier = Modifier
                .background(Color.White)
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Text(
                text = name,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                fontSize = 24.sp
            )
            Spacer(Modifier.height(24.dp))
            LazyColumn {
                item {
                    Item(
                        onClick = onBack,
                        imageVector = Icons.Filled.SubdirectoryArrowRight,
                        name = ". . ."
                    )
                }
                itemsIndexed(folders) { index, folder ->
                    Item(
                        onClick = { onFolderClick(index) },
                        imageVector = Icons.Filled.FolderOpen,
                        name = folder
                    )
                }
                itemsIndexed(files) { index, file ->
                    Item(
                        onClick = {
                            if(mode == DisplayMode.Download)
                                onFileClick(index)
                        },
                        imageVector = Icons.Filled.TextSnippet,
                        name = file
                    )
                }
            }
        }
        if(mode == DisplayMode.Upload) {
            BottomUploadButtons(
                modifier = Modifier.align(Alignment.BottomCenter),
                onSubmit = onSubmitFolder,
                onCreateNewFolder = onCreateNewFolder
            )
        }
        progress?.let {
            ProgressDialog(
                mode = mode,
                progress = it,
                onActionDone = onActionDone
            )
        }
    }
}

@Composable
private fun ProgressDialog(
    mode: DisplayMode,
    progress: Float,
    onActionDone: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = keyframes { },
    )
    AlertDialog(
        onDismissRequest = { },
        title = {
            val title = when (mode) {
                DisplayMode.Download -> "Download progress"
                DisplayMode.Upload -> "Upload progress"
            }
            Text(
                text = title,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        buttons = {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = animatedProgress
                )
                TextButton(
                    onClick = onActionDone,
                    enabled = animatedProgress >= 1f
                ) {
                    Text("OK")
                }
            }
        }
    )
}

@Composable
private fun BottomUploadButtons(
    modifier: Modifier = Modifier,
    onSubmit: () -> Unit,
    onCreateNewFolder: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .background(Color.White)
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Button(
            onClick = onSubmit,
            shape = CircleShape
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Done, null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Submit")
            }
        }
        Button(
            onClick = { showDialog = true },
            shape = CircleShape
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CreateNewFolder, null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Folder")
            }
        }
    }
    if(showDialog) {
        NewFolderDialog(
            onDismissRequest = { showDialog = false },
            onCreateNewFolder = onCreateNewFolder
        )
    }
}

@Composable
private fun NewFolderDialog(
    onDismissRequest: () -> Unit,
    onCreateNewFolder: (String) -> Unit
) {
    var newFolderName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = "Create New Folder",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        buttons = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = {
                        newFolderName = it
                    },
                    label = {
                        Text("New folder name")
                    },
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    TextButton(
                        onClick = {
                            onCreateNewFolder(newFolderName)
                            onDismissRequest()
                        }
                    ) {
                        Text("CREATE")
                    }
                    TextButton(
                        onClick = onDismissRequest
                    ) {
                        Text("CANCEL")
                    }
                }
            }
        }
    )
}

@Composable
private fun Item(
    onClick: () -> Unit,
    name: String = "",
    imageVector: ImageVector? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        imageVector?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .rotate(if (name == ". . .") 180f else 0f)
            )
        }
        Text(
            text = name,
            fontSize = 20.sp,
            modifier = Modifier.padding(4.dp)
        )
    }
}