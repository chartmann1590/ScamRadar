package com.charles.scamradar.app.premium

enum class EntitlementState {
    FREE,
    PREMIUM,
    FAMILY,
    FAMILY_MEMBER;

    fun unlocksPremium(): Boolean = this == PREMIUM || this == FAMILY || this == FAMILY_MEMBER

    fun unlocksFamily(): Boolean = this == FAMILY || this == FAMILY_MEMBER

    fun isFamilyOrganizer(): Boolean = this == FAMILY
}

object EntitlementSku {
    const val PREMIUM_MONTHLY = "scamradar_premium_monthly"
    const val PREMIUM_YEARLY = "scamradar_premium_yearly"
    const val FAMILY_MONTHLY = "scamradar_family_monthly"
    const val FAMILY_YEARLY = "scamradar_family_yearly"
}
