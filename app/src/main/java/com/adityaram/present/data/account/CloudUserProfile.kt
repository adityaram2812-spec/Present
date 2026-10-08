package com.adityaram.present.data.account

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class CloudUserProfile(
    val uid: String = "",
    val displayName: String? = null,
    val email: String? = null,
    val photoUrl: String? = null,
    @ServerTimestamp val createdAt: Date? = null,
    @ServerTimestamp val updatedAt: Date? = null
)
