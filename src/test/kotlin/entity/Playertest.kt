package entity

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Tests [Player] initialization and basic collection updates (hand and gainedStack). */
class PlayerTest {

    private val card1 = Card(CardSuit.CLUBS, CardValue.TWO)
    private val card2 = Card(CardSuit.HEARTS, CardValue.QUEEN)
    private val card3 = Card(CardSuit.SPADES, CardValue.KING)

    /** Verifies a new Player has the correct name and empty collections. */
    @Test
    fun createPlayer_hasEmptyCollections() {
        val player = Player("Ahmed")

        assertEquals("Ahmed", player.name)
        assertTrue(player.hand.isEmpty())
        assertTrue(player.gainedStack.isEmpty())
    }

    /** Verifies cards can be added to hand and gainedStack and are stored correctly. */
    @Test
    fun addCardsToHandAndGainedStack() {
        val player = Player("Ahmed")

        player.hand.add(card1)
        player.gainedStack.addAll(listOf(card2, card3))

        assertEquals(listOf(card1), player.hand)
        assertEquals(listOf(card2, card3), player.gainedStack)
    }
}

