package com.charles.scamradar.app.data.feedback

import com.charles.scamradar.app.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import java.io.IOException
import java.util.concurrent.TimeUnit

interface GithubApi {
    @POST("repos/{owner}/{repo}/issues")
    suspend fun createIssue(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: CreateIssueRequest
    ): GithubIssue

    @GET("repos/{owner}/{repo}/issues/{number}")
    suspend fun getIssue(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int
    ): GithubIssue

    @GET("repos/{owner}/{repo}/issues/{number}/comments")
    suspend fun getComments(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int
    ): List<GithubComment>

    @POST("repos/{owner}/{repo}/issues/{number}/comments")
    suspend fun postComment(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int,
        @Body request: PostCommentRequest
    ): GithubComment

    @PUT("repos/{owner}/{repo}/contents/{assetDir}/{filename}")
    suspend fun uploadAsset(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("assetDir") assetDir: String,
        @Path("filename") filename: String,
        @Body request: UploadAssetRequest
    ): UploadAssetResponse
}

object GithubClient {
    val isConfigured: Boolean
        get() = BuildConfig.GITHUB_API_TOKEN.isNotBlank() &&
            BuildConfig.GITHUB_REPO_OWNER.isNotBlank() &&
            BuildConfig.GITHUB_REPO_NAME.isNotBlank()

    val configurationError: String?
        get() = when {
            BuildConfig.GITHUB_API_TOKEN.isBlank() ->
                "GitHub feedback is not configured: missing github.api.token."
            BuildConfig.GITHUB_REPO_OWNER.isBlank() ->
                "GitHub feedback is not configured: missing github.repo.owner."
            BuildConfig.GITHUB_REPO_NAME.isBlank() ->
                "GitHub feedback is not configured: missing github.repo.name."
            else -> null
        }

    val api: GithubApi by lazy {
        val logger = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization")
        }
        val headers = Interceptor { chain ->
            val builder = chain.request().newBuilder()
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "ScamRadar-Android/0.1")
            if (BuildConfig.GITHUB_API_TOKEN.isNotBlank()) {
                builder.header("Authorization", "Bearer ${BuildConfig.GITHUB_API_TOKEN}")
            }
            chain.proceed(builder.build())
        }
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .addInterceptor(headers)
            .addInterceptor(logger)
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GithubApi::class.java)
    }

    fun friendlyError(throwable: Throwable): String =
        when (throwable) {
            is HttpException -> "GitHub request failed (${throwable.code()})."
            is IOException -> "Network error while contacting GitHub. Check your connection and try again."
            else -> throwable.message ?: "GitHub request failed."
        }
}
