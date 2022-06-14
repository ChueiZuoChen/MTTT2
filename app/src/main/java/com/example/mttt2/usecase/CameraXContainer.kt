package com.example.mttt2.usecase

import android.util.Size
import android.view.View
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.mttt2.GraphicOverlay
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BodyScanCamera private constructor(
    val resolution: Size?,
    val cameraSelector: CameraSelector?,
    val lifecycleOwner: LifecycleOwner?,
    val cameraProvider: ProcessCameraProvider?,
    val graphicOverlay: GraphicOverlay?,
    val analysisUseCase: ImageAnalysis?,
    val previewUseCase: Preview?,
) {
    data class Builder(
        var resolution: Size? = null,
        var cameraSelector: CameraSelector? = null,
        var lifecycleOwner: LifecycleOwner? = null,
        var cameraProvider: ProcessCameraProvider? = null,
        var graphicOverlay: GraphicOverlay? = null,
        var analysisUseCase: ImageAnalysis? = null,
        var previewUseCase: Preview? = null,
    ) {
        fun setResolution(resolution: Size) = apply { this.resolution = resolution }
        fun setCameraSelector(cameraSelector: CameraSelector) =
            apply { this.cameraSelector = cameraSelector }

        fun setLifecycleOwner(lifecycleOwner: LifecycleOwner) =
            apply { this.lifecycleOwner = lifecycleOwner }

        fun setAnalysisUseCase(analysisUseCase: ImageAnalysis) =
            apply { this.analysisUseCase = analysisUseCase }

        fun setPreview(previewUseCase: Preview) = apply { this.previewUseCase = previewUseCase }
        fun setCameraProvider(cameraProvider: ProcessCameraProvider?) = apply { this.cameraProvider = cameraProvider }
        fun setGraphicOverlay(graphicOverlay: GraphicOverlay) =
            apply { this.graphicOverlay = graphicOverlay }

        fun build() = BodyScanCamera(
            resolution,
            cameraSelector,
            lifecycleOwner,
            cameraProvider,
            graphicOverlay,
            analysisUseCase,
            previewUseCase)
    }

    fun bindAllCameraUseCases() {
        this.cameraSelector?.let {
            this.lifecycleOwner?.let { lifecycleOwner ->
                cameraProvider?.bindToLifecycle(lifecycleOwner,
                    it, previewUseCase,analysisUseCase)
            }
        }
    }
}