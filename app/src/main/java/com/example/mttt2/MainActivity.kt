package com.example.mttt2

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.mttt2.databinding.ActivityMainBinding
import com.example.mttt2.usecase.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), isInsideGreenZoneCallBack {
    lateinit var binding: ActivityMainBinding
    lateinit var cameraSelector: CameraSelector
    lateinit var previewView: PreviewView
    var imageCaptureUseCase: ImageCapture? = null
    var analysisUseCase: ImageAnalysis? = null
    var previewUseCase: Preview? = null
    lateinit var ahiImageCapture: AHImageCapture
    private lateinit var graphicOverlay: GraphicOverlay
    var cameraProvider: ProcessCameraProvider? = null
    lateinit var constraintLayout: ConstraintLayout
    lateinit var viewModel: CameraXViewModel
    var inGreenZone: Boolean = false

    companion object {
        private const val TAG = "PottiTest"
        private const val PERMISSION_REQUESTS = 1

        // Set of required runtime permissions.
        private val REQUIRED_RUNTIME_PERMISSIONS =
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
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
        constraintLayout = binding.constraintLayout
        graphicOverlay = binding.graphicOverlay
        previewView = binding.previewView

        // Get front camera option.
        cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()
//         Start camera progress and bind viewmodel instance.
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )
            .get(CameraXViewModel::class.java)
        // Get camera provider by viewmodel observation.
        viewModel.processCameraProvider.observe(this) {
            cameraProvider = it
            bindAllCameraUseCases()
        }
        lifecycleScope.launch {
            viewModel.isOnGreensharedFlow.collectLatest {
                if (it != inGreenZone) {
                    inGreenZone = it
                    Log.d(TAG, "onCreate: $inGreenZone")
                }
            }
        }
//        binding.captureButton.setOnClickListener {
//            val file: FileUtils by lazy { FileUtilsImlpl }
//            file.createDirectoryIfNotExist(this)
//            ahiImageCapture.takePicture(file.createFile(this))
//        }
    }

    private fun bindAllCameraUseCases() {
        bindPreviewUseCase()
        bindAnalysisUseCase()
//        bindImageCapture()
    }

    private fun bindImageCapture() {
        if (cameraProvider == null) {
            return
        }
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
        previewUseCase = PreviewUseCase(previewView).getPreviewUseCase()
        // Step 4-> re-bind previewUseCase to cameraProvider.
        cameraProvider!!.bindToLifecycle(this, cameraSelector, previewUseCase)
    }

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


        analysisUseCase = AnalysisUseCase().getAnalysisUseCase()
        analysisUseCase?.setAnalyzer(
            ContextCompat.getMainExecutor(this)
        ) { imageProxy ->
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
        // Bind to lifecycleOwner.
        cameraProvider!!.bindToLifecycle(this, cameraSelector, analysisUseCase)
    }

    /* Get boolean return check is on the green zone or not */
    override fun isInsideGreenZone(isInside: Boolean) {
//        Log.d(TAG, "isInsideGreenZone: $isInside")
        viewModel.triggerGreenZoneSharedFlow(isInside)
    }
}