package com.example.mttt2

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.mttt2.databinding.ActivityMainBinding
import com.example.mttt2.usecase.*
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), IsInsideGreenZoneCallBack {
    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraSelector: CameraSelector
    private lateinit var previewView: PreviewView
    private lateinit var cameraAnalyzer: CameraAnalyzer
    private var analysisUseCase: ImageAnalysis? = null
    private var previewUseCase: Preview? = null
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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        graphicOverlay = binding.graphicOverlay
        previewView = binding.previewView
        cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[CameraXViewModel::class.java]
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
                    cameraAnalyzer.captureNextPicture()
                }
            }
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun bindAllCameraUseCases() {
        bindPreviewUseCase()
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
        if (cameraProvider == null) {
            return
        }
        if (previewUseCase != null) {
            cameraProvider!!.unbind(previewUseCase)
        }
        previewUseCase = PreviewUseCase(previewView).getUseCaseInstance()
        cameraProvider!!.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, previewUseCase)
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun bindAnalysisUseCase() {
        if (cameraProvider == null) {
            return
        }
        if (analysisUseCase != null) {
            cameraProvider!!.unbind(analysisUseCase)
        }
        val options = PoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
            .build()
        val poseDetector = PoseDetection.getClient(options)
        cameraAnalyzer = CameraAnalyzer(
            graphicOverlay,
            poseDetector,
            this,
            this,
        )
        analysisUseCase = AnalysisUseCase().getUseCaseInstance()
        analysisUseCase?.setAnalyzer(ContextCompat.getMainExecutor(this), cameraAnalyzer)
        cameraProvider!!.bindToLifecycle(this, cameraSelector, analysisUseCase)
    }

    override fun isInsideGreenZone(isInside: Boolean) {
        viewModel.triggerGreenZoneSharedFlow(isInside)
    }
}

data class BitmapData(
    val bitmap: Bitmap,
    val rotationDegree: Int,
)