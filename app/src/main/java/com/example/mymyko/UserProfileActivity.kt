package com.example.mymyko

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.example.mymyko.adapters.UserPostsAdapter
import com.example.mymyko.data.models.Post
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.example.mymyko.databinding.ActivityUserProfileBinding

class UserProfileActivity : AppCompatActivity() {

  private lateinit var binding: ActivityUserProfileBinding

  private val EDIT_PROFILE_REQUEST = 1001
  private var currentUserId: String = ""

  private var userDocListener: ListenerRegistration? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityUserProfileBinding.inflate(layoutInflater)
    setContentView(binding.root)

    currentUserId = intent.getStringExtra("userId") ?: ""
    if (currentUserId.isEmpty()) {
      Toast.makeText(this, "User ID not provided", Toast.LENGTH_SHORT).show()
      finish()
      return
    }

    // Load posts once (real-time updates for posts aren’t implemented here)
    loadUserPosts(currentUserId)
  }

  override fun onStart() {
    super.onStart()
    attachUserDocumentListener()
  }

  override fun onStop() {
    super.onStop()
    detachUserDocumentListener()
  }

  private fun attachUserDocumentListener() {
    userDocListener = FirebaseFirestore.getInstance()
      .collection("users")
      .document(currentUserId)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          Toast.makeText(this, "Error loading profile: ${error.message}", Toast.LENGTH_SHORT).show()
          return@addSnapshotListener
        }
        if (snapshot != null && snapshot.exists()) {
          val firstName = snapshot.getString("firstname") ?: ""
          val lastName = snapshot.getString("lastname") ?: ""
          val email = snapshot.getString("email") ?: "No email"
          val city = snapshot.getString("city") ?: ""
          val country = snapshot.getString("country") ?: ""
          val profileImageUrl = snapshot.getString("profileImageUrl") ?: ""
          updateProfileUI(firstName, lastName, email, city, country, profileImageUrl)
        }
      }
  }

  private fun detachUserDocumentListener() {
    userDocListener?.remove()
    userDocListener = null
  }

  private fun updateProfileUI(
    firstName: String,
    lastName: String,
    email: String,
    city: String,
    country: String,
    profileImageUrl: String
  ) {
    binding.tvUserName.text = "$firstName $lastName"
    binding.tvEmail.text = email
    binding.tvLocation.text = if (city.isNotEmpty() && country.isNotEmpty()) "$city, $country" else country

    if (profileImageUrl.isNotEmpty()) {
      updateProfileImage(profileImageUrl)
    } else {
      binding.ivProfile.setImageResource(R.drawable.profile_icon)
    }
  }

  private fun updateProfileImage(imageUrl: String) {
    val separator = if (imageUrl.contains("?")) "&" else "?"
    Glide.with(this)
      .load(imageUrl + separator + "ts=" + System.currentTimeMillis())
      .signature(ObjectKey(imageUrl + System.currentTimeMillis()))
      .diskCacheStrategy(DiskCacheStrategy.NONE)
      .skipMemoryCache(true)
      .placeholder(R.drawable.profile_icon)
      .error(R.drawable.profile_icon)
      .into(binding.ivProfile)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == EDIT_PROFILE_REQUEST && resultCode == RESULT_OK) {
      val newImageUrl = data?.getStringExtra("profileImageUrl")
      Log.d("UserProfileActivity", "Received image URL from edit: $newImageUrl")
      if (!newImageUrl.isNullOrEmpty()) {
        Glide.with(this).clear(binding.ivProfile)
        updateProfileImage(newImageUrl)
      }
    }
  }

  private fun loadUserPosts(userId: String) {
    FirebaseFirestore.getInstance().collection("posts")
      .whereEqualTo("user_id", userId)
      .orderBy("timestamp", Query.Direction.DESCENDING)
      .get()
      .addOnSuccessListener { documents ->
        val posts = mutableListOf<Post>()
        for (document in documents) {
          val post = document.toObject(Post::class.java)
          posts.add(post)
        }
        binding.rvUserPosts.layoutManager = LinearLayoutManager(this)
        binding.rvUserPosts.adapter = UserPostsAdapter(posts, this)
      }
      .addOnFailureListener { e ->
        Toast.makeText(this, "Failed to load user's posts: ${e.message}", Toast.LENGTH_SHORT).show()
      }
  }
}
