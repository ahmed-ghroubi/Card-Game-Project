package service

import entity.*
import kotlin.test.*

/**
 * Test suite for [GameService].
 *
 * Verifies game start, log formatting and end-game conditions.
 */
class GameServicetest {

    private lateinit var rootService: RootService
    private lateinit var gameService: GameService
    private lateinit var testRefreshable: TestRefreshable

    /**
     * Sets up a fresh [RootService], [GameService] and [TestRefreshable] before each test.
     */
    @BeforeTest
    fun setUp() {
        rootService = RootService()
        gameService = rootService.gameService
        testRefreshable = TestRefreshable(rootService)
        rootService.addRefreshable(testRefreshable)
    }

    /**
     * startGame: creates a game, initializes players, hands, staircase and drawstack.
     * Also triggers refreshAfterGameStart on registered refreshables.
     */
    @Test
    fun testStartGameInitializesGameCorrectly() {
        gameService.startGame(listOf("Ahmed", "Wiem"))

        val game = rootService.currentGame ?: fail("No game created")
        assertNotNull(game)

        // players
        assertEquals("Ahmed", game.player1.name)
        assertEquals("Wiem", game.player2.name)

        // currentplayer is 0 or 1
        assertTrue(game.currentplayer == 0 || game.currentplayer == 1)

        // each player has exactly 5 cards
        assertEquals(5, game.player1.hand.size)
        assertEquals(5, game.player2.hand.size)

        // staircase has 5 rows with 1..5 cards
        assertEquals(5, game.staircase.size)
        val rowSizes = game.staircase.map { it.size }
        assertEquals(listOf(1, 2, 3, 4, 5), rowSizes)

        // total cards: 52 = 10 (hands) + 15 (staircase) + rest in drawstack
        val totalCards =
            game.player1.hand.size +
                    game.player2.hand.size +
                    game.staircase.sumOf { it.size } +
                    game.drawstack.size

        assertEquals(52, totalCards)

        assertTrue(game.hasstaircasechanged)
        assertFalse(game.gamehistory.isEmpty())


        assertTrue(testRefreshable.refreshAfterGameStartCalled)
    }

    /**
     * Ensures that [GameService.startGame] fails when both player names are identical.
     */
    @Test
    fun startGame_fails_for_equal_names() {
        assertFailsWith<IllegalArgumentException> {
            gameService.startGame(listOf("Ahmed", "Ahmed"))
        }
    }

    /**
     * Ensures that [GameService.startGame] fails if the first player's name is too long.
     */
    @Test
    fun startGame_fails_for_too_long_first_name() {
        assertFailsWith<IllegalArgumentException> {
            gameService.startGame(listOf("Ahmedbjbjbjbjjbjbjjbjbb", "Ahmed"))
        }
    }

    /**
     * Ensures that [GameService.startGame] fails if the second player's name is too long.
     */
    @Test
    fun startGame_fails_for_too_long_second_name() {
        assertFailsWith<IllegalArgumentException> {
            gameService.startGame(listOf("Ahmed", "Ahmedchgchgchkgchgchkchk"))
        }
    }

    /**
     * Ensures that [GameService.startGame] fails when the first player's name is empty.
     */
    @Test
    fun startGame_fails_for_empty_first_name() {
        assertFailsWith<IllegalArgumentException> {
            gameService.startGame(listOf("", "Ahmed"))
        }
    }

    /**
     * Ensures that [GameService.startGame] fails when the second player's name is empty.
     */
    @Test
    fun startGame_fails_for_empty_second_name() {
        assertFailsWith<IllegalArgumentException> {
            gameService.startGame(listOf("Ahmed", ""))
        }
    }

    /**
     * Ensures that [GameService.startGame] fails if more than two player names are provided.
     */
    @Test
    fun startGame_fails_for_three_names() {
        assertFailsWith<IllegalArgumentException> {
            gameService.startGame(listOf("Wiem", "Ahmed", "Meriam"))
        }
    }

    /**
     * Verifies that [GameService.getLog] formats different action types into readable log lines.
     */
    @Test
    fun testGetLogFormatting() {
        val player1 = Player("Ahmed")
        val player2 = Player("Eya")

        val action1 = PlayerAction(
            playerId = 0,
            action = Action.DRAW
        )

        val targetCard = Card(CardSuit.HEARTS, CardValue.TWO)

        val action2 = PlayerAction(
            playerId = 1,
            action = Action.DESTROY,
            playedCard = null,
            targetCard = targetCard,
            position = Pair(3, 1)
        )

        val action3 = PlayerAction(
            playerId = 1,
            action = Action.COMBINE,
            playedCard = Card(CardSuit.HEARTS, CardValue.THREE),
            targetCard = targetCard,
            position = Pair(3, 1)
        )
        val action4 = PlayerAction(
            playerId = 1,
            action = Action.START,

            )
        val action5 = PlayerAction(
            playerId = 1,
            action = Action.END

        )

        val game = CardStaircaseGame(
            currentplayer = 0,
            hasstaircasechanged = false,
            player1 = player1,
            player2 = player2,
            drawstack = mutableListOf(),
            dumpstack = mutableListOf(),
            staircase = mutableListOf(),
            gamehistory = mutableListOf(action1, action2, action3, action4, action5)
        )
        rootService.currentGame = game

        val log = gameService.getLog()

        assertEquals("Player Ahmed did a DRAW", log[0])
        assertTrue(log[1].contains("Player Eya did a DESTROY at (4,2) "))
        assertTrue(log[1].contains("target"))
        assertTrue(log[2].contains(action3.playedCard.toString()))
        assertEquals("Game started. Eya plays firstly ", log[3])
        assertEquals("Game finished . Last turn was from  Eya", log[4])
    }

    /**
     * Verifies that [GameService.getLog] throws an exception when no game is active.
     */
    @Test
    fun getLog_withoutActiveGame_throws() {
        rootService.currentGame = null

        assertFailsWith<IllegalStateException> {
            gameService.getLog()
        }
    }

    /**
     * Checks that [GameService.endGame] ends the game when the staircase is completely empty.
     */
    @Test
    fun testEndGameWhenStaircaseEmpty() {
        val game = CardStaircaseGame(
            currentplayer = 0,
            hasstaircasechanged = true,
            player1 = Player("P1"),
            player2 = Player("P2"),
            drawstack = mutableListOf(Card(CardSuit.CLUBS, CardValue.TWO)),
            dumpstack = mutableListOf(),
            staircase = mutableListOf(
                mutableListOf(), mutableListOf(), mutableListOf(),
                mutableListOf(), mutableListOf()
            ),
            gamehistory = mutableListOf()
        )
        rootService.currentGame = game

        assertTrue(gameService.endGame())

        // the END action must be appended
        assertEquals(1, game.gamehistory.size)
        val endAction = game.gamehistory.last()
        assertEquals(Action.END, endAction.action)
        assertEquals(game.currentplayer, endAction.playerId)
        assertNull(endAction.playedCard)
        assertNull(endAction.targetCard)
        assertNull(endAction.position)
    }

    /**
     * Checks that [GameService.endGame] ends the game if drawstack is empty and no staircase change occurred.
     */
    @Test
    fun testEndGameWhenDrawStackEmptyAndNoChange() {
        val game = CardStaircaseGame(
            currentplayer = 0,
            hasstaircasechanged = false,
            player1 = Player("P1"),
            player2 = Player("P2"),
            drawstack = mutableListOf(),
            dumpstack = mutableListOf(),
            staircase = mutableListOf(
                mutableListOf(Card(CardSuit.HEARTS, CardValue.THREE)),
                mutableListOf(),
                mutableListOf(),
                mutableListOf(),
                mutableListOf()
            ),
            gamehistory = mutableListOf()
        )
        rootService.currentGame = game

        assertTrue(gameService.endGame())

        // the END action must be appended
        assertEquals(1, game.gamehistory.size)
        val endAction = game.gamehistory.last()
        assertEquals(Action.END, endAction.action)
        assertEquals(game.currentplayer, endAction.playerId)
        assertNull(endAction.playedCard)
        assertNull(endAction.targetCard)
        assertNull(endAction.position)
    }

    /**
     * Ensures that [GameService.endGame] returns false if none of the end-game conditions are met.
     */
    @Test
    fun testEndGameReturnsFalseWhenGameNotFinished() {
        val game = CardStaircaseGame(
            currentplayer = 0,
            hasstaircasechanged = true,          // !hasstaircasechanged = false
            player1 = Player("P1"),
            player2 = Player("P2"),
            drawstack = mutableListOf(Card(CardSuit.SPADES, CardValue.FIVE)), // not empty
            dumpstack = mutableListOf(),
            staircase = mutableListOf(
                mutableListOf(Card(CardSuit.HEARTS, CardValue.ACE)), // at least one card
                mutableListOf(),
                mutableListOf(),
                mutableListOf(),
                mutableListOf()
            ),
            gamehistory = mutableListOf()
        )
        rootService.currentGame = game

        assertFalse(gameService.endGame())
    }

    /**
     * Verifies that an empty drawstack alone does not end the game if the staircase has changed.
     */
    @Test
    fun testEndGameDrawStackEmptyButStaircaseChanged() {
        val game = CardStaircaseGame(
            currentplayer = 0,
            hasstaircasechanged = true,                 // changed = true
            player1 = Player("P1"),
            player2 = Player("P2"),
            drawstack = mutableListOf(),                // empty
            dumpstack = mutableListOf(),
            staircase = mutableListOf(                  // not all empty so staircaseEmpty = false
                mutableListOf(Card(CardSuit.HEARTS, CardValue.THREE)),
                mutableListOf(),
                mutableListOf(),
                mutableListOf(),
                mutableListOf()
            ),
            gamehistory = mutableListOf()
        )
        rootService.currentGame = game

        // second if must NOT trigger
        assertFalse(gameService.endGame())
        assertTrue(game.gamehistory.isEmpty())
    }

    /**
     * Ensures that [GameService.endGame] throws an exception when no current game is set.
     */
    @Test
    fun endGame_withoutCurrentGame_throws() {
        rootService.currentGame = null
        assertFailsWith<IllegalStateException> {
            gameService.endGame()
        }
    }
}









