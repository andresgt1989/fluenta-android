package com.alturya.fluenta.network

import com.google.gson.annotations.SerializedName

data class OtpRequestBody(val phone: String)
data class OtpVerifyBody(val phone: String, val code: String)
data class OtpRequestResponse(val ok: Boolean, val delivered: Boolean?, val message: String?)
data class OtpVerifyResponse(val ok: Boolean, val token: String?, val isNewUser: Boolean?)

data class UserProfile(
    val id: String,
    val phone: String,
    val l1: String?,
    val l2: String?,
    val level: String?,
    val levelSystem: String?,
    val goalTrack: String?,
    val plan: String?,
    val streakDays: Int?,
    val totalXp: Int?,
    val currentState: String?
)

data class UserProgress(
    val streakDays: Int,
    val totalXp: Int,
    val completedLessons: Int,
    val l1: String?,
    val l2: String?,
    val level: String?,
    val levelSystem: String?
)

data class Lesson(
    val id: String,
    val number: Int,
    val title: String,
    val type: String,
    val completed: Boolean
)

data class CurriculumUnit(
    val id: String,
    val number: Int,
    val title: String,
    val description: String?,
    val lessons: List<Lesson>
)

data class CurriculumMapResponse(
    val map: List<CurriculumUnit>,
    val l1: String?,
    val l2: String?
)

data class LanguagePair(
    val l1: String,
    val l2: String,
    val tier: String?,
    val levelSystem: String?,
    val script: String?,
    val curriculumSeeded: Boolean?,
    val notes: String?
)

data class LanguagesResponse(val pairs: List<LanguagePair>)

data class ErrorItem(
    val id: String?,
    @SerializedName("error_type") val errorType: String?,
    @SerializedName("error_category") val errorCategory: String?,
    val original: String?,
    val corrected: String?,
    val priority: String?,
    @SerializedName("review_count") val reviewCount: Int?,
    @SerializedName("next_review_at") val nextReviewAt: String?,
    @SerializedName("mastered_at") val masteredAt: String?,
    @SerializedName("first_seen_at") val firstSeenAt: String?
)

data class ErrorsResponse(val errors: List<ErrorItem>)

data class Skill(
    val key: String,
    val label: String,
    val score: Int?,
    val open: Int,
    val mastered: Int,
    val total: Int
)

data class SkillsResponse(
    val skills: List<Skill>,
    val wpm: Int,
    val taskSuccessRate: Int,
    val totalOutputWords: Int
)

data class NextLesson(
    val id: String?,
    @SerializedName("lesson_number") val lessonNumber: Int?,
    val title: String?,
    @SerializedName("lesson_type") val lessonType: String?,
    @SerializedName("unit_title") val unitTitle: String?
)

data class NextLessonResponse(val next: NextLesson?, val message: String?)

data class StripeUrlResponse(val url: String?)
data class SelectLanguageBody(val l2: String)
data class SelectLanguageResponse(val ok: Boolean, val l1: String?, val l2: String?, val levelSystem: String?)
