package com.example.mymyko.data.repository

import com.example.mymyko.data.local.UserDao
import com.example.mymyko.data.local.User

class UserRepository(private val userDao: UserDao) {

  suspend fun insertUser(user: User) {
    var users: List<User> = this.getAllUsers()
    for (iterable_user in users) {

      this.deleteUser(iterable_user)

    }
    userDao.insertUser(user)
  }

  suspend fun getUser(id: String): User? = userDao.getUser(id)

  suspend fun getAllUsers(): List<User> {
    return userDao.getAllUsers()
  }

  suspend fun deleteUser(user: User) {
    userDao.deleteUser(user)
  }

  suspend fun logout() {
    userDao.logout()
  }

}
