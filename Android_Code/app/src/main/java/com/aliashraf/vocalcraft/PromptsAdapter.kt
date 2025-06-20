package com.aliashraf.vocalcraft

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.text.TextUtils
import com.aliashraf.vocalcraft.R

class PromptsAdapter(
    private val prompts: List<Prompt>,
    private val onPromptClick: (String) -> Unit // Lambda function to handle prompt click
) : RecyclerView.Adapter<PromptsAdapter.PromptViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PromptViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_view_item, parent, false)
        return PromptViewHolder(view)
    }

    // Bind the data to the ViewHolder
    override fun onBindViewHolder(holder: PromptViewHolder, position: Int) {
        val prompt = prompts[position]
        holder.bind(prompt)
    }

    // Return the number of items in the list
    override fun getItemCount(): Int = prompts.size

    inner class PromptViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val promptTextView: TextView = itemView.findViewById(R.id.promptTextView)

        fun bind(prompt: Prompt) {
            promptTextView.text = prompt.promptText

            // Set the maximum number of lines to 3 and add ellipsis if the text is longer
            promptTextView.maxLines = 3
            promptTextView.ellipsize = TextUtils.TruncateAt.END

            itemView.setOnClickListener {
                onPromptClick(prompt.promptText) // Call the lambda when the prompt is clicked
            }
        }
    }
}

