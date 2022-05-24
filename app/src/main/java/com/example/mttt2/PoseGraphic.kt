package com.example.mttt2

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import com.google.common.primitives.Ints
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import java.lang.Math.max
import java.lang.Math.min

/** Draw the detected pose in preview.  */
class PoseGraphic internal constructor(
    overlay: GraphicOverlay,
    private val pose: Pose,
    private val showInFrameLikelihood: Boolean,
    private val visualizeZ: Boolean,
    private val rescaleZForVisualization: Boolean,
    private val w: Int,
    private val h: Int,
    private val context: Context
) : GraphicOverlay.Graphic(overlay) {
    private var zMin = java.lang.Float.MAX_VALUE
    private var zMax = java.lang.Float.MIN_VALUE
    private val leftPaint: Paint
    private val rightPaint: Paint
    private val whitePaint: Paint
    private val topLimit: Float
    private val bottomLimit: Float
    private var greenZone: Paint = Paint()

    companion object {
        // Draw point dot's radius.
        private const val DOT_RADIUS = 38.0f
        private const val STROKE_WIDTH = 10.0f
    }

    init {
        // init color.
        topLimit = (w / 7).toFloat()
        bottomLimit = ((w / 7) * 6).toFloat()
        whitePaint = Paint()
        leftPaint = Paint()
        leftPaint.strokeWidth = STROKE_WIDTH
        leftPaint.color = Color.GREEN
        rightPaint = Paint()
        rightPaint.strokeWidth = STROKE_WIDTH
        rightPaint.color = Color.YELLOW
    }

    // Canvas view drawing.
    override fun draw(canvas: Canvas?) {
        // This array can we can pick up that which point we want to draw.
        val exclusive =
            intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 16, 17, 18, 19, 20, 29, 30, 31, 32)
        // Mapping pose landmarks with exclusive point.
        val landmarks = pose.allPoseLandmarks.filter {
            !exclusive.contains(it.landmarkType)
        }
        // Draw all the filtered landmark points.
        for (landmark in landmarks) {
            drawPoint(canvas!!, landmark, whitePaint)
            if (visualizeZ && rescaleZForVisualization) {
                zMin = min(zMin, landmark.position3D.z)
                zMax = max(zMax, landmark.position3D.z)
            }
        }
        // Green zone default color -> Green and alpha 90.
        greenZone.setARGB(90, 0, 255, 0)

        if (!landmarks.none {
                it.position.y < topLimit
            }) {
            Log.d("HIT", "hit top")
//            Toast.makeText(context,"hit top",Toast.LENGTH_SHORT).show()
            greenZone.setARGB(90, 255, 0, 0)
        }
        if (!landmarks.none {
                it.position.y > bottomLimit
            }) {
            Log.d("HIT", "hit bottom")
//            Toast.makeText(context,"hit bottom",Toast.LENGTH_SHORT).show()
            greenZone.setARGB(90, 255, 0, 0)
        }
        drawGreenZone(canvas!!, greenZone)
    }

    private fun drawGreenZone(canvas: Canvas, greenZone: Paint) {
        // Draw top green zone area.
        canvas.drawRect(
            translateX(0f),
            translateY(0f),
            translateX(h.toFloat()),
            translateY(topLimit),
            greenZone // Green zone color.
        )
        // Draw bottom green zone area.
        canvas.drawRect(
            translateX(0f),
            translateY(bottomLimit),
            translateX(h.toFloat()),
            translateY(w.toFloat()),
            greenZone // Green zone color
        )
    }

    // draw 3D point
    private fun drawPoint(canvas: Canvas, landmark: PoseLandmark, paint: Paint) {
        val point = landmark.position3D
        maybeUpdatePaintColor(paint, canvas, point.z)
        canvas.drawCircle(translateX(point.x), translateY(point.y), DOT_RADIUS, paint)
    }

//    private fun drawLine(
//        canvas: Canvas?,
//        startLandmark: PoseLandmark?,
//        endLandmark: PoseLandmark?,
//        paint: Paint
//    ) {
//        val start = startLandmark!!.position3D
//        val end = endLandmark!!.position3D
//
//        val avgZInImagePixel = (start.z + end.z) / 2
//        maybeUpdatePaintColor(paint, canvas!!, avgZInImagePixel)
//
//        canvas.drawLine(
//            translateX(start.x),
//            translateY(start.y),
//            translateX(end.x),
//            translateY(end.y),
//            paint
//        )
//    }

    // to draw and split colors on each point of Z-axis
    private fun maybeUpdatePaintColor(
        paint: Paint,
        canvas: Canvas,
        zInImagePixel: Float
    ) {
        if (!visualizeZ) {
            return
        }

        val zLowerBoundInScreenPixel: Float
        val zUpperBoundInScreenPixel: Float

        if (rescaleZForVisualization) {
            zLowerBoundInScreenPixel = min(-0.001f, scale(zMin))
            zUpperBoundInScreenPixel = max(0.001f, scale(zMax))
        } else {
            val defaultRangeFactor = 1f
            zLowerBoundInScreenPixel = -defaultRangeFactor * canvas.width
            zUpperBoundInScreenPixel = defaultRangeFactor * canvas.width
        }

        val zInScreenPixel = scale(zInImagePixel)

        if (zInScreenPixel < 0) {
            var v = (zInScreenPixel / zLowerBoundInScreenPixel * 255).toInt()
            v = Ints.constrainToRange(v, 0, 255)
            paint.setARGB(255, 255, 255 - v, 255 - v)
        } else {
            var v = (zInScreenPixel / zUpperBoundInScreenPixel * 255).toInt()
            v = Ints.constrainToRange(v, 0, 255)
            paint.setARGB(255, 255 - v, 255 - v, 255)
        }
    }
}
