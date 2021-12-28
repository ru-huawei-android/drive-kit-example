package com.sample.huawei.drivekit

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

class DriveActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel: DriveViewModel by viewModels()
        CredentialManager.signInWithDrivePermissionRequest(
            activity = this,
            onSuccess = {
                setContent {
                    Router(viewModel)
                }
            }
        )
    }

    companion object {
        const val TAG = "DriveKitDemo"
    }
}