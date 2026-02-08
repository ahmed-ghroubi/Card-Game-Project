package entity
import kotlin.test.*

/**
 * Tests [Card] for correct string formatting and point calculation.
 */
class Cardtest {
    // Some cards to perform the tests with
    private val nineOfClubs = Card(CardSuit.CLUBS, CardValue.NINE)
    private val nineOfdiamond = Card(CardSuit.DIAMONDS, CardValue.NINE)
    private val jackOfHearts = Card(CardSuit.HEARTS, CardValue.JACK)
    private val queenOfClubs = Card(CardSuit.CLUBS, CardValue.QUEEN)
    private val jackOfspades = Card(CardSuit.SPADES, CardValue.JACK)
    private val kingOfspades = Card(CardSuit.SPADES, CardValue.KING)
    //  characters for the suits, as those should be used by Card.toString
    private val heartsChar = '\u2665' // ♥
    private val diamondsChar = '\u2666' // ♦
    private val spadesChar = '\u2660' // ♠
    private val clubsChar = '\u2663' // ♣

    /** Verifies Card.toString uses the correct suit symbol and value shorthand. */
    @Test
    fun testToString() {
        assertEquals(clubsChar + "9", nineOfClubs.toString())
        assertEquals(diamondsChar + "9", nineOfdiamond.toString())
        assertEquals(heartsChar + "J", jackOfHearts.toString())
        assertEquals(clubsChar + "Q", queenOfClubs.toString())
        assertEquals(spadesChar + "J", jackOfspades.toString())
        assertEquals(spadesChar + "K", kingOfspades.toString())
    }

    /** Verifies getPoints() returns the expected scoring values for each card. */
    @Test
    fun testGetPoints() {
        assertEquals(10,jackOfspades.getPoints())
        assertEquals(15, queenOfClubs.getPoints())
        assertEquals(10, jackOfHearts.getPoints())
        assertEquals(9, nineOfdiamond.getPoints())
        assertEquals(9, nineOfClubs.getPoints())
        assertEquals(20, kingOfspades.getPoints())
        assertEquals(1, Card(CardSuit.HEARTS, CardValue.ACE).getPoints())
    }




}
