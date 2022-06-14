package com.example.mttt2.usecase

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.mttt2.BitmapData
import com.example.mttt2.GraphicOverlay
import com.example.mttt2.IsInsideGreenZoneCallBack
import com.example.mttt2.PoseGraphic
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetector
import java.io.FileOutputStream

class AnalysisUseCase : ICameraUseCase {
    companion object {
        private var analysisUseCase: ImageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setImageQueueDepth(4)
            .setTargetResolution(Size(720, 1280))
            .build()
    }

    override fun getUseCaseName(): String {
        return "AnalysisUseCase"
    }

    override fun <T> getUseCaseInstance(): T {
        return analysisUseCase as T
    }

}

