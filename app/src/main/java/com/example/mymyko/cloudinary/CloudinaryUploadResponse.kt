package com.example.mymyko.cloudinary

// This data class matches the JSON response from Cloudinary
data class CloudinaryUploadResponse(
    val public_id: String?,
    val version: Long?,
    val signature: String?,
    val width: Int?,
    val height: Int?,
    val format: String?,
    val resource_type: String?,
    val created_at: String?,
    val tags: List<String>?,
    val bytes: Int?,
    val type: String?,
    val etag: String?,
    val placeholder: Boolean?,
    val url: String?,
    val secure_url: String?
)
