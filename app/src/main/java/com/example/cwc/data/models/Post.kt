package com.example.mymyko.data.models

import com.google.firebase.firestore.DocumentId
import java.io.Serializable

data class Post(
  @DocumentId val id: String = "",
  val image_path: String = "", // שדה מעודכן
  val description: String = "",
  val user_id: String = "",
  val timestamp: Long = 0,
  val weather: Double = 0.0,
  var likes: Int = 0,
  val likedUsers: MutableList<String> = mutableListOf(),
  var comments: MutableList<Comment> = mutableListOf()
) : Serializable
