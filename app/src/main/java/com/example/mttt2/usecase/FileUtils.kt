package com.example.mttt2.usecase

import android.content.Context
import android.os.Environment
import java.io.File

interface FileUtils {
    fun createDirectoryIfNotExist(context: Context)
    fun createFile(context: Context): File
}

object FileUtilsImlpl : FileUtils {
    const val IMAGE_PREFIX = "Image_"
    const val JPG_SUFFIX = ".jpg"
    const val FOLDER_NAME = "Photo"

    override fun createDirectoryIfNotExist(context: Context) {
        val folder = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.absolutePath + File.separator + FOLDER_NAME
        )
        if (!folder.exists()) {
            folder.mkdirs()
        }
    }

    override fun createFile(context: Context) = File(
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.absolutePath + File.separator + IMAGE_PREFIX + System.currentTimeMillis() + JPG_SUFFIX
    )

}