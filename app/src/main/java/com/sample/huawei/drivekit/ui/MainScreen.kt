package com.sample.huawei.drivekit.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MainScreen(
    onFileSelected: (Uri) -> Unit = { },
    onDownloadClick: () -> Unit = { }
) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { onFileSelected(it) } }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(
            modifier = Modifier.padding(12.dp),
            onClick = {
                launcher.launch(arrayOf("*/*"))
            },
            shape = CircleShape
        ) {
            Text("Upload File")
        }
        Button(
            modifier = Modifier.padding(12.dp),
            onClick = onDownloadClick,
            shape = CircleShape
        ) {
            Text("Download File")
        }
    }
}