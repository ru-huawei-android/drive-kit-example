package com.sample.huawei.drivekit

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.huawei.cloud.services.drive.Drive
import com.huawei.cloud.services.drive.model.File
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import java.lang.Exception
import kotlin.math.max
import com.huawei.cloud.base.http.FileContent
import com.sample.huawei.drivekit.DriveActivity.Companion.TAG

import com.huawei.cloud.base.media.MediaHttpDownloaderProgressListener
import com.huawei.cloud.base.media.MediaHttpUploaderProgressListener
import java.io.FileOutputStream
import kotlin.coroutines.resume

@SuppressLint("MutableCollectionMutableState")
class DriveViewModel(private val app: Application): AndroidViewModel(app) {

    var signedIn: Boolean by mutableStateOf(false)
    var currentFolder: File? by mutableStateOf(File().apply { id = "root"})
    var childrenFolders = mutableStateListOf<File?>()
    var childrenFiles = mutableStateListOf<File?>()
    var progress by mutableStateOf<Float?>(null)
    lateinit var displayMode: DisplayMode

    private lateinit var fileUri: Uri
    private lateinit var file: java.io.File
    private var drive: Drive? = null
    private val context get() = app.applicationContext

    fun onSignInResult(data: Intent) {
        signedIn = CredentialManager.getSignInResult(data)
        if(signedIn) {
            displayToast( "Successfully authorized")
            drive = Drive.Builder(
                CredentialManager.credential,
                context
            ).build()
        } else {
            displayToast("Authorization failure. Please try again")
        }
    }

    fun moveToRootFolder() {
        currentFolder?.id = "root"
        update()
    }

    fun onActionDone() {
        progress = null
        update()
    }

    private fun update() {
        clear()
        fetch(RequestMode.Folders)
        fetch(RequestMode.Files)
    }

    private fun fetch(mode: RequestMode) {
        viewModelScope.launch {
            withContext(IO) {
                val request = drive?.files()?.list()
                var cursor: String?
                val fileId = currentFolder?.let { "'${it.id}'" } ?: "'root'"
                val query = when(mode) {
                    RequestMode.Folders ->
                        "$fileId in parentFolder and mimeType='application/vnd.huawei-apps.folder'"
                    RequestMode.Files ->
                        "$fileId in parentFolder and mimeType!='application/vnd.huawei-apps.folder'"
                }
                do {
                    val result = request
                        ?.setQueryParam(query)
                        ?.setOrderBy("fileName")
                        ?.execute()
                    when(mode) {
                        RequestMode.Folders -> result?.files?.forEach {
                            childrenFolders.add(it)
                        }
                        RequestMode.Files -> result?.files?.forEach {
                            childrenFiles.add(it)
                        }
                    }
                    cursor = result?.nextCursor
                    request?.cursor = cursor
                } while (!cursor.isNullOrEmpty())
            }
        }
    }

    fun openFolder(index: Int) {
        val folderId = listOf(currentFolder?.id)
        currentFolder = childrenFolders[index]
        currentFolder?.parentFolder = folderId
        update()
    }

    fun onPickDriveFile(index: Int) =
        childrenFiles[index]?.let { downloadFile(it) }

    private fun downloadFile(file: File) {
        viewModelScope.launch {
            withContext(IO) {
                try {
                    if(file.id == null) {
                        displayToast("executeFilesGet error, need to create file.")
                        return@withContext
                    }
                    val path = context.externalCacheDir?.path
                    Log.d(TAG, "Storage path: $path")
                    // Obtain file metadata.
                    val request = drive?.files()?.get(file.id)
                    request?.fields = "id,size"
                    val res = request?.execute()
                    // Download a file.
                    val size = res?.getSize() ?: 0
                    val fileRequest = drive?.files()?.get(file.id)
                    fileRequest?.form = "content"
                    val downloader = fileRequest?.mediaHttpDownloader
                    downloader?.setContentRange(0, size - 1)?.isDirectDownloadEnabled =
                        size < DIRECT_DOWNLOAD_MAX_SIZE
                    downloader?.progressListener =
                        MediaHttpDownloaderProgressListener { mediaHttpDownloader ->
                            Log.d(TAG, "download progress: ${mediaHttpDownloader.progress}")
                            progress = mediaHttpDownloader.progress.toFloat()
                        }
                    fileRequest?.let {
                        saveFileToDownloadsFolder(
                            context = context,
                            fileRequest = it,
                            fileName = file.fileName,
                            mimeType = file.mimeType
                        )
                        displayToast("File saved to downloads folder")
                    }
                } catch (e: Exception) {
                    displayToast("executeFilesGet exception: $e")
                }
                update()
            }
        }
    }

    private fun uploadFile(file: java.io.File) {
        viewModelScope.launch {
            withContext(IO) {
                try {
                    val fileContent = FileContent(null, file)
                    val content = File().apply {
                        fileName = file.name
                        parentFolder = listOf(currentFolder?.id)
                        mimeType = getMimeType(file)
                    }
                    val request = drive?.files()?.create(content, fileContent)
                    request?.mediaHttpUploader
                        ?.isDirectUploadEnabled = file.length() < DIRECT_UPLOAD_MAX_SIZE
                    request?.mediaHttpUploader?.progressListener =
                        MediaHttpUploaderProgressListener {
                            Log.d(TAG, "upload progress: ${it.progress}")
                            progress = it.progress.toFloat()
                        }
                    request?.execute()
                    displayToast("File successfully uploaded")
                } catch (e: Exception) {
                    displayToast("upload file exception: $e")
                }
                update()
            }
        }
    }

    private fun saveFileToDownloadsFolder(
        context: Context,
        fileRequest: Drive.Files.Get,
        fileName: String,
        mimeType: String
    ) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val resolver = context.contentResolver

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                fileRequest.executeContentAndDownloadTo(
                    resolver.openOutputStream(uri)
                )
            }
        }
        else {
            val target = java.io.File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                fileName
            )
            fileRequest.executeContentAndDownloadTo(FileOutputStream(target))
        }
    }

    fun onSelectFolder() {
        app.applicationContext
            .contentResolver
            .openInputStream(fileUri)?.use {
                file = java.io.File(
                    context.externalCacheDir,
                    getFileName(fileUri)?: "temp"
                )
                file.writeBytes(it.readBytes())
            }
        uploadFile(file)
    }

    private fun getMimeType(file: java.io.File): String {
        val extension = file.name.substringAfterLast('.', "")
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "image/jpeg"
    }

    private fun clear() {
        childrenFolders.clear()
        childrenFiles.clear()
    }

    fun onUploadFilePicked(uri: Uri) {
        fileUri = uri
        update()
    }

    private fun getFileName(uri: Uri) = context.contentResolver.query(
        uri, null, null, null, null, null
    )?.use {
        var displayName = ""
        if (it.moveToFirst()) {
            displayName = it.getString(
                max(it.getColumnIndex(OpenableColumns.DISPLAY_NAME), 0)
            )
        }
        displayName
    }

    fun moveToUpperLevel() {
        viewModelScope.launch {
            withContext(IO) {
                if(currentFolder?.parentFolder?.isNotEmpty() == true) {
                    val directoryId = currentFolder?.parentFolder?.get(0)
                    currentFolder = directoryId?.let {
                        val request = drive?.files()?.get(directoryId)
                        request?.fields = "*"
                        request?.execute()
                    }
                    update()
                }
            }
        }
    }

    fun createNewFolder(name: String) {
        viewModelScope.launch {
            withContext(IO) {
                try {
                    val appProperties = hashMapOf("appProperties" to "property")
                    val dir = File().apply {
                        fileName = name
                        appSettings = appProperties
                        parentFolder = currentFolder?.let { listOf(it.id) }
                        mimeType = "application/vnd.huawei-apps.folder"
                    }
                    drive?.files()?.create(dir)?.execute()
                    update()
                    displayToast("folder \"$name\" successfully created")
                } catch (e: Exception) {
                    displayToast("couldn't create folder: $e")
                }
            }
        }
    }

    private fun displayToast(message: String) {
        Log.d(TAG, message)
        viewModelScope.launch {
            withContext(Main) {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    enum class RequestMode {
        Folders,
        Files
    }
    companion object {
        const val DIRECT_UPLOAD_MAX_SIZE = 5000000
        const val DIRECT_DOWNLOAD_MAX_SIZE = 20000000
    }
}