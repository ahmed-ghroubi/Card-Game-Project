package entity

/**
 * Represents one of the two players in the CardStaircase game.
 *
 * Responsibilities:
 * - Stores the player's [name].
 * - Holds the current [hand] cards (up to 5, enforced by the service layer).
 * - Holds all cards in the [gainedStack] that the player has collected
 *   during the game (and will be used later to compute the final score).
 */
data class Player(
    val name: String,
    var hand: MutableList<Card> = mutableListOf(),
    var gainedStack: MutableList<Card> = mutableListOf()
)
