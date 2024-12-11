package com.aliashraf.vocalcraft

import android.annotation.SuppressLint
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.OutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class SvgActivity : AppCompatActivity() {

    private var initialX = 0f
    private var initialY = 0f
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var initialWidth = 0
    private var initialHeight = 0
    private var scaleFactor = 1f
    private var textSize = 16f  // Initial text size

    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private lateinit var gestureDetector: GestureDetector

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.imageview)  // Your layout file

        val frameLayout: FrameLayout = findViewById(R.id.imageFrame)  // Reference to the FrameLayout

        // Add a draggable, resizable, and editable EditText
        val editText = EditText(this)
        editText.setText("Editable Text")
        editText.setBackgroundResource(0)  // Remove the background (underline)
        editText.textSize = textSize  // Set initial text size
        val params = FrameLayout.LayoutParams(300, 150) // Initial size
        params.leftMargin = 50
        params.topMargin = 50
        editText.layoutParams = params

        // Add the EditText to the FrameLayout
        frameLayout.addView(editText)

        // Gesture Detectors for Double Tap and Pinch
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                // Show a dialog box to edit the text on double-tap
                Log.d("Gesture", "Double Tap detected!")

                showEditTextDialog(editText)  // Open dialog to edit the text
                return true
            }
        })

        scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                // Resize the EditText based on pinch gesture
                Log.d("Gesture", "Pinch gesture detected. Scale factor: $scaleFactor")
                scaleFactor *= detector.scaleFactor
                scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 5.0f))

                // Update text size with pinch gesture (while maintaining the box size)
                textSize *= detector.scaleFactor
                textSize = Math.max(10f, Math.min(textSize, 40f))  // Restrict text size

                editText.setTextSize(textSize)  // Apply new text size
                scaleFactor *= detector.scaleFactor
                scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 5.0f))

                val newWidth = (initialWidth * scaleFactor).toInt()
                val newHeight = (initialHeight * scaleFactor).toInt()

                editText.layoutParams.width = newWidth
                editText.layoutParams.height = newHeight
                editText.requestLayout()
                return true
            }
        })

        // Make the EditText draggable
        editText.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)  // Handle double tap for editing
            scaleGestureDetector.onTouchEvent(event)  // Handle pinch to resize

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    Log.d("Gesture", "Drag Start detected at: (${event.rawX}, ${event.rawY})")
                    initialX = v.x
                    initialY = v.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY

                    // Remember initial width and height for resizing
                    initialWidth = v.width
                    initialHeight = v.height
                }
                MotionEvent.ACTION_MOVE -> {
                    // Draggable functionality
                    Log.d("Gesture", "Dragging at: (${event.rawX}, ${event.rawY})")
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY
                    v.x = initialX + deltaX
                    v.y = initialY + deltaY
                }
            }
            true
        }

        // Button or some other trigger to save the content to gallery
        val saveButton = findViewById<Button>(R.id.saveButton)
        saveButton.setOnClickListener {
            saveToGallery(frameLayout)
        }
    }

    // Method to show the dialog for editing text
    private fun showEditTextDialog(editText: EditText) {
        val input = EditText(this)
        input.setText(editText.text.toString())  // Pre-fill with current text

        // Create the dialog
        val dialog = AlertDialog.Builder(this)
            .setTitle("Edit Text")
            .setMessage("Modify the text below:")
            .setView(input)
            .setPositiveButton("Save") { dialog, _ ->
                // Set the new text when saved
                val newText = input.text.toString()
                editText.setText(newText)
                Toast.makeText(this, "Text saved", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.show()
    }

    // Save the FrameLayout content to gallery
    private fun saveToGallery(frameLayout: FrameLayout) {
        // Create a bitmap from the FrameLayout
        val bitmap = Bitmap.createBitmap(frameLayout.width, frameLayout.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        frameLayout.draw(canvas)

        // Save the bitmap to the gallery
        val outputStream: OutputStream?
        val contentValues = ContentValues().apply {
            put(
                MediaStore.Images.Media.TITLE,
                "SavedImage_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            )
            put(MediaStore.Images.Media.DESCRIPTION, "Saved image from FrameLayout")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            outputStream = uri?.let { contentResolver.openOutputStream(it) }

            try {
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }
                outputStream?.close()
                Toast.makeText(this@SvgActivity, "Image saved to gallery", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this@SvgActivity, "Error saving image", Toast.LENGTH_SHORT).show()
            }
        }
    }
