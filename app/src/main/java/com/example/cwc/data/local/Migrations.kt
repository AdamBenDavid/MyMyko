package com.example.mymyko.data.local


import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
  override fun migrate(database: SupportSQLiteDatabase) {
    // Add a new table or column, or make other schema changes here
    database.execSQL("ALTER TABLE user ADD COLUMN email TEXT")
  }
}

val MIGRATION_2_3 = object : Migration(2, 3) {

  override fun migrate(database: SupportSQLiteDatabase) {
    // Add a new table or column, or make other schema changes here
    database.execSQL("ALTER TABLE users RENAME TO user")
    database.execSQL("ALTER TABLE user ADD COLUMN firstname TEXT DEFAULT 'undefined'")
    database.execSQL("ALTER TABLE user ADD COLUMN lastname TEXT DEFAULT 'undefined'")
    database.execSQL("ALTER TABLE user ADD COLUMN city TEXT DEFAULT 'undefined'")
    database.execSQL("ALTER TABLE user ADD COLUMN country TEXT DEFAULT 'undefined'")
  }

}

val MIGRATION_3_4 = object : Migration(3, 4) {
  override fun migrate(database: SupportSQLiteDatabase) {

    database.execSQL("ALTER TABLE user ADD COLUMN image BLOB")

  }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
  override fun migrate(database: SupportSQLiteDatabase) {

    database.execSQL("ALTER TABLE user DROP COLUMN image")
    database.execSQL("ALTER TABLE user ADD COLUMN profileImageUrl BLOB")

  }
}

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
