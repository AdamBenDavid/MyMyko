package com.example.mymyko

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.mymyko.data.local.AppDatabase
import com.example.mymyko.data.repository.UserRepository
import com.example.mymyko.viewmodel.UserViewModel
import com.example.mymyko.viewmodel.UserViewModelFactory
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LogoutFragment : Fragment() {
  private lateinit var userViewModel: UserViewModel

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    Firebase.auth.signOut()
    val userDao = AppDatabase.getDatabase(requireContext()).userDao()
    val repository = UserRepository(userDao)
    val factory = UserViewModelFactory(repository)
    userViewModel = ViewModelProvider(this, factory).get(UserViewModel::class.java)
    userViewModel.logout()
    findNavController().navigate(R.id.action_logoutFragment_to_loginFragment)
    return inflater.inflate(R.layout.fragment_logout, container, false)
  }
}
