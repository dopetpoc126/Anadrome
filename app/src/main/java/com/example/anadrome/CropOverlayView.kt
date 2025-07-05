package com.example.anadrome

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Style // Add this import
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.util.Log

/**
 * A custom View that draws a translucent overlay with a clear 9:16 aspect ratio
 * rectangle. This version allows the crop frame to be dragged and constrained
 * within a specified video display area.
 */
class CropOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val TAG = "CropOverlayView"

    private val overlayPaint = Paint().apply {
        color = Color.BLACK
        alpha = 150
        style = Paint.Style.FILL
    }

    private val cropFramePaint = Paint().apply {
        color = Color.WHITE
        style = Style.STROKE // Now using the explicitly imported Style
        strokeWidth = 4f
    }

    private val cropRect = RectF()
    private val videoDisplayRect = RectF()

    private var lastTouchX: Float = 0f
    private var lastTouchY: Float = 0f
    private var isDragging: Boolean = false

    private val CROP_ASPECT_RATIO = 9f / 16f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (!videoDisplayRect.isEmpty) {
            initializeCropRect()
        }
        Log.d(TAG, "onSizeChanged: View dimensions ${w}x${h}. videoDisplayRect empty: ${videoDisplayRect.isEmpty}")
    }

    fun setVideoDisplayRect(left: Float, top: Float, right: Float, bottom: Float) {
        videoDisplayRect.set(left, top, right, bottom)
        Log.d(TAG, "videoDisplayRect set: $videoDisplayRect")
        initializeCropRect()
        invalidate()
    }

    private fun initializeCropRect() {
        if (videoDisplayRect.isEmpty) {
            Log.w(TAG, "Cannot initialize cropRect: videoDisplayRect is empty.")
            return
        }

        val videoDisplayWidth = videoDisplayRect.width()
        val videoDisplayHeight = videoDisplayRect.height()

        val initialCropWidth: Float
        val initialCropHeight: Float

        val potentialHeight = videoDisplayWidth / CROP_ASPECT_RATIO
        if (potentialHeight <= videoDisplayHeight) {
            initialCropHeight = potentialHeight
            initialCropWidth = videoDisplayWidth
        } else {
            initialCropWidth = videoDisplayHeight * CROP_ASPECT_RATIO
            initialCropHeight = videoDisplayHeight
        }

        val centerX = videoDisplayRect.centerX()
        val centerY = videoDisplayRect.centerY()

        cropRect.set(
            centerX - initialCropWidth / 2,
            centerY - initialCropHeight / 2,
            centerX + initialCropWidth / 2,
            centerY + initialCropHeight / 2
        )
        Log.d(TAG, "Initial cropRect initialized to: $cropRect within videoDisplayRect: $videoDisplayRect")
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (videoDisplayRect.isEmpty) {
            Log.w(TAG, "onDraw: videoDisplayRect is empty, skipping drawing overlay.")
            return
        }

        canvas.drawRect(videoDisplayRect.left, videoDisplayRect.top, cropRect.left, videoDisplayRect.bottom, overlayPaint)
        canvas.drawRect(cropRect.left, videoDisplayRect.top, cropRect.right, cropRect.top, overlayPaint)
        canvas.drawRect(cropRect.right, videoDisplayRect.top, videoDisplayRect.right, videoDisplayRect.bottom, overlayPaint)
        canvas.drawRect(cropRect.left, cropRect.bottom, cropRect.right, videoDisplayRect.bottom, overlayPaint)

        canvas.drawRect(cropRect, cropFramePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (cropRect.contains(x, y)) {
                    isDragging = true
                    lastTouchX = x
                    lastTouchY = y
                    Log.d(TAG, "ACTION_DOWN: Started dragging cropRect.")
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val dx = x - lastTouchX
                    val dy = y - lastTouchY

                    var newLeft = cropRect.left + dx
                    var newTop = cropRect.top + dy
                    var newRight = cropRect.right + dx
                    var newBottom = cropRect.bottom + dy

                    if (newLeft < videoDisplayRect.left) {
                        newLeft = videoDisplayRect.left
                        newRight = newLeft + cropRect.width()
                    }
                    if (newTop < videoDisplayRect.top) {
                        newTop = videoDisplayRect.top
                        newBottom = newTop + cropRect.height()
                    }
                    if (newRight > videoDisplayRect.right) {
                        newRight = videoDisplayRect.right
                        newLeft = newRight - cropRect.width()
                    }
                    if (newBottom > videoDisplayRect.bottom) {
                        newBottom = videoDisplayRect.bottom
                        newTop = newBottom - cropRect.height()
                    }

                    cropRect.set(newLeft, newTop, newRight, newBottom)
                    lastTouchX = x
                    lastTouchY = y
                    invalidate()
                    Log.d(TAG, "ACTION_MOVE: cropRect updated to $cropRect")
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                    Log.d(TAG, "ACTION_UP/CANCEL: Stopped dragging cropRect. Final position: $cropRect")
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    fun getNormalizedCropRect(videoIntrinsicWidth: Int, videoIntrinsicHeight: Int): RectF {
        if (videoIntrinsicWidth == 0 || videoIntrinsicHeight == 0 || videoDisplayRect.isEmpty) {
            Log.e(TAG, "Cannot normalize crop rect: intrinsic dimensions or videoDisplayRect is invalid. Intrinsic: ${videoIntrinsicWidth}x${videoIntrinsicHeight}, DisplayRect empty: ${videoDisplayRect.isEmpty}")
            return RectF(0f, 0f, 0f, 0f)
        }

        // Calculate relative position within the video display area
        val cropXRelToVideoDisplay = cropRect.left - videoDisplayRect.left
        val cropYRelToVideoDisplay = cropRect.top - videoDisplayRect.top

        // Convert to normalized coordinates (0.0 to 1.0)
        val normalizedLeft = cropXRelToVideoDisplay / videoDisplayRect.width()
        val normalizedTop = cropYRelToVideoDisplay / videoDisplayRect.height()
        val normalizedRight = (cropXRelToVideoDisplay + cropRect.width()) / videoDisplayRect.width()
        val normalizedBottom = (cropYRelToVideoDisplay + cropRect.height()) / videoDisplayRect.height()

        val normalizedRect = RectF(normalizedLeft, normalizedTop, normalizedRight, normalizedBottom)
        Log.d(TAG, "Calculated normalizedCropRect: $normalizedRect (from cropRect: $cropRect, videoDisplayRect: $videoDisplayRect, videoIntrinsic: ${videoIntrinsicWidth}x${videoIntrinsicHeight})")
        return normalizedRect
    }
    fun debugCropCalculation(videoIntrinsicWidth: Int, videoIntrinsicHeight: Int) {
        Log.d(TAG, "=== DEBUG CROP CALCULATION ===")
        Log.d(TAG, "Video intrinsic: ${videoIntrinsicWidth}x${videoIntrinsicHeight}")
        Log.d(TAG, "Video display rect: $videoDisplayRect")
        Log.d(TAG, "Crop rect: $cropRect")

        // Log relative positions
        val cropXRelToVideoDisplay = cropRect.left - videoDisplayRect.left
        val cropYRelToVideoDisplay = cropRect.top - videoDisplayRect.top
        Log.d(TAG, "Crop relative to video display: ($cropXRelToVideoDisplay, $cropYRelToVideoDisplay)")

        // Log normalized coordinates
        val normalizedLeft = cropXRelToVideoDisplay / videoDisplayRect.width()
        val normalizedTop = cropYRelToVideoDisplay / videoDisplayRect.height()
        val normalizedRight = (cropXRelToVideoDisplay + cropRect.width()) / videoDisplayRect.width()
        val normalizedBottom = (cropYRelToVideoDisplay + cropRect.height()) / videoDisplayRect.height()

        Log.d(TAG, "Normalized coordinates:")
        Log.d(TAG, "  Left: $normalizedLeft")
        Log.d(TAG, "  Top: $normalizedTop")
        Log.d(TAG, "  Right: $normalizedRight")
        Log.d(TAG, "  Bottom: $normalizedBottom")
        Log.d(TAG, "  Width: ${normalizedRight - normalizedLeft}")
        Log.d(TAG, "  Height: ${normalizedBottom - normalizedTop}")

        // Check for issues
        if (normalizedLeft < 0f || normalizedTop < 0f || normalizedRight > 1f || normalizedBottom > 1f) {
            Log.w(TAG, "WARNING: Normalized coordinates are outside [0,1] range!")
        }

        Log.d(TAG, "=== END DEBUG ===")
    }
}