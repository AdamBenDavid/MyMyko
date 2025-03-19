package com.example.mymyko.viewmodel

import android.graphics.BitmapFactory
import android.widget.ImageView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mymyko.data.local.User
import com.example.mymyko.data.repository.UserRepository
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch

class UserViewModel(private val repository: UserRepository) : ViewModel() {

  private val _users = MutableLiveData<List<User>>()
  val users: LiveData<List<User>> get() = _users

  private val _user = MutableLiveData<User?>()
  val user: LiveData<User?> get() = _user

  private val _logoutStatus = MutableLiveData<Boolean>()
  val logoutStatus: LiveData<Boolean> get() = _logoutStatus

  fun addUser(user: User) {
    viewModelScope.launch {
      repository.insertUser(user)
    }
  }

  fun getUsers() {
    viewModelScope.launch {
      val userList = repository.getAllUsers()
      _users.value = userList
    }
  }

  fun getUser(id: String) {
    viewModelScope.launch {
      val retrievedUser = repository.getUser(id)
      _user.postValue(retrievedUser)
    }
  }

  fun deleteUser(user: User) {
    viewModelScope.launch {
      repository.deleteUser(user)
    }
  }

  fun logout() {
    viewModelScope.launch {
      repository.logout()
      _logoutStatus.postValue(true)
    }
  }

  // If imageBlob is null, load the image from profileImageUrl via Picasso
  fun loadProfilePicture(userId: String, profileImage: ImageView) {
    user.observeForever { user ->
      user?.let {
        if (it.imageBlob != null) {
          val bitmap = BitmapFactory.decodeByteArray(it.imageBlob, 0, it.imageBlob.size)
          profileImage.setImageBitmap(bitmap)
        } else if (!it.profileImageUrl.isNullOrEmpty()) {
          Picasso.get()
            .load(it.profileImageUrl)
            .placeholder(com.example.mymyko.R.drawable.profile_foreground)
            .error(com.example.mymyko.R.drawable.profile_foreground)
            .into(profileImage)
        }
      }
    }
  }
}
