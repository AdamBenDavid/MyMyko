package com.example.mymyko.data.local

import androidx.room.*

@Dao
interface UserDao {
  @Insert
  suspend fun insertUser(user: User)

  @Query("SELECT * FROM user")
  suspend fun getAllUsers(): List<User>

  @Query("SELECT * FROM user where id = :id")
  suspend fun getUser(id: String): User?

  @Delete
  suspend fun deleteUser(user: User)

  @Query("DELETE FROM user")
  suspend fun logout()

}
