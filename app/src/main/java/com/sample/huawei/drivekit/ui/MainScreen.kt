package com.sample.huawei.drivekit.ui

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sample.huawei.drivekit.CredentialManager

@Composable
fun MainScreen(
    signedIn: Boolean,
    onSignInResult: (Intent?) -> Unit,
    onFileSelected: (Uri) -> Unit = { },
    onDownloadClick: () -> Unit = { }
) {
    val context = LocalContext.current

    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        onSignInResult(it.data)
    }

    val pickFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { onFileSelected(it) } }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(
            modifier = Modifier.padding(12.dp),
            enabled = !signedIn,
            onClick = {
                signInLauncher.launch(CredentialManager.getSignInIntent(context))
            },
            shape = CircleShape
        ) {
            Text("Sign in")
        }
        Button(
            modifier = Modifier.padding(12.dp),
            enabled = signedIn,
            onClick = {
                pickFileLauncher.launch(arrayOf("*/*"))
            },
            shape = CircleShape
        ) {
            Text("Upload File")
        }
        Button(
            modifier = Modifier.padding(12.dp),
            enabled = signedIn,
            onClick = onDownloadClick,
            shape = CircleShape
        ) {
            Text("Download File")
        }
    }
}