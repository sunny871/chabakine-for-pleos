package kr.sanho.chabakine_pleos.app

import kr.sanho.chabakine_pleos.vehicle.DrivingState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DepartRuleTest {

    @Test
    fun parkedKeepsScreen() = assertNull(departTarget(listOf(Route.CampMode), DrivingState.PARKED, campPromptOpen = true))

    @Test
    fun parkedOnlyScreenClosesToFind() =
        assertEquals(Route.Find(), departTarget(listOf(Route.Home, Route.Settings), DrivingState.DRIVING, campPromptOpen = false))

    @Test
    fun homeAndFavoritesStay() {
        assertNull(departTarget(listOf(Route.Home), DrivingState.DRIVING, campPromptOpen = false))
        assertNull(departTarget(listOf(Route.Favorites), DrivingState.UNKNOWN, campPromptOpen = false))
    }

    @Test
    fun openCampPromptClosesToFindEvenOverHome() =
        assertEquals(Route.Find(), departTarget(listOf(Route.Home), DrivingState.DRIVING, campPromptOpen = true))

    @Test
    fun firstRunGoesToOnboardingDriving() {
        assertEquals(Route.OnboardingDriving, departTarget(listOf(Route.Onboarding(2)), DrivingState.DRIVING, campPromptOpen = false))
        assertEquals(Route.OnboardingDriving, departTarget(listOf(Route.Onboarding(0)), DrivingState.UNKNOWN, campPromptOpen = false))
    }

    @Test
    fun replayFromSettingsClosesToFind() =
        assertEquals(Route.Find(), departTarget(listOf(Route.Onboarding(1, replay = true)), DrivingState.DRIVING, campPromptOpen = false))

    @Test
    fun parkedConfirmClosesOnlyWhenSystemSaysDriving() {
        assertNull(departTarget(listOf(Route.Home, Route.ParkedConfirm), DrivingState.UNKNOWN, campPromptOpen = false))
        assertEquals(Route.Find(), departTarget(listOf(Route.Home, Route.ParkedConfirm), DrivingState.DRIVING, campPromptOpen = false))
    }

    @Test
    fun guidingStaysAndPhotoViewerCloses() {
        assertNull(departTarget(listOf(Route.Home, Route.NaviGuiding("p1")), DrivingState.DRIVING, campPromptOpen = false))
        assertEquals(Route.Find(), departTarget(listOf(Route.Find(), Route.PhotoViewer("p1", 2)), DrivingState.DRIVING, campPromptOpen = false))
    }
}
