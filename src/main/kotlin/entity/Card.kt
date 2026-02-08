package entity

    /**
     * Represents a card with suit and value.
     *
     * Each card has a [suit] (e.g., Hearts) and a [value] (e.g., ACE, TEN, KING).
     * The card can provide its corresponding point value in the game via [getPoints].
     *
     * @param suit The suit of the card.
     * @param value The value of the card.
     */
    data class Card(val suit: CardSuit, val value: CardValue) {
        /**
         * Returns the point value of this card according to the game rules.
         *
         * The points are determined as follows:
         * - ACE = 1
         * - TWO = 2
         * - THREE = 3
         * - FOUR = 4
         * - FIVE = 5
         * - SIX = 6
         * - SEVEN = 7
         * - EIGHT = 8
         * - NINE = 9
         * - TEN = 10
         * - JACK = 10
         * - QUEEN = 15
         * - KING = 20
         *
         * @return The point value of the card.
         */
        fun getPoints(): Int {
            return when (value) {
                CardValue.ACE -> 1
                CardValue.TWO -> 2
                CardValue.THREE -> 3
                CardValue.FOUR -> 4
                CardValue.FIVE -> 5
                CardValue.SIX -> 6
                CardValue.SEVEN -> 7
                CardValue.EIGHT -> 8
                CardValue.NINE -> 9
                CardValue.TEN -> 10
                CardValue.JACK -> 10
                CardValue.QUEEN -> 15
                CardValue.KING -> 20
            }
        }


    override fun toString() = "$suit$value"
}
