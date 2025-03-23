package com.example.mymyko

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.mymyko.adapters.ProfilePostAdapter
import com.example.mymyko.cloudinary.CloudinaryService
import com.example.mymyko.cloudinary.CloudinaryUploadResponse
import com.example.mymyko.data.models.Post
import com.example.mymyko.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class ProfileFragment : Fragment() {

  private var _binding: FragmentProfileBinding? = null
  private val binding get() = _binding!!

  private lateinit var profilePostAdapter: ProfilePostAdapter

  private var postList = mutableListOf<Post>()
  private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
  private val auth: FirebaseAuth = FirebaseAuth.getInstance()

  private var userDocListener: ListenerRegistration? = null

  override fun onResume() {
    super.onResume()
    fetchPosts()
    attachUserSnapshotListener()
  }

  override fun onPause() {
    super.onPause()
    detachUserSnapshotListener()
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentProfileBinding.inflate(inflater, container, false)
    val view = binding.root

    val childFragment = BottomNavFragment()
    val bundle = Bundle()
    bundle.putString("current_page", "profile")
    childFragment.arguments = bundle
    childFragmentManager.beginTransaction()
      .replace(R.id.navbar_container, childFragment)
      .commit()

    binding.editProfilePictureText.setOnClickListener {
      val intent = android.content.Intent(requireContext(), EditProfileActivity::class.java)
      startActivity(intent)
    }

    binding.swipeRefreshLayoutProfile.setOnRefreshListener {
      fetchPosts()
    }

    binding.recyclerViewProfile.layoutManager = GridLayoutManager(requireContext(), 2)
    profilePostAdapter = ProfilePostAdapter(postList, requireContext())
    binding.recyclerViewProfile.adapter = profilePostAdapter

    fetchPosts()

    return view
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  override fun onStart() {
    super.onStart()
    attachUserSnapshotListener()
  }

  override fun onStop() {
    super.onStop()
    detachUserSnapshotListener()
  }

  private fun attachUserSnapshotListener() {
    val userId = auth.currentUser?.uid ?: return
    userDocListener = db.collection("users").document(userId)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          Toast.makeText(requireContext(), "Error loading profile: ${error.message}", Toast.LENGTH_SHORT).show()
          return@addSnapshotListener
        }
        if (snapshot != null && snapshot.exists()) {
          val firstName = snapshot.getString("firstname") ?: "undefined"
          val lastName = snapshot.getString("lastname") ?: "undefined"
          val city = snapshot.getString("city") ?: "undefined"
          val country = snapshot.getString("country") ?: "undefined"
          val email = snapshot.getString("email") ?: "undefined"
          val profileImageUrl = snapshot.getString("profileImageUrl")

          Log.d("ProfileFragment", "Fetched user details: firstname=$firstName, lastname=$lastName, city=$city, country=$country, email=$email")

          binding.fullnameText.text = "$firstName $lastName"
          binding.locationText.text = country
          binding.emailText.text = email

          if (!profileImageUrl.isNullOrEmpty()) {
            if (profileImageUrl.startsWith("http://") || profileImageUrl.startsWith("https://")) {
              val finalUrl = "$profileImageUrl?ts=${System.currentTimeMillis()}"
              Picasso.get()
                .load(finalUrl)
                .placeholder(R.drawable.profile_icon)
                .error(R.drawable.profile_icon)
                .networkPolicy(NetworkPolicy.NO_CACHE, NetworkPolicy.NO_STORE)
                .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                .into(binding.profilePicture)
            } else {
              val bitmap = BitmapFactory.decodeFile(profileImageUrl)
              if (bitmap != null) {
                binding.profilePicture.setImageBitmap(bitmap)
              } else {
                binding.profilePicture.setImageResource(R.drawable.profile_icon)
              }
            }
          } else {
            binding.profilePicture.setImageResource(R.drawable.profile_icon)
          }
        }
      }
  }

  private fun detachUserSnapshotListener() {
    userDocListener?.remove()
    userDocListener = null
  }

  private fun fetchPosts() {
    binding.swipeRefreshLayoutProfile.isRefreshing = true
    val currentUserId = auth.currentUser?.uid
    if (currentUserId == null) {
      Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
      binding.swipeRefreshLayoutProfile.isRefreshing = false
      return
    }

    db.collection("posts")
      .whereEqualTo("user_id", currentUserId)
      .orderBy("timestamp", Query.Direction.DESCENDING)
      .get()
      .addOnSuccessListener { documents ->
        Log.d("ProfileFragment", "Fetched posts count: ${documents.size()}")
        postList.clear()
        for (document in documents) {
          val post = document.toObject(Post::class.java)
          if (post.user_id == currentUserId) {
            postList.add(post)
          }
        }
        profilePostAdapter.notifyDataSetChanged()
        binding.swipeRefreshLayoutProfile.isRefreshing = false
      }
      .addOnFailureListener { exception ->
        Log.e("ProfileFragment", "Error fetching posts", exception)
        Toast.makeText(requireContext(), "Failed to load posts", Toast.LENGTH_SHORT).show()
        binding.swipeRefreshLayoutProfile.isRefreshing = false
      }
  }
}
