package com.example.mymyko.cloudinary

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface CloudinaryApi {

    // Endpoint for image uploads.
    @Multipart
    @POST("v1_1/{cloudName}/image/upload")
    fun uploadImage(
        @Path("cloudName") cloudName: String,
        @Part file: MultipartBody.Part,
        @Part("upload_preset") uploadPreset: RequestBody
    ): Call<CloudinaryUploadResponse>

    // Endpoint for deleting an image.
    @FormUrlEncoded
    @POST("v1_1/{cloudName}/image/destroy")
    fun deleteImage(
        @Path("cloudName") cloudName: String,
        @Field("public_id") publicId: String,
        @Field("timestamp") timestamp: Long,
        @Field("signature") signature: String,
        @Field("api_key") apiKey: String
    ): Call<CloudinaryUploadResponse>
}
