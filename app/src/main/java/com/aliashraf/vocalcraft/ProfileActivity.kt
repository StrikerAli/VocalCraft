package com.aliashraf.vocalcraft

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.view.View
import yuku.ambilwarna.AmbilWarnaDialog
import yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile)

        // Access components using findViewById
        val companyNameEditText: EditText = findViewById(R.id.companyNameEditText)
        val primaryThemePicker: View = findViewById(R.id.primaryThemePicker)
        val secondaryThemePicker: View = findViewById(R.id.secondaryThemePicker)
        val saveButton: Button = findViewById(R.id.saveButton)

        // Example: Set up color picker for the primary theme
        primaryThemePicker.setOnClickListener {
            // Open the color picker dialog for primary theme
            val initialColor = Color.BLACK // Replace with the color you want to start with
            val colorPickerDialog = AmbilWarnaDialog(this, initialColor, object :
                OnAmbilWarnaListener {
                override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                    // Set the selected color to the primary theme picker
                    primaryThemePicker.setBackgroundColor(color)
                }

                override fun onCancel(dialog: AmbilWarnaDialog?) {
                    // Handle the cancel action if needed
                }
            })
            colorPickerDialog.show()
        }

        // Example: Set up color picker for the secondary theme
        secondaryThemePicker.setOnClickListener {
            // Open the color picker dialog for secondary theme
            val initialColor = Color.GRAY // Replace with the color you want to start with
            val colorPickerDialog = AmbilWarnaDialog(this, initialColor, object :
                OnAmbilWarnaListener {
                override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                    // Set the selected color to the secondary theme picker
                    secondaryThemePicker.setBackgroundColor(color)
                }

                override fun onCancel(dialog: AmbilWarnaDialog?) {
                    // Handle the cancel action if needed
                }
            })
            colorPickerDialog.show()
        }

        // Example: Handle Save Button click
        saveButton.setOnClickListener {
            val companyName = companyNameEditText.text.toString()
            if (companyName.isBlank()) {
                // Show an error message
                companyNameEditText.error = "Company Name is required"
            } else {
                // Save the data or perform your logic
                println("Company Name: $companyName")
            }
        }
    }
}
