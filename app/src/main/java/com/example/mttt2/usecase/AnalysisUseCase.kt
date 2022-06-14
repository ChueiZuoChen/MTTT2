package com.example.mttt2.usecase

import android.util.Size
import androidx.camera.core.ImageAnalysis

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