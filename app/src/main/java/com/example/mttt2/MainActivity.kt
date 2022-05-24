package com.example.mttt2

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.mttt2.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    lateinit var cameraSelector: CameraSelector
    lateinit var previewView: PreviewView
    var analysisUseCase: ImageAnalysis? = null
    var previewUseCase: Preview? = null
    lateinit var graphicOverlay: GraphicOverlay
    var cameraProvider: ProcessCameraProvider? = null
    lateinit var constraintLayout: ConstraintLayout
    lateinit var viewModel: CameraXViewModel

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

        // Checking runtime permission granted.
        if (!allRuntimePermissionsGranted()) {
            getRuntimePermissions()
        } else {
            // Start camera progress and bind viewmodel instance.
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
        }
    }

    private fun bindAllCameraUseCases() {
        bindPreviewUseCase()
        bindAnalysisUseCase()
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
        previewUseCase = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
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
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
        // Step 3-> init poseDetector.
        val poseDetector = PoseDetection.getClient(options)
        analysisUseCase =
            ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
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
                                true,
                                true,
                                true,
                                imageProxy.width,
                                image.height,
                                this
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

    /* Check all the permission are granted. */
    private fun allRuntimePermissionsGranted(): Boolean {
        for (permission in REQUIRED_RUNTIME_PERMISSIONS) {
            permission.let {
                if (!isPermissionGranted(this, it)) {
                    return false
                }
            }
        }
        return true
    }

    /* Permission request. */
    private fun getRuntimePermissions() {
        val permissionsToRequest = ArrayList<String>()
        for (permission in REQUIRED_RUNTIME_PERMISSIONS) {
            permission.let {
                if (!isPermissionGranted(this, it)) {
                    permissionsToRequest.add(permission)
                }
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUESTS
            )
        }
    }

    /* Sub-checking program to mapping activity permissions. */
    private fun isPermissionGranted(context: Context, permission: String): Boolean {
        if (ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.i(TAG, "Permission granted: $permission")
            return true
        }
        Log.i(TAG, "Permission NOT granted: $permission")
        return false
    }
}