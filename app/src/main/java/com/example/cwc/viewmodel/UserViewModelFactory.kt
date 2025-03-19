package com.example.mymyko.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mymyko.data.repository.UserRepository

class UserViewModelFactory(private val repository: UserRepository) : ViewModelProvider.Factory {

  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    // בדוק אם המחלקה הנדרשת היא UserViewModel
    if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
      return UserViewModel(repository) as T
    }

    // זרוק חריגה מדויקת אם המחלקה אינה תואמת
    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
  }
}
