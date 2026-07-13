package com.charles.scamradar.app.family

import android.content.Context
import com.charles.scamradar.app.data.datastore.UserPrefs
import com.charles.scamradar.app.premium.EntitlementRepository
import com.charles.scamradar.app.premium.EntitlementState
import kotlinx.coroutines.flow.first

/**
 * Keeps Family pod coverage honest against live Play Billing state:
 *  - If this device is the organizer, push whether their own subscription is
 *    currently a Family plan so members can see it.
 *  - If this device joined someone else's pod, revoke the free FAMILY_MEMBER
 *    entitlement the moment the organizer's subscription is no longer active
 *    (and grant it if a previously-inactive organizer becomes active again),
 *    without ever touching a user's own paid PREMIUM/FAMILY tier.
 *
 * Called after every Play Billing entitlement refresh (app launch and after
 * any purchase update) so cancellations propagate promptly instead of only
 * on the next manual visit to the Premium screen.
 */
object FamilySync {

    suspend fun sync(context: Context) {
        val userPrefs = UserPrefs(context.applicationContext)
        val code = userPrefs.familyCode.first()
        if (code.isBlank()) return

        val entitlementRepository = EntitlementRepository(context.applicationContext)
        val familyRepo = FamilyRepository(context.applicationContext)
        val isOrganizer = userPrefs.familyIsOrganizer.first()
        val entitlement = entitlementRepository.entitlement.first()

        if (isOrganizer) {
            familyRepo.syncOrganizerStatus(code, active = entitlement == EntitlementState.FAMILY)
            return
        }

        if (entitlement != EntitlementState.FAMILY_MEMBER && entitlement != EntitlementState.FREE) {
            // User has their own paid PREMIUM/FAMILY subscription — leave it alone.
            return
        }

        val organizerActive = familyRepo.isOrganizerActive(code)
        when {
            organizerActive && entitlement == EntitlementState.FREE ->
                entitlementRepository.applyEntitlement(EntitlementState.FAMILY_MEMBER)
            !organizerActive && entitlement == EntitlementState.FAMILY_MEMBER ->
                entitlementRepository.applyEntitlement(EntitlementState.FREE)
        }
    }
}
