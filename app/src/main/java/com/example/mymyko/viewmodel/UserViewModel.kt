package com.example.mymyko.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mymyko.data.local.User
import com.example.mymyko.data.repository.UserRepository
import kotlinx.coroutines.launch

class UserViewModel(private val repository: UserRepository) : ViewModel() {

  private val _users = MutableLiveData<List<User>>()
  val users: LiveData<List<User>> get() = _users

  private val _user = MutableLiveData<User?>()
  val user: LiveData<User?> get() = _user

  private val _logoutStatus = MutableLiveData<Boolean>()

  fun addUser(user: User) {
    viewModelScope.launch {
      repository.insertUser(user)
    }
  }

  fun logout() {
    viewModelScope.launch {
      repository.logout()
      _logoutStatus.postValue(true)
    }
  }
}
