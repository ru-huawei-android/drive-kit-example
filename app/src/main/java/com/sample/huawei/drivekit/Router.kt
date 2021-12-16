package com.sample.huawei.drivekit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sample.huawei.drivekit.ui.DriveScreen
import com.sample.huawei.drivekit.ui.UploadScreen

@Composable
fun Router(viewModel: FolderViewModel) {
    val navController = rememberNavController()
    LaunchedEffect(true) {
        viewModel.getChildren()
    }
    NavHost(
        navController = navController,
        startDestination = Destinations.Upload
    ) {
        with(viewModel) {
            composable(Destinations.Upload) {
                UploadScreen(
                    onFileSelected = {
                        onFilePicked(it)
                        navController.navigate(Destinations.Drive)
                    }
                )
            }
            composable(Destinations.Drive) {
                DriveScreen(
                    name = currentFolder?.fileName ?: "Select a cloud folder to upload",
                    children = children.map { it?.fileName ?: "" },
                    onFolderClick = ::openFolder,
                    onSelect = {
                        onSelectFolder()
                        navController.navigate(Destinations.Upload)
                    },
                    onBack = ::moveToUpperLevel,
                    onCreateNewFolder = ::createNewFolder
                )
            }
        }
    }
}

object Destinations {
    const val Upload = "upload"
    const val Drive = "drive"
}