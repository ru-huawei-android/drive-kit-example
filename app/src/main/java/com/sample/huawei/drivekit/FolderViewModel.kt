package com.sample.huawei.drivekit

import android.annotation.SuppressLint
import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

@SuppressLint("MutableCollectionMutableState")
class FolderViewModel(private val app: Application): AndroidViewModel(app) {

    var currentFolder: File? by mutableStateOf(File().apply { id = "root"})
    var children = mutableStateListOf<File?>()
    private lateinit var fileUri: Uri
    private lateinit var file: java.io.File
    private var drive: Drive? = null
    private val context get() = app.applicationContext

    init {
        viewModelScope.launch {
            withContext(Main) {
                drive = Drive.Builder(
                    CredentialManager.credential,
                    context
                ).build()
            }
        }
    }

    fun getChildren() {
        viewModelScope.launch {
            withContext(IO) {
                children.clear()
                val request = drive?.files()?.list()
                var cursor: String?
                val directoryId = currentFolder?.let { "'${it.id}'" } ?: "'root'"
                val query =
                    "$directoryId in parentFolder and mimeType='application/vnd.huawei-apps.folder'"
                do {
                    val result = request
                        ?.setQueryParam(query)
                        ?.setOrderBy("fileName")
                        ?.execute()
                    result?.files?.forEach {
                        children.add(it)
                    }
                    cursor = result?.nextCursor
                    request?.cursor = cursor
                } while (!cursor.isNullOrEmpty())
            }
        }
    }

    fun openFolder(index: Int) {
        val folderId = listOf(currentFolder?.id)
        currentFolder = children[index]
        currentFolder?.parentFolder = folderId
        getChildren()
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

    fun onFilePicked(uri: Uri) {
        fileUri = uri
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
                    children.clear()
                    val directoryId = currentFolder?.parentFolder?.get(0)
                    currentFolder = directoryId?.let {
                        val request = drive?.files()?.get(directoryId)
                        request?.fields = "*"
                        request?.execute()
                    }
                    displayToast("current folder id: ${currentFolder?.id}")
                    getChildren()
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
                    getChildren()
                    displayToast("folder \"$name\" successfully created")
                } catch (e: Exception) {
                    displayToast("couldn't create folder: $e")
                }
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
                    request?.execute()
                    displayToast("File successfully uploaded")
                } catch (e: Exception) {
                    displayToast("upload file exception: $e")
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

    companion object {
        const val DIRECT_UPLOAD_MAX_SIZE = 20000000
    }
}