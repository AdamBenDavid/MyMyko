package com.example.mymyko.data.models

import com.google.firebase.firestore.DocumentId
import java.io.Serializable

data class Post(
  @DocumentId val id: String = "",
  val image_path: String = "",
  val description: String = "",
  val user_id: String = "",
  val timestamp: Long = 0,
  val weather: Double = 0.0,
  var likes: Int = 0,
  val likedUsers: MutableList<String> = mutableListOf(),
  var comments: MutableList<Comment> = mutableListOf(),
  val place_name: String = "",
  val place_lat: Double = 0.0,
  val place_lng: Double = 0.0,
) : Serializable
