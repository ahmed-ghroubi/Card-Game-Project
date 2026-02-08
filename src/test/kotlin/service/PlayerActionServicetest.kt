package service

import entity.*
import kotlin.test.*

class PlayerActionServiceTest {

    private lateinit var rootService: RootService
    private lateinit var actionService: PlayerActionService
    private lateinit var testRef: TestRefreshable
    private lateinit var player1: Player
    private lateinit var player2: Player

    /** Sets up a fresh RootService, game state, and refreshable before each test. */
    @BeforeTest
    fun beforetestngGame() {
        rootService = RootService()
        actionService = rootService.playerActionService

        player1 = Player("Ahmed")
        player2 = Player("meriam")

        // staircase:
        val staircase = mutableListOf(
            mutableListOf(Card(CardSuit.HEARTS, CardValue.TWO)),                   // row 0
            mutableListOf(
                Card(CardSuit.HEARTS, CardValue.THREE),
                Card(CardSuit.HEARTS, CardValue.FOUR)
            ),                                                                     // row 1
            mutableListOf(
                Card(CardSuit.SPADES, CardValue.FIVE),
                Card(CardSuit.SPADES, CardValue.SIX),
                Card(CardSuit.SPADES, CardValue.SEVEN)
            ),                                                                     // row 2
            mutableListOf(
                Card(CardSuit.CLUBS, CardValue.EIGHT),
                Card(CardSuit.CLUBS, CardValue.NINE),
                Card(CardSuit.CLUBS, CardValue.TEN),
                Card(CardSuit.CLUBS, CardValue.JACK)
            ),                                                                     // row 3
            mutableListOf(
                Card(CardSuit.DIAMONDS, CardValue.TWO),
                Card(CardSuit.DIAMONDS, CardValue.THREE),
                Card(CardSuit.DIAMONDS, CardValue.FOUR),
                Card(CardSuit.DIAMONDS, CardValue.FIVE),
                Card(CardSuit.DIAMONDS, CardValue.SIX)
            )                                                                      // row 4
        )

        val drawstack = mutableListOf(
            Card(CardSuit.CLUBS, CardValue.ACE),
            Card(CardSuit.SPADES, CardValue.ACE),
            Card(CardSuit.HEARTS, CardValue.KING)
        )

        val dumpstack = mutableListOf<Card>()
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

        rootService.currentGame = game
        testRef = TestRefreshable(rootService)
        rootService.addRefreshable(testRef)

    }

    /** Returns the current game or throws if none is active. */
    private fun game(): CardStaircaseGame =
        rootService.currentGame ?: error("No active game")

    /** Verifies destroyCard keeps all relevant state unchanged. */
    private fun assertDestroyDoesNothing(g: CardStaircaseGame, col: Int) {  // used to  minimize duplication
        val row = 0
        val rowBefore = g.staircase[row].toList()
        val dumpSizeBefore = g.dumpstack.size
        val histSizeBefore = g.gamehistory.size
        val stateBefore = g.hasstaircasechanged

        actionService.destroyCard(row, col)

        assertEquals(rowBefore, g.staircase[row])
        assertEquals(dumpSizeBefore, g.dumpstack.size)
        assertEquals(histSizeBefore, g.gamehistory.size)
        assertEquals(stateBefore, g.hasstaircasechanged)
    }

    /** Ensures destroyCard is ignored when preconditions are not met. */
    @Test
    fun destroy_invalidConditions_doNothing() {
        val g = game()
        val row = 0
        val col = g.staircase[row].lastIndex// in that case col is 0 because row is 0
        val p = player1

        // not enough points
        p.gainedStack.clear()
        g.gamehistory.clear()
        g.gamehistory.add(PlayerAction(playerId = 1, action = Action.DRAW))
        assertDestroyDoesNothing(g, col)

        //  empty history
        p.gainedStack.clear()
        p.gainedStack.add(Card(CardSuit.HEARTS, CardValue.FIVE))
        g.gamehistory.clear()
        assertDestroyDoesNothing(g,  col)

        // last action by same player
        g.gamehistory.clear()
        g.gamehistory.add(PlayerAction(playerId = 0, action = Action.DRAW)) // gained stack is not cleared so i m not testing the enoughpoint condition
        assertDestroyDoesNothing(g,  col)
    }
    /**
     * destroyCard: allowed → removes card, logs action and triggers refresh;
     * if the staircase becomes empty, also triggers game end.
     */

    @Test
    fun destroy_allowed_forBothPlayers() {
        val g = game()
        testRef.reset()

        // player1
        g.currentplayer = 0
        player1.gainedStack.clear()
        player1.gainedStack.add(Card(CardSuit.HEARTS, CardValue.SIX)) // >= 5 points
        g.gamehistory.clear()
        g.gamehistory.add(PlayerAction(playerId = 1, action = Action.DRAW)) // other player
        val row1 = 0
        val col1 = g.staircase[row1].lastIndex
        val target1 = g.staircase[row1][col1]

        actionService.destroyCard(row1, col1)

        assertFalse(g.staircase[row1].contains(target1))
        assertEquals(target1, g.dumpstack.last())
        assertTrue(g.hasstaircasechanged)
        assertEquals(Action.DESTROY, g.gamehistory.last().action)
        assertEquals(0, g.gamehistory.last().playerId)

        // player2
        testRef.reset()
        g.currentplayer = 1
        player2.gainedStack.clear()
        player2.gainedStack.add(Card(CardSuit.SPADES, CardValue.SIX))
        g.gamehistory.add(PlayerAction(playerId = 0, action = Action.DRAW))
        val row2 = 1
        val col2 = g.staircase[row2].lastIndex
        val target2 = g.staircase[row2][col2]

        actionService.destroyCard(row2, col2)

        assertFalse(g.staircase[row2].contains(target2))
        assertEquals(target2, g.dumpstack.last())
        assertEquals(Action.DESTROY, g.gamehistory.last().action)
        assertEquals(1, g.gamehistory.last().playerId)
    }


    /**
     *  throws if there is no active game.
     */
    @Test
    fun destroy_noGame_throws() {
        rootService.currentGame = null

        assertFailsWith<IllegalStateException> {
            actionService.destroyCard(0, 0)
        }
    }
    /**
     * destroyCard: when the last staircase card is destroyed,
     * endGame() returns true and refreshAfterGameEnd is called.
     */
    @Test
    fun destroy_endsGame_whenStaircaseBecomesEmpty() {
        val g = game()
        val p = player1
        testRef.reset()

        // enough points (>= 5)
        p.gainedStack.clear()
        p.gainedStack.add(Card(CardSuit.HEARTS, CardValue.SIX))

        // in staircase exactly one card total
        g.staircase.clear()
        g.staircase.add(mutableListOf(Card(CardSuit.CLUBS, CardValue.ACE)))

        // last action by other  player so canDestroy = true
        g.gamehistory.clear()
        g.gamehistory.add(PlayerAction(playerId = 1, action = Action.DRAW))

        actionService.destroyCard(0, 0)

        //  game ended so END action added and refreshAfterGameEnd called
        assertEquals(Action.END, g.gamehistory.last().action)
        assertTrue(testRef.refreshAfterDestroyCardCalled)
        assertTrue(testRef.refreshAfterGameEndCalled)
    }

    /** Verifies combineCard keeps all relevant state unchanged. */
    private fun assertCombineDoesNothing(
        g: CardStaircaseGame,
        handCard: Card,
        player: Player
    ) {
        val row= 1
        val  col= 1
        val rowBefore = g.staircase[row].toList()
        val handBefore = player.hand.toList()
        val gainedBefore = player.gainedStack.toList()
        val histBefore = g.gamehistory.toList()
        val flagBefore = g.hasstaircasechanged

        testRef.reset()
        actionService.combineCard(handCard, row, col)

        assertEquals(rowBefore, g.staircase[row])
        assertEquals(handBefore, player.hand)
        assertEquals(gainedBefore, player.gainedStack)
        assertEquals(histBefore, g.gamehistory)
        assertEquals(flagBefore, g.hasstaircasechanged)
        assertFalse(testRef.refreshAfterCombineCalled)
    }

    /** Ensures combineCard is ignored when preconditions are not met. */
    @Test
    fun combine_invalidConditions_doNothing() {
        val g = game()
        val row = 1
        val col = 0
        val stairCard = g.staircase[row][col]
        val p = player1

        //  no suit or value match
        p.hand.clear()
        val noMatch = Card(CardSuit.CLUBS, CardValue.KING)
        p.hand.add(noMatch)
        g.gamehistory.clear()
        g.gamehistory.add(PlayerAction(playerId = 0, action = Action.START))
        assertCombineDoesNothing(g, noMatch, p)

        //  same player, last action not DESTROY (canCombine false)
        p.hand.clear()
        val matching = Card(stairCard.suit, stairCard.value)
        p.hand.add(matching)
        g.gamehistory.clear()
        g.gamehistory.add(PlayerAction(playerId = 0, action = Action.DRAW))
        assertCombineDoesNothing(g, matching, p)

        //  card not in hand (second check in combineCard)
        p.hand.clear()
        g.gamehistory.clear()
        g.gamehistory.add(PlayerAction(playerId = 1, action = Action.DISCARD))
        assertCombineDoesNothing(g, matching, p)
    }


    /** Verifies combineCard works for both players in valid scenarios. */
    @Test
    fun combine_allowed_forBothPlayers() {
        val g = game()

        //player 1: last action = START, same VALUE (different suit)
        g.currentplayer = 0
        val p1 = player1
        testRef.reset()

        val row1 = 1
        val col1 = 0
        val stair1 = g.staircase[row1][col1]

        // same VALUE, different SUIT . tests value branch in the START case
        val hand1 = Card(CardSuit.CLUBS, stair1.value)
        require(hand1.suit != stair1.suit)

        p1.hand.clear()
        p1.hand.add(hand1)
        p1.gainedStack.clear()
        g.gamehistory.clear()
        g.gamehistory.add(PlayerAction(playerId = 0, action = Action.START))

        actionService.combineCard(hand1, row1, col1)

        assertFalse(g.staircase[row1].contains(stair1))
        assertFalse(p1.hand.contains(hand1))
        assertTrue(p1.gainedStack.containsAll(listOf(hand1, stair1)))
        assertEquals(Action.COMBINE, g.gamehistory.last().action)
        assertEquals(0, g.gamehistory.last().playerId)
        assertTrue(testRef.refreshAfterCombineCalled)
        assertFalse(testRef.refreshAfterGameEndCalled)

        //player 2: last action from other player, same SUIT and VALUE
        g.currentplayer = 1
        val p2 = player2
        testRef.reset()

        val row2 = 2
        val col2 = 0
        val stair2 = g.staircase[row2][col2]
        val hand2 = Card(stair2.suit, stair2.value)

        p2.hand.clear()
        p2.hand.add(hand2)
        p2.gainedStack.clear()
        g.gamehistory.clear()
        g.gamehistory.add(PlayerAction(playerId = 0, action = Action.DISCARD)) // other player aka player 1

        actionService.combineCard(hand2, row2, col2)

        assertFalse(g.staircase[row2].contains(stair2))
        assertFalse(p2.hand.contains(hand2))
        assertTrue(p2.gainedStack.containsAll(listOf(hand2, stair2)))
        assertEquals(Action.COMBINE, g.gamehistory.last().action)
        assertEquals(1, g.gamehistory.last().playerId)
        assertTrue(testRef.refreshAfterCombineCalled)
        assertFalse(testRef.refreshAfterGameEndCalled)

        // VALUE-only match, last action != START, other player
        g.currentplayer = 0
        testRef.reset()

        val row3 = 3
        val col3 = 0
        val stair3 = g.staircase[row3][col3]

        // same VALUE, different SUIT brings  evaluation of `handCard.value == stairCard.value`
        val hand3 = Card(CardSuit.DIAMONDS, stair3.value)
        require(hand3.suit != stair3.suit)

        p1.hand.clear()
        p1.hand.add(hand3)
        g.gamehistory.clear()
        // last action by other player, and not START
        g.gamehistory.add(PlayerAction(playerId = 0, action = Action.DESTROY))
        actionService.combineCard(hand3, row3, col3)
        assertFalse(g.staircase[row3].contains(stair3))
        assertFalse(p1.hand.contains(hand3))
        assertTrue(p1.gainedStack.containsAll(listOf(hand3, stair3)))
        assertEquals(Action.COMBINE, g.gamehistory.last().action)
        assertEquals(0, g.gamehistory.last().playerId)
        assertTrue(testRef.refreshAfterCombineCalled)
    }



    @Test
    fun combine_endsGame_whenStaircaseBecomesEmpty() {
        val g = game()
        val p = player1
        testRef.reset()

        val hand = Card(CardSuit.HEARTS, CardValue.TWO)
        p.hand.clear()
        p.hand.add(hand)

        g.staircase.clear()
        g.staircase.add(mutableListOf(Card(CardSuit.HEARTS, CardValue.TWO)))

        g.gamehistory.clear()
        g.gamehistory.add(PlayerAction(playerId = 1, action = Action.DISCARD))

        actionService.combineCard(hand, 0, 0)

        assertTrue(testRef.refreshAfterCombineCalled)
        assertTrue(testRef.refreshAfterGameEndCalled)
    }

    @Test
    fun combine_noGame_throws() {
        rootService.currentGame = null

        assertFailsWith<IllegalStateException> {
            actionService.combineCard(Card(CardSuit.CLUBS, CardValue.ACE), 0, 0)
        }
    }

    /** Verifies drawCard does not draw but still switches turn and refreshes. */
    private fun assertDrawDoesNothing(player: Player, game: CardStaircaseGame) {
        val handBefore = player.hand.toList()
        val drawBefore = game.drawstack.toList()
        val dumpBefore = game.dumpstack.toList()
        val histBefore = game.gamehistory.toList()
        val currentBefore = game.currentplayer

        testRef.reset()
        actionService.drawCard()

        assertEquals(handBefore, player.hand)
        assertEquals(drawBefore, game.drawstack)
        assertEquals(dumpBefore, game.dumpstack)
        assertEquals(histBefore, game.gamehistory)
        assertEquals(if (currentBefore == 0) 1 else 0, game.currentplayer)
        assertTrue(testRef.refreshAfterDrawCardCalled)
        assertTrue(testRef.refreshAfterTurnStartCalled)
    }

    /** Ensures drawCard is ignored when preconditions are not met. */
    @Test
    fun draw_invalidConditions_doNothing() {
        val g = game()
        val p = player1

        //  hand size != 4
        g.currentplayer = 0
        p.hand.clear()
        repeat(3) { p.hand.add(Card(CardSuit.CLUBS, CardValue.TWO)) }
        g.drawstack.clear()
        g.drawstack.add(Card(CardSuit.HEARTS, CardValue.ACE))
        g.dumpstack.clear()
        g.gamehistory.clear()
        g.gamehistory.add(PlayerAction(playerId = 0, action = Action.DISCARD))
        assertDrawDoesNothing(p, g)
        //  last action by other player
        g.currentplayer = 0
        p.hand.clear()
        repeat(4) { p.hand.add(Card(CardSuit.CLUBS, CardValue.FOUR)) }
        g.drawstack.clear()
        g.drawstack.add(Card(CardSuit.HEARTS, CardValue.ACE))
        g.dumpstack.clear()
        g.gamehistory.clear()
        g.gamehistory.add(PlayerAction(playerId = 1, action = Action.DISCARD))
        assertDrawDoesNothing(p, g)

        //  last action by same player but not DISCARD/COMBINE
        g.currentplayer = 0
        p.hand.clear()
        repeat(4) { p.hand.add(Card(CardSuit.CLUBS, CardValue.FIVE)) }
        g.drawstack.clear()
        g.drawstack.add(Card(CardSuit.HEARTS, CardValue.ACE))
        g.dumpstack.clear()
        g.gamehistory.clear()
        g.gamehistory.add(PlayerAction(playerId = 0, action = Action.DESTROY))
        assertDrawDoesNothing(p, g)
    }

    /** Verifies drawCard draws and logs correctly for both players. */
    @Test
    fun draw_allowed_forBothPlayers() {
        val g = game()

        // player 1
        g.currentplayer = 0
        val p1 = player1
        testRef.reset()

        p1.hand.clear()
        p1.hand.addAll(
            listOf(
                Card(CardSuit.CLUBS, CardValue.TWO),
                Card(CardSuit.CLUBS, CardValue.THREE),
                Card(CardSuit.CLUBS, CardValue.FOUR),
                Card(CardSuit.CLUBS, CardValue.FIVE)
            )
        )

        g.gamehistory.clear()
        g.gamehistory.add(PlayerAction(playerId = 0, action = Action.DISCARD))

        val drawBefore1 = g.drawstack.size
        val handBefore1 = p1.hand.size
        val histBefore1 = g.gamehistory.size

        actionService.drawCard()

        assertEquals(handBefore1 + 1, p1.hand.size)
        assertEquals(drawBefore1 - 1, g.drawstack.size)
        assertEquals(histBefore1 + 1, g.gamehistory.size)
        val last1 = g.gamehistory.last()
        assertEquals(Action.DRAW, last1.action)
        assertEquals(0, last1.playerId)
        assertEquals(1, g.currentplayer)
        assertTrue(testRef.refreshAfterDrawCardCalled)

        // player 2
        g.currentplayer = 1
        val p2 = player2
        testRef.reset()

        p2.hand.clear()
        p2.hand.addAll(
            listOf(
                Card(CardSuit.HEARTS, CardValue.TWO),
                Card(CardSuit.HEARTS, CardValue.THREE),
                Card(CardSuit.HEARTS, CardValue.FOUR),
                Card(CardSuit.HEARTS, CardValue.FIVE)
            )
        )

        g.gamehistory.clear()
        g.gamehistory.add(PlayerAction(playerId = 1, action = Action.COMBINE))

        val drawBefore2 = g.drawstack.size
        val handBefore2 = p2.hand.size
        val histBefore2 = g.gamehistory.size

        actionService.drawCard()

        assertEquals(handBefore2 + 1, p2.hand.size)
        assertEquals(drawBefore2 - 1, g.drawstack.size)
        assertEquals(histBefore2 + 1, g.gamehistory.size)
        val last2 = g.gamehistory.last()
        assertEquals(Action.DRAW, last2.action)
        assertEquals(1, last2.playerId)
        assertEquals(0, g.currentplayer)
        assertTrue(testRef.refreshAfterDrawCardCalled)
    }

    /** Ensures no reshuffle/draw happens when hand size is not 4, even if dumpstack has cards. */
    @Test
    fun draw_emptyDrawStack_handNotFour_noReshuffle_noDraw() {
        val g = game()
        val p = player1
        testRef.reset()

        // hand size = 3  so candraw() must be false
        p.hand.clear()
        p.hand.addAll(
            listOf(
                Card(CardSuit.CLUBS, CardValue.TWO),
                Card(CardSuit.CLUBS, CardValue.THREE),
                Card(CardSuit.CLUBS, CardValue.FOUR)
            )
        )

        // drawstack EMPTY, dumpstack NOT empty
        g.drawstack.clear()
        g.dumpstack.clear()
        g.dumpstack.add(Card(CardSuit.HEARTS, CardValue.ACE))

        g.gamehistory.clear()
        g.gamehistory.add(PlayerAction(playerId = 0, action = Action.DISCARD))

        val handBefore = p.hand.toList()
        val dumpBefore = g.dumpstack.toList()
        val histBefore = g.gamehistory.toList()

        actionService.drawCard()
        assertTrue(g.drawstack.isEmpty())
        assertEquals(dumpBefore, g.dumpstack)
        assertEquals(handBefore, p.hand)
        assertEquals(histBefore, g.gamehistory)

        //  turn still switches and refresh is called
        assertEquals(1, g.currentplayer)
        assertTrue(testRef.refreshAfterDrawCardCalled)
    }

    /** Verifies reshuffle from dumpstack occurs and a card is drawn. */
    @Test
    fun draw_reshuffle() {
        val g = game()
        val p = player1
        testRef.reset()

        p.hand.clear()
        p.hand.addAll(
            listOf(
                Card(CardSuit.SPADES, CardValue.TWO),
                Card(CardSuit.SPADES, CardValue.THREE),
                Card(CardSuit.SPADES, CardValue.FOUR),
                Card(CardSuit.SPADES, CardValue.FIVE)
            )
        )

        g.drawstack.clear()
        g.dumpstack.clear()
        g.dumpstack.addAll(
            listOf(
                Card(CardSuit.HEARTS, CardValue.ACE),
                Card(CardSuit.CLUBS, CardValue.KING)
            )
        )

        g.gamehistory.clear()
        g.gamehistory.add(PlayerAction(playerId = 0, action = Action.DISCARD))

        actionService.drawCard()

        assertEquals(5, p.hand.size)
        assertEquals(1, g.drawstack.size)
        assertTrue(g.dumpstack.isEmpty())
        assertEquals(1, g.currentplayer)
        assertTrue(testRef.refreshAfterDrawCardCalled)
    }

    /** Ensures drawCard throws when no active game exists. */
    @Test
    fun draw_noGame() {
        rootService.currentGame = null

        assertFailsWith<IllegalStateException> {
            actionService.drawCard()
        }
    }

    /** Verifies discard keeps all relevant state unchanged. */
    private fun assertDiscardDoesNothing(
        g: CardStaircaseGame,
        p: Player,
        card: Card
    ) {
        val handBefore = p.hand.toList()
        val dumpBefore = g.dumpstack.toList()
        val histBefore = g.gamehistory.toList()

        testRef.reset()
        actionService.discard(card)

        assertEquals(handBefore, p.hand)
        assertEquals(dumpBefore, g.dumpstack)
        assertEquals(histBefore, g.gamehistory)
        assertFalse(testRef.refreshAfterDiscardCalled)
    }

    /** Ensures discard is ignored when preconditions are not met. */
    @Test
    fun discard_invalidConditions_doNothing() {
        val g = game()
        val p = player1

        // a) hand size != 5  → candiscard = false
        p.hand.clear()
        val c1 = Card(CardSuit.DIAMONDS, CardValue.TWO)
        p.hand.addAll(listOf(c1, Card(CardSuit.DIAMONDS, CardValue.THREE)))
        g.gamehistory.clear()
        assertDiscardDoesNothing(g, p, c1)

        // b) card not in hand  → candiscard = false
        p.hand.clear()
        p.hand.addAll(
            listOf(
                Card(CardSuit.HEARTS, CardValue.THREE),
                Card(CardSuit.HEARTS, CardValue.FOUR),
                Card(CardSuit.HEARTS, CardValue.FIVE),
                Card(CardSuit.HEARTS, CardValue.SIX),
                Card(CardSuit.HEARTS, CardValue.SEVEN)
            )
        )
        val wrongCard = Card(CardSuit.CLUBS, CardValue.ACE)
        g.gamehistory.clear()
        assertDiscardDoesNothing(g, p, wrongCard)
    }

    /** Verifies discard works and logs correctly for both players. */
    @Test
    fun discard_allowed_forBothPlayers() {
        val g = game()

        // player 1
        g.currentplayer = 0
        val p1 = player1
        testRef.reset()

        p1.hand.clear()
        val discard1 = Card(CardSuit.CLUBS, CardValue.TWO)
        p1.hand.addAll(
            listOf(
                discard1,
                Card(CardSuit.CLUBS, CardValue.THREE),
                Card(CardSuit.CLUBS, CardValue.FOUR),
                Card(CardSuit.CLUBS, CardValue.FIVE),
                Card(CardSuit.CLUBS, CardValue.SIX)
            )
        )

        val histBefore1 = g.gamehistory.size
        val dumpBefore1 = g.dumpstack.size

        actionService.discard(discard1)

        assertFalse(p1.hand.contains(discard1))
        assertEquals(dumpBefore1 + 1, g.dumpstack.size)
        assertEquals(discard1, g.dumpstack.last())
        assertEquals(histBefore1 + 1, g.gamehistory.size)
        val last1 = g.gamehistory.last()
        assertEquals(Action.DISCARD, last1.action)
        assertEquals(discard1, last1.targetCard)
        assertEquals(0, last1.playerId)
        assertTrue(testRef.refreshAfterDiscardCalled)
        assertFalse(testRef.refreshAfterGameEndCalled)

        // player 2
        g.currentplayer = 1
        val p2 = player2
        testRef.reset()

        p2.hand.clear()
        val discard2 = Card(CardSuit.SPADES, CardValue.FOUR)
        p2.hand.addAll(
            listOf(
                discard2,
                Card(CardSuit.SPADES, CardValue.FIVE),
                Card(CardSuit.SPADES, CardValue.SIX),
                Card(CardSuit.SPADES, CardValue.SEVEN),
                Card(CardSuit.SPADES, CardValue.EIGHT)
            )
        )

        val histBefore2 = g.gamehistory.size
        val dumpBefore2 = g.dumpstack.size

        actionService.discard(discard2)

        assertFalse(p2.hand.contains(discard2))
        assertEquals(dumpBefore2 + 1, g.dumpstack.size)
        assertEquals(histBefore2 + 1, g.gamehistory.size)
        val last2 = g.gamehistory.last()
        assertEquals(Action.DISCARD, last2.action)
        assertEquals(discard2, last2.targetCard)
        assertEquals(1, last2.playerId)
        assertTrue(testRef.refreshAfterDiscardCalled)
        assertFalse(testRef.refreshAfterGameEndCalled)
    }

    /** Ensures discard triggers game end when the staircase is already empty. */
    @Test
    fun discard_endsGame_whenStaircaseEmpty() {
        val g = game()
        val p = player1
        testRef.reset()

        // make staircase empty so GameService.endGame() returns true
        g.staircase.forEach { it.clear() }

        // valid discard: 5 cards including discardCard
        p.hand.clear()
        val discard = Card(CardSuit.HEARTS, CardValue.TWO)
        p.hand.addAll(
            listOf(
                discard,
                Card(CardSuit.HEARTS, CardValue.THREE),
                Card(CardSuit.HEARTS, CardValue.FOUR),
                Card(CardSuit.HEARTS, CardValue.FIVE),
                Card(CardSuit.HEARTS, CardValue.SIX)
            )
        )

        g.gamehistory.clear()

        actionService.discard(discard)

        // normal discard behaviour
        assertFalse(p.hand.contains(discard))
        assertTrue(g.dumpstack.contains(discard))

        // endGame branch executed
        assertEquals(Action.END, g.gamehistory.last().action)
        assertTrue(testRef.refreshAfterDiscardCalled)
        assertTrue(testRef.refreshAfterGameEndCalled)
    }

    /** Ensures discard throws when no active game exists. */
    @Test
    fun discard_noGame() {
        rootService.currentGame = null

        assertFailsWith<IllegalStateException> {
            actionService.discard(Card(CardSuit.CLUBS, CardValue.ACE))
        }
    }

    /** Ensures reshuffle is triggered for both players when drawstack is empty and dumpstack has cards. */
    @Test
    fun draw_triggersReshuffle_forBothPlayers() {
        val g = game()
        //player 1
        g.currentplayer = 0
        val p1 = player1
        p1.hand.clear()
        repeat(4) { p1.hand.add(Card(CardSuit.HEARTS, CardValue.TWO)) }
        g.drawstack.clear()
        val dumpCard1 = Card(CardSuit.SPADES, CardValue.NINE)
        g.dumpstack.clear()
        g.dumpstack.add(dumpCard1)
        g.hasstaircasechanged = true
        g.gamehistory.clear()
        g.gamehistory.add(PlayerAction(playerId = 0, action = Action.DISCARD))
        actionService.drawCard()

        // reshuffle + draw
        assertTrue(p1.hand.contains(dumpCard1))
        assertTrue(g.drawstack.isEmpty())
        assertTrue(g.dumpstack.isEmpty())
        assertFalse(g.hasstaircasechanged)

        //player 2
        g.currentplayer = 1
        val p2 = player2
        p2.hand.clear()
        repeat(4) { p2.hand.add(Card(CardSuit.HEARTS, CardValue.THREE)) }

        g.drawstack.clear()
        val dumpCard2 = Card(CardSuit.CLUBS, CardValue.KING)
        g.dumpstack.clear()
        g.dumpstack.add(dumpCard2)
        g.hasstaircasechanged = true

        g.gamehistory.clear()
        g.gamehistory.add(PlayerAction(playerId = 1, action = Action.DISCARD))

        actionService.drawCard()

        assertTrue(p2.hand.contains(dumpCard2))
        assertTrue(g.drawstack.isEmpty())
        assertTrue(g.dumpstack.isEmpty())
        assertFalse(g.hasstaircasechanged)
    }

    /** Ensures no reshuffle happens when both drawstack and dumpstack are empty. */
    @Test
    fun draw_noReshuffle_whenDumpStackEmpty() {
        val g = game()
        val p = player1
        testRef.reset()

        g.currentplayer = 0

        // hand size = 3  so candraw() must be false
        p.hand.clear()
        repeat(3) {
            p.hand.add(Card(CardSuit.CLUBS, CardValue.TWO))
        }
        // drawstack EMPTY, dumpstack EMPTY
        g.drawstack.clear()
        g.dumpstack.clear()
        g.hasstaircasechanged = true
        g.gamehistory.clear()
        g.gamehistory.add(PlayerAction(playerId = 0, action = Action.DISCARD))
        val handBefore = p.hand.toList()
        val hasStairBefore = g.hasstaircasechanged
        actionService.drawCard()
        // reshuffleofdumpstack returned at dumpstack.isEmpty()
        assertTrue(g.drawstack.isEmpty())
        assertTrue(g.dumpstack.isEmpty())
        assertEquals(hasStairBefore, g.hasstaircasechanged)
        // candraw() was false so no card drawn
        assertEquals(handBefore, p.hand)
        // but turn still switches and refreshes are called
        assertEquals(1, g.currentplayer)
        assertTrue(testRef.refreshAfterDrawCardCalled)
        assertTrue(testRef.refreshAfterTurnStartCalled)
    }

    /** Verifies winner() returns player1 as winner when player1 has more points. */
    @Test
    fun winner_player1HasMorePoints_returnsPlayer1Name() {
        val g = game()
        // make sure both start with 0
        g.player1.gainedStack.clear()
        g.player2.gainedStack.clear()
        // give player1 one scoring card
        g.player1.gainedStack.add(Card(CardSuit.HEARTS, CardValue.TEN))
        val result = actionService.winner(g)
        assertEquals("WINNER is " + g.player1.name, result)
    }

    /** Verifies winner() returns player2 as winner when player2 has more points. */
    @Test
    fun winner_player2HasMorePoints_returnsPlayer2Name() {
        val g = game()
        g.player1.gainedStack.clear()
        g.player2.gainedStack.clear()
        // give player2 one scoring card
        g.player2.gainedStack.add(Card(CardSuit.SPADES, CardValue.TEN))
        val result = actionService.winner(g)
        assertEquals("WINNER is " + g.player2.name, result)
    }

    /** Verifies winner() returns "Draw" when both players have equal points. */
    @Test
    fun winner_equalPoints_returnsDraw() {
        val g = game()
        g.player1.gainedStack.clear()
        g.player2.gainedStack.clear()
        // same points for both (e.g. both get a TEN)
        g.player1.gainedStack.add(Card(CardSuit.HEARTS, CardValue.TEN))
        g.player2.gainedStack.add(Card(CardSuit.SPADES, CardValue.TEN))
        val result = actionService.winner(g)
        assertEquals("Draw", result)
    }


}
