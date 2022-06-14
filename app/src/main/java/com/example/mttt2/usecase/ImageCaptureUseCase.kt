package com.example.mttt2.usecase

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.util.Size
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import java.io.File
import java.util.concurrent.Executors

private const val TAG = "ImageCaptureUseCase"

interface IImageCapture {
    fun takePicture(context: Context, fileName: File)
    fun getImageCapture(): ImageCapture
}

class AHImageCapture : IImageCapture {
    private var imageCaptureUseCase: ImageCapture = ImageCapture.Builder()
        .setTargetResolution(Size(720, 1280))
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .build()

    override fun takePicture(context: Context, fileName: File) {
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(fileName)
            .build()
        imageCaptureUseCase.takePicture(
            outputFileOptions,
            Executors.newSingleThreadExecutor(),
            object :
                ImageCapture.OnImageSavedCallback {
                @SuppressLint("RestrictedApi")
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Log.d(TAG, "onImageSaved: ${outputFileResults.savedUri?.path}")
                    val inputStream =
                        context.contentResolver.openInputStream(outputFileResults.savedUri!!)!!
                    // TODO: image saved Uri(path)
                }

                override fun onError(exception: ImageCaptureException) {
                    // TODO: Error handle
                }
            }
        )
    }

    override fun getImageCapture(): ImageCapture {
        return imageCaptureUseCase
    }
}