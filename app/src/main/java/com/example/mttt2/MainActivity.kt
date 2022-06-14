package com.example.mttt2

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.mttt2.databinding.ActivityMainBinding
import com.example.mttt2.usecase.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.FileOutputStream

class MainActivity : AppCompatActivity(), isInsideGreenZoneCallBack, TakePictureCallback {
    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraSelector: CameraSelector
    private lateinit var previewView: PreviewView
    private var imageCaptureUseCase: ImageCapture? = null
    private var analysisUseCase: ImageAnalysis? = null
    private var previewUseCase: Preview? = null
    private lateinit var ahiImageCapture: AHImageCapture
    private lateinit var graphicOverlay: GraphicOverlay
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var viewModel: CameraXViewModel
    private var inGreenZone: Boolean = false

    companion object {
        private const val TAG = "PottiTest"
        private const val PERMISSION_REQUESTS = 1
    }

    override fun onResume() {
        super.onResume()
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), PERMISSION_REQUESTS)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUESTS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "CameraX permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "CameraX permission NIT granted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Layout inflate and bind on the MainActivity lifecycle owner.
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        graphicOverlay = binding.graphicOverlay
        previewView = binding.previewView
        // Get front camera option.
        cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()
//         Start camera progress and bind viewmodel instance.
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[CameraXViewModel::class.java]
        // Get camera provider by viewmodel observation.
        viewModel.processCameraProvider.observe(this) {
            cameraProvider = it
            bindAllCameraUseCases()
        }
        lifecycleScope.launch {
            viewModel.isOnGreenSharedFlow.collectLatest {
                if (it != inGreenZone) {
                    inGreenZone = it
                    Log.d(TAG, "onCreate: $inGreenZone")
                }
            }
        }
        binding.captureButton.setOnClickListener {
            val file: FileUtils by lazy { FileUtilsImlpl }
            file.createDirectoryIfNotExist(this)
            lifecycleScope.launch(Dispatchers.IO) {
                for (i in 1..4) {
                    delay(250)
                    takePicture()
                }
            }
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun bindAllCameraUseCases() {
        bindPreviewUseCase()
        bindImageCapture()
        bindAnalysisUseCase()

        lifecycleScope.launch {
            delay(3000)
            cameraProvider?.let {
                graphicOverlay.visibility = View.GONE
            }
            delay(5000)
            cameraProvider?.let {
                graphicOverlay.visibility = View.VISIBLE
            }
        }
    }

    private fun bindPreviewUseCase() {
        // Step 1-> check current cameraProvider got a control from lifecycle.
        if (cameraProvider == null) {
            return
        }
        // Step 2-> check current cameraProvider has been bind previewUseCase or not.
        if (previewUseCase != null) {
            // if(yes) unbind previous previewUseCase.
            cameraProvider!!.unbind(previewUseCase)
        }
        // Step 3-> overwrite current and create a new instance.
        previewUseCase = PreviewUseCase(previewView).getUseCaseInstance()
        // Step 4-> re-bind previewUseCase to cameraProvider.
        cameraProvider!!.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, previewUseCase)
    }


    private fun bindImageCapture() {
        if (imageCaptureUseCase != null) {
            cameraProvider!!.unbind(imageCaptureUseCase)
        }
        ahiImageCapture = AHImageCapture()
        imageCaptureUseCase = ahiImageCapture.getImageCapture()

        cameraProvider!!.bindToLifecycle(
            this,
            cameraSelector,
            imageCaptureUseCase
        )
    }

    lateinit var bmp: BitmapModel

    @SuppressLint("UnsafeOptInUsageError")
    private fun bindAnalysisUseCase() {
        // Step 1-> check current cameraProvider got a control from lifecycle.
        if (cameraProvider == null) {
            return
        }
        // Step 2-> check current cameraProvider has been bind analysisUseCase or not.
        if (analysisUseCase != null) {
            // If(yes) unbind previous analysisUseCase.
            cameraProvider!!.unbind(analysisUseCase)
        }
        // PoseDetectorOptions: 4 mode ->  STREAM_MODE, SINGLE_IMAGE_MODE, CPU_GPU, CPU.
        val options = PoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
            .build()
        // Step 3-> init poseDetector.
        val poseDetector = PoseDetection.getClient(options)

        analysisUseCase = AnalysisUseCase().getUseCaseInstance()
        analysisUseCase!!.setAnalyzer(
            ContextCompat.getMainExecutor(this)
        ) { imageProxy ->
            val rotationDegree = imageProxy.imageInfo.rotationDegrees
            // Yuv to Bit map
            bmp = BitmapModel(yuvToBitmap(imageProxy.image!!), rotationDegree)
            if (viewModel.takePicture.value!!) {

                // save image
                saveImage2(bmp.bitmap, rotationDegree)
                viewModel.triggerTakePicture(false)
            }

            // Get image from imageProxy stream.
            val image = imageProxy.image
            graphicOverlay.setImageSourceInfo(imageProxy.height, imageProxy.width)
            if (image != null) {
                val processImage =
                    InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)
                poseDetector.process(processImage)
                    .addOnSuccessListener {
                        // Clear previous view.
                        graphicOverlay.clear()
                        // Pass in an image to PoseGraphic, to analysis pose detection, and draw dots, lines,....etc.
                        graphicOverlay.add(
                            PoseGraphic(
                                graphicOverlay,
                                it,
                                visualizeZ = true,
                                rescaleZForVisualization = true,
                                w = imageProxy.width,
                                h = image.height,
                                context = this,
                                isInsideGreenZoneCallBack = this
                            )
                        )
                        // Every time when finished callback must need to close imageProxy, to prevent memory leak.
                        imageProxy.close()
                    }
                    .addOnFailureListener {
                        // Every time when finished callback must need to close imageProxy, to prevent memory leak.
                        imageProxy.close()
                        Log.d(TAG, it.stackTraceToString())
                    }
            }
            // This method can be invoked from outside of the UI thread only when this View is attached to a window.
            graphicOverlay.postInvalidate()
        }
        cameraProvider!!.bindToLifecycle(this, cameraSelector, analysisUseCase)
    }

    private fun saveImage2(bmp: Bitmap, rotationDegree: Int) {
        val matrix = Matrix()
        matrix.postRotate(rotationDegree.toFloat())
        matrix.postScale(-1f, 1f, bmp.width / 2f, bmp.height / 2f)
        val rotationBitmap = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
        try {
            val out = FileOutputStream(FileUtilsImlpl.createFile(this))
            rotationBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            rotationBitmap.recycle()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /* Get boolean return check is on the green zone or not */
    override fun isInsideGreenZone(isInside: Boolean) {
//        Log.d(TAG, "isInsideGreenZone: $isInside")
        viewModel.triggerGreenZoneSharedFlow(isInside)
    }

    override fun takePicture() {
        saveImage2(bmp.bitmap, bmp.rotationDegree)
    }
}

// pass value callback
interface TakePictureCallback {
    fun takePicture()
}

data class BitmapModel(
    val bitmap: Bitmap,
    val rotationDegree: Int,
)