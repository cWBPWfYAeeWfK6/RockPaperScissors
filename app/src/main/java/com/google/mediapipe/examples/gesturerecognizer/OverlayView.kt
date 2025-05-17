package com.google.mediapipe.examples.gesturerecognizer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import kotlin.math.max
import kotlin.math.min

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results: GestureRecognizerResult? = null
    private var linePaint = Paint()
    private var pointPaint = Paint()
    private var textPaint = TextPaint()
    private var outlinePaint = Paint()

    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1

    init {
        initPaints()
    }

    fun clear() {
        results = null
        linePaint.reset()
        pointPaint.reset()
        textPaint.reset()
        outlinePaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        linePaint.color = ContextCompat.getColor(context!!, R.color.mp_color_primary)
        linePaint.strokeWidth = LANDMARK_STROKE_WIDTH
        linePaint.style = Paint.Style.STROKE

        pointPaint.color = Color.YELLOW
        pointPaint.strokeWidth = LANDMARK_STROKE_WIDTH
        pointPaint.style = Paint.Style.FILL

        textPaint.color = Color.WHITE
        textPaint.textSize = 120f // Większy tekst
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) // Pogrubiona czcionka
        textPaint.style = Paint.Style.FILL
        textPaint.isAntiAlias = true

        outlinePaint.color = Color.BLACK
        outlinePaint.textSize = 120f
        outlinePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        outlinePaint.style = Paint.Style.STROKE
        outlinePaint.strokeWidth = 8f // Grube obramowanie
        outlinePaint.isAntiAlias = true
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        results?.let { gestureRecognizerResult ->
            // Rysowanie punktów orientacyjnych dłoni
            for (landmark in gestureRecognizerResult.landmarks()) {
                for (normalizedLandmark in landmark) {
                    canvas.drawPoint(
                        normalizedLandmark.x() * imageWidth * scaleFactor,
                        normalizedLandmark.y() * imageHeight * scaleFactor,
                        pointPaint
                    )
                }

                HandLandmarker.HAND_CONNECTIONS.forEach {
                    canvas.drawLine(
                        gestureRecognizerResult.landmarks()[0][it.start()].x() * imageWidth * scaleFactor,
                        gestureRecognizerResult.landmarks()[0][it.start()].y() * imageHeight * scaleFactor,
                        gestureRecognizerResult.landmarks()[0][it.end()].x() * imageWidth * scaleFactor,
                        gestureRecognizerResult.landmarks()[0][it.end()].y() * imageHeight * scaleFactor,
                        linePaint
                    )
                }
            }

            // Wyświetlanie rozpoznanego gestu na 3/4 wysokości, wyśrodkowanego
            gestureRecognizerResult.gestures().firstOrNull()?.firstOrNull()?.let { gesture ->
                val rawGesture = gesture.categoryName()
                val gestureName = when (rawGesture) {
                    "Closed_Fist" -> "Kamień"
                    "Thumb_Up" -> "Kamień"
                    "Open_Palm" -> "Papier"
                    "Victory" -> "Nożyce"
                    "Thumb_Down" -> "Kamień"
                    "Pointing_Up" -> "Nożyce"
                    else -> ""
                }
                Log.d("OverlayView", "Raw gesture: $rawGesture, Displayed: $gestureName")

                // Obliczanie pozycji dla wyśrodkowania
                val textWidth = textPaint.measureText(gestureName)
                val x = (width - textWidth) / 2f // Wyśrodkowanie poziome
                val y = height * 3 / 4f // 3/4 wysokości

                // Rysowanie obramowania i tekstu
                canvas.drawText(gestureName, x, y, outlinePaint)
                canvas.drawText(gestureName, x, y, textPaint)
            } ?: run {
                val gestureName = ""
                val textWidth = textPaint.measureText(gestureName)
                val x = (width - textWidth) / 2f
                val y = height * 3 / 4f
                canvas.drawText(gestureName, x, y, outlinePaint)
                canvas.drawText(gestureName, x, y, textPaint)
            }
        }
    }

    fun setResults(
        gestureRecognizerResult: GestureRecognizerResult,
        imageHeight: Int,
        imageWidth: Int,
        runningMode: RunningMode = RunningMode.IMAGE
    ) {
        results = gestureRecognizerResult
        this.imageHeight = imageHeight
        this.imageWidth = imageWidth

        scaleFactor = when (runningMode) {
            RunningMode.IMAGE, RunningMode.VIDEO -> {
                min(width * 1f / imageWidth, height * 1f / imageHeight)
            }
            RunningMode.LIVE_STREAM -> {
                max(width * 1f / imageWidth, height * 1f / imageHeight)
            }
        }
        invalidate()
    }

    companion object {
        private const val LANDMARK_STROKE_WIDTH = 8F
    }
}