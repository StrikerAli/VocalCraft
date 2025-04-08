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
    private val dropboxAccessToken = "sl.u.AFlS6ksXIm7pVE0Hos42W16a2QXZqA1XM17KvX1X-T6EuxFrj60rtA9UZ12LURbqWIN-k7GY305AngidQAOjPx1w4wtKcLPHcHj0LT6TcwL9XM1A3LgTMnfR2m7EpwSOWzESg9Dd_TEctZpQ88vwWGxbWgKPgRAu98On8ORsFj9tyTj4WaVdZJcBKeQzMNfTpjT98YSPtqyy6zViuAa_AlRRH4Tr9TRcCaKJkh01xl6d9jaNw_KCdthbKS8DTTG6yw3PrNQMI6TEaTo8kVrksiyRuTs8CUOGJEYvT-COdmqzTP30cbAaNQVsuhPw9k_Wet8hVE33CURXxL_UaF7W_6dqclVU1yt5Dzxb6kgZYZN56KBEPBikrJ5CyQNEopA-wYzeAwdQ9pBU9hkEnrL6eEcH3An9ZmZ_DF1tBCT7L0LbUwcC2D1Smp0aZy3YqrLXTRoNscD0YMdK93nSopMdYJmHDLFHmJhQeXLVacvosjqYJJYE5PGDL05JRqNSad2Hp4zIi-i117L_X9GmW6f6KJ4Vc-AmIXgh_37FSZlunGTMBS4Z1wuogDCqJfsEZumstAl4hYozr3-QIaZMD55C-idQR4ExnYF6XaHPolRqBbqlGTJRzKFyKLmIxnB8pJrHQsWEZattGId9B9FMtu0nSr0Z0qcNAuqTWOvJjH7kOsdAKajCRhkU9JiNL-jzeAyklWVk1TPfuAlBfD1H7hNXc4DtEVCJ57QamE6qiAa_278-qMLX4cj2O3DrLTfMkVYone0OCAP2dln2kOrFvDkCsw6v1o9gdj3CCBXfthuPf9Skw6-CfF2ertXyj_hhGrlCh0cPIobR0SC6XLUqXewrVDy_ZZk3tYfrSfkr9s0zS5FHLzkpPkPIlX6svP5jcFfUH7WoI1txzcIPtAOTs4a3sq63EndP0nu1w33qmlvmt_q9gvWoRhjd_VtJTKRlykuTtGCbZpwmZ20mkY-Q6YS0w4eVg10dKFJKcOyNwA31v5XK20sGY8kIvTLtKGmM46oN5RApON4o2qVPTuEgG1cHqHu8nZFuGhe8UM2R-dr8jquSCFijWVCuCmWHiSFkt5zT9eu5geNPgQGAO8cSyMWdVPnXAA65XmwdTrTkEpLiLEj_zOtZ6_FLwuWUUSZiQUnp7P5bFahiV0zquUYkT8KYfyG4_2axmUgsVCAAU_PCbS-n1-m2trQuJ8YsdXY76DrjSK1Jy-q9SnNwjM17ch-KI3XMb_1S05oq3tCh2RmaZA_9MpBzXGGRbNH637pf1fkjDXexshePnQuYaWNrbh1hixbHBHfN_wZaSMT1Wg9azfcSWYEsVIfW4KreifeyKBEtVmC0mbAHLgR5cY0fY4s8i8VIqLR8jN9Al6BAKtp4vYG46ngI3UmHTpwg4aN4wsZzzRP87r6i87BtdI2DRCwp--lgY21GheHDIfeNbVPEhnxMJg"



    // Request code for permissions
    private val REQUEST_CODE_STORAGE_PERMISSION = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.imageview)

        imageView = findViewById(R.id.largeImageView)
        saveButton = findViewById(R.id.saveButton)

        Glide.with(this)
            .asGif() // Load as GIF
            .load(R.drawable.loadingimage) // You can also use a URL or Base64
            .into(imageView)

        val jsonData = intent.getStringExtra("json_data")
        val promptData = intent.getStringExtra("prompt_data")

        if (jsonData != null && promptData != null) {
            sendQueryToAPI(jsonData, promptData)
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

    private fun sendQueryToAPI(jsonData: String, promptData: String) {
        val apiUrl = "https://exotic-crab-miserably.ngrok-free.app/generate_image" // API URL for generating images

        // Parse the jsonData into a JSONObject
        Log.d("Original_Request_Body", jsonData)
        val jsonObject = JSONObject(jsonData)

        // Add promptData under the key "old_prompt"
        jsonObject.put("old_prompt", promptData)

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

        // Make the asynchronous request
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
