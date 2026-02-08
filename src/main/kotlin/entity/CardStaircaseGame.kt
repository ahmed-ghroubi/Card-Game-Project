
package entity

/**
 * Represents the full state of a running CardStaircase game.
 *
 * It keeps track of:
 * - [currentplayer]: Index of the player whose turn it is (0 = player1, 1 = player2).
 * - [hasstaircasechanged]: Tracks whether at least one card has been removed from the
 *   staircase since the last reshuffle. Used for end-game condition 2.
 * - [player1], [player2]: The two participating players.
 * - [drawstack]: Face-down stack of cards that can still be drawn.
 * - [dumpstack]: Discard pile where used or destroyed cards are placed.
 * - [staircase]: The current card staircase as rows; each inner list represents one row.
 * - [gamehistory]: Structured history of all actions performed during the game.
 *
 * The scoring logic (including destroy penalties) is provided by [getPlayerPoints].
 */
data class CardStaircaseGame(
    var currentplayer: Int,
    var hasstaircasechanged: Boolean ,
    val player1: Player,
    val player2: Player,
    var drawstack: MutableList<Card> = mutableListOf(),
    var dumpstack: MutableList<Card> = mutableListOf(),
    var staircase: MutableList<MutableList<Card>> = mutableListOf(),
    var gamehistory: MutableList<PlayerAction> = mutableListOf(),
) {

    /**
     * Calculates the total points for the given [player].
     *
     * Scoring rules:
     * - Sum of all card points in the player's [Player.gainedStack].
     * - Minus 5 points for each DESTROY action that this player has performed.
     *
     * The number of destroy actions is derived from [gamehistory],
     * using the [PlayerAction.playerId] and [PlayerAction.action].
     *
     * @param player the player whose score should be calculated; must be [player1] or [player2].
     * @return the total number of points for this player.
     * @throws IllegalArgumentException if the given player does not belong to this game.
     */
    fun getPlayerPoints(player: Player): Int {

            val playerId = when (player) {
                player1 -> 0
                player2 -> 1
                else -> throw IllegalArgumentException("Player not part of this game")
            }

            val totalCardsPoints = player.gainedStack.sumOf { it.getPoints() }

            val destroyCount = gamehistory.count {
                it.playerId == playerId && it.action == Action.DESTROY
            }

            return totalCardsPoints - destroyCount * 5
        }




}
