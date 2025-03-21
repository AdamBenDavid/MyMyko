package com.example.mymyko.data.local


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [User::class], version = 7)
abstract class AppDatabase : RoomDatabase() {
  abstract fun userDao(): UserDao

  companion object {
    @Volatile private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "mymyco"
        ).addMigrations(MIGRATION_5_6, MIGRATION_6_7).build()
        INSTANCE = instance
        instance
      }
    }
  }
}
