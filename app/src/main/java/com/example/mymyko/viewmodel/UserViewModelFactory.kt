package com.example.mymyko.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mymyko.data.repository.UserRepository

//build UserViewModel with user Repository
class UserViewModelFactory(private val repository: UserRepository) : ViewModelProvider.Factory {

  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    // if we need userViewModel
    if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
      return UserViewModel(repository) as T
    }

    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
  }
}
