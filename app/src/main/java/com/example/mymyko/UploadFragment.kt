package com.example.mymyko

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mymyko.cloudinary.CloudinaryService
import com.example.mymyko.cloudinary.CloudinaryUploadResponse
import com.example.mymyko.databinding.FragmentUploadBinding
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
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
import android.widget.Filter

class UploadFragment : Fragment() {
    private var selectedImageUri: Uri? = null
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var selectedPlaceName: String? = null
    private var selectedPlaceLatLng: Pair<Double, Double>? = null

    private var _binding: FragmentUploadBinding? = null
    private val binding get() = _binding!!

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImageUri = it
                Toast.makeText(requireContext(), "Image selected", Toast.LENGTH_SHORT).show()
            }
        }

    private fun openGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun saveImageLocally(uri: Uri): String? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            inputStream?.let {
                val picturesDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val appDir = File(picturesDir, "mymykoImages")
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

    private fun uploadImageToCloudinary(
        imageUri: Uri,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        Log.d("clouds", "start func");
        val localImagePath = saveImageLocally(imageUri)
        if (localImagePath == null) {
            onFailure("Failed to save image locally")
            Log.d("clouds", "fail image local");
            return
        }
        Log.d("clouds", "suscces saves local path");

        val file = File(localImagePath)
        val requestFile = RequestBody.create("image/*".toMediaTypeOrNull(), file)
        val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)

        val preset = "post_pictures_preset"
        val presetRequestBody = RequestBody.create("text/plain".toMediaTypeOrNull(), preset)

        val call = CloudinaryService.api.uploadImage("dkogrec1q", filePart, presetRequestBody)
        call.enqueue(object : Callback<CloudinaryUploadResponse> {
            override fun onResponse(
                call: Call<CloudinaryUploadResponse>,
                response: Response<CloudinaryUploadResponse>
            ) {
                Log.d("clouds", "Cloudinary response code: ${response.code()}")
                Log.d("clouds", "Cloudinary response message: ${response.message()}")

                if (response.isSuccessful) {
                    val uploadResponse = response.body()
                    if (uploadResponse?.secure_url != null) {
                        onSuccess(uploadResponse.secure_url)
                    } else {
                        onFailure("Upload succeeded but no URL returned")
                        Log.d("clouds", "not successful1")
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    onFailure("Upload failed: $errorBody")
                    Log.e("clouds", "Cloudinary error: $errorBody")
                    Log.d("clouds", "not successful2")
                }
            }

            override fun onFailure(call: Call<CloudinaryUploadResponse>, t: Throwable) {
                onFailure("Upload failed: ${t.message}")
                Log.d("clouds", "upload failed ya ben zona");
            }
        })
    }

    private fun uploadPost(imageUri: Uri, description: String) {
        val userId = auth.currentUser?.uid ?: return

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

    private fun savePostToFirestore(imageUrl: String, description: String) {
        val userId = auth.currentUser?.uid ?: return

        val post = hashMapOf(
            "user_id" to userId,
            "image_path" to imageUrl,
            "description" to description,
            "timestamp" to System.currentTimeMillis(),
            "likes" to 0,
            "likedUsers" to emptyList<String>(),
            "comments" to emptyList<String>(),
            "place_name" to (selectedPlaceName ?: ""),
            "place_lat" to (selectedPlaceLatLng?.first ?: 0.0),
            "place_lng" to (selectedPlaceLatLng?.second ?: 0.0)
        )

        db.collection("posts").add(post)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Post Shared!", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
            .addOnFailureListener {
                Log.e("UploadFragment", "Failed to share post: ${it.message}", it)
                Toast.makeText(
                    requireContext(),
                    "Failed to share post: ${it.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUploadBinding.inflate(inflater, container, false)
        val view = binding.root

        setHasOptionsMenu(true)
        (activity as AppCompatActivity).supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Upload Post"
        }

        val childFragment = BottomNavFragment()
        val bundle = Bundle()
        bundle.putString("current_page", "upload")
        childFragment.arguments = bundle
        childFragmentManager.beginTransaction()
            .replace(R.id.navbar_container, childFragment)
            .commit()

        binding.uploadButton.setOnClickListener {
            openGallery()
        }

        binding.share.setOnClickListener {
            val descriptionInput =
                binding.description.editText?.text.toString()
            if (selectedImageUri == null) {
                Toast.makeText(requireContext(), "Please select an image first", Toast.LENGTH_SHORT)
                    .show()
            } else {
                uploadPost(selectedImageUri!!, descriptionInput)
            }
        }

        val apiKey = getString(R.string.google_places_api_key)
        if (!Places.isInitialized()) {
            Places.initialize(requireContext().applicationContext, apiKey)
        }

        setupAutocomplete(binding.root)
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                findNavController().navigateUp()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupAutocomplete(view: View) {
        val locationInput = view.findViewById<AutoCompleteTextView>(R.id.location_input)
        val placesClient = Places.createClient(requireContext())
        val token = AutocompleteSessionToken.newInstance()
        val placeIdMap = mutableMapOf<String, String>()

        val adapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line
        ) {
            private val suggestions = mutableListOf<String>()

            override fun getCount(): Int = suggestions.size
            override fun getItem(position: Int): String = suggestions[position]

            override fun getFilter(): Filter {
                return object : Filter() {
                    override fun performFiltering(constraint: CharSequence?): FilterResults {
                        val results = FilterResults()
                        if (!constraint.isNullOrEmpty()) {
                            val request = FindAutocompletePredictionsRequest.builder()
                                .setSessionToken(token)
                                .setQuery(constraint.toString())
                                .build()

                            val task = placesClient.findAutocompletePredictions(request)
                            val response = Tasks.await(task)

                            suggestions.clear()
                            placeIdMap.clear()
                            response.autocompletePredictions.forEach {
                                val fullText = it.getFullText(null).toString()
                                suggestions.add(fullText)
                                placeIdMap[fullText] = it.placeId
                            }

                            results.values = suggestions
                            results.count = suggestions.size
                        }
                        return results
                    }

                    override fun publishResults(
                        constraint: CharSequence?,
                        results: FilterResults?
                    ) {
                        notifyDataSetChanged()
                    }
                }
            }
        }

        locationInput.setAdapter(adapter)

        locationInput.setOnItemClickListener { _, _, position, _ ->
            val selectedPlace = locationInput.adapter.getItem(position) as String
            val placeId = placeIdMap[selectedPlace]

            placeId?.let {
                val request =
                    FetchPlaceRequest.builder(it, listOf(Place.Field.LAT_LNG, Place.Field.NAME))
                        .build()
                placesClient.fetchPlace(request)
                    .addOnSuccessListener { response ->
                        val place = response.place
                        val latLng = place.latLng
                        selectedPlaceName = place.name
                        selectedPlaceLatLng = latLng?.let { Pair(it.latitude, it.longitude) }
                        Log.d("Place Selected", "Saved: ${place.name} at $latLng")
                    }
                    .addOnFailureListener { e ->
                        Log.e("Place Error", "Could not fetch place: ${e.message}")
                    }
            }
        }
    }
}
