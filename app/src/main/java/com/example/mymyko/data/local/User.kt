package com.example.mymyko.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class User(
  @PrimaryKey(autoGenerate = false) val id: String = "",
  val email: String? = null,
  val firstname: String = "undefined",
  val lastname: String = "undefined",
  val city: String = "undefined",
  val country: String = "undefined",
  val profileImageUrl: String? = null,
  val imageBlob: ByteArray? = null
  )
