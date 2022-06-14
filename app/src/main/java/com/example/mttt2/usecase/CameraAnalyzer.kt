package com.example.mttt2.usecase

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.mttt2.BitmapData
import com.example.mttt2.GraphicOverlay
import com.example.mttt2.IsInsideGreenZoneCallBack
import com.example.mttt2.PoseGraphic
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetector
import java.io.FileOutputStream

class CameraAnalyzer(
    private val graphicOverlay: GraphicOverlay?,
    private val poseDetector: PoseDetector?,
    private val context: Context?,
    private val isInsideGreenZoneCallBack: IsInsideGreenZoneCallBack?,
) : ImageAnalysis.Analyzer {

    data class Builder(
        var graphicOverlay: GraphicOverlay? = null,
        var poseDetector: PoseDetector? = null,
        var context: Context? = null,
        var isInsideGreenZoneCallBack: IsInsideGreenZoneCallBack? = null,
    ) {
        fun setGraphicOverlay(graphicOverlay: GraphicOverlay) =
            apply { this.graphicOverlay = graphicOverlay }

        fun setGreenZoneCallBackListener(isInsideGreenZoneCallBack: IsInsideGreenZoneCallBack) =
            apply {
                this.isInsideGreenZoneCallBack = isInsideGreenZoneCallBack
            }

        fun setPoseDetector(poseDetector: PoseDetector) = apply { this.poseDetector = poseDetector }
        fun setContext(context: Context) = apply { this.context = context }
        fun build():CameraAnalyzer{
            return CameraAnalyzer(graphicOverlay, poseDetector, context, isInsideGreenZoneCallBack)
        }
    }

    private lateinit var bmpData: BitmapData
    private var rotationDegree = 0

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        imageProxy.let { imageProxy ->
            val image = imageProxy.image
            rotationDegree = imageProxy.imageInfo.rotationDegrees
            bmpData = BitmapData(yuvToBitmap(image!!), rotationDegree)
            graphicOverlay?.setImageSourceInfo(imageProxy.height, imageProxy.width)
            if (imageProxy.image != null) {
                val processImage = InputImage.fromMediaImage(image, rotationDegree)
                poseDetector?.process(processImage)
                    ?.addOnSuccessListener { pose ->
                        graphicOverlay?.clear()
                        context?.let {
                            graphicOverlay?.let { graphicOverlay ->
                                isInsideGreenZoneCallBack?.let { callback ->
                                    PoseGraphic(
                                        overlay = graphicOverlay,
                                        pose = pose,
                                        visualizeZ = true,
                                        rescaleZForVisualization = true,
                                        w = imageProxy.width,
                                        h = imageProxy.height,
                                        context = it,
                                        isInsideGreenZoneCallBack = callback
                                    )
                                }
                            }
                        }?.let {
                            graphicOverlay?.add(it)
                        }
                        imageProxy.close()
                    }?.addOnFailureListener {
                        imageProxy.close()
                    }
            }
            graphicOverlay?.postInvalidate()
        }
    }

    fun captureNextPicture() {
        val matrix = Matrix()
        val (bitmap, rotationDegree) = bmpData
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
            val out = FileOutputStream(FileUtilsImlpl.createFile(context!!))
            rotationBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            rotationBitmap.recycle()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}