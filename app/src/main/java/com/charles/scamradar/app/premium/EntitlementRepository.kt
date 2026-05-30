package com.charles.scamradar.app.premium

import android.content.Context
import com.charles.scamradar.app.data.datastore.UserPrefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EntitlementRepository(context: Context) {

    private val userPrefs = UserPrefs(context.applicationContext)

    val entitlement: Flow<EntitlementState> = userPrefs.tier.map { stored ->
        when (stored) {
            UserPrefs.TIER_PREMIUM -> EntitlementState.PREMIUM
            UserPrefs.TIER_FAMILY -> EntitlementState.FAMILY
            UserPrefs.TIER_FAMILY_MEMBER -> EntitlementState.FAMILY_MEMBER
            else -> EntitlementState.FREE
        }
    }

    suspend fun applyEntitlement(state: EntitlementState) {
        val tier = when (state) {
            EntitlementState.PREMIUM -> UserPrefs.TIER_PREMIUM
            EntitlementState.FAMILY -> UserPrefs.TIER_FAMILY
            EntitlementState.FAMILY_MEMBER -> UserPrefs.TIER_FAMILY_MEMBER
            EntitlementState.FREE -> UserPrefs.TIER_FREE
        }
        userPrefs.setTier(tier)
    }
}
