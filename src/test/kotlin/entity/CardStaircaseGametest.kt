package entity

import kotlin.test.*

/** Tests [CardStaircaseGame] initialization, state changes, and point calculation logic. */
class CardStaircaseGameTest {

    private val card1 = Card(CardSuit.HEARTS, CardValue.TWO)
    private val card2 = Card(CardSuit.SPADES, CardValue.QUEEN)
    private val card3 = Card(CardSuit.CLUBS, CardValue.JACK)
    private val card4 = Card(CardSuit.CLUBS, CardValue.TWO)
    private val card5 = Card(CardSuit.CLUBS, CardValue.THREE)
    private val card6 = Card(CardSuit.DIAMONDS, CardValue.ACE)

    private val player1 = Player("Ahmed")
    private val player2 = Player("Wiem")

    /** Verifies constructor assigns all fields correctly and history starts empty. */
    @Test
    fun testGameInitialization() {
        val drawstack = mutableListOf(card1, card2)
        val dumpstack = mutableListOf(card3)

        // staircase as list-of-lists (2 rows here just for testing)
        val staircase = mutableListOf(
            mutableListOf(card4),               // row 0
            mutableListOf(card5, card6)         // row 1
        )

        // structured history (empty at start, or add an initial action )
        val history = mutableListOf<PlayerAction>()

        val game = CardStaircaseGame(
            currentplayer = 0,
            hasstaircasechanged = false,
            player1 = player1,
            player2 = player2,
            drawstack = drawstack,
            dumpstack = dumpstack,
            staircase = staircase,
            gamehistory = history
        )

        assertEquals(0, game.currentplayer)
        assertFalse(game.hasstaircasechanged)
        assertEquals(player1, game.player1)
        assertEquals(player2, game.player2)
        assertEquals(drawstack, game.drawstack)
        assertEquals(dumpstack, game.dumpstack)
        assertEquals(staircase, game.staircase)
        assertTrue(game.gamehistory.isEmpty())
    }

    /** Verifies mutable state (turn, flags, history) can be updated as expected. */
    @Test
    fun testModifyGameState() {
        val staircase = mutableListOf(
            mutableListOf(card4),
            mutableListOf(card5, card6)
        )

        val game = CardStaircaseGame(
            currentplayer = 1,
            hasstaircasechanged = true,
            player1 = player1,
            player2 = player2,
            staircase = staircase,
            gamehistory = mutableListOf()
        )

        // modify turn
        game.currentplayer = 0
        game.hasstaircasechanged = false

        // addinga structured history
        game.gamehistory.add(
            PlayerAction(
                playerId = 0,
                action = Action.DRAW
            )
        )

        assertEquals(0, game.currentplayer)
        assertFalse(game.hasstaircasechanged)
        assertEquals(1, game.gamehistory.size)
        assertEquals(Action.DRAW, game.gamehistory.last().action)
        assertEquals(0, game.gamehistory.last().playerId)
    }

    /** Verifies getPlayerPoints sums gained cards when no destroy penalty applies. */
    @Test
    fun testGetPlayerPoints_noPenalty() {
        val game = CardStaircaseGame(
            currentplayer = 0,
            hasstaircasechanged = false,
            player1 = player1,
            player2 = player2,
            staircase = mutableListOf(),
            gamehistory = mutableListOf()
        )

        // Player1 collects two cards
        player1.gainedStack.addAll(listOf(card1, card2))
        val expected = card1.getPoints() + card2.getPoints()

        assertEquals(expected, game.getPlayerPoints(player1))
        // Player2 has nothing yet
        assertEquals(0, game.getPlayerPoints(player2))
    }

    /** Verifies destroy penalties are applied per player based on their own DESTROY actions only. */
    @Test
    fun testGetPlayerPoints_withDestroyPenalty_onlyOwnDestroysCount() {

        val game = CardStaircaseGame(
            currentplayer = 0,
            hasstaircasechanged = false,
            player1 = player1,
            player2 = player2,
            staircase = mutableListOf(),
            gamehistory = mutableListOf()
        )

        // Player1 collects three cards
        player1.gainedStack.addAll(listOf(card1, card2, card3))
        val scoreP1 = card1.getPoints() + card2.getPoints() + card3.getPoints()
        // Player2 collects one card
        player2.gainedStack.add(card2)
        val scoreP2 = card2.getPoints()

        // History: two DESTROY by player1 (id 0), one DESTROY by player2 (id 1)
        game.gamehistory.add(PlayerAction(playerId = 0, action = Action.DESTROY))
        game.gamehistory.add(PlayerAction(playerId = 0, action = Action.DESTROY))
        game.gamehistory.add(PlayerAction(playerId = 1, action = Action.DESTROY))
        // Player1: score- 2 * 5
        assertEquals(scoreP1 - 10, game.getPlayerPoints(player1))

        // Player2: score - 1 * 5
        assertEquals(scoreP2 - 5, game.getPlayerPoints(player2))
    }

    /** Verifies points and destroy penalties are computed independently for each player. */
    @Test
    fun testGetPlayerPoints_independentPlayers() {
        val game = CardStaircaseGame(
            currentplayer = 0,
            hasstaircasechanged = false,
            player1 = player1,
            player2 = player2,
            staircase = mutableListOf(),
            gamehistory = mutableListOf()
        )

        // P1 gains card5; P2 gains card6
        player1.gainedStack.add(card5)
        player2.gainedStack.add(card6)

        // One destroy by P2 only
        game.gamehistory.add(PlayerAction(playerId = 1, action = Action.DESTROY))

        val expectedP1 = card5.getPoints()            // no penalty for P1
        val expectedP2 = card6.getPoints() - 5        // 1 destroy penalty

        assertEquals(expectedP1, game.getPlayerPoints(player1))
        assertEquals(expectedP2, game.getPlayerPoints(player2))
    }

}

