package com.adityaram.present.data.import

import com.google.gson.annotations.SerializedName

/**
 * Maps the 3-tier JSON structure established natively by the Express Gemini backend.
 */
data class AiBackendResponse(
    @SerializedName("division") val division: String?,
    @SerializedName("groups_found") val groupsFound: List<String>?,
    @SerializedName("classes") val classes: List<AiClassData>?
)

data class AiClassData(
    @SerializedName("day") val day: String?,
    @SerializedName("startTime") val startTime: String?,
    @SerializedName("endTime") val endTime: String?,
    @SerializedName("subject") val subject: String?,
    @SerializedName("teacher") val teacher: String?,
    @SerializedName("room") val room: String?,
    @SerializedName("group") val group: String?
)
