package com.example.mymyko.adapters

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mymyko.R
import com.example.mymyko.UpdateImageActivity
import com.example.mymyko.data.models.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File

class ProfilePostAdapter(private val postList: MutableList<Post>, private val context: Context) :
  RecyclerView.Adapter<ProfilePostAdapter.ProfilePostViewHolder>() {

  //  view holder- holds 1 line view
  class ProfilePostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val postImage: ImageView = view.findViewById(R.id.post_image_profile)
    val postDescription: TextView = view.findViewById(R.id.post_description_profile)
    val likeButton: ImageView = view.findViewById(R.id.like_button_profile)
    val likeCount: TextView = view.findViewById(R.id.like_count_profile)
    val deletePost: Button = view.findViewById(R.id.delete_button)
    val editButton: Button = view.findViewById(R.id.edit_button)
  }

  // onCreateViewHolder- create view for 1 line
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfilePostViewHolder {
    val view = LayoutInflater.from(parent.context)
      .inflate(R.layout.item_post_profile, parent, false)
    return ProfilePostViewHolder(view)
  }

  // on bind view- fill the view in data (from firebase)
  override fun onBindViewHolder(holder: ProfilePostViewHolder, position: Int) {
    val post = postList[position]
    val currentUser = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // move to edit image fragment
    holder.editButton.setOnClickListener {
      val intent = Intent(context, UpdateImageActivity::class.java)
      intent.putExtra("post", post)
      context.startActivity(intent)
      Log.d("ProfilePostAdapter", "Edit button clicked - launching UpdateImageActivity")
    }

    // load post's picture from local path (image_path)
    Glide.with(holder.itemView.context)
      .load(post.image_path)
      .into(holder.postImage)

    // post description
    holder.postDescription.text = post.description

    // show like count
    holder.likeCount.text = post.likes.toString()
    updateLikeUI(holder, post, currentUser)

    // on like click
    holder.likeButton.setOnClickListener {
      toggleLike(holder, post, currentUser)
    }

    // delete post (only the post owner)
    holder.deletePost.setOnClickListener {
      FirebaseFirestore.getInstance().collection("posts").document(post.id).delete()
        .addOnSuccessListener {
          Toast.makeText(context, "Post Deleted!", Toast.LENGTH_SHORT).show()
          postList.removeAt(position)
          notifyItemRemoved(position)
        }
        .addOnFailureListener {
          Toast.makeText(context, "Failed to delete post", Toast.LENGTH_SHORT).show()
        }
    }
  }

  // items count
  override fun getItemCount(): Int = postList.size

  // update like- UI
  private fun updateLikeUI(holder: ProfilePostViewHolder, post: Post, userId: String) {
    val isLiked = post.likedUsers.contains(userId)
    holder.likeButton.setImageResource(
      if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
    )
    holder.likeCount.text = post.likes.toString()
  }

  // on like\ unlike
  private fun toggleLike(holder: ProfilePostViewHolder, post: Post, userId: String) {
    val db = FirebaseFirestore.getInstance().collection("posts").document(post.id)
    val isLiked = post.likedUsers.contains(userId)

    if (isLiked) {
      post.likedUsers.remove(userId)
      post.likes -= 1
    } else {
      post.likedUsers.add(userId)
      post.likes += 1
    }

    db.update("likes", post.likes, "likedUsers", post.likedUsers)
      .addOnSuccessListener {
        updateLikeUI(holder, post, userId)
      }
  }
}
