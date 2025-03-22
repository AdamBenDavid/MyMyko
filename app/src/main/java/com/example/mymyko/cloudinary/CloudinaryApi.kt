package com.example.mymyko.cloudinary

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface CloudinaryApi {

    // POST request to cloudinary to upload picture
    @Multipart
    @POST("v1_1/{cloudName}/image/upload")
    fun uploadImage(
        @Path("cloudName") cloudName: String,
        @Part file: MultipartBody.Part, // picture
        @Part("upload_preset") uploadPreset: RequestBody // upload without authentication
    ): Call<CloudinaryUploadResponse>

    // POST request to cloudinary to delete image
    @FormUrlEncoded
    @POST("v1_1/{cloudName}/image/destroy")
    fun deleteImage(
        @Path("cloudName") cloudName: String,
        @Field("public_id") publicId: String, // picture id to delete
        @Field("timestamp") timestamp: Long,
        @Field("signature") signature: String,
        @Field("api_key") apiKey: String
    ): Call<CloudinaryUploadResponse> // show cloudinary response
}
