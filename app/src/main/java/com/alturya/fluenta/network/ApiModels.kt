package com.alturya.fluenta.network

package com.alturya.fluenta.network

data class OtpRequestBody(val phone: String)
data class OtpVerifyBody(val phone: String, val code: String)
data class OtpRequestResponse(val ok: Boolean, val message: String?)
data class OtpVerifyResponse(val ok: Boolean, val token: String?, val isNewUser: Boolean?)

data class UserProfile(
    val id: String,
    val phone: String,
    val l1: String?,
    val l2: String?,
    val level: String?,
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
    val level: String?
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

data class StripeUrlResponse(val url: String?)
data class SelectLanguageBody(val l2: String)
data class SelectLanguageResponse(val ok: Boolean, val l1: String?, val l2: String?)