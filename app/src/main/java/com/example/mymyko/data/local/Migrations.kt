package com.example.mymyko.data.local


import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// update room between versions
val MIGRATION_5_6 = object : Migration(5, 6) {
  override fun migrate(database: SupportSQLiteDatabase) {
    database.execSQL("ALTER TABLE user DROP COLUMN profileImageUrl")
    database.execSQL("ALTER TABLE user ADD COLUMN profileImageUrl TEXT")
    database.execSQL("ALTER TABLE user ADD COLUMN image BLOB")

  }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
  override fun migrate(database: SupportSQLiteDatabase) {

    database.execSQL("ALTER TABLE user DROP COLUMN image")
    database.execSQL("ALTER TABLE user ADD COLUMN imageBlob BLOB")

  }
}
