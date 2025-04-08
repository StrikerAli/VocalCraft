package com.aliashraf.vocalcraft

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ImageInsertActivity : AppCompatActivity() {

    private val PICK_IMAGES_REQUEST = 100
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ImagesAdapter
    private val imageList = mutableListOf<ImageItem>()
    private val dropboxAccessToken = "sl.u.AFlS6ksXIm7pVE0Hos42W16a2QXZqA1XM17KvX1X-T6EuxFrj60rtA9UZ12LURbqWIN-k7GY305AngidQAOjPx1w4wtKcLPHcHj0LT6TcwL9XM1A3LgTMnfR2m7EpwSOWzESg9Dd_TEctZpQ88vwWGxbWgKPgRAu98On8ORsFj9tyTj4WaVdZJcBKeQzMNfTpjT98YSPtqyy6zViuAa_AlRRH4Tr9TRcCaKJkh01xl6d9jaNw_KCdthbKS8DTTG6yw3PrNQMI6TEaTo8kVrksiyRuTs8CUOGJEYvT-COdmqzTP30cbAaNQVsuhPw9k_Wet8hVE33CURXxL_UaF7W_6dqclVU1yt5Dzxb6kgZYZN56KBEPBikrJ5CyQNEopA-wYzeAwdQ9pBU9hkEnrL6eEcH3An9ZmZ_DF1tBCT7L0LbUwcC2D1Smp0aZy3YqrLXTRoNscD0YMdK93nSopMdYJmHDLFHmJhQeXLVacvosjqYJJYE5PGDL05JRqNSad2Hp4zIi-i117L_X9GmW6f6KJ4Vc-AmIXgh_37FSZlunGTMBS4Z1wuogDCqJfsEZumstAl4hYozr3-QIaZMD55C-idQR4ExnYF6XaHPolRqBbqlGTJRzKFyKLmIxnB8pJrHQsWEZattGId9B9FMtu0nSr0Z0qcNAuqTWOvJjH7kOsdAKajCRhkU9JiNL-jzeAyklWVk1TPfuAlBfD1H7hNXc4DtEVCJ57QamE6qiAa_278-qMLX4cj2O3DrLTfMkVYone0OCAP2dln2kOrFvDkCsw6v1o9gdj3CCBXfthuPf9Skw6-CfF2ertXyj_hhGrlCh0cPIobR0SC6XLUqXewrVDy_ZZk3tYfrSfkr9s0zS5FHLzkpPkPIlX6svP5jcFfUH7WoI1txzcIPtAOTs4a3sq63EndP0nu1w33qmlvmt_q9gvWoRhjd_VtJTKRlykuTtGCbZpwmZ20mkY-Q6YS0w4eVg10dKFJKcOyNwA31v5XK20sGY8kIvTLtKGmM46oN5RApON4o2qVPTuEgG1cHqHu8nZFuGhe8UM2R-dr8jquSCFijWVCuCmWHiSFkt5zT9eu5geNPgQGAO8cSyMWdVPnXAA65XmwdTrTkEpLiLEj_zOtZ6_FLwuWUUSZiQUnp7P5bFahiV0zquUYkT8KYfyG4_2axmUgsVCAAU_PCbS-n1-m2trQuJ8YsdXY76DrjSK1Jy-q9SnNwjM17ch-KI3XMb_1S05oq3tCh2RmaZA_9MpBzXGGRbNH637pf1fkjDXexshePnQuYaWNrbh1hixbHBHfN_wZaSMT1Wg9azfcSWYEsVIfW4KreifeyKBEtVmC0mbAHLgR5cY0fY4s8i8VIqLR8jN9Al6BAKtp4vYG46ngI3UmHTpwg4aN4wsZzzRP87r6i87BtdI2DRCwp--lgY21GheHDIfeNbVPEhnxMJg"
    private val databaseRef = FirebaseDatabase.getInstance().getReference("input_image")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.image_insert)

        recyclerView = findViewById(R.id.imagesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ImagesAdapter(imageList)
        recyclerView.adapter = adapter

        findViewById<Button>(R.id.pickImagesButton).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            startActivityForResult(Intent.createChooser(intent, "Select Images"), PICK_IMAGES_REQUEST)
        }

        findViewById<Button>(R.id.uploadButton).setOnClickListener {
            uploadAllImages()
        }

        findViewById<Button>(R.id.exitButton).setOnClickListener {
            startActivity(Intent(this, PromptActivity::class.java))
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGES_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data?.clipData != null) {
                for (i in 0 until data.clipData!!.itemCount) {
                    val uri = data.clipData!!.getItemAt(i).uri
                    imageList.add(ImageItem(uri, ""))
                }
            } else if (data?.data != null) {
                imageList.add(ImageItem(data.data!!, ""))
            }
            adapter.notifyDataSetChanged()
        }
    }

    private fun uploadAllImages() {
        val dropboxClient = DbxClientV2(DbxRequestConfig.newBuilder("VocalCraftApp").build(), dropboxAccessToken)

        // ✅ Check if all captions are filled
        for (i in imageList.indices) {
            if (imageList[i].caption.isBlank()) {
                runOnUiThread {
                    recyclerView.scrollToPosition(i)
                    val holder = recyclerView.findViewHolderForAdapterPosition(i) as? ImagesAdapter.ImageViewHolder
                    holder?.layout?.findViewById<EditText>(R.id.editCaption)?.apply {
                        requestFocus()
                        Toast.makeText(this@ImageInsertActivity, "Please enter caption for image ${i + 1}", Toast.LENGTH_SHORT).show()
                    }
                }
                return // ✅ Stop upload until captions are filled
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            for (item in imageList) {
                try {
                    val originalBitmap = MediaStore.Images.Media.getBitmap(contentResolver, item.uri)

                    // ✅ Downscale the bitmap if needed
                    val downscaledBitmap = downscaleBitmapIfNeeded(originalBitmap, 1000)

                    val stream = ByteArrayOutputStream()
                    downscaledBitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
                    val inputStream = ByteArrayInputStream(stream.toByteArray())

                    val filename = "image_${System.currentTimeMillis()}.png"
                    dropboxClient.files().uploadBuilder("/$filename").uploadAndFinish(inputStream)

                    val sharedLink = dropboxClient.sharing()
                        .createSharedLinkWithSettings("/$filename").url.replace("?dl=0", "?raw=1")

                    val dataMap = mapOf(
                        "image_name" to filename,
                        "caption" to item.caption,
                        "image_link" to sharedLink
                    )
                    databaseRef.push().setValue(dataMap)

                    withContext(Dispatchers.Main) {
                        Log.d("ImageInsertActivity", "Uploaded image: ${item.caption}")
                        Toast.makeText(this@ImageInsertActivity, "Uploaded image: ${item.caption}", Toast.LENGTH_SHORT).show()
                    }

                    inputStream.close()
                    stream.close()

                } catch (e: Exception) {
                    Log.e("UploadError", e.message ?: "Error uploading")
                    withContext(Dispatchers.Main) {
                        Log.d("ImageInsertActivity", "Failed to upload one image")
                        Toast.makeText(this@ImageInsertActivity, "Failed to upload one image", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // ✅ Clear image list and update UI after all uploads
            withContext(Dispatchers.Main) {
                imageList.clear()
                adapter.notifyDataSetChanged()
                Log.d("ImageInsertActivity", "All images uploaded and cleared")
                Toast.makeText(this@ImageInsertActivity, "All images uploaded and cleared", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun downscaleBitmapIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxDimension && height <= maxDimension) return bitmap // ✅ No downscale needed

        val scaleFactor = if (width > height) {
            maxDimension.toFloat() / width
        } else {
            maxDimension.toFloat() / height
        }

        val newWidth = (width * scaleFactor).toInt()
        val newHeight = (height * scaleFactor).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }



    data class ImageItem(val uri: Uri, var caption: String)

    inner class ImagesAdapter(private val images: List<ImageItem>) :
        RecyclerView.Adapter<ImagesAdapter.ImageViewHolder>() {

        inner class ImageViewHolder(val layout: ViewGroup) : RecyclerView.ViewHolder(layout)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_image_caption, parent, false) as ViewGroup
            return ImageViewHolder(view)
        }

        override fun onBindViewHolder(holder: ImageViewHolder, @SuppressLint("RecyclerView") position: Int) {
            val imageView = holder.layout.findViewById<ImageView>(R.id.imageView)
            val captionEditText = holder.layout.findViewById<EditText>(R.id.editCaption)

            imageView.setImageURI(images[position].uri)
            captionEditText.setText(images[position].caption)

            captionEditText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    images[position].caption = s.toString()
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }
        override fun getItemCount(): Int = images.size
    }
}
