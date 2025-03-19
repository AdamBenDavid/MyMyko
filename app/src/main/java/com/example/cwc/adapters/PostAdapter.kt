package com.example.mymyko.adapters

import android.content.Context
import android.content.Intent
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mymyko.R
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

  class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val profileImage: CircleImageView = view.findViewById(R.id.profile_picture_in_post)
    val userName: TextView = view.findViewById(R.id.post_user_name)
    val postImage: ImageView = view.findViewById(R.id.post_image)
    val postDescription: TextView = view.findViewById(R.id.post_description)
    val likeButton: ImageView = view.findViewById(R.id.like_button)
    val likeCount: TextView = view.findViewById(R.id.like_count)
    val etComment: EditText = view.findViewById(R.id.etComment)
    val btnSendComment: Button = view.findViewById(R.id.btnSendComment)
    val rvComments: RecyclerView = view.findViewById(R.id.rvComments)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
    val view = LayoutInflater.from(parent.context)
      .inflate(R.layout.item_post, parent, false)
    return PostViewHolder(view)
  }

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
              .placeholder(R.drawable.profile_foreground)
              .error(R.drawable.profile_foreground)
              .into(holder.profileImage)
          } else {
            holder.profileImage.setImageResource(R.drawable.profile_foreground)
          }
        } else {
          holder.userName.text = "Unknown User"
          holder.profileImage.setImageResource(R.drawable.profile_foreground)
        }
      }
      .addOnFailureListener {
        holder.userName.text = "Unknown User"
        holder.profileImage.setImageResource(R.drawable.profile_foreground)
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
        Toast.makeText(context, "אנא הכנס תגובה", Toast.LENGTH_SHORT).show()
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
          Toast.makeText(context, "תגובה נשלחה", Toast.LENGTH_SHORT).show()
          holder.etComment.text.clear()
          post.comments = updatedComments
          holder.rvComments.adapter = CommentAdapter(updatedComments) { userId ->
            val intent = Intent(context, UserProfileActivity::class.java)
            intent.putExtra("userId", userId)
            context.startActivity(intent)
          }
        }
        .addOnFailureListener {
          Toast.makeText(context, "שגיאה בשליחת תגובה", Toast.LENGTH_SHORT).show()
        }
    }

    holder.rvComments.layoutManager = LinearLayoutManager(context)
    holder.rvComments.adapter = CommentAdapter(post.comments) { userId ->
      val intent = Intent(context, UserProfileActivity::class.java)
      intent.putExtra("userId", userId)
      context.startActivity(intent)
    }
  }

  override fun getItemCount(): Int = postList.size

  private fun updateLikeUI(holder: PostViewHolder, post: Post, userId: String) {
    val isLiked = post.likedUsers.contains(userId)
    holder.likeButton.setImageResource(
      if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
    )
    holder.likeCount.text = post.likes.toString()
  }

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