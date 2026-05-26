package com.alturya.fluenta.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Streaming

interface ApiService {

    @Streaming
    @GET("api/tts")
    suspend fun getTts(@Query("text") text: String): Response<ResponseBody>

    @POST("api/auth/phone-request")
    suspend fun requestOtp(@Body body: OtpRequestBody): OtpRequestResponse

    @POST("api/auth/phone-verify")
    suspend fun verifyOtp(@Body body: OtpVerifyBody): OtpVerifyResponse

    @GET("api/user/profile")
    suspend fun getProfile(): UserProfile

    @GET("api/user/progress")
    suspend fun getProgress(): UserProgress

    @GET("api/user/errors")
    suspend fun getErrors(): ErrorsResponse

    @GET("api/user/skills")
    suspend fun getSkills(): SkillsResponse

    @GET("api/curriculum/map")
    suspend fun getCurriculumMap(): CurriculumMapResponse

    @GET("api/languages")
    suspend fun getLanguages(): LanguagesResponse

    @POST("api/languages/select")
    suspend fun selectLanguage(@Body body: SelectLanguageBody): SelectLanguageResponse

    @GET("api/lessons/next")
    suspend fun getNextLesson(): NextLessonResponse

    @GET("api/stripe/portal")
    suspend fun getPortalUrl(): StripeUrlResponse

    @GET("api/stripe/checkout")
    suspend fun getCheckoutUrl(@Query("plan") plan: String): StripeUrlResponse
}
