package com.aliashraf.vocalcraft

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class ImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Link the XML layout
        setContentView(R.layout.imageview)
    }
}
