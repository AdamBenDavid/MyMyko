package com.example.cwc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.cwc.data.local.AppDatabase
import com.example.cwc.data.local.User
import com.example.cwc.data.repository.UserRepository
import com.example.cwc.databinding.FragmentLoginBinding
import com.example.cwc.viewmodel.UserViewModel
import com.example.cwc.viewmodel.UserViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import androidx.lifecycle.ViewModelProvider


class LoginFragment : Fragment() {
  private lateinit var auth: FirebaseAuth
  private lateinit var userViewModel: UserViewModel
  private val db = FirebaseFirestore.getInstance()
  private var _binding: FragmentLoginBinding? = null
  private val binding get() = _binding!!

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentLoginBinding.inflate(inflater, container, false)
    auth = FirebaseAuth.getInstance()
    checkIfLoggedIn()

    binding.submit.setOnClickListener {
      val email = binding.email.text.toString()
      val password = binding.password.text.toString()
      if (email.isEmpty() || password.isEmpty()) {
        Toast.makeText(requireContext(), "Email and Password cannot be empty", Toast.LENGTH_SHORT).show()
        return@setOnClickListener
      }
      handleLogin(email, password)
    }

    binding.registerButton.setOnClickListener {
      findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
    }

    return binding.root
  }

  private fun handleLogin(email: String, password: String) {
    auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(requireActivity()) {
      if (it.isSuccessful) {
        Toast.makeText(requireActivity(), "Successfully Logged In", Toast.LENGTH_SHORT).show()
        val userDao = AppDatabase.getDatabase(requireContext()).userDao()
        val repository = UserRepository(userDao)
        val factory = UserViewModelFactory(repository)
        userViewModel = ViewModelProvider(this, factory).get(UserViewModel::class.java)
        fetchUserData(email)
      } else {
        Toast.makeText(requireActivity(), "Log In failed", Toast.LENGTH_SHORT).show()
      }
    }
  }

  private fun fetchUserData(email: String) {
    db.collection("users").whereEqualTo("email", email).get()
      .addOnSuccessListener { documents: QuerySnapshot ->
        if (!documents.isEmpty) {
          val user = documents.documents[0].toObject(User::class.java)
          user?.let {
            storeUser(it.copy(imageBlob = null))
          }
        }
      }
  }

  private fun storeUser(user: User) {
    userViewModel.addUser(user)
    findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
  }

  private fun checkIfLoggedIn() {
    val currentUser = FirebaseAuth.getInstance().currentUser
    if (currentUser != null) {
      val navController = findNavController()
      if (navController.currentDestination?.id == R.id.loginFragment) {
        navController.navigate(R.id.action_loginFragment_to_homeFragment)
      }
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
