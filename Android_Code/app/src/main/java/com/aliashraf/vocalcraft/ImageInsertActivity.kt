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
    private val dropboxAccessToken = "sl.u.AFq_SNfuM-N9dpdZFG5mtGRR-vwrJnAqWokMwaJoQbqzLkxBB9yEnW6_oJgrIxbJq5XHJRsiWzlWSD_jHRJVuxYWMYAL5iWIRczfLofDesno0PrPG6mdhprKfZKZo7ipxwDhNce51_cFvAwE0dys_C9dq8B7fKKzrX5s8YkpxOQoIV8cmhPdp1TubDihn-v7PcsPwQ7DtwLdyMg0di5Yfk8D3BXxjxGh1N_snkhFQ2TeQr3cQ_x8EVJSCeuj21h0zA7RQ0VPWxj3i9YLGGIJsSQhYwJz7CdUgzl69ntju3eqL3LR8ye0uK6V9B2fW3EkV0CLsiz3UVbSLUlf7mfNm1wrU1KiwN8hj1AqDawrxi9Qr-c0SKm7QcMXUlwqVxjCVquvBjiT4anYWjw4m0N_gkHfgpgiVLytnm-5JMaxStD6fAuB7EclnT2MAo8U4ocoUyFhFmfK5urqfG59lBwW37OaJRcI2MRj_gQ1G6ZhOdICdEBwFjOWT5fvuelktiVa3XxOlVa1tGugt1hzYcz9dDRYxs_aOkLuSShQVjasThfotELfteBTjkI5sfgdZuztdBBPtwORHXsY3eldgzZE8cD0Nj4nDTX1phTCeiJgh_JLTcBAFEEExRXg72tjTqKtGgVK6t7QnuQTK8ZZ-ddpsly7bIdCcoYHMnnmk1NxWXCGHEMHUADVQmL3DtCPVzVWdZRzgCFC4eHI2VBPNq6ACodWLUkxUgANeqDT-MW9ezXaPqmOrc-woIMJOnoyhUCgHbtZwfzQnocbnicfWvffU1YBAPqUpD6SOqgmTOXoiii6uezgbQmPX51GQGjafVygzXYryAEbVb4cZt3lWqIk_rKp1eUC7rbeddnhZuusctmtqVa9XfDW7oPzur--DFdjiH_eei6UpLJ3vV8YfnsSbdWzyM0uopnMczQYbNQTXKXRnU3-a55mdhRnrNXP8yy9sqRr8z13RTXauvdpaiP5E84TaLHLngTlmUhDW78aUow18wcFNRqfpDZGd7qzh7VhVE8Iv3XzKjXdgtRJwYfUOAYc2zCBGQihSIlGHARGk56Z3ZUDXG1xbBzHGBkT6B0SvGySFtO_2ZOaFyEHulNDxdLasrcM99EORogobwi4_kS0IIOoXoq41v8T0sZPWYAsIKDCq_zzG7W0T4glR5INu9NjP2mS-3K1JgpOHVRo_9QVXRLPgx42FADnswhlLQ0ZdsEW5ayeMoFGbaAtQa4kbY_83YqidbB6XiEKyWuFszIQGhHKt06yaA1n984DpPVqaEIPYTBEJ4mLGniB987EgoPIUmygwA-eDdPvLvyCuhdb3PUzOFvpj_2OacsnMQqsIPEP_UlGYXoDGpo9h3EGTf7bqdSVo-Cq5nr-J1QdB_JP6V4mA2r9R8onvXwk30-_ypZRJbrymkqOEy_1EYqAC7QBChVB71IVzjgRHi-y_7eZFQ"
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
