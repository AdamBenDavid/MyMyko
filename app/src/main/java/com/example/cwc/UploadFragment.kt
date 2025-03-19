package com.example.cwc

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.cwc.cloudinary.CloudinaryService
import com.example.cwc.cloudinary.CloudinaryUploadResponse
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class UploadFragment : Fragment() {

  private var selectedImageUri: Uri? = null
  private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
  private val auth: FirebaseAuth = FirebaseAuth.getInstance()

  // Launcher for picking an image
  private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
    uri?.let {
      selectedImageUri = it
      Toast.makeText(requireContext(), "Image selected", Toast.LENGTH_SHORT).show()
    }
  }

  // Opens the gallery to pick an image
  private fun openGallery() {
    pickImageLauncher.launch("image/*")
  }

  // Saves the picked image locally before uploading to Cloudinary
  private fun saveImageLocally(uri: Uri): String? {
    return try {
      val inputStream = requireContext().contentResolver.openInputStream(uri)
      inputStream?.let {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val appDir = File(picturesDir, "CWCImages")
        if (!appDir.exists()) {
          appDir.mkdirs()
        }
        val file = File(appDir, "post_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        it.copyTo(outputStream)
        it.close()
        outputStream.close()
        file.absolutePath
      }
    } catch (e: Exception) {
      Log.e("UploadFragment", "Failed to save image locally: ${e.message}")
      null
    }
  }

  // Uploads the local file to Cloudinary
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
      override fun onResponse(call: Call<CloudinaryUploadResponse>, response: Response<CloudinaryUploadResponse>) {
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

  // Uploads the post data (image URL + description) to Firestore
  private fun uploadPost(imageUri: Uri, description: String) {
    val userId = auth.currentUser?.uid ?: return

    // First upload the selected image to Cloudinary
    uploadImageToCloudinary(
      imageUri,
      onSuccess = { secureUrl ->
        savePostToFirestore(secureUrl, description)
      },
      onFailure = { errorMsg ->
        Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
      }
    )
  }

  // Saves the post to Firestore
  private fun savePostToFirestore(imageUrl: String, description: String) {
    val userId = auth.currentUser?.uid ?: return

    val post = hashMapOf(
      "user_id" to userId,
      "image_path" to imageUrl,
      "description" to description,
      "timestamp" to System.currentTimeMillis(),
      "likes" to 0,
      "likedUsers" to emptyList<String>(),
      "comments" to emptyList<String>()
    )

    db.collection("posts").add(post)
      .addOnSuccessListener {
        Toast.makeText(requireContext(), "Post Shared!", Toast.LENGTH_SHORT).show()
        findNavController().navigateUp()
      }
      .addOnFailureListener {
        Log.e("UploadFragment", "Failed to share post: ${it.message}", it)
        Toast.makeText(requireContext(), "Failed to share post: ${it.message}", Toast.LENGTH_SHORT).show()
      }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    val view = inflater.inflate(R.layout.fragment_upload, container, false)

    setHasOptionsMenu(true)
    (activity as AppCompatActivity).supportActionBar?.apply {
      setDisplayHomeAsUpEnabled(true)
      title = "Upload Post"
    }

    // Bottom Nav Integration:
    // Make sure you have a container (FrameLayout) in fragment_upload.xml with id = navbar_container
    val childFragment = BottomNavFragment()
    val bundle = Bundle()
    bundle.putString("current_page", "upload")
    childFragment.arguments = bundle
    childFragmentManager.beginTransaction()
      .replace(R.id.navbar_container, childFragment)
      .commit()

    // Buttons
    view.findViewById<Button>(R.id.upload_button).setOnClickListener {
      openGallery()
    }

    view.findViewById<Button>(R.id.share).setOnClickListener {
      val descriptionInput = view.findViewById<TextInputLayout>(R.id.description).editText?.text.toString()
      if (selectedImageUri == null) {
        Toast.makeText(requireContext(), "Please select an image first", Toast.LENGTH_SHORT).show()
      } else {
        uploadPost(selectedImageUri!!, descriptionInput)
      }
    }

    return view
  }

  // Handle the back arrow in the action bar
  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      android.R.id.home -> {
        findNavController().navigateUp()
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }
}
