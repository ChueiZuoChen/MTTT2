package com.example.mttt2.usecase

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.util.Size
import android.view.Surface.ROTATION_0
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import java.io.File
import java.util.concurrent.Executors

class PreviewUseCase(val previewView: PreviewView) : ICameraUseCase {
    private var previewUseCase: Preview

    init {
        previewUseCase = Preview.Builder()
            .setTargetResolution(Size(720, 1280))
            .setTargetRotation(ROTATION_0)
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
    }

    override fun getUseCaseName(): String {
        return "PerviewUseCase"
    }

    override fun <T> getUseCaseInstance(): T {
        return previewUseCase as T
    }
}



interface ICameraUseCase {
    fun getUseCaseName(): String
    fun <T> getUseCaseInstance(): T
}






private const val TAG = "PreviewUseCase"