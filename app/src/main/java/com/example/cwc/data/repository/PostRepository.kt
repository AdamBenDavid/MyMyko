package com.example.mymyko.data.repository

import android.content.Context
import android.widget.Toast
import com.example.mymyko.data.models.Post
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class PostRepository {

  private val db = FirebaseFirestore.getInstance()

  // עדכון הלייק על פוסט
  fun updatePostLike(context: Context, postId: String, userId: String) {
    val postRef = db.collection("posts").document(postId)

    postRef.get().addOnSuccessListener { document ->
      if (document.exists()) {
        val post = document.toObject(Post::class.java)
        post?.let {
          val likedUsers = it.likedUsers.toMutableList()

          // אם המשתמש כבר אהב את הפוסט, הסר את הלייק
          if (likedUsers.contains(userId)) {
            likedUsers.remove(userId)
            postRef.update("likes", FieldValue.increment(-1), "likedUsers", likedUsers)
              .addOnSuccessListener {
                Toast.makeText(context, "Like removed", Toast.LENGTH_SHORT).show()
              }
              .addOnFailureListener {
                Toast.makeText(context, "Failed to remove like", Toast.LENGTH_SHORT).show()
              }
          } else {
            // אם המשתמש לא אהב את הפוסט, הוסף את הלייק
            likedUsers.add(userId)
            postRef.update("likes", FieldValue.increment(1), "likedUsers", likedUsers)
              .addOnSuccessListener {
                Toast.makeText(context, "Like added", Toast.LENGTH_SHORT).show()
              }
              .addOnFailureListener {
                Toast.makeText(context, "Failed to add like", Toast.LENGTH_SHORT).show()
              }
          }
        }
      } else {
        Toast.makeText(context, "Post not found", Toast.LENGTH_SHORT).show()
      }
    }.addOnFailureListener {
      Toast.makeText(context, "Failed to retrieve post", Toast.LENGTH_SHORT).show()
    }
  }
}
