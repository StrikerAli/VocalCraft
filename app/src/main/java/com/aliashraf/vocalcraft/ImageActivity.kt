package com.aliashraf.vocalcraft

import com.bumptech.glide.Glide
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException

class ImageActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.imageview)

        imageView = findViewById(R.id.largeImageView)
        Glide.with(this)
            .asGif() // Load as GIF
            .load(R.drawable.loadingimage) // You can also use a URL or Base64
            .into(imageView)
        // Get the JSON data from the intent extras
        val jsonData = intent.getStringExtra("json_data")
        val promptData = intent.getStringExtra("prompt_data")

        if (jsonData != null && promptData != null) {
            sendQueryToAPI(promptData, jsonData)
        } else {
            Toast.makeText(this, "No data received", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendQueryToAPI(oldPrompt: String, jsonData: String) {
        val apiUrl = "https://exotic-crab-miserably.ngrok-free.app/generate_image"

        // Create the JSON payload
        val payload = JSONObject()
        payload.put("old_prompt", oldPrompt)
        payload.put("company_name", "KFC") // You can dynamically add values as needed
        payload.put("company_name_position", "top-left")
        val contentJson = JSONObject(jsonData)
        payload.put("content", contentJson.optJSONObject("content"))
        payload.put("content_position", "center")
        payload.put("image_description", contentJson.optString("image_description"))
        payload.put("image_description_position", "None")
        payload.put("text_elements", contentJson.optJSONArray("text_elements"))

        val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), payload.toString())
        val request = Request.Builder()
            .url(apiUrl)
            .post(requestBody)
            .build()

        // Make an asynchronous request
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ImageActivity, "Error fetching image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        handleAPIResponse(responseBody)
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@ImageActivity, "Empty response from server", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@ImageActivity, "Failed to fetch image: ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun handleAPIResponse(responseBody: String) {
        try {
            val responseJson = JSONObject(responseBody)

            // Get the Base64 image string
            val imageBase64 = responseJson.getString("image")

            // Decode Base64 string to Bitmap
            val imageBitmap = decodeBase64ToBitmap(imageBase64)

            // Display the image in the ImageView
            runOnUiThread {
                if (imageBitmap != null) {
                    imageView.setImageBitmap(imageBitmap)
                    Toast.makeText(this, "Image retrieved successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to decode image", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            runOnUiThread {
                Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun decodeBase64ToBitmap(base64String: String): Bitmap? {
        return try {
            val imageByteArray = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(imageByteArray, 0, imageByteArray.size)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            null
        }
    }
}
