package com.alturya.fluenta.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
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

    @GET("api/user/badges")
    suspend fun getBadges(): BadgesResponse

    @GET("api/referral")
    suspend fun getReferral(): ReferralResponse

    @POST("api/referral/claim")
    suspend fun claimReferral(@Body body: ReferralClaimBody): ReferralClaimResponse

    // No-auth guest lesson for play-first onboarding
    @GET("api/guest/lesson")
    suspend fun getGuestLesson(
        @Query("l1") l1: String = "en",
        @Query("l2") l2: String = "es",
    ): GuestLessonResponse

    // Pronunciation speak_repeat assessment
    @Multipart
    @POST("api/pronunciation/assess")
    suspend fun assessPronunciation(
        @Part audio: MultipartBody.Part,
        @Part("text") text: RequestBody,
        @Part("l2") l2: RequestBody,
    ): PronunciationAssessResponse

    // Conversación de voz abierta (el wedge)
    @POST("api/conversation/start")
    suspend fun convoStart(@Body body: ConvoStartBody): ConvoStartResponse

    @POST("api/conversation/turn")
    suspend fun convoTurn(@Body body: ConvoTurnBody): ConvoTurnResponse

    @POST("api/conversation/end")
    suspend fun convoEnd(@Body body: ConvoEndBody): ConvoEndResponse

    // Guest (sin cuenta) — hablar en <60s antes de registrarse
    @POST("api/guest/conversation/start")
    suspend fun guestConvoStart(@Body body: GuestConvoStartBody): ConvoStartResponse

    @POST("api/guest/conversation/turn")
    suspend fun guestConvoTurn(@Body body: ConvoTurnBody): ConvoTurnResponse

    @POST("api/guest/conversation/end")
    suspend fun guestConvoEnd(@Body body: ConvoEndBody): ConvoEndResponse
}
