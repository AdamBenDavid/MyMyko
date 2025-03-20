package com.example.mymyko.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mymyko.R
import com.example.mymyko.data.models.Post
import java.io.File

class UserPostsAdapter(
  private val postList: MutableList<Post>,
  private val context: Context
) : RecyclerView.Adapter<UserPostsAdapter.UserPostViewHolder>() {

  class UserPostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val postImage: ImageView = view.findViewById(R.id.post_image)
    val postDescription: TextView = view.findViewById(R.id.post_description)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserPostViewHolder {
    val view = LayoutInflater.from(parent.context)
      .inflate(R.layout.item_user_post, parent, false)
    return UserPostViewHolder(view)
  }

  override fun onBindViewHolder(holder: UserPostViewHolder, position: Int) {
    val post = postList[position]
    // טעינת תמונת הפוסט
    Glide.with(context)
      .load(post.image_path)
      .into(holder.postImage)
    // הצגת תיאור הפוסט
    holder.postDescription.text = post.description
  }

  override fun getItemCount(): Int = postList.size
}
