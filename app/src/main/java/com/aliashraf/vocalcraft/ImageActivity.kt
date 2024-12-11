package com.aliashraf.vocalcraft

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*

class ImageActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var saveButton: Button
    private var savedBitmap: Bitmap? = null
    private val client = OkHttpClient()

    // Dropbox setup
    private val dropboxAccessToken = "sl.u.AFZoPN84FR5clAQAkJZgNbI2jgMFN8uaNAFKmUjlcO5dUiPmKsvJFGf8qSCdqOG2nGqNuCmZUKza5djQPTGlsFZejga7sOUh2Sr4RT1wicg640D6qBolvfC8uYDYHFFwbzeulVt39uriZYRncXtYmr3PvYPmKBzOSAcb-j2E0cMkFlnQI51b4E_ryH8Kh0PzAYJWLAJtNsyyMAQoDMCtKMtXNfVCSLKLnNoZiSdLsLwImAVGMnPCmrRsol2ksYFBCNHl5u6rX9l3K3tnpZ4Vp1tgVUZWe7BST4-nm8je3GhvABFdsn_DKzpcMXs8lzQBffFuS-p3aYBGyqm5lMGep6LmQrmf9hIJmiFIo3TOROVNx6IHC2owc8nYKaDrwFKIka_inbuRwFt5Z8m0Q-J-MyBtZ1DsrTc-JVCmh8j3rPNKZ5lCbhfVyX4Xi57Ijs4i5a0rXwnJwUpS5wuEQwFm-qU-GX5JIavaTmyae5r5h5gnC9oDWcrodD6QydEdVHqSzG2X-t2TNr_kyOXhFWYHBgopdw133Zz4szNLvTdJnpld_Ihulh4woQF6a-GShKQKl1dgNu2qoZco0a4-9eVX5Cw2yKYdFaRreaw4TRqjbRqQTt22Cj-yu-IhNR2yccDJDRoPzEBkPEcTXqoglHJ8dVLuADt6NZ1p84QV75SfPbRPtXq5awPe4Z5rXqrOuob-8UbJ2bjEzLky7DcZ5AaM5DK267Is9MKS6asSvAp9DHgykUsYIftuA0X4lgvEXc055BAHiNeeMkAAdCa1xiQU8fJyj4i4qI-FhVMFGFu95GKiOcXycDGZGNf_-J3tsjoahTWZjuyFEdPoGvHwBbbCKIg6O1N2CKUljSJhJ3EMUz4VxyfPVMZdAF726eptx4prm3k1dfJqoqY-YWuFWWQtWwZr0C9ir7Bx4T3daCizSHMHyfAdPAMlhfMZ8j0J82SUgLr97q02HJsTKKDdjrI26aO6Yi9ZnPsZmrjSdaqrBjQ8DK46AOJSmNFu4Lzg3b9PLe2CNDJslPl7kSVfM1hG-asl2lYkl9_mIJfzfzLEUPM3kcX08TJqegmTeXOriJBjzF5W_-BfsglQI8axjA83WIkQhTZmSC6Kx3PCD2hPjzsIRaMu5sw7ncNCKEp5eQ-Wata-UJk5Hi-Q5dilQaYthWdCnR5ya66ufPCYO2Te8BQ-oWmMHZhQfzut4QDuotDNz61DJic9AR9xlzHgBhqH9QyPwk9vva1blgrJu_L72b2FDzuHW62Sl-lKhZ-k4y1q0sOLsNaBnS-GEJ11UsIY56zFlXCXyofNVdDh9nQoVaqAphOM1T1AzQLUbdipvLcWhN465WQZpV8UR6-j_vktdjynClLItIWYcpSJu6w3B61wOVmmJx1PMflsf_DJLwqC6guJahTmLpC5qh5OFxwKdt3e7MfpyIJ33vQvybMjDK-Lew" // Replace with your Dropbox access token

    // Request code for permissions
    private val REQUEST_CODE_STORAGE_PERMISSION = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.imageview)

        imageView = findViewById(R.id.largeImageView)
        saveButton = findViewById(R.id.saveButton)

        Glide.with(this)
            .asGif() // Load as GIF
            //.load(R.drawable.loadingimage) // You can also use a URL or Base64
            .load("https://media.giphy.com/media/3oEjI6SIIHBdRxXI40/giphy.gif")
            .into(imageView)

        val jsonData = intent.getStringExtra("json_data")
        val promptData = intent.getStringExtra("prompt_data")

        if (jsonData != null && promptData != null) {
            sendQueryToAPI(promptData, jsonData)
        } else {
            Toast.makeText(this, "No data received", Toast.LENGTH_SHORT).show()
        }

        saveButton.setOnClickListener {
            savedBitmap?.let {
                if (checkStoragePermission()) {
                    // Generate a random filename
                    val randomFilename = generateRandomFilename()

                    // Launch a coroutine to handle the background tasks
                    CoroutineScope(Dispatchers.Main).launch {
                        val isGallerySaved = saveImageToGallery(it, randomFilename)
                        val isDropboxSaved = saveImageToDropbox(it, randomFilename)
                                                                                                // Notify the user based on the results
                        if (isGallerySaved && isDropboxSaved) {
                            Toast.makeText(this@ImageActivity, "Image saved successfully to Gallery and Dropbox!", Toast.LENGTH_SHORT).show()
                        } else if (!isGallerySaved) {
                            Toast.makeText(this@ImageActivity, "Failed to save image to Gallery", Toast.LENGTH_SHORT).show()
                        } else if (!isDropboxSaved) {
                            Toast.makeText(this@ImageActivity, "Failed to save image to Dropbox", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    requestStoragePermission()
                }
            } ?: Toast.makeText(this, "No image to save", Toast.LENGTH_SHORT).show()
        }


    }

    private fun generateRandomFilename(): String {
        val uuid = UUID.randomUUID().toString()
        return "Image_$uuid.png" // Use UUID as part of the filename
    }

    private fun sendQueryToAPI(oldPrompt: String, jsonData: String) {
        val apiUrl = "https://exotic-crab-miserably.ngrok-free.app/generate_image" //API URL for generating images

        val payload = JSONObject()
        payload.put("old_prompt", oldPrompt)
        payload.put("company_name", "KFC")
        payload.put("company_name_position", "top-left")
        val contentJson = JSONObject(jsonData)
        payload.put("content", contentJson.optJSONObject("content"))
        payload.put("content_position", "center")
        payload.put("image_description", contentJson.optString("image_description"))
        payload.put("image_description_position", "None")
        payload.put("text_elements", contentJson.optJSONArray("text_elements"))

        val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), payload.toString())
        Log.d("Request Body", payload.toString())
        val request = Request.Builder()
            .url(apiUrl)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback { // Make an asynchronous request
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ImageActivity, "Error fetching image: ${e.message}", Toast.LENGTH_SHORT).show() // Notify the user on the main thread
                }
            }

            override fun onResponse(call: Call, response: Response) { // Handle the response
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        handleAPIResponse(responseBody)
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@ImageActivity, "Empty response from server", Toast.LENGTH_SHORT).show() // Notify the user on the main thread
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@ImageActivity, "Failed to fetch image: ${response.message}", Toast.LENGTH_SHORT).show() // Notify the user on the main thread
                    }
                }
            }
        })
    }

    private fun handleAPIResponse(responseBody: String) {
        try {
            val responseJson = JSONObject(responseBody)
            val imageBase64 = responseJson.getString("image")
            val imageBitmap = decodeBase64ToBitmap(imageBase64)

            runOnUiThread {
                if (imageBitmap != null) {
                    savedBitmap = imageBitmap
                    imageView.setImageBitmap(imageBitmap)
                    Toast.makeText(this, "Image retrieved successfully", Toast.LENGTH_SHORT).show() // Notify the user on the main thread
                } else {
                    Toast.makeText(this, "Failed to decode image", Toast.LENGTH_SHORT).show() // Notify the user on the main thread
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            runOnUiThread {
                Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show() // Notify the user on the main thread
            }
        }
    }

    private fun saveImageToGallery(bitmap: Bitmap, fileName: String): Boolean { // Save the image to the Gallery
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES) // Save to the Pictures directory
            }

            val resolver = contentResolver
            val uri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                val outputStream = resolver.openOutputStream(it)
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream) // Compress the image
                }
                outputStream?.close()
                return true
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun saveImageToDropbox(bitmap: Bitmap, fileName: String): Boolean { //
        // Use a coroutine to run the network operation in the background
        return withContext(Dispatchers.IO) {
            try {
                // Configure Dropbox client
                val appKey = "your_app_key"
                val appSecret = "your_app_secret"
                val config = DbxRequestConfig.newBuilder("vocalcraft-app").build()

                // Use the access token for authorization
                val client = DbxClientV2(config, dropboxAccessToken)

                // Convert bitmap to input stream
                val outputStream = ByteArrayOutputStream()
                val compressSuccess = bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                Log.d("DropboxUpload", "Bitmap compression successful: $compressSuccess")

                val byteArray = outputStream.toByteArray()
                Log.d("DropboxUpload", "Byte array size: ${byteArray.size}")

                val inputStream = ByteArrayInputStream(byteArray)

                // Upload the file to Dropbox
                val metadata = client.files().uploadBuilder("/$fileName")
                    .uploadAndFinish(inputStream)

                Log.d("DropboxUpload", "File uploaded successfully: ${metadata.pathLower}")

                // Notify the user on the main thread
                withContext(Dispatchers.Main) {
                }

                true // Return true on success
            } catch (e: Exception) {
                Log.e("DropboxUpload", "Failed to upload to Dropbox: ${e.message}", e)

                // Notify the user on the main thread
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ImageActivity, "Failed to upload image to Dropbox", Toast.LENGTH_SHORT).show()
                }
                false
            }
        }
    }






    private fun decodeBase64ToBitmap(base64String: String): Bitmap? { // Decode a Base64 string to a Bitmap
        return try {
            val imageByteArray = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(imageByteArray, 0, imageByteArray.size)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            null
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
                    val isGallerySaved = saveImageToGallery(it, "GeneratedImage.png")
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

}
