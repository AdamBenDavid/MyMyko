package com.example.mymyko.data.models

import com.google.firebase.auth.FirebaseAuth
import java.io.Serializable

data class Comment(
  var id: String = "",
  var userId: String = "",
  var firstname: String = "",
  var text: String = "",
  var timestamp: Long = System.currentTimeMillis()
) : Serializable
