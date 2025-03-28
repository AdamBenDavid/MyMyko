package com.example.mymyko.adapters

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mymyko.R
import com.example.mymyko.UserProfileActivity
import com.example.mymyko.data.models.Comment
import com.example.mymyko.data.models.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

class PostAdapter(
  private val postList: MutableList<Post>,
  private val context: Context
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

  //  view holder- holds 1 line view
  class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val profileImage: CircleImageView = view.findViewById(R.id.profile_picture_in_post)
    val userName: TextView = view.findViewById(R.id.post_user_name)
    val postImage: ImageView = view.findViewById(R.id.post_image)
    val postDescription: TextView = view.findViewById(R.id.post_description)
    val postLocation: TextView = view.findViewById(R.id.post_location)
    val likeButton: ImageView = view.findViewById(R.id.like_button)
    val likeCount: TextView = view.findViewById(R.id.like_count)
    val etComment: EditText = view.findViewById(R.id.etComment)
    val btnSendComment: Button = view.findViewById(R.id.btnSendComment)
    val rvComments: RecyclerView = view.findViewById(R.id.rvComments)
  }

  // onCreareViewHolder- create view for 1 line
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
    val view = LayoutInflater.from(parent.context)
      .inflate(R.layout.item_post, parent, false)
    return PostViewHolder(view)
  }

  // on bind view- fill the view in data (from firebase)
  override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
    val post = postList[position]
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    Glide.with(holder.itemView.context)
      .load(post.image_path)
      .into(holder.postImage)

    holder.postDescription.text = post.description

    FirebaseFirestore.getInstance().collection("users")
      .document(post.user_id)
      .get()
      .addOnSuccessListener { document ->
        if (document.exists()) {
          val firstName = document.getString("firstname") ?: "Unknown"
          val lastName = document.getString("lastname") ?: ""
          holder.userName.text = "$firstName $lastName"
          val profileImageUrl = document.getString("profileImageUrl") ?: ""
          if (profileImageUrl.isNotEmpty()) {
            val loadUrl = when {
              profileImageUrl.startsWith("http://") || profileImageUrl.startsWith("https://") -> profileImageUrl
              profileImageUrl.startsWith("file://") -> profileImageUrl
              else -> "file://$profileImageUrl"
            }
            Picasso.get()
              .load(loadUrl)
              .placeholder(R.drawable.profile_icon)
              .error(R.drawable.profile_icon)
              .into(holder.profileImage)
          } else {
            holder.profileImage.setImageResource(R.drawable.profile_icon)
          }
        } else {
          holder.userName.text = "Unknown User"
          holder.profileImage.setImageResource(R.drawable.profile_icon)
        }
      }
      .addOnFailureListener {
        holder.userName.text = "Unknown User"
        holder.profileImage.setImageResource(R.drawable.profile_icon)
      }

    holder.profileImage.setOnClickListener {
      val intent = Intent(context, UserProfileActivity::class.java)
      intent.putExtra("userId", post.user_id)
      context.startActivity(intent)
    }
    holder.userName.setOnClickListener {
      val intent = Intent(context, UserProfileActivity::class.java)
      intent.putExtra("userId", post.user_id)
      context.startActivity(intent)
    }
    if (post.place_name.isNotEmpty()) {
      holder.postLocation.visibility = View.VISIBLE
      holder.postLocation.text = post.place_name

      //  click to open a map
      holder.postLocation.setOnClickListener {
        val bundle = Bundle().apply {
          putDouble("focus_lat", post.place_lat)
          putDouble("focus_lng", post.place_lng)
        }

        val navController = Navigation.findNavController(holder.itemView)
        navController.navigate(R.id.action_home_to_map, bundle)
      }

    } else {
      holder.postLocation.visibility = View.GONE
    }

    holder.likeCount.text = post.likes.toString()
    updateLikeUI(holder, post, currentUserId)

    holder.likeButton.setOnClickListener {
      toggleLike(holder, post, currentUserId)
    }

    val gestureDetector = GestureDetector(holder.itemView.context, object : GestureDetector.SimpleOnGestureListener() {
      override fun onDoubleTap(e: MotionEvent): Boolean {
        toggleLike(holder, post, currentUserId)
        return true
      }
    })

    holder.postImage.setOnTouchListener { _, event ->
      gestureDetector.onTouchEvent(event)
      true
    }

    holder.btnSendComment.setOnClickListener {
      val commentText = holder.etComment.text.toString().trim()
      if (commentText.isEmpty()) {
        Toast.makeText(context, "Please add a comment", Toast.LENGTH_SHORT).show()
        return@setOnClickListener
      }
      val newComment = Comment(
        id = FirebaseFirestore.getInstance().collection("posts").document().id,
        userId = currentUserId,
        text = commentText,
        timestamp = System.currentTimeMillis()
      )
      val updatedComments = post.comments.toMutableList().apply { add(newComment) }
      FirebaseFirestore.getInstance().collection("posts")
        .document(post.id)
        .update("comments", updatedComments)
        .addOnSuccessListener {
          Toast.makeText(context, "Comment added", Toast.LENGTH_SHORT).show()
          holder.etComment.text.clear()
          post.comments = updatedComments
          holder.rvComments.adapter = CommentAdapter(updatedComments) { userId ->
            val intent = Intent(context, UserProfileActivity::class.java)
            intent.putExtra("userId", userId)
            context.startActivity(intent)
          }
        }
        .addOnFailureListener {
          Toast.makeText(context, "Error adding a comment", Toast.LENGTH_SHORT).show()
        }
    }

    // show all comments under a post
    holder.rvComments.layoutManager = LinearLayoutManager(context)
    holder.rvComments.adapter = CommentAdapter(post.comments) { userId ->
      val intent = Intent(context, UserProfileActivity::class.java)
      intent.putExtra("userId", userId)
      context.startActivity(intent)
    }
  }

  // how many items in a list
  override fun getItemCount(): Int = postList.size

  // like\ unlike- UI
  private fun updateLikeUI(holder: PostViewHolder, post: Post, userId: String) {
    val isLiked = post.likedUsers.contains(userId)
    holder.likeButton.setImageResource(
      if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
    )
    holder.likeCount.text = post.likes.toString()
  }

  // like\ unlike- FIREBASE
  private fun toggleLike(holder: PostViewHolder, post: Post, userId: String) {
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