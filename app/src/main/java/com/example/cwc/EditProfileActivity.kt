package com.example.cwc

import android.content.Intent
import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import com.example.cwc.cloudinary.CloudinaryService
import com.example.cwc.cloudinary.CloudinaryUploadResponse
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

  private lateinit var profileImageView: ImageView
  private lateinit var etFirstName: EditText
  private lateinit var etLastName: EditText
  private lateinit var etEmail: EditText
  private lateinit var etLocation: EditText
  private lateinit var btnSave: Button
  private lateinit var btnCancel: Button

  private val auth = FirebaseAuth.getInstance()
  private val db = FirebaseFirestore.getInstance()

  private var selectedImageUri: Uri? = null
  private var oldProfileImageUrl: String? = null

  private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
    uri?.let {
      selectedImageUri = it
      profileImageView.setImageURI(it)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.fragment_edit_profile)
    supportActionBar?.apply {
      title = "Edit Profile"
      setDisplayHomeAsUpEnabled(true)
    }

    profileImageView = findViewById(R.id.edit_profile_image)
    etFirstName = findViewById(R.id.edit_et_firstname)
    etLastName = findViewById(R.id.edit_et_lastname)
    etEmail = findViewById(R.id.edit_et_email)
    etLocation = findViewById(R.id.edit_et_location)
    btnSave = findViewById(R.id.edit_btn_save)
    btnCancel = findViewById(R.id.edit_btn_cancel)

    loadCurrentProfile()

    profileImageView.setOnClickListener {
      pickImageLauncher.launch("image/*")
    }

    btnSave.setOnClickListener {
      saveProfile()
    }

    btnCancel.setOnClickListener {
      setResult(RESULT_CANCELED)
      finish()
    }
  }

  private fun loadCurrentProfile() {
    val userId = auth.currentUser?.uid ?: return
    db.collection("users").document(userId).get()
      .addOnSuccessListener { document ->
        if (document.exists()) {
          etFirstName.setText(document.getString("firstname") ?: "")
          etLastName.setText(document.getString("lastname") ?: "")
          etEmail.setText(document.getString("email") ?: "")
          val city = document.getString("city") ?: ""
          val country = document.getString("country") ?: ""
          etLocation.setText(if (city.isNotEmpty() && country.isNotEmpty()) "$city, $country" else country)

          val profileImageUrl = document.getString("profileImageUrl")
          oldProfileImageUrl = profileImageUrl
          if (!profileImageUrl.isNullOrEmpty()) {
            if (profileImageUrl.startsWith("http://") || profileImageUrl.startsWith("https://")) {
              Picasso.get()
                .load(profileImageUrl)
                .placeholder(R.drawable.profile_foreground)
                .error(R.drawable.profile_foreground)
                .networkPolicy(
                  com.squareup.picasso.NetworkPolicy.NO_CACHE,
                  com.squareup.picasso.NetworkPolicy.NO_STORE
                )
                .memoryPolicy(
                  com.squareup.picasso.MemoryPolicy.NO_CACHE,
                  com.squareup.picasso.MemoryPolicy.NO_STORE
                )
                .into(profileImageView)
            } else {
              val bitmap = BitmapFactory.decodeFile(profileImageUrl)
              if (bitmap != null) {
                profileImageView.setImageBitmap(bitmap)
              } else {
                profileImageView.setImageResource(R.drawable.profile_foreground)
              }
            }
          } else {
            profileImageView.setImageResource(R.drawable.profile_foreground)
          }
        }
      }
      .addOnFailureListener { e ->
        Toast.makeText(this, "Failed to load profile: ${e.message}", Toast.LENGTH_SHORT).show()
      }
  }

  private fun saveProfile() {
    val userId = auth.currentUser?.uid ?: return
    val updatedFirstName = etFirstName.text.toString().trim()
    val updatedLastName = etLastName.text.toString().trim()
    val updatedEmail = etEmail.text.toString().trim()
    val updatedLocation = etLocation.text.toString().trim()
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
          .placeholder(R.drawable.profile_foreground)
          .error(R.drawable.profile_foreground)
          .networkPolicy(
            com.squareup.picasso.NetworkPolicy.NO_CACHE,
            com.squareup.picasso.NetworkPolicy.NO_STORE
          )
          .memoryPolicy(
            com.squareup.picasso.MemoryPolicy.NO_CACHE,
            com.squareup.picasso.MemoryPolicy.NO_STORE
          )
          .into(profileImageView)
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

    val preset = "CWC - Content With Coffee"
    val presetRequestBody = RequestBody.create("text/plain".toMediaTypeOrNull(), preset)

    val call = CloudinaryService.api.uploadImage("dtdw1bmq4", filePart, presetRequestBody)
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
          onFailure("Upload failed: ${response.message()}")
        }
      }
      override fun onFailure(call: Call<CloudinaryUploadResponse>, t: Throwable) {
        onFailure("Upload failed: ${t.message}")
      }
    })
  }

  private fun saveImageLocally(uri: Uri): String? {
    return try {
      val inputStream = contentResolver.openInputStream(uri)
      inputStream?.let {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val appDir = File(picturesDir, "CWCImages")
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
    val apiSecret = "UIOi_lsef1LfVRNGKmLBCC3yjt8"
    val signatureData = "public_id=$publicId&timestamp=$timestamp"
    val signature = sha1(signatureData + apiSecret)

    CloudinaryService.api.deleteImage( "dtdw1bmq4",publicId, timestamp, signature, "316232596576643")
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
