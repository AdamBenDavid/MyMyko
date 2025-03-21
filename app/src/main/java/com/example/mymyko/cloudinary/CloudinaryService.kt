package com.example.mymyko.cloudinary

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object CloudinaryService {
    private const val BASE_URL = "https://api.cloudinary.com/"

    val api: CloudinaryApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CloudinaryApi::class.java)
    }
}
