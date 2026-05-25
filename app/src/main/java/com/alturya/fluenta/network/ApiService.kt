package com.alturya.fluenta.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    @POST("api/auth/phone-request")
    suspend fun requestOtp(@Body body: OtpRequestBody): OtpRequestResponse

    @POST("api/auth/phone-verify")
    suspend fun verifyOtp(@Body body: OtpVerifyBody): OtpVerifyResponse

    @GET("api/user/profile")
    suspend fun getProfile(): UserProfile

    @GET("api/user/progress")
    suspend fun getProgress(): UserProgress

    @GET("api/curriculum/map")
    suspend fun getCurriculumMap(): CurriculumMapResponse

    @GET("api/stripe/portal")
    suspend fun getPortalUrl(): StripeUrlResponse

    @GET("api/stripe/checkout")
    suspend fun getCheckoutUrl(@Query("plan") plan: String): StripeUrlResponse

    @POST("api/languages/select")
    suspend fun selectLanguage(@Body body: SelectLanguageBody): SelectLanguageResponse
}