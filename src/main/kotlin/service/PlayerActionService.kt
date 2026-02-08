package service
import entity.*

/**
 * Service layer for player actions in CardStaircase:
 * - destroyCard(row, col): destroy an open staircase card (costs 5 points)
 * - (others like combine/draw/discard can be added similarly)
 *
 * Depends on RootService to access the other service methods and entity layer.
 */
class PlayerActionService(private val rootService: RootService) : AbstractRefreshingService() {
    /**
     * Destroys the open card at the given staircase position.
     *
     * Removes the card at (row, col) from the staircase, adds it to the
     * dump stack, logs a DESTROY [PlayerAction] and triggers UI refresh.
     * The player must have at least 5 points to perform this action.
     *
     * @param row the row index in the staircase
     * @param col the column index in the staircase row
     */
    fun destroyCard(row: Int, col: Int) {
        val game = rootService.currentGame ?: error("No active game")
        val currentplayer= if (game.currentplayer == 0) game.player1 else game.player2
        val rowList = game.staircase[row]
        if (!canDestroy(game, currentplayer)) {
           return
        }

        val destroyedCard = rowList.removeAt(col)
        game.dumpstack.add(destroyedCard)
        game.hasstaircasechanged = true
        game.gamehistory.add(
            PlayerAction(
                playerId = game.currentplayer,
                action = Action.DESTROY,
                playedCard = null,
                targetCard = destroyedCard,
                position = Pair(row, col)
            )
        )

        onAllRefreshables { refreshAfterdestroycard() }

        if (rootService.gameService.endGame()) {
            val winner = winner(game)
            onAllRefreshables { refreshAfterGameEnd(winner) }
        }
    }

    /**
     * Checks whether the given player is allowed to perform a destroy action.
     *
     * A destroy is only permitted if:
     * - the player has at least 5 points according to [CardStaircaseGame.getPlayerPoints], and
     * - the game history is not empty, and
     * - the last logged action was performed by the other player.
     * @param game the current game state
     * @param player the player who wants to destroy a card
     * @return true if a destroy action is allowed, false otherwise
     */
    private fun canDestroy(game: CardStaircaseGame, player: Player): Boolean {
        return game.getPlayerPoints(player) >= 5 &&  game.gamehistory.isNotEmpty() && game.gamehistory.last().playerId!=game.currentplayer
    }
    /**
     * Combines a hand card with a staircase card at the given position.
     *
     * Both cards must share either the same suit or the same value.
     * The hand card and target staircase card are removed from hand
     * and staircase, added to the player's gainedStack, a COMBINE
     * [PlayerAction] is logged and the UI is refreshed.
     *
     * @param theHandCard the card from the player's hand
     * @param row the row index of the target on the staircase
     * @param col the column index of the target in that row
     */
    fun combineCard(theHandCard: Card, row: Int, col: Int) {
        val game = rootService.currentGame ?: error("No active game")
        val currentplayer= if (game.currentplayer == 0) game.player1 else game.player2
        val rowList = game.staircase[row]
        val actualStairCard = rowList[col]
        if (!canCombine(theHandCard, actualStairCard,game)) {
            return
        }
        if (!currentplayer.hand.contains(theHandCard)) return
        rowList.removeAt(col)
       currentplayer.hand.remove(theHandCard)

        currentplayer.gainedStack.add(theHandCard)
        currentplayer.gainedStack.add(actualStairCard)

        game.hasstaircasechanged = true
        game.gamehistory.add(
            PlayerAction(
                playerId = game.currentplayer,
                action = Action.COMBINE,
                playedCard = theHandCard,
                targetCard = actualStairCard,
                position = Pair(row, col)
            )
        )

        onAllRefreshables { refreshAftercombine() }

        if (rootService.gameService.endGame()) {
            val winner = winner(game)
            onAllRefreshables { refreshAfterGameEnd(winner) }
        }
    }
    /**
     * Checks whether the given hand card may be combined with the given staircase card.
     *
     * A combine is only allowed if:
     * - the two cards share the same suit or the same value, and
     * - either no action has been logged yet (first action of the game), or
     * - the last logged action was performed by the other player, or
     * - the last logged action was a [Action.DESTROY] by the current player.
     * @param handCard the card from the player's hand
     * @param stairCard the open card on the staircase that should be combined with
     * @param game the current game state used to inspect the action history
     * @return true if a combine action is allowed, false otherwise
     */
    private fun canCombine(
        handCard: Card,
        stairCard: Card,
        game: CardStaircaseGame
    ): Boolean {
        if (!(handCard.suit == stairCard.suit || handCard.value == stairCard.value)) return false
        if (game.gamehistory.last().action == Action.START) return true
        if (game.gamehistory.last().playerId != game.currentplayer) return true
        return game.gamehistory.last().action == Action.DESTROY
    }




    /**
     * Draws one card from the draw stack into the current player's hand.
     *
     * A draw is only performed if the current player has exactly 4 cards
     * in hand. The top card of the draw stack is added to the hand,
     * a DRAW [PlayerAction] is logged, and the UI is refreshed. If the
     * draw stack becomes empty, the dump stack may be reshuffled into it.
     */
    fun drawCard() {
            val game = rootService.currentGame ?: error("no Active game")
            val currentplayer= if (game.currentplayer == 0) game.player1 else game.player2

            reshuffleofdumpstack(game)

            if (candraw(currentplayer, game)) {
                val lastofdrawstack = game.drawstack.last()
                currentplayer.hand.add(lastofdrawstack)
                game.drawstack.remove(lastofdrawstack)
                game.gamehistory.add(
                    PlayerAction(
                        playerId = game.currentplayer,
                        action = Action.DRAW,
                        playedCard = null,
                        targetCard = null,
                        position = null
                    )
                )

            }
            game.currentplayer = if (game.currentplayer == 0) 1 else 0
            onAllRefreshables {
                refreshAfterdrawcard()
                refreshAfterTurnStart()
            }
        }
    /**
     * Checks whether the current player is allowed to draw a card.
     *
     * A draw action is only permitted if:
     * - the player currently holds exactly 4 cards, and
     * - the game history is not empty, and
     * - the last logged action was performed by the same player, and
     * - the last logged action was either [Action.DISCARD] or [Action.COMBINE].
     *
     * @param player the player attempting to draw
     * @param game the current game state
     * @return true if a draw action is allowed, false otherwise
     */
    private fun candraw(player: Player, game: CardStaircaseGame): Boolean {
        if (player.hand.size != 4) {
            return false
        }
        if (game.gamehistory.last().playerId != game.currentplayer) {
            return false
        }
        if (game.gamehistory.last().action != Action.DISCARD &&
            game.gamehistory.last().action != Action.COMBINE) {
            return false
        }
        return true
    }

    /**
     * Discards the given card from the current player's hand.
     *
     * If [candiscard] returns true, the card is removed from the current player's hand,
     * added to the dump stack, and a [PlayerAction] with action type [Action.DISCARD]
     * is appended to the game history. Afterwards, the UI is refreshed and, if the
     * end game condition is met, a final game refresh is triggered as well.
     *
     * @param discardcard the card the current player attempts to discard
     * @throws IllegalStateException if no active game exists
     */

    fun discard(discardcard: Card) {
        val game = rootService.currentGame ?: error("no Active game")
        val currentplayer= if (game.currentplayer == 0) game.player1 else game.player2
        if (!candiscard(currentplayer,  discardcard)) {
            return
        }
        currentplayer.hand.remove(discardcard)
        game.dumpstack.add(discardcard)
        game.gamehistory.add(
            PlayerAction(
                playerId = game.currentplayer,
                action = Action.DISCARD,
                playedCard = null,
                targetCard = discardcard,
                position = null
            )
        )

        if (rootService.gameService.endGame()) {
            onAllRefreshables { refreshAfterGameEnd(winner(game)) }
        }

        onAllRefreshables { refreshAfterdiscard() }
    }

    /**
     * Checks whether the given card can be discarded by the player.
     *
     * A discard is allowed only if:
     * - the player currently holds exactly 5 cards,
     * - the given [thecard] is contained in the player's hand, and
     * - either no action has been logged yet, or
     *   the last logged action was performed by the other player, or
     *   the last logged action was a [Action.DESTROY] by the same player.
     * @param player the player who wants to discard
     * @param thecard the card to discard
     * @return true if the discard is allowed, false otherwise
     */

    private fun candiscard(player: Player, thecard: Card): Boolean {
        if (player.hand.size != 5) {
            return false
        }
        if (!player.hand.contains(thecard)) {
            return false
        }
        return true
    }



    /**
     * Reshuffles the dump stack into the draw stack if needed.
     *
     * If the draw stack is empty and the dump stack is not, all cards
     * from the dump stack are moved into the draw stack, shuffled, and
     * the dump stack is cleared. The hasstaircasechanged flag is reset.
     *
     * @param game the current game
     */
    private fun reshuffleofdumpstack(game: CardStaircaseGame) {
        val currentPlayer =
            if (game.currentplayer == 0) game.player1 else game.player2
        if (game.drawstack.isNotEmpty()) {
            return
        }
        if (game.dumpstack.isEmpty()) {
            return
        }
        if (currentPlayer.hand.size != 4) {
            return
        }

        // if conditon satistfies then  reshuffle dumpstack into drawstack
        game.drawstack = game.dumpstack.toMutableList()
        game.drawstack.shuffle()
        game.dumpstack.clear()
        game.hasstaircasechanged = false
    }

    /** Returns a winner string based on comparing both players' points, or "Draw" if equal. */

    fun winner(game: CardStaircaseGame): String =
        if (game.getPlayerPoints(game.player1) > game.getPlayerPoints(game.player2))
           "WINNER is " + game.player1.name
        else if (game.getPlayerPoints(game.player2) > game.getPlayerPoints(game.player1))
            "WINNER is "+ game.player2.name
        else
            "Draw"



}

