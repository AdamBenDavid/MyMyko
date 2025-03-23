package com.example.mymyko

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.mymyko.cloudinary.CloudinaryService
import com.example.mymyko.cloudinary.CloudinaryUploadResponse
import com.example.mymyko.databinding.ActivityEditProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

class EditProfileActivity : AppCompatActivity() {

  private lateinit var binding: ActivityEditProfileBinding

  private val auth = FirebaseAuth.getInstance()
  private val db = FirebaseFirestore.getInstance()

  private var selectedImageUri: Uri? = null
  private var oldProfileImageUrl: String? = null

  private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
    uri?.let {
      selectedImageUri = it
      binding.editProfileImage.setImageURI(it)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityEditProfileBinding.inflate(layoutInflater)
    setContentView(binding.root)

    supportActionBar?.apply {
      title = "Edit Profile"
      setDisplayHomeAsUpEnabled(true)
    }

    loadCurrentProfile()

    binding.editProfileImage.setOnClickListener {
      pickImageLauncher.launch("image/*")
    }

    binding.editBtnSave.setOnClickListener {
      saveProfile()
    }

    binding.editBtnCancel.setOnClickListener {
      setResult(RESULT_CANCELED)
      finish()
    }
  }

  private fun loadCurrentProfile() {
    val userId = auth.currentUser?.uid ?: return
    db.collection("users").document(userId).get()
      .addOnSuccessListener { document ->
        if (document.exists()) {
          binding.editEtFirstname.setText(document.getString("firstname") ?: "")
          binding.editEtLastname.setText(document.getString("lastname") ?: "")
          binding.editEtEmail.setText(document.getString("email") ?: "")
          val city = document.getString("city") ?: ""
          val country = document.getString("country") ?: ""
          binding.editEtLocation.setText(if (city.isNotEmpty() && country.isNotEmpty()) "$city, $country" else country)

          val profileImageUrl = document.getString("profileImageUrl")
          oldProfileImageUrl = profileImageUrl
          if (!profileImageUrl.isNullOrEmpty()) {
            if (profileImageUrl.startsWith("http://") || profileImageUrl.startsWith("https://")) {
              Picasso.get()
                .load(profileImageUrl)
                .placeholder(R.drawable.profile_icon)
                .error(R.drawable.profile_icon)
                .networkPolicy(
                  com.squareup.picasso.NetworkPolicy.NO_CACHE,
                  com.squareup.picasso.NetworkPolicy.NO_STORE
                )
                .memoryPolicy(
                  com.squareup.picasso.MemoryPolicy.NO_CACHE,
                  com.squareup.picasso.MemoryPolicy.NO_STORE
                )
                .into(binding.editProfileImage)
            } else {
              val bitmap = BitmapFactory.decodeFile(profileImageUrl)
              if (bitmap != null) {
                binding.editProfileImage.setImageBitmap(bitmap)
              } else {
                binding.editProfileImage.setImageResource(R.drawable.profile_icon)
              }
            }
          } else {
            binding.editProfileImage.setImageResource(R.drawable.profile_icon)
          }
        }
      }
      .addOnFailureListener { e ->
        Toast.makeText(this, "Failed to load profile: ${e.message}", Toast.LENGTH_SHORT).show()
      }
  }

  private fun saveProfile() {
    val userId = auth.currentUser?.uid ?: return
    val updatedFirstName = binding.editEtFirstname.text.toString().trim()
    val updatedLastName = binding.editEtLastname.text.toString().trim()
    val updatedEmail = binding.editEtEmail.text.toString().trim()
    val updatedLocation = binding.editEtLocation.text.toString().trim()
    val locationParts = updatedLocation.split(",").map { it.trim() }
    val updatedCity: String
    val updatedCountry: String
    if (locationParts.size == 1) {
      updatedCity = ""
      updatedCountry = locationParts[0]
    } else if (locationParts.size >= 2) {
      updatedCity = locationParts[0]
      updatedCountry = locationParts[1]
    } else {
      updatedCity = ""
      updatedCountry = ""
    }

    val updates = hashMapOf(
      "firstname" to updatedFirstName,
      "lastname" to updatedLastName,
      "email" to updatedEmail,
      "city" to updatedCity,
      "country" to updatedCountry
    )

    if (selectedImageUri != null) {
      uploadImageToCloudinary(selectedImageUri!!, onSuccess = { secureUrl ->
        Log.d("EditProfileActivity", "New secureUrl: $secureUrl")
        updates["profileImageUrl"] = secureUrl
        if (!oldProfileImageUrl.isNullOrEmpty() && oldProfileImageUrl != secureUrl) {
          deleteOldImage(oldProfileImageUrl!!)
        }
        updateProfileInFirestore(userId, updates)
        Picasso.get()
          .load(secureUrl)
          .placeholder(R.drawable.profile_icon)
          .error(R.drawable.profile_icon)
          .networkPolicy(
            com.squareup.picasso.NetworkPolicy.NO_CACHE,
            com.squareup.picasso.NetworkPolicy.NO_STORE
          )
          .memoryPolicy(
            com.squareup.picasso.MemoryPolicy.NO_CACHE,
            com.squareup.picasso.MemoryPolicy.NO_STORE
          )
          .into(binding.editProfileImage)
      }, onFailure = { errorMsg ->
        Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
      })
    } else {
      updateProfileInFirestore(userId, updates)
    }
  }

  private fun updateProfileInFirestore(userId: String, updates: Map<String, Any>) {
    Log.d("EditProfileActivity", "Updating Firestore with: $updates")
    db.collection("users").document(userId)
      .update(updates)
      .addOnSuccessListener {
        Log.d("EditProfileActivity", "Firestore update successful")
        Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
        if (updates.containsKey("profileImageUrl")) {
          val newImageUrl = updates["profileImageUrl"] as String
          Log.d("EditProfileActivity", "New profileImageUrl: $newImageUrl")
          val resultIntent = Intent()
          resultIntent.putExtra("profileImageUrl", newImageUrl)
          setResult(RESULT_OK, resultIntent)
        } else {
          setResult(RESULT_OK)
        }
        finish()
      }
      .addOnFailureListener { e ->
        Log.e("EditProfileActivity", "Firestore update failed: ${e.message}")
        Toast.makeText(this, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
      }
  }

  private fun uploadImageToCloudinary(
    imageUri: Uri,
    onSuccess: (String) -> Unit,
    onFailure: (String) -> Unit
  ) {
    val localImagePath = saveImageLocally(imageUri)
    if (localImagePath == null) {
      onFailure("Failed to save image locally")
      return
    }
    val file = File(localImagePath)
    val requestFile = RequestBody.create("image/*".toMediaTypeOrNull(), file)
    val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)

    val preset = "profile_pictures_preset"
    val presetRequestBody = RequestBody.create("text/plain".toMediaTypeOrNull(), preset)

    val call = CloudinaryService.api.uploadImage("dkogrec1q", filePart, presetRequestBody)
    call.enqueue(object : Callback<CloudinaryUploadResponse> {
      override fun onResponse(
        call: Call<CloudinaryUploadResponse>,
        response: Response<CloudinaryUploadResponse>
      ) {
        if (response.isSuccessful) {
          val uploadResponse = response.body()
          if (uploadResponse?.secure_url != null) {
            onSuccess(uploadResponse.secure_url)
          } else {
            onFailure("Upload succeeded but no URL returned")
          }
        } else {
          onFailure("Upload failed ya ben zona: ${response.message()}")
        }
      }
      override fun onFailure(call: Call<CloudinaryUploadResponse>, t: Throwable) {
        onFailure("Upload failed : ${t.message}")
      }
    })
  }

  private fun saveImageLocally(uri: Uri): String? {
    return try {
      val inputStream = contentResolver.openInputStream(uri)
      inputStream?.let {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val appDir = File(picturesDir, "mymykoImages")
        if (!appDir.exists()) {
          appDir.mkdirs()
        }
        val file = File(appDir, "profile_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        it.copyTo(outputStream)
        it.close()
        outputStream.close()
        file.absolutePath
      }
    } catch (e: Exception) {
      Log.e("EditProfileActivity", "Failed to save image locally: ${e.message}")
      null
    }
  }

  private fun deleteOldImage(oldImageUrl: String) {
    val publicId = getPublicId(oldImageUrl)
    if (publicId == null) {
      Log.e("EditProfileActivity", "Could not extract public ID from URL")
      return
    }
    val timestamp = System.currentTimeMillis() / 1000
    val apiSecret = "WGnb7PpUlyoEnWbj1-_PNTmyQfs"
    val signatureData = "public_id=$publicId&timestamp=$timestamp"
    val signature = sha1(signatureData + apiSecret)

    CloudinaryService.api.deleteImage("dkogrec1q", publicId, timestamp, signature, "296316728133841")
      .enqueue(object : Callback<CloudinaryUploadResponse> {
        override fun onResponse(call: Call<CloudinaryUploadResponse>, response: Response<CloudinaryUploadResponse>) {
          if (response.isSuccessful) {
            Log.d("EditProfileActivity", "Old image deleted successfully")
          } else {
            Log.e("EditProfileActivity", "Failed to delete old image: ${response.message()}")
          }
        }
        override fun onFailure(call: Call<CloudinaryUploadResponse>, t: Throwable) {
          Log.e("EditProfileActivity", "Error deleting old image: ${t.message}")
        }
      })
  }

  private fun getPublicId(imageUrl: String): String? {
    try {
      val urlWithoutQuery = imageUrl.split("?")[0]
      val index = urlWithoutQuery.indexOf("/upload/")
      if (index != -1) {
        val publicIdWithVersion = urlWithoutQuery.substring(index + "/upload/".length)
        val parts = publicIdWithVersion.split("/")
        val publicIdWithExtension = if (parts[0].startsWith("v") && parts.size > 1) {
          parts.drop(1).joinToString("/")
        } else {
          publicIdWithVersion
        }
        return publicIdWithExtension.substringBeforeLast(".")
      }
    } catch (e: Exception) {
      Log.e("EditProfileActivity", "Error extracting public ID: ${e.message}")
      return null
    }
    return null
  }

  private fun sha1(input: String): String {
    val bytes = MessageDigest.getInstance("SHA-1").digest(input.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      android.R.id.home -> {
        finish()
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }
}
