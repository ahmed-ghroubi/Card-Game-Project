package service
import kotlin.random.Random
import entity.*

/**
 * Service layer class that provides the logic for actions not directly
 * related to a single player.
 *
 * @param rootService The [RootService] instance to access the other service methods and entity layer.
 */
class GameService(private val rootService: RootService) : AbstractRefreshingService() {

    /**
     * Starts a new game and initializes the complete game state.
     *
     * Creates a new game with exactly two players from the given names, sets their
     * initial score to 0, selects a starting player and clears any existing log or
     * history. Finally, all registered refreshables are notified via
     * `refreshAfterGameStart()`.
     *
     * @param playerNames List containing exactly two distinct, non-empty player names.
     *
     * @throws IllegalArgumentException
     * If `playerNames` does not contain exactly two entries, if any name is empty,
     * if the names are not unique, or if they contain invalid characters.
     *
     * @throws IllegalStateException
     * If a non-finished game is already active when this method is called.
     */

    fun startGame(
        playerNames: List<String>,
    ) {
        require(playerNames.size == 2) {
            "Please provide exactly 2 player names."
        }

        require(playerNames[0].isNotEmpty()) {
            "The first player name must not be empty."
        }
        require(playerNames[1].isNotEmpty()) {
            "The second player name must not be empty."
        }

        require(playerNames[0].length <= 20) {
            "Please give a name with at most 20 letters for the first player."
        }
        require(playerNames[1].length <= 20) {
            "Please give a name with at most 20 letters for the second player."
        }
        require(playerNames[0] != playerNames[1]) {
            "Player names must be different."
        }

        val player1 = Player(playerNames[0])
        val player2 = Player(playerNames[1])
        val allCards: MutableList<Card> = defaultRandomCardList().toMutableList()
        val game = CardStaircaseGame(
            currentplayer = (0..1).random(),
            hasstaircasechanged = true,
            player1,
            player2,
            gamehistory =  mutableListOf()

        )
        rootService.currentGame = game
         dealCards(player1,allCards)
         dealCards(player2,allCards)
        // build staircase (removes 15 from deck, fills game.staircase)
        constructPlayfield(allCards,game)
        game.drawstack = allCards
        game.gamehistory.add(
            PlayerAction(
                playerId = game.currentplayer,
                action = Action.START,
                playedCard = null,
                targetCard = null,
                position = null
            )
        )
         onAllRefreshables {  refreshAfterGameStart() }

    }
    /**
     * Builds the staircase (playfield) from the given deck.
     *
     * Takes 15 cards from [deck] and arranges them into 5 rows
     * (1, 2, 3, 4, 5 cards per row). The resulting rows are stored
     * in the current game's [CardStaircaseGame.staircase].
     *
     * @param deck the mutable deck from which staircase cards are taken
     */
    private fun constructPlayfield(deck: MutableList<Card>,game: CardStaircaseGame) {


        val staircase = mutableListOf<MutableList<Card>>()

        for (i in 1..5) {
            val row = deck.take(i).toMutableList()
            deck.subList(0, i).clear()
            staircase.add(row)
        }

        game.staircase = staircase
    }
    /**
     * Deals exactly 5 cards from the deck to the given player.
     *
     * Takes the first 5 cards from [deck], assigns them as the hand
     * of [thePlayer], and removes these cards from [deck].
     *
     * @param thePlayer the player who receives the 5 hand cards
     * @param deck the deck to draw the cards from
     */

  private fun dealCards(thePlayer: Player, deck: MutableList<Card>) {
      val deal = deck.take(5)
      thePlayer.hand = deal.toMutableList()
      deck.subList(0, 5).clear()   // remove the dealt cards from the deck
  }
    /**
     * Returns the game log as a list of  strings.
     *
     * Each entry describes one [PlayerAction] from the current game's
     * history, including the acting player, action type, position,
     * played card and target card (if available).
     *
     * @return a mutable list of log lines describing all recorded actions
     */

    fun getLog(): MutableList<String> {
        val game = rootService.currentGame ?: error("No active game")

        return game.gamehistory.map { action ->
            val thecurrentplayerName = if (action.playerId == 0) game.player1.name else game.player2.name
            if (action.action == Action.START) {
                return@map "Game started. $thecurrentplayerName plays firstly "
            }
            if (action.action == Action.END) {
                return@map "Game finished . Last turn was from  $thecurrentplayerName"
            }
            var text = "Player ${thecurrentplayerName} did a ${action.action}"
            if (action.position != null) {
                text += " at (${action.position.first+1},${action.position.second+1})"
            }
            if (action.playedCard != null) {
                text += " and played=${action.playedCard.toString()}"
            }

            if (action.targetCard != null) {
                text += " and the target is ${action.targetCard.toString()}"
            }
            text
        }.toMutableList()
    }


    /**
     * Checks whether the current game has ended.
     *
     * The game is considered finished if either the staircase is completely
     * empty or the draw stack is empty and no staircase card has been removed
     * since the last shuffle.
     *
     * @return true if the game is over, false otherwise
     */

    fun endGame():Boolean {
        val game = rootService.currentGame ?: error("No game currently running.")

        // all rows of the staircase are empty
        val staircaseEmpty = game.staircase.all { it.isEmpty() }
        if (staircaseEmpty) {
            game.gamehistory.add(
                PlayerAction(
                    playerId = game.currentplayer,
                    action = Action.END,
                    playedCard = null,
                    targetCard = null,
                    position = null
                )
            )
            return true
        }
          if ((game.drawstack.isEmpty() && !game.hasstaircasechanged)) {
            game.gamehistory.add(
                PlayerAction(
                    playerId = game.currentplayer,
                    action = Action.END,
                    playedCard = null,
                    targetCard = null,
                    position = null
                )
            )
            return true
        }

        return false

    }
    /**
     * Creates a shuffled standard deck of 52 cards.
     *
     * Builds one card for each combination of [CardSuit] and [CardValue]
     * and returns the result in random order.
     *
     * @return a shuffled list of 52 unique cards
     */

    private fun defaultRandomCardList() = List(52) { index ->
        Card(
            CardSuit.values()[index / 13],
            CardValue.values()[index % 13]
        )
    }.shuffled()



}




