package com.example.mymyko.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mymyko.R
import com.example.mymyko.data.models.Comment
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView


class CommentAdapter(
  private val commentList: List<Comment>,
  private val onUsernameClick: (String) -> Unit
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

  class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val commentAvatar: CircleImageView = view.findViewById(R.id.comment_avatar)
    val commentUserName: TextView = view.findViewById(R.id.comment_user_name)
    val commentText: TextView = view.findViewById(R.id.comment_text)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
    val view = LayoutInflater.from(parent.context)
      .inflate(R.layout.item_comment, parent, false)
    return CommentViewHolder(view)
  }

  override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
    val comment = commentList[position]
    holder.commentText.text = comment.text

    FirebaseFirestore.getInstance().collection("users")
      .document(comment.userId)
      .get()
      .addOnSuccessListener { document ->
        if (document.exists()) {
          val firstName = document.getString("firstname") ?: ""
          val lastName = document.getString("lastname") ?: ""
          holder.commentUserName.text = "$firstName $lastName"
          var profileUrl = document.getString("profileImageUrl") ?: ""
          Log.d("CommentAdapter", "Profile URL for user ${comment.userId}: $profileUrl")
          if (profileUrl.isNotEmpty() &&
            !profileUrl.startsWith("http://") &&
            !profileUrl.startsWith("https://") &&
            !profileUrl.startsWith("file://")
          ) {
            profileUrl = "file://$profileUrl"
          }
          if (profileUrl.isNotEmpty()) {
            Picasso.get()
              .load(profileUrl)
              .placeholder(R.drawable.profile_foreground)
              .error(R.drawable.profile_foreground)
              .fit()
              .centerCrop()
              .into(holder.commentAvatar)
          } else {
            holder.commentAvatar.setImageResource(R.drawable.profile_foreground)
          }

          // Set click listener on the username
          holder.commentUserName.setOnClickListener {
            onUsernameClick(comment.userId)
          }
        } else {
          holder.commentUserName.text = "Unknown User"
          holder.commentAvatar.setImageResource(R.drawable.profile_foreground)
        }
      }
      .addOnFailureListener {
        holder.commentUserName.text = "Unknown User"
        holder.commentAvatar.setImageResource(R.drawable.profile_foreground)
      }
  }

  override fun getItemCount(): Int = commentList.size
}
