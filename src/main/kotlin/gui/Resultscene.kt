package gui

import entity.Card
import entity.CardStaircaseGame
import service.Refreshable
import service.RootService
import tools.aqua.bgw.components.container.LinearLayout
import tools.aqua.bgw.components.gamecomponentviews.CardView
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.core.Alignment
import tools.aqua.bgw.core.BoardGameScene
import tools.aqua.bgw.core.Color
import tools.aqua.bgw.util.Font
import tools.aqua.bgw.visual.ColorVisual
import tools.aqua.bgw.components.uicomponents.ListView
import tools.aqua.bgw.components.uicomponents.Orientation
import tools.aqua.bgw.components.uicomponents.SelectionMode
/**
 * Shows final results: gained stacks of both players, their points and full game log.
 */
class ResultScene(private val rootService: RootService) :
    BoardGameScene(1920, 1080, background = ColorVisual(Color(0x0C2027))),
    Refreshable {

    private val cardImageLoader = CardImageLoader()

    /** Title at the top */
    private val titleLabel = Label(
        text = "Final Result",
        width = 1920,
        height = 80,
        posX = 0,
        posY = 40,
        alignment = Alignment.CENTER,
        font = Font(36, Color(0xFFFFFF), "JetBrains Mono ExtraBold")
    )



    private val player1NameLabel = Label(
        text = "Player 1",
        width = 600,
        height = 40,
        posX = 150,
        posY = 150,
        alignment = Alignment.CENTER_LEFT,
        font = Font(26, Color(0xFFFFFF), "JetBrains Mono ExtraBold")
    )

    private val player1PointsLabel = Label(
        text = "",
        width = 600,
        height = 40,
        posX = 150,
        posY = 200,
        alignment = Alignment.CENTER_LEFT,
        font = Font(22, Color(0xFFFFFF), "JetBrains Mono ExtraBold")
    )

    /** All gained cards of player 1 */
    private val player1GainedLayout = LinearLayout<CardView>(
        posX = 150,
        posY = 270,
        width = 700,
        height = 200,
        alignment = Alignment.CENTER_LEFT,
        spacing = -30
    )



    private val player2NameLabel = Label(
        text = "Player 2",
        width = 600,
        height = 40,
        posX = 1920 - 150 - 600,
        posY = 150,
        alignment = Alignment.CENTER_RIGHT,
        font = Font(26, Color(0xFFFFFF), "JetBrains Mono ExtraBold")
    )

    private val player2PointsLabel = Label(
        text = "",
        width = 600,
        height = 40,
        posX = 1920 - 150 - 600,
        posY = 200,
        alignment = Alignment.CENTER_RIGHT,
        font = Font(22, Color(0xFFFFFF), "JetBrains Mono ExtraBold")
    )

    /** All gained cards of player 2 */
    private val player2GainedLayout = LinearLayout<CardView>(
        posX = 1920 - 150 - 700,
        posY = 270,
        width = 700,
        height = 200,
        alignment = Alignment.CENTER_RIGHT,
        spacing = -30
    )



    private val logTitleLabel = Label(
        text = "Game Log",
        width = 400,
        height = 40,
        posX = (1920 - 400) / 2,
        posY = 520,
        alignment = Alignment.CENTER,
        font = Font(24, Color(0xFFFFFF), "JetBrains Mono ExtraBold")
    )
    // left log
    private val logListLeft = ListView<String>(
        posX = 250,
        posY = 570,
        width = 780,
        height = 430,
        items = mutableListOf(),
        visual = ColorVisual(Color(0x0C2027)),
        font = Font(18, Color(0xFFFFFF), "JetBrains Mono"),
        selectionMode = SelectionMode.SINGLE,
        selectionBackground = ColorVisual(Color(0x0C2027))
    )

    // right log
    private val logListRight = ListView<String>(
        posX = 250 + 780 + 60,
        posY = 570,
        width = 780,
        height = 430,
        items = mutableListOf(),
        visual = ColorVisual(Color(0x0C2027)),
        font = Font(18, Color(0xFFFFFF), "JetBrains Mono"),
        selectionMode = SelectionMode.SINGLE,
        selectionBackground = ColorVisual(Color(0x0C2027))
    )




    init {
        addComponents(
            titleLabel,
            player1NameLabel,
            player1PointsLabel,
            player1GainedLayout,
            player2NameLabel,
            player2PointsLabel,
            player2GainedLayout,
            logTitleLabel,
            logListLeft,
            logListRight
        )
    }

    /** Called from services when the game ends. */
    override fun refreshAfterGameEnd(winner: String) {
        val game = rootService.currentGame ?: return
        updateFromGame(game)
    }

    /** Forces reloading the result view from the current game state. */
    fun showresult() {
        val game = rootService.currentGame ?: return
        updateFromGame(game)
    }



    /** Updates labels, gained cards and log from the given game state. */
    private fun updateFromGame(game: CardStaircaseGame) {
        val p1 = game.player1
        val p2 = game.player2

        player1NameLabel.text = p1.name
        player2NameLabel.text = p2.name

        player1PointsLabel.text = "Points: ${game.getPlayerPoints(p1)}"
        player2PointsLabel.text = "Points: ${game.getPlayerPoints(p2)}"

        fillGainedLayout(player1GainedLayout, p1.gainedStack)
        fillGainedLayout(player2GainedLayout, p2.gainedStack)

        //  full game log from GameService aftre storage
        showLog(rootService.gameService.getLog())
    }

    /** Fills a layout with front-facing CardViews for the given gained cards. */
    private fun fillGainedLayout(layout: LinearLayout<CardView>, cards: List<Card>) {
        layout.clear()
        cards.forEach { card ->
            layout.add(
                CardView(
                    width = 120,
                    height = 170,
                    front = cardImageLoader.frontImageFor(card.suit, card.value),
                    back = cardImageLoader.backImage
                ).apply {
                    showFront()
                    isDraggable = false
                }
            )
        }
    }

    /** Splits the log into two columns and numbers each entry. */
    private fun showLog(lines: List<String>) {
        val leftItems = logListLeft.items
        val rightItems = logListRight.items
        leftItems.clear()
        rightItems.clear()

        val half = (lines.size + 1) / 2  // first half size

        // left column: 1..half
        lines.take(half).forEachIndexed { index, line ->
            leftItems.add("Action ${index + 1}: $line")
        }

        // right column: half+1..n
        lines.drop(half).forEachIndexed { index, line ->
            val number = half + index + 1
            rightItems.add("Action $number: $line")
        }

    }



}
