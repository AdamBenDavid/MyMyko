package com.example.mymyko.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mymyko.data.local.AppDatabase
import com.example.mymyko.data.local.User
import com.example.mymyko.data.repository.UserRepository
import com.example.mymyko.databinding.FragmentLoginBinding
import com.example.mymyko.viewmodel.UserViewModel
import com.example.mymyko.viewmodel.UserViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import androidx.lifecycle.ViewModelProvider
import com.example.mymyko.R

class LoginFragment : Fragment() {
    private lateinit var auth: FirebaseAuth // fireBase manager
    private lateinit var userViewModel: UserViewModel // save user in Room (local db)
    private val db = FirebaseFirestore.getInstance()
    private var _binding: FragmentLoginBinding? = null // get view from xml
    private val binding get() = _binding!!

    override fun onCreateView(
          inflater: LayoutInflater, container: ViewGroup?,
          savedInstanceState: Bundle?
        ): View {
          _binding = FragmentLoginBinding.inflate(inflater, container, false)
          auth = FirebaseAuth.getInstance()
          checkIfLoggedIn()

          // click on submit button
          binding.submit.setOnClickListener {
            val email = binding.email.text.toString()
            val password = binding.password.text.toString()

            // if there is user login
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Email and Password cannot be empty",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            // if there is not user login
            handleLogin(email, password)
        }

        // navigate to register
        binding.registerButton.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        return binding.root
    }

    // login with Firebase
    private fun handleLogin(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(requireActivity()) {
            // save user in local db with ROOM
            if (it.isSuccessful) {
                Toast.makeText(requireActivity(), "Successfully Logged In", Toast.LENGTH_SHORT)
                    .show()
                val userDao = AppDatabase.getDatabase(requireContext()).userDao() //get ROOM
                val repository = UserRepository(userDao) // get DAO from ROOM object
                val factory = UserViewModelFactory(repository)
                userViewModel = ViewModelProvider(this, factory).get(UserViewModel::class.java)
                fetchUserData(email) // get user from firestore by email
            } else {
                Toast.makeText(requireActivity(), "Log In failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // get user from firestore by email
    private fun fetchUserData(email: String) {
        db.collection("users").whereEqualTo("email", email).get()
            .addOnSuccessListener { documents: QuerySnapshot ->
                if (!documents.isEmpty) {
                    val user = documents.documents[0].toObject(User::class.java) // convert to user
                    user?.let {
                        storeUser(it.copy(imageBlob = null))
                    }
                }
            }
    }

    // save user in Room
    private fun storeUser(user: User) {
        userViewModel.addUser(user)
        findNavController().navigate(R.id.action_loginFragment_to_homeFragment) //navigate to hove fragment
    }

    // if user already logged in
    private fun checkIfLoggedIn() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) { // if yes, skip login page
            val navController = findNavController()
            if (navController.currentDestination?.id == R.id.loginFragment) {
                navController.navigate(R.id.action_loginFragment_to_homeFragment)
            }
        }
    }

    // clean binding when close the window to prevent memory leaks
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
