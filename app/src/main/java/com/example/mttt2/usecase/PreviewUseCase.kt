package com.example.mttt2.usecase

import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import java.io.File
import java.util.concurrent.Executors

interface IPreviewUseCase {
    fun getPreviewUseCase(): Preview
}

class PreviewUseCase(previewView: PreviewView) : IPreviewUseCase {
    private var previewUseCase: Preview

    init {
        previewUseCase = Preview.Builder()
            .setTargetResolution(Size(720, 1280))
            .setTargetRotation(Surface.ROTATION_90)
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
    }

    override fun getPreviewUseCase(): Preview {
        return previewUseCase
    }
}


interface IAnalysisUseCase {
    fun getAnalysisUseCase(): ImageAnalysis
}

class AnalysisUseCase : IAnalysisUseCase {
    private var analysisUseCase: ImageAnalysis

    init {
        analysisUseCase = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetResolution(Size(720, 1280))
            .build()
    }

    override fun getAnalysisUseCase(): ImageAnalysis {
        return analysisUseCase
    }
}


interface IImageCapture {
    fun takePicture(fileName: File)
    fun getImageCapture(): ImageCapture
}

class AHImageCapture : IImageCapture {
    private var imageCaptureUseCase: ImageCapture

    init {
        imageCaptureUseCase = ImageCapture.Builder()
            .setTargetResolution(Size(720, 1280))
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }


    override fun takePicture(fileName: File) {
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(fileName).build()
        imageCaptureUseCase.takePicture(
            outputFileOptions,
            Executors.newSingleThreadExecutor(),
            object :
                ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Log.d(TAG, "onImageSaved: ${outputFileResults.savedUri?.path}")
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.d(TAG, "onError: ${exception.message}")
                }
            }
        )
    }

    override fun getImageCapture(): ImageCapture {
        return imageCaptureUseCase
    }
}

private const val TAG = "PreviewUseCase"