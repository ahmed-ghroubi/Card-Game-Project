package entity

import kotlin.test.*

/** Tests creation and field of [PlayerAction] for different action types. */
class PlayerActionTest {

    private val played = Card(CardSuit.HEARTS, CardValue.TEN)
    private val target = Card(CardSuit.CLUBS, CardValue.JACK)

    /** Verifies a fully specified PlayerAction stores all provided values correctly. */
    @Test
    fun testPlayerActionCreation() {
        val action = PlayerAction(
            playerId   = 1,
            action     = Action.DESTROY,
            playedCard = null,
            targetCard = target,
            position   = Pair(0, 2)   // e.g. row=0, col=2
        )

        assertEquals(1, action.playerId)
        assertEquals(Action.DESTROY, action.action)
        assertNull(action.playedCard)
        assertEquals(target, action.targetCard)
        assertEquals(Pair(0, 2), action.position)
    }

    /** Verifies optional fields are null for DRAW and set for COMBINE actions. */
    @Test
    fun testDifferentActions() {
        // DRAW: no cards/position needed
        val drawAction = PlayerAction(
            playerId = 0,
            action   = Action.DRAW
        )

        // COMBINE: uses played + target + position
        val combineAction = PlayerAction(
            playerId   = 1,
            action     = Action.COMBINE,
            playedCard = played,
            targetCard = target,
            position   = Pair(3, 1)
        )

        assertEquals(Action.DRAW, drawAction.action)
        assertNull(drawAction.playedCard)
        assertNull(drawAction.targetCard)
        assertNull(drawAction.position)

        assertEquals(Action.COMBINE, combineAction.action)
        assertEquals(played, combineAction.playedCard)
        assertEquals(target, combineAction.targetCard)
        assertEquals(Pair(3, 1), combineAction.position)

        assertNotEquals(drawAction, combineAction)
    }
}


