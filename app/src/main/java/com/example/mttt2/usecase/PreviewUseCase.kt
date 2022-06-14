package com.example.mttt2.usecase

import android.util.Size
import android.view.Surface.ROTATION_0
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView

class PreviewUseCase(val previewView: PreviewView) : ICameraUseCase {
    private var previewUseCase: Preview = Preview.Builder()
        .setTargetResolution(Size(720, 1280))
        .setTargetRotation(ROTATION_0)
        .build()
        .also {
            it.setSurfaceProvider(previewView.surfaceProvider)
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