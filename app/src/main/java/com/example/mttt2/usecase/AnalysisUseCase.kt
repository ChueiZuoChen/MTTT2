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

class CameraAnalyzer(
    private val graphicOverlay: GraphicOverlay,
    private val poseDetector: PoseDetector,
    private val context: Context,
    private val isInsideGreenZoneCallBack: IsInsideGreenZoneCallBack,
) : ImageAnalysis.Analyzer {
    private lateinit var bmpData: BitmapData
    private var rotationDegree = 0

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        imageProxy.let { imageProxy ->
            val image = imageProxy.image
            rotationDegree = imageProxy.imageInfo.rotationDegrees
            bmpData = BitmapData(yuvToBitmap(image!!), rotationDegree)
            graphicOverlay.setImageSourceInfo(imageProxy.height, imageProxy.width)
            if (imageProxy.image != null) {
                val processImage = InputImage.fromMediaImage(image, rotationDegree)
                poseDetector.process(processImage)
                    .addOnSuccessListener { pose ->
                        graphicOverlay.clear()
                        graphicOverlay.add(
                            PoseGraphic(
                                overlay = graphicOverlay,
                                pose = pose,
                                visualizeZ = true,
                                rescaleZForVisualization = true,
                                w = imageProxy.width,
                                h = imageProxy.height,
                                context = context,
                                isInsideGreenZoneCallBack = isInsideGreenZoneCallBack
                            )
                        )
                        imageProxy.close()
                    }
                    .addOnFailureListener {
                        imageProxy.close()
                    }
            }
            graphicOverlay.postInvalidate()
        }
    }

    fun captureNextPicture() {
        val matrix = Matrix()
        val (bitmap,rotationDegree)= bmpData
        matrix.postRotate(rotationDegree.toFloat())
        matrix.postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
        val rotationBitmap = Bitmap.createBitmap(bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true)
        try {
            val out = FileOutputStream(FileUtilsImlpl.createFile(context))
            rotationBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            rotationBitmap.recycle()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}