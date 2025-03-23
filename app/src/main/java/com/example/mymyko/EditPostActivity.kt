package com.example.mymyko

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.mymyko.data.models.Post
import com.example.mymyko.databinding.FragmentEditPostBinding
import com.google.firebase.firestore.FirebaseFirestore

class EditPostActivity : AppCompatActivity() {

  private lateinit var binding: FragmentEditPostBinding
  private lateinit var post: Post

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = FragmentEditPostBinding.inflate(layoutInflater)
    setContentView(binding.root)

    post = intent.getSerializableExtra("post") as? Post ?: run {
      Toast.makeText(this, "No post data found", Toast.LENGTH_SHORT).show()
      finish()
      return
    }

    Glide.with(this)
      .load(post.image_path)
      .into(binding.ivPostImage)

    binding.etDescription.setText(post.description)

    binding.btnSave.setOnClickListener {
      val updatedDescription = binding.etDescription.text.toString().trim()
      if (updatedDescription.isEmpty()) {
        binding.etDescription.error = "Description cannot be empty"
        return@setOnClickListener
      }
      updatePostDescription(updatedDescription)
    }

    binding.btnCancel.setOnClickListener {
      finish()
    }
  }

  private fun updatePostDescription(newDescription: String) {
    FirebaseFirestore.getInstance().collection("posts")
      .document(post.id)
      .update("description", newDescription)
      .addOnSuccessListener {
        Toast.makeText(this, "Post updated", Toast.LENGTH_SHORT).show()
        setResult(Activity.RESULT_OK)
        finish()
      }
      .addOnFailureListener { e ->
        Toast.makeText(this, "Failed to update post: ${e.message}", Toast.LENGTH_SHORT).show()
      }
  }
}
