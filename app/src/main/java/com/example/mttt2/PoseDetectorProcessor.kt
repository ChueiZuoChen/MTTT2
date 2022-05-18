package com.example.mttt2

import android.content.Context
import com.google.mlkit.vision.pose.PoseDetector
import java.util.concurrent.Executor

class PoseDetectorProcessor(
    val context: Context,
    val poseDetector: PoseDetector
) {
    lateinit var executorExecutor:Executor
}