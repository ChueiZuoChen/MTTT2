package com.example.mttt2.usecase

import android.content.Context
import android.graphics.*
import android.media.Image
import android.os.Environment
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File

private const val TAG = "FileUtils"
interface FileUtils {
    fun createDirectoryIfNotExist(context: Context)
    fun createFile(context: Context): File
}

object FileUtilsImlpl : FileUtils {
    const val JPG_SUFFIX = ".png"

    override fun createDirectoryIfNotExist(context: Context) {
        val folder = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.absolutePath + File.separator + "tester/"
        )
        if (!folder.exists()) {
            folder.mkdirs()
            Log.d(TAG, "paht: ${folder.absolutePath}")
        }
    }

    override fun createFile(context: Context) = File(
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.absolutePath + File.separator + "tester/" + System.currentTimeMillis() + JPG_SUFFIX
    )
}



fun yuvToBitmap(image: Image): Bitmap {
    val yBuffer = image.planes[0].buffer
    val vuBuffer = image.planes[2].buffer

    val ySize: Int = yBuffer.capacity()
    val vuSize: Int = vuBuffer.capacity()

    val nv21 = ByteArray(ySize + vuSize)

    yBuffer.get(nv21, 0, ySize)
    vuBuffer.get(nv21, ySize, vuSize)

    val baOutputStream = ByteArrayOutputStream()
    val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
    yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 100, baOutputStream)
    val byteForBitmap = baOutputStream.toByteArray()
    val bitmap = BitmapFactory.decodeByteArray(byteForBitmap, 0, byteForBitmap.size)
    return bitmap
}