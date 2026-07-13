package com.charles.scamradar.app.data.feedback

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.charles.scamradar.app.BuildConfig
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

object ImageAttachmentHelper {
    fun uriToBase64(context: Context, uri: Uri): String {
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes()
        } ?: throw IllegalArgumentException("Could not open selected image.")
        if (bytes.isEmpty()) throw IllegalArgumentException("Selected image is empty.")
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    suspend fun uploadImage(context: Context, uri: Uri): String {
        val filename = uniqueFilename(uri)
        val response = GithubClient.api.uploadAsset(
            owner = BuildConfig.GITHUB_REPO_OWNER,
            repo = BuildConfig.GITHUB_REPO_NAME,
            assetDir = BuildConfig.FEEDBACK_ASSETS_DIR,
            filename = filename,
            request = UploadAssetRequest(
                message = "Upload feedback attachment $filename",
                content = uriToBase64(context, uri)
            )
        )
        return response.content?.downloadUrl
            ?: response.content?.htmlUrl
            ?: throw IllegalStateException("GitHub did not return an attachment URL.")
    }

    private fun uniqueFilename(uri: Uri): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.US))
        val extension = uri.lastPathSegment
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.lowercase(Locale.US)
            ?.takeIf { it in setOf("png", "jpg", "jpeg", "webp", "gif") }
            ?: "png"
        return "issue-$timestamp-${UUID.randomUUID().toString().take(8)}.$extension"
    }
}
