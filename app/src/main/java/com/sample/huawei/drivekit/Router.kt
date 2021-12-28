package com.sample.huawei.drivekit

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sample.huawei.drivekit.ui.DriveScreen
import com.sample.huawei.drivekit.ui.MainScreen

@Composable
fun Router(viewModel: DriveViewModel) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Destinations.Main
    ) {
        with(viewModel) {
            composable(Destinations.Main) {
                MainScreen(
                    onFileSelected = {
                        onUploadFilePicked(it)
                        displayMode = DisplayMode.Upload
                        navController.navigate(Destinations.Drive)
                    },
                    onDownloadClick = {
                        displayMode = DisplayMode.Download
                        getChildren()
                        navController.navigate(Destinations.Drive)
                    }
                )
            }
            composable(Destinations.Drive) {
                DriveScreen(
                    mode = displayMode,
                    name = currentFolder?.fileName ?: "My Drive",
                    folders = childrenFolders.map { it?.fileName ?: "" },
                    onFolderClick = ::openFolder,
                    files = childrenFiles.map { it?.fileName ?: "" },
                    onFileClick = {
                        onPickDriveFile(it)
                    },
                    onSubmitFolder = {
                        onSelectFolder()
                        navController.navigate(Destinations.Main)
                    },
                    onBack = ::moveToUpperLevel,
                    onCreateNewFolder = ::createNewFolder
                )
            }
        }
    }
}

object Destinations {
    const val Main = "main"
    const val Drive = "drive"
}

enum class DisplayMode {
    Upload,
    Download
}
