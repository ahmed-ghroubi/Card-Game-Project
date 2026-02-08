package entity

/**
 * Represents a single action performed by a player in the CardStaircase game.
 *
 * Each entry captures:
 * - [playerId]: Index of the player who performed the action
 *   ( 0 for player1, 1 for player2).
 * - [action]: The type of action (DRAW, DESTROY, COMBINE, DISCARD).
 * - [playedCard]: The card actively used by the player for this action.
 *   For example, the hand card used in a COMBINE action.
 * - [targetCard]: The affected card on the staircase or elsewhere.
 *   For example, the staircase card combined with or destroyed.
 * - [position]: The column and line index on the staircase that was targeted.
 *
 * This class is used by the game to maintain a structured history (log) of all moves.
 * Not every field is meaningful for every action type:
 * - DRAW / DISCARD usually have no [targetCard] or [position].
 * - DESTROY uses [targetCard] and [position], but no [playedCard].
 * - COMBINE uses both [playedCard] and [targetCard], and may use [position].
 */
data class PlayerAction(
    val playerId: Int,
    val action: Action,
    val playedCard: Card? = null,
    val targetCard: Card? = null,
    val position: Pair<Int, Int>? = null   // (row, col)
)




