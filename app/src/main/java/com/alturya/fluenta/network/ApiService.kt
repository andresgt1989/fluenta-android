package com.alturya.fluenta.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
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

    @POST("api/user/errors/{id}/review")
    suspend fun reviewError(@Path("id") id: String, @Body body: ErrorReviewBody): ErrorReviewResponse

    @GET("api/user/skills")
    suspend fun getSkills(): SkillsResponse

    @GET("api/user/coach-message")
    suspend fun getCoachMessage(): CoachMessageResponse

    @GET("api/pronunciation/drill")
    suspend fun getDrill(@Query("phoneme") phoneme: String? = null): DrillResponse

    @GET("api/exercises/match")
    suspend fun getMatchExercise(): MatchResponse

    @POST("api/feedback")
    suspend fun sendFeedback(@Body body: FeedbackBody)

    @POST("api/diagnostic/start")
    suspend fun diagnosticStart(): DiagnosticStartResponse

    @POST("api/diagnostic/answer")
    suspend fun diagnosticAnswer(@Body body: DiagnosticAnswerBody): DiagnosticAnswerResponse

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

    @GET("api/lessons/{id}/play")
    suspend fun getLessonPlay(@Path("id") id: String): LessonPlayResponse

    @POST("api/lessons/{id}/check")
    suspend fun checkExercise(@Path("id") id: String, @Body body: ExerciseCheckBody): ExerciseCheckResponse

    @POST("api/lessons/{id}/submit")
    suspend fun submitLesson(@Path("id") id: String, @Body body: LessonSubmitBody): LessonSubmitResponse

    @GET("api/handoff/whatsapp")
    suspend fun getWhatsAppHandoff(
        @Query("intent") intent: String,
        @Query("lessonId") lessonId: String? = null,
    ): HandoffResponse

    @GET("api/verbs/today")
    suspend fun getVerbsToday(): VerbsTodayResponse

    @POST("api/verbs/{id}/answer")
    suspend fun answerVerb(@Path("id") id: String, @Body body: VerbAnswerBody): VerbAnswerResponse

    @GET("api/leagues/me")
    suspend fun getMyLeague(): LeagueResponse

    @POST("api/notifications/register")
    suspend fun registerFcmToken(@Body body: FcmRegisterBody): FcmRegisterResponse

    @GET("api/i18n/ui")
    suspend fun getUiStrings(@Query("lang") lang: String): UiStringsResponse
}
