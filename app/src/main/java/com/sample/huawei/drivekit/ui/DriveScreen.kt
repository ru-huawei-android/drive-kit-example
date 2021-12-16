package com.sample.huawei.drivekit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DriveScreen(
    name: String,
    children: List<String>,
    onFolderClick: (Int) -> Unit = { },
    onBack: () -> Unit = { },
    onSelect: () -> Unit = { },
    onCreateNewFolder: (String) -> Unit = { }
) {
    var showDialog by remember { mutableStateOf(false) }
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
            FolderItem(
                onClick = onBack,
                imageVector = Icons.Filled.SubdirectoryArrowRight,
                name = ". . ."
            )
            LazyColumn {
                itemsIndexed(children) { index, child ->
                    FolderItem(
                        onClick = { onFolderClick(index) },
                        imageVector = Icons.Outlined.Folder,
                        name = child
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .background(Color.White)
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = onSelect,
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
private fun FolderItem(
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

@Preview
@Composable
fun FolderScreenPreview() {
    DriveScreen(
        name = "My Drive",
        children = List(7) {
            "Folder ${it + 1}"
        }
    )
}