package com.example.mymyko.cloudinary

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// singleton to cloudinary (global)
object CloudinaryService {
    private const val BASE_URL = "https://api.cloudinary.com/"

    // lazy = build only if needed
    val api: CloudinaryApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) // convert json to kotlin
            .build()
            .create(CloudinaryApi::class.java)
    }
}
