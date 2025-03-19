package com.example.mymyko.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mymyko.data.repository.PostRepository

class PostViewModel : ViewModel() {
  private val repository = PostRepository()

  // יצירת MutableLiveData להצלחה/כישלון
  private val _likeStatus = MutableLiveData<String>()
  val likeStatus: LiveData<String> get() = _likeStatus

  // פונקציה לעדכון הלייק
  fun updatePostLike(context: Context?, postId: String, userId: String) {
    context?.let {
      repository.updatePostLike(it, postId, userId)
      _likeStatus.value = "Like updated successfully"
    } ?: run {
      // במקרה שאין Context
      _likeStatus.value = "Context is not available"
    }
  }
}
