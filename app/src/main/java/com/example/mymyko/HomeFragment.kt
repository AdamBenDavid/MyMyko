package com.example.mymyko

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mymyko.adapters.PostAdapter
import com.example.mymyko.data.local.User
import com.example.mymyko.data.models.Post
import com.example.mymyko.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HomeFragment : Fragment() {

  private var _binding: FragmentHomeBinding? = null
  private val binding get() = _binding!!

  private var postList = mutableListOf<Post>()
  private lateinit var postAdapter: PostAdapter

  fun renderNav(user: User) {
    Log.d("HomeFragment", "Rendering BottomNavFragment for user: ${user.firstname} ${user.lastname}")
    val childFragment = BottomNavFragment()
    val bundle = Bundle()
    bundle.putString("current_page", "home")
    bundle.putString("firstname", user.firstname)
    bundle.putString("lastname", user.lastname)
    childFragment.arguments = bundle
    parentFragmentManager.beginTransaction()
      .replace(R.id.navbar_container, childFragment)
      .commit()
  }

  private fun fetchPosts() {
    binding.swipeRefreshLayout.isRefreshing = true
    FirebaseFirestore.getInstance().collection("posts")
      .orderBy("timestamp", Query.Direction.DESCENDING)
      .get()
      .addOnSuccessListener { documents ->
        postList.clear()
        for (document in documents) {
          val post = document.toObject(Post::class.java).copy(
            id = document.id,
            place_name = document.getString("place_name") ?: "Unknown Location",
          )
          postList.add(post)
        }
        postAdapter.notifyDataSetChanged()
        binding.swipeRefreshLayout.isRefreshing = false
      }
      .addOnFailureListener { exception ->
        Log.e("HomeFragment", "Error fetching posts", exception)
        Toast.makeText(requireContext(), "Failed to load posts", Toast.LENGTH_SHORT).show()
        binding.swipeRefreshLayout.isRefreshing = false
      }
  }

  private fun fetchCurrentUserAndRenderNav() {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    if (currentUserId == null) {
      Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
      findNavController().navigate(R.id.action_homeFragment_to_logoutFragment)
      return
    }
    FirebaseFirestore.getInstance().collection("users")
      .document(currentUserId)
      .get()
      .addOnSuccessListener { document ->
        if (document.exists()) {
          val firstName = document.getString("firstname") ?: ""
          val lastName = document.getString("lastname") ?: ""
          val email = document.getString("email") ?: ""
          val profileImageUrl = document.getString("profileImageUrl") ?: ""
          val user = User(
            id = currentUserId,
            firstname = firstName,
            lastname = lastName,
            email = email,
            profileImageUrl = profileImageUrl,
            imageBlob = null
          )
          renderNav(user)
        } else {
          Toast.makeText(requireContext(), "User data not found", Toast.LENGTH_SHORT).show()
        }
      }
      .addOnFailureListener { e ->
        Log.e("HomeFragment", "Error fetching user data", e)
        Toast.makeText(requireContext(), "Failed to fetch user data", Toast.LENGTH_SHORT).show()
      }
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentHomeBinding.inflate(inflater, container, false)
    val view = binding.root

    binding.swipeRefreshLayout.setOnRefreshListener { fetchPosts() }

    binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
    postAdapter = PostAdapter(postList, requireContext())
    binding.recyclerView.adapter = postAdapter

    fetchPosts()
    fetchCurrentUserAndRenderNav()

    return view
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  override fun onResume() {
    super.onResume()
    Log.d("HomeFragment", "onResume")
    fetchPosts()
  }
}
