package com.aliashraf.vocalcraft

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject

class TemplateActivity : AppCompatActivity() {

    private lateinit var dynamicFormContainer: LinearLayout

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.template_form)

        // Find the container layout in the XML
        dynamicFormContainer = findViewById(R.id.dynamic_form_container)

        // Custom JSON string
        val jsonString = """
            {
                "Size": {
                    "Width": 800,
                    "Height": 600
                },
                "Type": "Menu",
                "Pages": [
                    {
                        "Page": "1",
                        "Company_name": "Ali's Diner",
                        "Motto": "Delicious Food, Great Service",
                        "Content": [
                            {
                                "Main_heading": "Starters",
                                "Sub_heading": "Appetizers to kick off your meal",
                                "Items": [
                                    {
                                        "Name": "Garlic Bread",
                                        "Price": 5.99
                                    },
                                    {
                                        "Name": "Stuffed Mushrooms",
                                        "Price": 7.99
                                    },
                                    {
                                        "Name": "Chicken Wings",
                                        "Price": 8.99
                                    },
                                    {
                                        "Name": "Bruschetta",
                                        "Price": 6.99
                                    },
                                    {
                                        "Name": "Mozzarella Sticks",
                                        "Price": 6.49
                                    }
                                ]
                            }
                        ],
                        "Image_description": "Image of starters"
                    },
                    {
                        "Page": "2",
                        "Company_name": "Ali's Diner",
                        "Motto": "Delicious Food, Great Service",
                        "Content": [
                            {
                                "Main_heading": "Main Courses",
                                "Sub_heading": "Hearty meals for everyone",
                                "Items": [
                                    {
                                        "Name": "Cheeseburger",
                                        "Price": 10.99,
                                        "Description": "Juicy beef burger with cheese"
                                    }
                                ]
                            }
                        ],
                        "Image_description": "Image of main courses"
                    }
                ]
            }
        """.trimIndent()

        // Check if the JSON string is not null or empty
        if (jsonString.isNotEmpty()) {
            val jsonObject = JSONObject(jsonString)

            // Recursively generate views from the JSON object
            generateViewsFromJson(jsonObject, dynamicFormContainer)
        } else {
            // Handle the case where no JSON data was provided
            Log.e("TemplateActivity", "No data available")
        }
    }

    // Recursive function to generate views from JSON
    @SuppressLint("UseCompatLoadingForDrawables")
    private fun generateViewsFromJson(jsonElement: Any, parentView: LinearLayout) {
        when (jsonElement) {
            is JSONObject -> {
                val iterator = jsonElement.keys()
                while (iterator.hasNext()) {
                    val key = iterator.next()
                    val value = jsonElement.get(key)

                    // Replace underscores with spaces in the key
                    val formattedKey = key.replace("_", " ")

                    if (value is JSONObject || value is JSONArray) {
                        // Create a TextView for the parent (if it's an object or array)
                        val textView = TextView(this)
                        textView.layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        textView.text = formattedKey // Use formatted key
                        textView.textSize = 18f
                        textView.setPadding(0, 16, 0, 8)
                        parentView.addView(textView)

                        // Create a new nested LinearLayout to hold child elements
                        val nestedLayout = LinearLayout(this)
                        nestedLayout.orientation = LinearLayout.VERTICAL
                        parentView.addView(nestedLayout)

                        // Recursively handle child elements
                        generateViewsFromJson(value, nestedLayout)
                    } else {
                        // Create a TextView for the key (parent node)
                        val textView = TextView(this)
                        textView.layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        textView.text = formattedKey // Use formatted key
                        textView.textSize = 16f
                        parentView.addView(textView)

                        // Create an EditText for the value (leaf node)
                        val editText = EditText(this)
                        editText.layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        editText.setPadding(0, 0, 0, 16)
                        editText.hint = formattedKey
                        editText.setTextColor(ContextCompat.getColor(this, R.color.black)) // Set text color
                        editText.setHintTextColor(ContextCompat.getColor(this, android.R.color.darker_gray)) // Set hint text color
                        editText.background = resources.getDrawable(R.drawable.rounded_edittext_noborder) // Set background
                        editText.setPadding(24, 12, 12, 12) // Set padding
                        editText.isFocusable = false
                        editText.isFocusableInTouchMode = false
                        editText.inputType = android.text.InputType.TYPE_NULL // Prevent keyboard from appearing


                        // Add to the list of EditTexts

                        // Set existing value in the EditText if present, else set empty
                        if (value is String && value.isNotEmpty()) {
                            editText.setText(value)
                        } else if (value is Int) {
                            editText.setText(value.toString())
                        } else if (value is Double) {
                            editText.setText(value.toString())
                        }

                        parentView.addView(editText)
                    }
                }
            }
            is JSONArray -> {
                // Handle array elements
                for (i in 0 until jsonElement.length()) {
                    val item = jsonElement.get(i)

                    // Create a TextView for array index or label
                    val textView = TextView(this)
                    textView.layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    textView.textSize = 16f
                    textView.setPadding(0, 16, 0, 8)
                    parentView.addView(textView)

                    // Recursively process each element in the array
                    generateViewsFromJson(item, parentView)
                }
            }
        }
    }
}
