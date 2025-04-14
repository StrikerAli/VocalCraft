package com.aliashraf.vocalcraft

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.Glide
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import yuku.ambilwarna.AmbilWarnaDialog
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

class SvgActivity : AppCompatActivity() {

    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var gestureDetector: GestureDetector? = null
    private lateinit var imageView: ImageView
    private lateinit var buttonContainer: LinearLayout
    private lateinit var saveButton: Button
    private var savedBitmap: Bitmap? = null
    private var activeEditText: EditText? = null
    private var selectedAspectRatio: String = ""
    private var TEXT_PADDING =30

    private lateinit var defaultInputPanel: LinearLayout
    private lateinit var textEditPanel: LinearLayout

    private val REQUEST_CODE_STORAGE_PERMISSION = 1

    private val apiUrl = "http://35.171.159.55:8000/resizer"
    private val dropboxAccessToken = "sl.u.AFq_SNfuM-N9dpdZFG5mtGRR-vwrJnAqWokMwaJoQbqzLkxBB9yEnW6_oJgrIxbJq5XHJRsiWzlWSD_jHRJVuxYWMYAL5iWIRczfLofDesno0PrPG6mdhprKfZKZo7ipxwDhNce51_cFvAwE0dys_C9dq8B7fKKzrX5s8YkpxOQoIV8cmhPdp1TubDihn-v7PcsPwQ7DtwLdyMg0di5Yfk8D3BXxjxGh1N_snkhFQ2TeQr3cQ_x8EVJSCeuj21h0zA7RQ0VPWxj3i9YLGGIJsSQhYwJz7CdUgzl69ntju3eqL3LR8ye0uK6V9B2fW3EkV0CLsiz3UVbSLUlf7mfNm1wrU1KiwN8hj1AqDawrxi9Qr-c0SKm7QcMXUlwqVxjCVquvBjiT4anYWjw4m0N_gkHfgpgiVLytnm-5JMaxStD6fAuB7EclnT2MAo8U4ocoUyFhFmfK5urqfG59lBwW37OaJRcI2MRj_gQ1G6ZhOdICdEBwFjOWT5fvuelktiVa3XxOlVa1tGugt1hzYcz9dDRYxs_aOkLuSShQVjasThfotELfteBTjkI5sfgdZuztdBBPtwORHXsY3eldgzZE8cD0Nj4nDTX1phTCeiJgh_JLTcBAFEEExRXg72tjTqKtGgVK6t7QnuQTK8ZZ-ddpsly7bIdCcoYHMnnmk1NxWXCGHEMHUADVQmL3DtCPVzVWdZRzgCFC4eHI2VBPNq6ACodWLUkxUgANeqDT-MW9ezXaPqmOrc-woIMJOnoyhUCgHbtZwfzQnocbnicfWvffU1YBAPqUpD6SOqgmTOXoiii6uezgbQmPX51GQGjafVygzXYryAEbVb4cZt3lWqIk_rKp1eUC7rbeddnhZuusctmtqVa9XfDW7oPzur--DFdjiH_eei6UpLJ3vV8YfnsSbdWzyM0uopnMczQYbNQTXKXRnU3-a55mdhRnrNXP8yy9sqRr8z13RTXauvdpaiP5E84TaLHLngTlmUhDW78aUow18wcFNRqfpDZGd7qzh7VhVE8Iv3XzKjXdgtRJwYfUOAYc2zCBGQihSIlGHARGk56Z3ZUDXG1xbBzHGBkT6B0SvGySFtO_2ZOaFyEHulNDxdLasrcM99EORogobwi4_kS0IIOoXoq41v8T0sZPWYAsIKDCq_zzG7W0T4glR5INu9NjP2mS-3K1JgpOHVRo_9QVXRLPgx42FADnswhlLQ0ZdsEW5ayeMoFGbaAtQa4kbY_83YqidbB6XiEKyWuFszIQGhHKt06yaA1n984DpPVqaEIPYTBEJ4mLGniB987EgoPIUmygwA-eDdPvLvyCuhdb3PUzOFvpj_2OacsnMQqsIPEP_UlGYXoDGpo9h3EGTf7bqdSVo-Cq5nr-J1QdB_JP6V4mA2r9R8onvXwk30-_ypZRJbrymkqOEy_1EYqAC7QBChVB71IVzjgRHi-y_7eZFQ"
        override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.imageview)  // Your layout file
        saveButton = findViewById(R.id.saveButton)
        imageView = findViewById(R.id.largeImageView)

        Glide.with(this)
            .asGif() // Load as GIF
            .load(R.drawable.loadingimage) // You can also use a URL or Base64
            .into(imageView)

        val jsonData = intent.getStringExtra("json_data")
        val promptData = intent.getStringExtra("prompt_data")
        val imageLink = intent.getStringExtra("image_link")
        // Enable edit button and input
        val editButton: Button = findViewById(R.id.editButton)
        val editTextInput: EditText = findViewById(R.id.editTextInput)
        if (jsonData != null && promptData != null && imageLink != null) {
            sendRequestToAPI(jsonData, promptData, imageLink)
        } else {
            Toast.makeText(this, "No data received", Toast.LENGTH_SHORT).show()
        }
        editButton.setOnClickListener {
            val editTextInput: EditText = findViewById(R.id.editTextInput)
            val enteredCaption = editTextInput.text.toString().trim()

            if (enteredCaption.isNotEmpty()) {
                searchFirebaseForImageLink(enteredCaption)
            } else {
                Toast.makeText(this, "Enter a valid caption", Toast.LENGTH_SHORT).show()
            }
        }
        defaultInputPanel = findViewById(R.id.defaultInputPanel)
        textEditPanel = findViewById(R.id.textEditPanel)
        defaultInputPanel.visibility = View.VISIBLE
        textEditPanel.visibility = View.GONE

        scaleGestureDetector = ScaleGestureDetector(this, ScaleListener())
/*
        gestureDetector = GestureDetector(this, GestureListener())
*/

        buttonContainer = findViewById(R.id.buttonContainer)
        buttonContainer.visibility = View.GONE

        saveButton.setOnClickListener {
            if (selectedAspectRatio.isNotEmpty()) {
                sendImageToApi()
            } else {
                buttonContainer.visibility = View.VISIBLE
            }
        }

        setupAspectRatioButtons()





}

    private fun searchFirebaseForImageLink(caption: String) {
        val database = FirebaseDatabase.getInstance()
        val ref = database.getReference("input_image") // Navigate to 'input_image' node
        Log.d("SvgActivity", "Searching for inpainting image with caption: $caption")
        ref.orderByChild("caption").equalTo(caption).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (child in snapshot.children) {
                        val imageLink = child.child("image_link").getValue(String::class.java)
                        if (imageLink != null) {
                            Log.d("SvgActivity", "Found inpainting image: $imageLink")
                            sendInpaintingRequest(imageLink) // Send request if found
                            return
                        }
                    }
                } else {
                    Toast.makeText(this@SvgActivity, "No matching image found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@SvgActivity, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun sendInpaintingRequest(imageLink: String) {
        val apiUrl = "http://35.171.159.55:8000/inpainting"
        val jsonObject = JSONObject().apply {
            put("image", imageLink)
        }

        val requestBody = jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder().url(apiUrl).post(requestBody).build()
        Log.d("SvgActivity", "Sending inpainting request to API")
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(this@SvgActivity, "Inpainting request failed", Toast.LENGTH_SHORT).show() }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { responseBody ->
                    try {
                        val jsonResponse = JSONObject(responseBody)
                        val base64MaskedImage = jsonResponse.optString("masked_image", "")
                        Log.d("SvgActivity", "Received inpainting response: $base64MaskedImage")
                        if (base64MaskedImage.isNotEmpty()) {
                            runOnUiThread {
                                replaceSelectedObject(base64MaskedImage)
                            }
                        } else {
                            runOnUiThread { Toast.makeText(this@SvgActivity, "No image returned", Toast.LENGTH_SHORT).show() }
                        }
                    } catch (e: Exception) {
                        runOnUiThread { Toast.makeText(this@SvgActivity, "Invalid API response", Toast.LENGTH_SHORT).show() }
                    }
                }
            }
        })
    }

    private fun replaceSelectedObject(base64Image: String) {
        val decodedBytes = Base64.decode(base64Image, Base64.DEFAULT)
        val newBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        Log.d("SvgActivity", "Replacing object image")
        val frameLayout: FrameLayout = findViewById(R.id.imageFrame)
        for (i in 0 until frameLayout.childCount) {
            val child = frameLayout.getChildAt(i)
            if (child is ImageView && child.tag == "selectedObject") {
                child.setImageBitmap(newBitmap) // Replace image
                Log.d("SvgActivity", "Object image replaced successfully")
                break
            }
        }
    }


    fun getBitmapFromView(view: View): Bitmap {
        // Create a Bitmap object with the width and height of the view
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        // Draw the view onto the canvas, which is drawn into the bitmap
        view.draw(canvas)
        return bitmap
    }


    private fun generateRandomFilename(): String {
        val uuid = UUID.randomUUID().toString()
        return "Image_$uuid.png" // Use UUID as part of the filename
    }
    // Send POST request to API
    private fun sendRequestToAPI(jsonData: String, promptData: String, imageLink: String) {
        val client = OkHttpClient.Builder()
            .connectTimeout(240, TimeUnit.SECONDS)  // Set connection timeout
            .writeTimeout(240, TimeUnit.SECONDS)    // Set write timeout
            .readTimeout(240, TimeUnit.SECONDS)     // Set read timeout
            .build()
        val apiUrl = "http://35.171.159.55:8000/generate_image" // API URL for generating images

        // Parse the jsonData into a JSONObject
        Log.d("Original_Request_Body", jsonData)
        val jsonObject = JSONObject(jsonData)

        // Add promptData under the key "old_prompt"
        jsonObject.put("old_prompt", promptData)

        // Add imageLink under the key "image_link"
        jsonObject.put("base_image_url", imageLink)

        // Convert the modified JSON object back to a string
        val modifiedJsonData = jsonObject.toString()
        Log.d("Modified_Request_Body", modifiedJsonData)
        // Create the request body with the modified JSON data
        val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), modifiedJsonData)

        Log.d("Modified Request Body", modifiedJsonData)

        // Create the request
        val request = Request.Builder()
            .url(apiUrl)
            .post(requestBody)
            .build()

        // Call and response handling
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Log.e("SvgActivity", "Request failed", e)
                    Toast.makeText(this@SvgActivity, "Request failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()

                    // Log the raw response body
                    Log.d("SvgActivity", "Response: $responseBody")

                    // Parse the JSON response
                    val responseJson = responseBody?.let { parseJsonResponse(it) }

                    // Handle image and text elements
                    runOnUiThread {
                        if (responseJson != null) {
                            Log.d("SvgActivity", "Parsed response: Image - ${responseJson.background}, Text Elements - ${responseJson.text_elements.size}")
                            Log.d("SvgActivity", "Object Image - ${responseJson.objectImage}, Object Position - ${responseJson.objectPosition}")
                            displayImage(responseJson.background)
                            createTextElements(responseJson.text_elements)
                            displayObject(responseJson.objectImage, responseJson.objectPosition) // Display object
                        }
                    }
                } else {
                    runOnUiThread {
                        Log.e("SvgActivity", "Image Generation Error: ${response.message}")
                        Toast.makeText(this@SvgActivity, "Error: ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    // Parse the JSON response (including text elements)
    private fun parseJsonResponse(response: String): ResponseData? {
        try {
            val jsonObject = JSONObject(response)

            val background = jsonObject.getString("background") // Background image
            Log.d("SvgActivity", "Background image: $background")
            val objImage = jsonObject.getString("object") // Object image in base64

            val objPosition = jsonObject.getJSONObject("object_position")
            val objectPosition = ObjectPosition(
                objPosition.getInt("x"),
                objPosition.getInt("y"),
                objPosition.getInt("width"),
                objPosition.getInt("height")
            )
            Log.d("SvgActivity", "Object position: $objectPosition")

            val textElementsJsonArray = jsonObject.getJSONArray("text_elements")
            val textElements = mutableListOf<TextElement>()

            for (i in 0 until textElementsJsonArray.length()) {
                val textElementObj = textElementsJsonArray.getJSONObject(i)
                val content = textElementObj.getString("content")
                val fontSize = try {
                    val rawFontSize = textElementObj.get("font_size")

                    when (rawFontSize) {
                        is Int -> rawFontSize / 3
                        is String -> rawFontSize.toIntOrNull()?.div(3) ?: 20
                        else -> 20
                    }
                } catch (e: Exception) {
                    20
                }
                val textColor = try {
                    val rawColor = textElementObj.getString("font_color")
                    Color.parseColor(rawColor) // Validates the color string
                    rawColor // Return the original if it's valid
                } catch (e: Exception) {
                    "black" // Fallback to "black" if invalid
                }


                textElements.add(TextElement(content, fontSize, textColor))
            }

            return ResponseData(background, textElements, objImage, objectPosition)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("SvgActivity", "Error parsing JSON response", e)
            return null
        }
    }
    @SuppressLint("ClickableViewAccessibility")
    private fun displayObject(base64Object: String, position: ObjectPosition) {
        try {
            val decodedString = Base64.decode(base64Object, Base64.DEFAULT)
            val decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

            val frameLayout: FrameLayout = findViewById(R.id.imageFrame)
            val imageView = ImageView(this)
            val boundingBox = View(this) // Bounding box
            boundingBox.tag = "boundingBox" // Set the tag for easy identification

            // Padding size
            val PADDING_SIZE = 30

            // Get original width and height
            val originalWidth = position.width
            val originalHeight = position.height

            val layoutParams = FrameLayout.LayoutParams(originalWidth, originalHeight)
            layoutParams.leftMargin = position.x
            layoutParams.topMargin = position.y

            imageView.layoutParams = layoutParams
            imageView.setImageBitmap(decodedBitmap)

            // Create Bounding Box with Padding
            val boxParams = FrameLayout.LayoutParams(
                originalWidth + PADDING_SIZE * 2,
                originalHeight + PADDING_SIZE * 2
            )
            boxParams.leftMargin = position.x - PADDING_SIZE
            boxParams.topMargin = position.y - PADDING_SIZE

            boundingBox.layoutParams = boxParams
            boundingBox.setBackgroundResource(R.drawable.bounding_box) // Dashed border drawable
            boundingBox.tag = "boundingBox" // Set the tag for easy identification

            frameLayout.addView(imageView)
            Log.d("SvgActivity", "Object displayed successfully")
            frameLayout.addView(boundingBox) // Add bounding box above the image
            Log.d("SvgActivity", "Bounding Box displayed successfully")

            boundingBox.visibility = View.GONE // Hide initially
            defaultInputPanel.visibility = View.VISIBLE
            textEditPanel.visibility = View.GONE
            // Gesture Detector for Pinch-to-Zoom and Dragging
            val scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                private var scaleFactor = 1f  // Initial scale

                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    scaleFactor *= detector.scaleFactor
                    scaleFactor = scaleFactor.coerceIn(0.5f, 3.0f)

                    imageView.scaleX = scaleFactor
                    imageView.scaleY = scaleFactor

                    boundingBox.scaleX = scaleFactor
                    boundingBox.scaleY = scaleFactor
                    boundingBox.tag = "boundingBox" // Set the tag for easy identification

                    Log.d("SvgActivity", "Resizing: Scale Factor = $scaleFactor")
                    return true
                }
            })

            imageView.setOnTouchListener(object : View.OnTouchListener {
                private var dX = 0f
                private var dY = 0f
                private var lastAction = 0

                override fun onTouch(view: View, event: MotionEvent): Boolean {
                    scaleGestureDetector.onTouchEvent(event)

                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            setScrollEnabled(false)  // Disable scrolling
                            dX = view.x - event.rawX
                            dY = view.y - event.rawY
                            lastAction = MotionEvent.ACTION_DOWN
                            highlightSelectedObject(view, boundingBox)
                        }
                        MotionEvent.ACTION_MOVE -> {
                            if (!scaleGestureDetector.isInProgress) {
                                val newX = event.rawX + dX
                                val newY = event.rawY + dY

                                val minX = 0f
                                val minY = 0f
                                val maxX = frameLayout.width - view.width
                                val maxY = frameLayout.height - view.height

                                view.x = newX.coerceIn(minX, maxX.toFloat())
                                view.y = newY.coerceIn(minY, maxY.toFloat())

                                boundingBox.x = view.x - PADDING_SIZE
                                boundingBox.y = view.y - PADDING_SIZE
                            }
                        }

                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            setScrollEnabled(true)  // Re-enable scroll after drag
                            if (lastAction == MotionEvent.ACTION_DOWN) {
                                view.performClick()
                            }
                        }
                    }
                    return true
                }
            })


            // Detect tap outside to deselect the object
            frameLayout.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    Log.d("SvgActivity", "Tapped outside object")
                    val frameLayout = findViewById<FrameLayout>(R.id.imageFrame)
                    for (i in 0 until frameLayout.childCount) {
                        val child = frameLayout.getChildAt(i)
                        if (child.tag == "boundingBox" || child.tag == "textBoundingBox") {
                            child.visibility = View.GONE
                        }
                    }
                }
                false
            }

        } catch (e: Exception) {
            Log.e("SvgActivity", "Error displaying object image", e)
        }
    }

    private fun highlightSelectedObject(view: View, boundingBox: View) {
        boundingBox.visibility = View.VISIBLE
        boundingBox.bringToFront() // Ensure it's always visible on top of the image
        view.tag = "selectedObject"
        defaultInputPanel.visibility = View.VISIBLE
        textEditPanel.visibility = View.GONE
        val editButton: Button = findViewById(R.id.editButton)
        val editTextInput: EditText = findViewById(R.id.editTextInput)
        editButton.isEnabled = true
        editButton.alpha = 1.0f // Restore color
        editButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.orange_500))

        editTextInput.isEnabled = true
        editTextInput.alpha = 1.0f
    }









    // Decode base64 image and set it as the background
    private fun displayImage(base64Image: String) {
        try {
            // Remove metadata prefix if present
            val cleanedBase64 = base64Image.replace("^data:image/\\w+;base64,".toRegex(), "")
            val decodedString = Base64.decode(cleanedBase64.trim(), Base64.DEFAULT)

            val decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

            // Get image dimensions
            val imageWidth = decodedBitmap.width
            val imageHeight = decodedBitmap.height

            // Find the ImageView by its ID and set the image
            val largeImageView: ImageView = findViewById(R.id.largeImageView)
            largeImageView.setImageBitmap(decodedBitmap)  // Set the decoded image bitmap

            // Display image dimensions in logs
            Log.d("SvgActivity", "Image displayed successfully. Size: ${imageWidth}x${imageHeight}")

            // Get the actual size of largeImageView after layout pass
            largeImageView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    largeImageView.viewTreeObserver.removeOnGlobalLayoutListener(this)

                    val viewWidth = largeImageView.width
                    val viewHeight = largeImageView.height

                    Log.d("SvgActivity", "largeImageView Size: ${viewWidth}x${viewHeight}")

                    runOnUiThread {
                        Toast.makeText(
                            this@SvgActivity,
                            "Image Size: ${imageWidth}x${imageHeight}\nView Size: ${viewWidth}x${viewHeight}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            })
        } catch (e: Exception) {
            Log.e("SvgActivity", "Error displaying image", e)
        }
    }



    // Dynamically create text elements based on response
    @SuppressLint("ClickableViewAccessibility")
    private fun createTextElements(textElements: List<TextElement>) {
        val frameLayout: FrameLayout = findViewById(R.id.imageFrame)

        for (textElement in textElements) {
            val editText = EditText(this)
            editText.setText(textElement.content)
            editText.setBackgroundResource(0)  // Remove the background (underline)
            editText.textSize = textElement.fontSize.toFloat()  // Set text size from response
            editText.setTextColor(Color.parseColor(textElement.textColor))  // Set text color
            val typeface = ResourcesCompat.getFont(this, R.font.kfc_bold)
            editText.typeface = typeface

            var layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            }
            editText.layoutParams = layoutParams

            // Create Bounding Box for text
            val boundingBox = View(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
                setBackgroundResource(R.drawable.bounding_box) // Use your dashed border drawable
                tag = "textBoundingBox"
            }

            // Add EditText first, then bounding box
            frameLayout.addView(editText)
            frameLayout.addView(boundingBox)
            boundingBox.visibility = View.GONE // Hide initially

            // Position the bounding box to match EditText
            editText.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    editText.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    boundingBox.layoutParams.width = editText.width + TEXT_PADDING * 2
                    boundingBox.layoutParams.height = editText.height + TEXT_PADDING * 2
                    boundingBox.x = editText.x - TEXT_PADDING
                    boundingBox.y = editText.y - TEXT_PADDING
                }
            })


            // Make EditText draggable with bounding box
            editText.setOnTouchListener { v, event ->
                scaleGestureDetector?.onTouchEvent(event)
                gestureDetector?.onTouchEvent(event)

                val frameLayout = findViewById<FrameLayout>(R.id.imageFrame)
                val parentLocation = IntArray(2)
                frameLayout.getLocationOnScreen(parentLocation)

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        setScrollEnabled(false)
                        activeEditText = v as? EditText
                        boundingBox.visibility = View.VISIBLE
                        textEditPanel.visibility = View.VISIBLE
                        defaultInputPanel.visibility = View.GONE
                        setupLiveTextEditing(activeEditText!!, boundingBox)
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val relativeX = event.rawX - parentLocation[0] - v.width / 2
                        val relativeY = event.rawY - parentLocation[1] - v.height / 2

                        val minX = 0f
                        val minY = 0f
                        val maxX = frameLayout.width - v.width
                        val maxY = frameLayout.height - v.height

                        v.x = relativeX.coerceIn(minX, maxX.toFloat())
                        v.y = relativeY.coerceIn(minY, maxY.toFloat())

                        boundingBox.x = v.x - TEXT_PADDING
                        boundingBox.y = v.y - TEXT_PADDING
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        setScrollEnabled(true)
                        boundingBox.visibility = View.VISIBLE
                    }
                }
                true
            }





            // Optional: Tap outside to hide all bounding boxes
            frameLayout.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    Log.d("SvgActivity", "Tapped outside text element")
                    val frameLayout = findViewById<FrameLayout>(R.id.imageFrame)
                    for (i in 0 until frameLayout.childCount) {
                        val child = frameLayout.getChildAt(i)
                        if (child.tag == "boundingBox" || child.tag == "textBoundingBox") {
                            child.visibility = View.GONE
                        }
                    }
                }
                false
            }

            Log.d("SvgActivity", "Text element with bounding box added: ${textElement.content}")
        }
    }


    // GestureListener to handle double tap for enabling editing
/*    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            activeEditText?.let { editText ->

                Log.d("Gesture", "Double tap detected on: ${editText.text}")

                // Inflate the custom dialog layout
                val dialogView = layoutInflater.inflate(R.layout.dialog_custom, null)

                // Get references to views
                val input = dialogView.findViewById<EditText>(R.id.inputText)
                val textSizeSeekBar = dialogView.findViewById<SeekBar>(R.id.textSizeSeekBar)
                val textSizeLabel = dialogView.findViewById<TextView>(R.id.textSizeLabel)
                val btnSave = dialogView.findViewById<Button>(R.id.btnSave)
                val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
                val btnColorPicker = dialogView.findViewById<Button>(R.id.btnColorPicker)
                val fontSpinner = dialogView.findViewById<Spinner>(R.id.fontSpinner) // Font dropdown

                // Pre-fill input with current text and color
                input.setText(editText.text)
                input.setTextColor(editText.currentTextColor)

                // Load available fonts
                val fonts = listOf(
                    "Default" to Typeface.DEFAULT,
                    "Sans Serif" to Typeface.SANS_SERIF,
                    "Serif" to Typeface.SERIF,
                    "Monospace" to Typeface.MONOSPACE,
                    "KFC Bold" to ResourcesCompat.getFont(editText.context, R.font.kfc_bold),
                    "Montserrat Bold Italic" to ResourcesCompat.getFont(editText.context, R.font.montserrat_bold_italic)
                )

                // Populate the font dropdown (Spinner)
                val fontNames = fonts.map { it.first }
                val adapter = ArrayAdapter(editText.context, android.R.layout.simple_spinner_dropdown_item, fontNames)
                fontSpinner.adapter = adapter

                var selectedTypeface = editText.typeface // Store selected typeface

                // Handle font selection and apply preview
                fontSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        selectedTypeface = fonts[position].second ?: Typeface.DEFAULT
                        input.typeface = selectedTypeface // Apply font preview in dialog
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }

                // Create the AlertDialog
                val builder = AlertDialog.Builder(editText.context)
                dialogView.background = ContextCompat.getDrawable(editText.context, R.drawable.dialog_background)
                builder.setView(dialogView)
                var dialog: AlertDialog? = null

                // Set up SeekBar listener to update text size preview
                textSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        val scaleFactor = 0.5f + (progress / 100f) // 50% to 200%
                        val newTextSize = editText.textSize * scaleFactor / editText.resources.displayMetrics.scaledDensity
                        input.textSize = newTextSize
                        textSizeLabel.text = "Text Size: ${progress + 50}%"
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })

                var selectedColor = editText.currentTextColor // Store selected color

                // Color Picker Button
                btnColorPicker.setOnClickListener {
                    val colorPicker = AmbilWarnaDialog(editText.context, selectedColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
                        override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                            selectedColor = color
                            input.setTextColor(color) // Apply color preview
                        }

                        override fun onCancel(dialog: AmbilWarnaDialog?) {}
                    })
                    colorPicker.show()
                }

                // Save Button
                btnSave.setOnClickListener {
                    val scaleFactor = 0.5f + (textSizeSeekBar.progress / 100f)
                    val newTextSize = editText.textSize * scaleFactor / editText.resources.displayMetrics.scaledDensity

                    activeEditText?.apply {
                        textSize = newTextSize
                        setTextColor(selectedColor)
                        setTypeface(selectedTypeface)
                        setText(input.text.toString())
                    }

                    dialog?.dismiss()
                }

                // Cancel Button
                btnCancel.setOnClickListener { dialog?.dismiss() }

                // Create and show the dialog
                dialog = builder.create()
                dialog.show()
            }
            return true
        }
    }*/




    // ScaleGestureListener to handle pinch to resize
    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            activeEditText?.let { editText ->
                val rawScaleFactor = detector.scaleFactor
                val smoothedScaleFactor = 1 + (rawScaleFactor - 1) * 0.5f // Adjust sensitivity

                // Ensure the scaling doesn't result in excessively small or large text
                val newTextSize = editText.textSize * smoothedScaleFactor

                // Clamp the new text size to a reasonable range (for example, between 10sp and 50sp)
                val clampedTextSize = newTextSize.coerceIn(10f, 20f) // Adjust these values to suit your needs

                // Apply the smoothed and clamped text size
                editText.textSize = clampedTextSize

                Log.d("Gesture", "Pinch detected. New text size: ${editText.textSize}")
            }
            return true
        }
    }
    private fun checkStoragePermission(): Boolean { // Check if the storage permission is granted
        val readMediaPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        }

        return ContextCompat.checkSelfPermission(this, readMediaPermission) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() { // Request the storage permission
        val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_STORAGE_PERMISSION)
    }

    override fun onRequestPermissionsResult( // Handle the permission request result
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults) // Call the super method
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted!", Toast.LENGTH_SHORT).show()
                savedBitmap?.let {
                    val isGallerySaved = saveImageToGallery(it)
                    if (isGallerySaved) {
                        Toast.makeText(this, "Image saved successfully to Gallery!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Failed to save image to Gallery", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun setupAspectRatioButtons() {
        val aspectRatios = listOf("Square", "Portrait", "Landscape", "A4", "Poster", "Story", "Facebook Cover")
        val aspectValues = listOf("square", "portrait", "landscape", "a4", "poster", "story", "facebook_cover")

        aspectRatios.zip(aspectValues).forEach { (label, value) ->
            val button = Button(this).apply {
                text = label
                backgroundTintList = ContextCompat.getColorStateList(this@SvgActivity, R.color.orange_500)
                setTextColor(ContextCompat.getColor(this@SvgActivity, android.R.color.white))
                setPadding(16, 16, 16, 16)
                setOnClickListener {
                    selectedAspectRatio = value
                    Log.d("SvgActivity", "Selected Aspect Ratio: $selectedAspectRatio")
                    disableOtherButtons(this)
                }
            }
            buttonContainer.addView(button)
        }
    }

    private fun disableOtherButtons(activeButton: Button) {
        for (i in 0 until buttonContainer.childCount) {
            val button = buttonContainer.getChildAt(i) as Button
            button.alpha = if (button == activeButton) 1.0f else 0.5f // Highlight selected, dim others
        }
    }

    private fun setScrollEnabled(enabled: Boolean) {
        val scrollView = findViewById<ScrollView>(R.id.scrollView) // ID of your ScrollView in XML
        scrollView.requestDisallowInterceptTouchEvent(!enabled)
    }


    private fun sendImageToApi() {
        val frameLayout: FrameLayout = findViewById(R.id.imageFrame)

        // Hide all bounding boxes before capturing the image
        for (i in 0 until frameLayout.childCount) {
            val child = frameLayout.getChildAt(i)
            if (child.tag == "boundingBox" || child.tag == "textBoundingBox") {
                child.visibility = View.GONE
            }
        }

        // Create a bitmap of the FrameLayout
        val bitmap = Bitmap.createBitmap(frameLayout.width, frameLayout.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        frameLayout.draw(canvas)

        if (bitmap == null) {
            Toast.makeText(this, "No image to send", Toast.LENGTH_SHORT).show()
            return
        }

        savedBitmap = bitmap
        // Now you can proceed with sending `savedBitmap` to the API

        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val base64Image = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT)

        val jsonObject = JSONObject().apply {
            put("aspect_ratio", selectedAspectRatio)
            put("image", base64Image)
        }

        val body = jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull())
        Log.d("SvgActivity", "Resizer Request Body: $jsonObject")
        val request = Request.Builder().url(apiUrl).post(body).build()

        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(this@SvgActivity, "Request failed", Toast.LENGTH_SHORT).show() }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { responseBody ->
                    try {
                        Log.d("SvgActivity", "Resizer Response: $responseBody")
                        val jsonResponse = JSONObject(responseBody)
                        if (jsonResponse.optBoolean("success", false)) {
                            val base64ResponseImage = jsonResponse.optString("image", "")

                            if (base64ResponseImage.isNotEmpty()) {
                                // Remove "data:image/png;base64," prefix if present
                                val cleanedBase64 = base64ResponseImage.substringAfter("base64,", base64ResponseImage)

                                saveAndUploadImage(cleanedBase64)
                            } else {
                                runOnUiThread { Toast.makeText(this@SvgActivity, "No image returned by API", Toast.LENGTH_SHORT).show() }
                            }
                        } else {
                            runOnUiThread { Toast.makeText(this@SvgActivity, "API request failed", Toast.LENGTH_SHORT).show() }
                        }
                    } catch (e: Exception) {
                        runOnUiThread { Toast.makeText(this@SvgActivity, "Invalid API response", Toast.LENGTH_SHORT).show() }
                        Log.e("SvgActivity", "JSON Parsing Error: ${e.message}")
                    }
                }
            }

        })
    }


    private fun saveAndUploadImage(base64Image: String) {
        val decodedBytes = Base64.decode(base64Image, Base64.DEFAULT)
        val decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        CoroutineScope(Dispatchers.Main).launch {
            val savedToGallery = saveImageToGallery(decodedBitmap)
            val uploadedToDropbox = uploadImageToDropbox(decodedBitmap)
            if (savedToGallery && uploadedToDropbox) {
                Toast.makeText(this@SvgActivity, "Image saved & uploaded!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveImageToGallery(bitmap: Bitmap): Boolean {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "ResizedImage.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        val uri: Uri? = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        return uri?.let {
            contentResolver.openOutputStream(it)?.use { outputStream ->
                outputStream.write(bitmapToByteArray(bitmap))
                Log.d("SvgActivity", "Image saved to Gallery")
                true
            }
        } ?: false
    }

    private fun uploadImageToDropbox(bitmap: Bitmap): Boolean {
        return try {
            CoroutineScope(Dispatchers.IO).launch {
                Log.d("SvgActivity", "Uploading to Dropbox")

                val config = DbxRequestConfig.newBuilder("vocalcraft-app").build()
                val client = DbxClientV2(config, dropboxAccessToken)

                // Generate a unique filename using the timestamp
                val timestamp = System.currentTimeMillis()
                val filename = "/ResizedImage_$timestamp.png"

                client.files().uploadBuilder(filename)
                    .uploadAndFinish(ByteArrayInputStream(bitmapToByteArray(bitmap)))

                Log.d("SvgActivity", "Uploaded to Dropbox: $filename")

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SvgActivity, "Uploaded to Dropbox: $filename", Toast.LENGTH_SHORT).show()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun setupLiveTextEditing(selectedEditText: EditText, boundingBox: View) {
        val inputText = textEditPanel.findViewById<EditText>(R.id.inputText)
        val textSizeSeekBar = textEditPanel.findViewById<SeekBar>(R.id.textSizeSeekBar)
        val fontSpinner = textEditPanel.findViewById<Spinner>(R.id.fontSpinner)
        val btnColorPicker = textEditPanel.findViewById<Button>(R.id.btnColorPicker)

        inputText.setText(selectedEditText.text.toString())

        fun updateBoundingBox() {
            boundingBox.layoutParams.width = selectedEditText.width + TEXT_PADDING * 2
            boundingBox.layoutParams.height = selectedEditText.height + TEXT_PADDING * 2
            boundingBox.x = selectedEditText.x - TEXT_PADDING
            boundingBox.y = selectedEditText.y - TEXT_PADDING
            boundingBox.requestLayout()
        }


        inputText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                selectedEditText.setText(s.toString())
                selectedEditText.post { updateBoundingBox() }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        textSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val newSize = (10 + (progress * 0.3)).toFloat()
                selectedEditText.textSize = newSize
                selectedEditText.post { updateBoundingBox() }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnColorPicker.setOnClickListener {
            val colorPicker = AmbilWarnaDialog(this, selectedEditText.currentTextColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
                override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                    selectedEditText.setTextColor(color)
                }
                override fun onCancel(dialog: AmbilWarnaDialog?) {}
            })
            colorPicker.show()
        }
        val fonts = listOf(
            "KFC Bold",
            "Bowlby One",
            "Alfa Slab One",
            "Mochiy Pop P One",
            "Montserrat Bold Italic",
            "Rubik Mono One"
        )

        val fontResources = listOf(
            R.font.kfc_bold,
            R.font.bowlby_one,
            R.font.alfa_slab_one,
            R.font.mochiy_pop_p_one,
            R.font.montserrat_bold_italic,
            R.font.rubik_mono_one
        )

        val fontAdapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, fonts) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                val typeface = ResourcesCompat.getFont(this@SvgActivity, fontResources[position])
                view.typeface = typeface
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                val typeface = ResourcesCompat.getFont(this@SvgActivity, fontResources[position])
                view.typeface = typeface
                return view
            }
        }

        fontAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        fontSpinner.adapter = fontAdapter
        val savedFontPosition = selectedEditText.getTag(R.id.font_position_tag) as? Int
        if (savedFontPosition != null) {
            fontSpinner.setSelection(savedFontPosition)
        } else {
            // Default to the first font if not set
            fontSpinner.setSelection(0)
        }
        fontSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                activeEditText?.let { editText ->
                    val selectedFont = ResourcesCompat.getFont(this@SvgActivity, fontResources[position])
                    editText.typeface = selectedFont
                    editText.setTag(R.id.font_position_tag, position)  // ✅ Save font index
                    editText.post { updateBoundingBox() }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }



    }



    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return outputStream.toByteArray()
    }



}





// Data class to hold image and text element data

// Data class to hold individual text element details
data class TextElement(val content: String, val fontSize: Int, val textColor: String)
data class ResponseData(
    val background: String,
    val text_elements: List<TextElement>,
    val objectImage: String,   // Base64-encoded object image
    val objectPosition: ObjectPosition // Object coordinates
)

data class ObjectPosition(val x: Int, val y: Int, val width: Int, val height: Int)