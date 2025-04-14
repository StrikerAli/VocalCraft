package com.aliashraf.vocalcraft

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

class NERActivity : AppCompatActivity() {

    private lateinit var dynamicFormContainer: LinearLayout
    private lateinit var submitButton: Button
    private val editTexts = mutableListOf<EditText>() // To hold references to EditTexts
    private var emptyCounter = 0
    private lateinit var database: DatabaseReference
    private var itemNameEditText: EditText? = null
    private var fontSizeEditText: EditText? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.llm_form)

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance().getReference("prompts")

        // Find the container layout and submit button in the XML
        dynamicFormContainer = findViewById(R.id.dynamic_form_container)
        submitButton = findViewById(R.id.submitButton)

        // Get the JSON string passed through the Intent
        val jsonString = intent.getStringExtra("json_data")
        val promptData = intent.getStringExtra("prompt_data")
        Log.d("MainActivity", "Prompt data: $promptData")
        Log.d("MainActivity", "JSON Data: $jsonString")
        submitButton.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FF8C00"))

        // Check if the JSON string is not null or empty
        if (!jsonString.isNullOrEmpty()) {
            val jsonObject = JSONObject(jsonString)

            // Recursively generate views from the JSON object
            generateViewsFromJson(jsonObject, dynamicFormContainer)

            // Set up the submit button click listener
            submitButton.setOnClickListener {
                if (submitButton.isEnabled) {
                    submitButton.isEnabled = false // Disable the button
                    handleSubmit(jsonObject, promptData)

                    // Re-enable the button after 3 seconds
                    submitButton.postDelayed({
                        submitButton.isEnabled = true
                    }, 3000)
                }
            }

        } else {
            // Handle the case where no JSON data was passed
            Toast.makeText(this, "No data available", Toast.LENGTH_SHORT).show()
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

                    val formattedKey = key.replace("_", " ")

                    if (value is JSONObject || value is JSONArray) {
                        val textView = TextView(this)
                        textView.layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        textView.text = formattedKey
                        textView.textSize = 18f
                        textView.setPadding(0, 16, 0, 8)
                        parentView.addView(textView)

                        val nestedLayout = LinearLayout(this)
                        nestedLayout.orientation = LinearLayout.VERTICAL
                        parentView.addView(nestedLayout)

                        generateViewsFromJson(value, nestedLayout)
                    } else {
                        // ✅ Check for empty/null/blank value before adding views
                        if ((value == JSONObject.NULL) || (value is String && value.isBlank())) {
                            // Skip adding TextView and EditText
                            continue
                        }

                        val textView = TextView(this)
                        textView.layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        textView.text = formattedKey
                        textView.textSize = 16f
                        parentView.addView(textView)

                        val editText = EditText(this)
                        editText.layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        editText.setPadding(0, 0, 0, 16)
                        editText.hint = formattedKey
                        editText.setTextColor(ContextCompat.getColor(this, R.color.black))
                        editText.setHintTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
                        editText.background = resources.getDrawable(R.drawable.rounded_edittext_noborder)
                        editText.setPadding(24, 12, 12, 12)
                        if (key == "name" && itemNameEditText == null) {
                            itemNameEditText = editText
                            Log.d("NERActivity", "Item name EditText initialized: $itemNameEditText")
                        }
                        val formattedKey = key.replace("_", " ")
                        if (formattedKey.lowercase() == "font size" && fontSizeEditText == null) {
                            Log.d("NERActivity", "Font size EditText initialized: $editText")
                            fontSizeEditText = editText
                        }

                        // Add to the list of EditTexts
                        editTexts.add(editText)

                        if (value is String && value.isNotEmpty()) {
                            editText.setText(value)
                        } else if (value is Int) {
                            editText.setText(value.toString())
                        } else if (value is Double) {
                            editText.setText(value.toString())
                        } else {
                            val emptyId = emptyCounter
                            editText.id = emptyId
                            emptyCounter++
                        }

                        parentView.addView(editText)
                    }
                }
            }

            is JSONArray -> {
                for (i in 0 until jsonElement.length()) {
                    val item = jsonElement.get(i)
                    generateViewsFromJson(item, parentView)
                }
            }
        }
    }


    // Handle submit button click
    private var isSecondPress = false // Track if this is the second press

    // Handle submit button click
    private fun handleSubmit(jsonObject: JSONObject, promptData: String?) {
        var firstEmptyEditText: EditText? = null
        var allFilled = true

        val emptyFields = mutableListOf<String>()
        itemNameEditText?.let { editText ->
            val updatedName = editText.text.toString()
            try {
                val ner = jsonObject.getJSONObject("content")
                val items = ner.getJSONArray("items")
                if (items.length() > 0) {
                    Log.d("NERActivity", "Found Item: ${items.getJSONObject(0)}")
                    items.getJSONObject(0).put("name", updatedName)
                    Log.d("NERActivity", "Updated item name: $updatedName")
                } else {

                }
            } catch (e: Exception) {
                Log.e("NERActivity", "Failed to update item name: ${e.message}")
            }
        }
        fontSizeEditText?.let { editText ->
            val updatedFontSize = editText.text.toString()
            try {
                val textArray = jsonObject.getJSONArray("text_elements")
                if (textArray.length() > 0) {
                    val textItem = textArray.getJSONObject(0)
                    textItem.put("font_size", updatedFontSize)
                    Log.d("NERActivity", "Updated font size: $updatedFontSize")
                } else {

                }
            } catch (e: Exception) {
                Log.e("NERActivity", "Failed to update font size: ${e.message}")
            }
        }


        // Replace nulls in the jsonObject with "None" if editText is empty
        var updatedJson = JSONObject(jsonObject.toString())

        for (i in 0 until emptyCounter) {
            val editText = findViewById<EditText>(i)
            val label = editTexts[i].hint?.toString() ?: "Field $i"

            if (editText.text.isEmpty()) {
                if (!isSecondPress) {
                    allFilled = false
                    emptyFields.add(label)
                    if (firstEmptyEditText == null) {
                        firstEmptyEditText = editText
                    }
                } else {
                    // Optional: You can handle second press logic if needed
                }
            } else {
                // Replace "null" placeholders if necessary
            }
        }

        if (!allFilled && !isSecondPress) {
            Toast.makeText(
                this,
                "Please fill the following fields: ${emptyFields.joinToString(", ")}",
                Toast.LENGTH_LONG
            ).show()
            firstEmptyEditText?.requestFocus()
            scrollToView(firstEmptyEditText)
            isSecondPress = true
        } else {
            // Now build the correct NER structure
            val nerObject = JSONObject()
            val finalObject = JSONObject()

            // Move only the NER fields inside "ner"
            val nerKeys = jsonObject.keys()
            while (nerKeys.hasNext()) {
                val key = nerKeys.next()
                if (key != "old_prompt" && key != "base_image_url") {
                    // Replace nulls with "None"
                    val value = jsonObject.get(key)
                    nerObject.put(key, processNulls(value))
                }
            }

            // Add old_prompt and base_image_url outside ner
            finalObject.put("ner", nerObject)
            finalObject.put("old_prompt", jsonObject.optString("old_prompt", ""))
            finalObject.put("base_image_url", jsonObject.optString("base_image_url", ""))

            Log.d("NERActivity", "Final JSON: $finalObject")

            // Save to Firebase
            savePromptToFirebase(promptData, finalObject.toString())

            Toast.makeText(this, "Form Submitted. JSON: $finalObject", Toast.LENGTH_LONG).show()

            lifecycleScope.launch {
                val imageLink = getImageLinkMatchingCaption(finalObject.toString())
                if (imageLink != null) {
                    val intent = Intent(this@NERActivity, SvgActivity::class.java)
                    intent.putExtra("json_data", finalObject.toString())
                    intent.putExtra("prompt_data", promptData)
                    intent.putExtra("image_link", imageLink)
                    startActivity(intent)
                } else {
                    Log.e("MainActivity", "No matching image link found.")
                }
            }
        }
    }

    // Recursively replace nulls with "None"
    private fun processNulls(value: Any?): Any? {
        return when (value) {
            JSONObject.NULL -> "None"
            is JSONObject -> {
                val obj = JSONObject()
                val keys = value.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    obj.put(k, processNulls(value.get(k)))
                }
                obj
            }
            is JSONArray -> {
                val arr = JSONArray()
                for (i in 0 until value.length()) {
                    arr.put(processNulls(value.get(i)))
                }
                arr
            }
            else -> value
        }
    }



    // Save the prompt to Firebase
    private fun savePromptToFirebase(promptData: String?, jsonString: String) {
        if (promptData != null) {
            // Query to check if a prompt with the same promptText exists
            database.orderByChild("promptText").equalTo(promptData).get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        // If it exists, get the existing prompt key
                        val existingPromptKey = snapshot.children.first().key
                        // Update the nerText for the existing prompt
                        existingPromptKey?.let {
                            database.child(it).child("nerText").setValue(jsonString)
                                .addOnSuccessListener {
                                    Log.d("MainActivity", "NerText updated successfully.")
                                } // success
                                .addOnFailureListener { e ->
                                    Log.e("MainActivity", "Failed to update NerText: ${e.message}")
                                }
                        }
                    } else {
                        // If it doesn't exist, create a new prompt
                        val prompt = Prompt(promptText = promptData, nerText = jsonString)
                        database.push().setValue(prompt)
                            .addOnSuccessListener {
                                // Success
                                Log.d("MainActivity", "New prompt saved successfully.")
                            }
                            .addOnFailureListener { e ->
                                // Failure
                                Log.e("MainActivity", "Failed to save new prompt: ${e.message}")
                            }
                    }
                }
                .addOnFailureListener { e ->
                    // Error querying the database
                    Log.e("MainActivity", "Error querying database: ${e.message}")
                }
        } else {
            Log.e("MainActivity", "Prompt data is null.")
        }
    }
    fun getFirstItemName(jsonString: String): String? {
        return try {
            Log.d("NERActivity", "JSON for getting item name from: $jsonString")
            val jsonObject = JSONObject(jsonString)
            val content = jsonObject.getJSONObject("content")
            val items = content.getJSONArray("items")
            if (items.length() > 0) {
                val firstItem = items.getJSONObject(0)
                Log.d("NERActivity", "First item name: $firstItem")
                firstItem.getString("name")
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    suspend fun getImageLinkMatchingCaption(jsonString: String): String? {
        // Extract first item's name from the new JSON format
        val firstItemName = try {
            val jsonObject = JSONObject(jsonString)
            val nerObject = jsonObject.getJSONObject("ner")
            val contentObject = nerObject.getJSONObject("content")
            val itemsArray = contentObject.getJSONArray("items")
            if (itemsArray.length() > 0) {
                val firstItem = itemsArray.getJSONObject(0)
                Log.d("NERActivity", "First item name: $firstItem")
                firstItem.optString("name", null)?.lowercase()
            } else null
        } catch (e: Exception) {
            null
        }

        if (firstItemName == null) return null

        val database = FirebaseDatabase.getInstance().reference.child("input_image")

        return try {
            val snapshot = database.get().await()
            for (imageSnapshot in snapshot.children) {
                val caption = imageSnapshot.child("caption").getValue(String::class.java)?.lowercase()?.trimEnd()
                val imageLink = imageSnapshot.child("image_link").getValue(String::class.java)

                if (caption == firstItemName) {
                    Log.d("NERActivity", "Matching caption found: $caption")
                    return imageLink // Return the matching image link
                }
            }
            null // No match found
        } catch (e: Exception) {
            null // In case of error
        }
    }




    // Scroll to the specified view
    private fun scrollToView(view: EditText?) {
        view?.let {
            // Get the ScrollView
            val scrollView = findViewById<ScrollView>(R.id.scroll_view) // Get the ScrollView
            val y = it.y.toInt() + it.height // Get the y position of the EditText, plus its height
            scrollView.smoothScrollTo(0, y + 80) // Smooth scroll to the position
        }
    }
}
