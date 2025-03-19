package com.example.cwc

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.cwc.adapters.PostAdapter
import com.example.cwc.data.local.AppDatabase
import com.example.cwc.data.local.User
import com.example.cwc.data.models.Post
import com.example.cwc.data.repository.UserRepository
import com.example.cwc.viewmodel.UserViewModel
import com.example.cwc.viewmodel.UserViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class HomeFragment : Fragment() {

  private lateinit var userViewModel: UserViewModel
  private lateinit var recyclerView: RecyclerView
  private var postList = mutableListOf<Post>()
  private lateinit var postAdapter: PostAdapter
  private lateinit var swipeRefreshLayout: SwipeRefreshLayout

  private lateinit var tvCoffeeRecommendation: TextView

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
    swipeRefreshLayout.isRefreshing = true
    FirebaseFirestore.getInstance().collection("posts")
      .orderBy("timestamp", Query.Direction.DESCENDING)
      .get()
      .addOnSuccessListener { documents ->
        postList.clear()
        for (document in documents) {
          val post = document.toObject(Post::class.java)
          postList.add(post)
        }
        postAdapter.notifyDataSetChanged()
        swipeRefreshLayout.isRefreshing = false
      }
      .addOnFailureListener { exception ->
        Log.e("HomeFragment", "Error fetching posts", exception)
        Toast.makeText(requireContext(), "Failed to load posts", Toast.LENGTH_SHORT).show()
        swipeRefreshLayout.isRefreshing = false
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

  private fun updateCoffeeRecommendation() {
    viewLifecycleOwner.lifecycleScope.launch {
      try {
        val jsonData = withContext(Dispatchers.IO) {
          val client = OkHttpClient()
          val apiKey = "200b857da17d668dbf479de6ff89c982"
          val url = "https://api.openweathermap.org/data/2.5/weather?q=Tel%20Aviv,IL&units=metric&appid=$apiKey"
          val request = Request.Builder().url(url).build()
          val response = client.newCall(request).execute()
          response.body?.string()
        }

        if (jsonData != null) {
          val jsonObject = JSONObject(jsonData)
          val main = jsonObject.getJSONObject("main")
          val temp = main.getDouble("temp")
          val recommendation = when {
            temp < 10 -> "It's chilly at ${temp}°C! How about a hot cappuccino?"
            temp in 10.0..20.0 -> "It's ${temp}°C, perfect weather for a classic latte."
            else -> "It's ${temp}°C outside! Try an iced coffee for a refreshing twist!"
          }

          // Now update the UI on the main thread
          tvCoffeeRecommendation.text = recommendation
        } else {
          tvCoffeeRecommendation.text = "Weather data not available"
        }
      } catch (e: Exception) {
        e.printStackTrace()
        tvCoffeeRecommendation.text = "Weather data not available"
      }
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    val view = inflater.inflate(R.layout.fragment_home, container, false)

    swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
    swipeRefreshLayout.setOnRefreshListener { fetchPosts() }

    recyclerView = view.findViewById(R.id.recycler_view)
    recyclerView.layoutManager = LinearLayoutManager(requireContext())
    postAdapter = PostAdapter(postList, requireContext())
    recyclerView.adapter = postAdapter

    tvCoffeeRecommendation = view.findViewById(R.id.tvCoffeeRecommendation)

    val userDao = AppDatabase.getDatabase(requireContext()).userDao()
    val repository = UserRepository(userDao)
    val factory = UserViewModelFactory(repository)
    userViewModel = ViewModelProvider(this, factory).get(UserViewModel::class.java)

    viewLifecycleOwner.lifecycleScope.launch {
      delay(1000)
      userViewModel.getUsers()
    }
    userViewModel.users.observe(viewLifecycleOwner) { users ->
      Log.d("HomeFragment", "Users list from ViewModel: $users")
      if (users.isEmpty()) {
        Toast.makeText(requireActivity(), "Not logged in", Toast.LENGTH_SHORT).show()
        findNavController().navigate(R.id.action_homeFragment_to_logoutFragment)
      } else {
        renderNav(users[0])
      }
    }

    fetchPosts()
    fetchCurrentUserAndRenderNav()
    updateCoffeeRecommendation()

    return view
  }

  override fun onResume() {
    super.onResume()
    Log.d("HomeFragment", "onResume")
    fetchPosts()
    updateCoffeeRecommendation()
  }
}
