package service

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RootServiceTest {

    /** Ensures addRefreshables registers all refreshables and they receive start-game updates. */
    @Test
    fun addRefreshables_registersAll() {
        val rootService = RootService()
        val ref1 = TestRefreshable(rootService)
        val ref2 = TestRefreshable(rootService)


        rootService.addRefreshables(ref1, ref2)


        rootService.gameService.startGame(listOf("ahmed", "wiem"))


        assertNotNull(rootService.currentGame)

        // both refreshables must have been registered
        assertTrue(ref1.refreshAfterGameStartCalled)
        assertTrue(ref2.refreshAfterGameStartCalled)
    }
}

