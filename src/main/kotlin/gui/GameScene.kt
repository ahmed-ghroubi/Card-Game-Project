package gui

import entity.Card
import entity.CardSuit
import entity.CardValue
import entity.CardStaircaseGame
import service.Refreshable
import service.RootService
import tools.aqua.bgw.components.container.LinearLayout
import tools.aqua.bgw.components.gamecomponentviews.CardView
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.core.Alignment
import tools.aqua.bgw.core.BoardGameScene
import tools.aqua.bgw.core.Color
import tools.aqua.bgw.util.BidirectionalMap
import tools.aqua.bgw.util.Font
import tools.aqua.bgw.visual.ColorVisual
import tools.aqua.bgw.components.container.CardStack
import tools.aqua.bgw.visual.ImageVisual
import tools.aqua.bgw.components.uicomponents.ListView
import tools.aqua.bgw.components.uicomponents.SelectionMode
/**
 * Main in-game scene using [RootService] to access game state and actions.
 */
class GameScene(private val rootService: RootService) :
    BoardGameScene(1920, 1080,  background = ImageVisual("img.png") ),
    Refreshable {

    /** Card and CardView mapping, built once. */
    private val cards: BidirectionalMap<Card, CardView> = BidirectionalMap()

    /** Currently selected card from the current player's hand (if any). */
    private var selectedCard: Card? = null // for discrad
    private var selectedStairRow: Int? = null // for destroy and combine
    private var selectedStairCol: Int? = null //for destroy and combine
    private val cardImageLoader = CardImageLoader()

    /** Current player's hand at the bottom. */
    private val playerHand = LinearLayout<CardView>(
        posX = 0,
        posY = 1080 - 170,
        width = 1920,
        height = 180,
        alignment = Alignment.CENTER,
        spacing = -30
    )

    /** Current player's name (bottom). */
    private val playerName = Label(
        posX = 1920 - 250,
        posY = 1080 - 120,
        width = 200,
        height = 50,
        text = "",
        alignment = Alignment.CENTER,
        visual = ColorVisual(Color(0x0C2027)),
        font = Font(22, Color(0xFFFFFF), "JetBrains Mono ExtraBold")
    )
    /** Current player's score (bottom, below the name). */
    private val playerScoreLabel = Label(
        posX = 1920 - 250,
        posY = 1080 - 70,
        width = 200,
        height = 40,
        text = "",
        alignment = Alignment.CENTER,
        visual = ColorVisual(Color(0x0C2027)),
        font = Font(18, Color(0xFFFFFF), "JetBrains Mono ExtraBold")
    )


    /** Opponent’s hand at the top (rotated, cards face down). */
    private val opponentHand = LinearLayout<CardView>(
        posX = 0,
        posY = 20,
        width = 1920,
        height = 150,
        alignment = Alignment.TOP_CENTER,
        spacing = -30
    ).apply {
        rotation = 180.0
    }

    /** Opponent’s name (top). */
    private val opponentName = Label(
        posX = 350,
        posY = 70,
        width = 200,
        height = 50,
        text = "",
        alignment = Alignment.CENTER,
        visual = ColorVisual(Color(0x0C2027)),
        font = Font(22, Color(0xFFFFFF), "JetBrains Mono ExtraBold")
    ).apply {
        rotation = 0.0
    }
    /** Opponent’s score (top, above the name). */
    private val opponentScoreLabel = Label(
        posX = 350,
        posY = 40,
        width = 200,
        height = 40,
        text = "",
        alignment = Alignment.CENTER,
        visual = ColorVisual(Color(0x0C2027)),
        font = Font(18, Color(0xFFFFFF), "JetBrains Mono ExtraBold")
    )
    /** Title above the log panel. */
    private val logTitleLabel = Label(
        posX = 40,
        posY = 170,
        width = 220,
        height = 35,
        text = "Log",
        alignment = Alignment.CENTER,
        visual = ColorVisual(Color(0x0C2027)),
        font = Font(20, Color(0xFFFFFF), "JetBrains Mono ExtraBold")
    )



    private val logListView = ListView<String>(
        posX = 60,
        posY = 210,
        width = 400,
        height = 430,
        items = mutableListOf(),
        visual = ColorVisual(Color(0x0C2027)),
        font = Font(18, Color(0xFFFFFF), "JetBrains Mono"),
        selectionMode = SelectionMode.NONE
    )

    /** 5 staircase rows on the left side, forming a half-square. */
    private val staircaseRows: List<LinearLayout<CardView>> =
        List(5) { rowIndex ->
            LinearLayout<CardView>(
                posX = 1920 / 2 - 300,
                posY = 220 + rowIndex * 120,
                width = 700,
                height = 60,
                alignment = Alignment.TOP_LEFT,
                spacing = 20
            )
        }

    /** Dump stack (discard pile) shown as a stack on the right side. */
    private val dumpStackView = CardStack<CardView>(
        posX = 1350,
        posY = 250,
        width = 130,
        height = 180,
        alignment = Alignment.CENTER
    )

    /** Placeholder label when dump stack is empty. */
    private val dumpStackEmptyLabel = Label(
        posX = 1350,
        posY = 250,
        width = 130,
        height = 180,
        text = "Dumpstack",
        alignment = Alignment.CENTER,
        visual = ColorVisual(Color(0x0C2027)),
        font = Font(18, Color(0xFFFFFF), "JetBrains Mono ExtraBold")
    ).apply {
        isVisible = false
    }

    /** Draw stack (face-down pile from which players draw). */
    private val drawStackView = CardStack<CardView>(
        posX = 1350,
        posY = 430,
        width = 130,
        height = 180,
        alignment = Alignment.CENTER
    )
    /** Label showing number of cards in the draw stack (for testing). */
    private val drawStackCountLabel = Label(
        posX = 1370,
        posY =  610,
        width = 100,
        height = 30,
        text = "Cards: 0",
        alignment = Alignment.CENTER,
        visual = ColorVisual(Color(0x0C2027)),
        font = Font(18, Color(0xFFFFFF), "JetBrains Mono ExtraBold")
    )


    /** Gained cards stack for current player. */
    private val gainedStackView = CardStack<CardView>(
        posX = 450,
        posY = 700,
        width = 130,
        height = 180,
        alignment = Alignment.CENTER
    )

    /** Placeholder label when gained cards stack is empty. */
    private val gainedStackEmptyLabel = Label(
        posX = 450,
        posY = 700,
        width = 130,
        height = 180,
        text = "GainedStack",
        alignment = Alignment.CENTER,
        visual = ColorVisual(Color(0x0C2027)),
        font = Font(18, Color(0xFFFFFF), "JetBrains Mono ExtraBold")
    ).apply {
        isVisible = false
    }

    private val drawButton = Button(
        posX = 1700,
        posY = 800,
        text = "Draw",
        width = 200,
        height = 40,
        font = Font(22, color = Color(0xFFFFFF), "JetBrains Mono ExtraBold"),
        visual = ColorVisual(Color(0x49585D))
    ).apply {
        rotation = 0.0
        onMouseClicked = { rootService.playerActionService.drawCard() }
    }

    private val discard = Button(
        posX = 1700,
        posY = 600,
        text = "Discard",
        width = 200,
        height = 40,
        font = Font(22, color = Color(0xFFFFFF), "JetBrains Mono ExtraBold"),
        visual = ColorVisual(Color(0x49585D))
    ).apply {
        rotation = 0.0
        isDisabled = true

        onMouseClicked = {
            val cardToDiscard = selectedCard
            if (cardToDiscard != null) {
                rootService.playerActionService.discard(cardToDiscard)
                selectedCard = null
                isDisabled = true
            }
        }
    }

    private val destroyButton = Button(
        posX = 1700,
        posY = 700,
        text = "Destroy",
        width = 200,
        height = 40,
        font = Font(22, color = Color(0xFFFFFF), "JetBrains Mono ExtraBold"),
        visual = ColorVisual(Color(0x49585D))

    ).apply {
        rotation = 0.0
        isDisabled = true

        onMouseClicked = {
            val row = selectedStairRow
            val col = selectedStairCol

            // if nothing selected: do nothing
            if (row != null && col != null) {
                rootService.playerActionService.destroyCard(row, col)

                // selection will be cleared in refreshAfterdestroycard,
                // but we clear local state here as well
                selectedStairRow = null
                selectedStairCol = null
                isDisabled = true
            }
        }
    }


    init {
        addComponents(
            playerHand,
            playerName,
            opponentHand,
            opponentName,
            *staircaseRows.toTypedArray(),
            drawButton,
            discard,
            dumpStackView,
            dumpStackEmptyLabel,
            gainedStackView,
            drawStackView,
            destroyButton,
            logTitleLabel,
            logListView,
            drawStackCountLabel,
            gainedStackEmptyLabel,
            playerScoreLabel,
            opponentScoreLabel
        )
    }

    /** Called after startGame() in service layer. */
    override fun refreshAfterGameStart() {
        val game = rootService.currentGame ?: return

        if (cards.isEmpty()) {
            initCardViews()
        }
        selectedCard = null
        discard.isDisabled = true
        selectedStairRow = null
        selectedStairCol = null
        destroyButton.isDisabled = true
        updateHandsAndNames(game)
        updateStaircase(game)
        updateDrawStack(game)
        updateDumpStack(game)
        updateGainedStack(game)
        showLog(rootService.gameService.getLog())
    }

    /** Updates UI after a draw action (hands, stacks, and log). */
    override fun refreshAfterdrawcard() {
        val game = rootService.currentGame ?: return

        // after drawing there is no selected card anymore so we do null and we do disabled fro discrad
        selectedCard = null
        discard.isDisabled = true
        selectedStairRow = null
        selectedStairCol = null
        destroyButton.isDisabled = true
        updateDumpStack(game)
        updateDrawStack(game)
        updateHandsAndNames(game)
        showLog(rootService.gameService.getLog())
    }


    /** Updates UI after a discard action (hands, dump stack, and log). */
    override fun refreshAfterdiscard() {
        val game = rootService.currentGame ?: return

        selectedCard = null
        discard.isDisabled = true
        selectedStairRow = null
        selectedStairCol = null
        destroyButton.isDisabled = true

        updateHandsAndNames(game)
        updateDumpStack(game)
        showLog(rootService.gameService.getLog())
    }

    /** Updates UI after a combine action (hands, staircase, gained stack, and log). */
    override fun refreshAftercombine() {
        val game = rootService.currentGame ?: return

        // after combining, the selected hand card is gone
        selectedCard = null
        discard.isDisabled = true
        selectedStairRow = null
        selectedStairCol = null
        destroyButton.isDisabled = true


        // sources first (hand and staircase), then destination (gained)
        updateHandsAndNames(game)
        updateStaircase(game)
        updateGainedStack(game)
        showLog(rootService.gameService.getLog())
    }

    /** Updates UI after a destroy action (staircase, dump stack, and log). */
    override fun refreshAfterdestroycard() {
        val game = rootService.currentGame ?: return

        // after destroy, no staircase card is selected
        selectedStairRow = null
        selectedStairCol = null
        destroyButton.isDisabled = true

        // source first staircase, then destination dump stack
        updateStaircase(game)
        updateDumpStack(game)
        showLog(rootService.gameService.getLog())
    }



    /** Build CardView for every Card in the deck. */
    private fun initCardViews() {
        cards.clear()
        CardValue.entries.forEach { value ->
            CardSuit.entries.forEach { suit ->
                val card = Card(suit, value)
                cards[card] = CardView(
                    posX = 0,
                    posY = 0,
                    width = 120,
                    height = 170,
                    front = cardImageLoader.frontImageFor(suit, value),
                    back = cardImageLoader.backImage
                )
            }
        }
    }

    /** Show current player's hand (front) and the other player's hand (back). */
    private fun updateHandsAndNames(game: CardStaircaseGame) {
        playerHand.clear()
        opponentHand.clear()

        val currentPlayer =
            if (game.currentplayer == 0) game.player1 else game.player2
        val otherPlayer =
            if (game.currentplayer == 0) game.player2 else game.player1

        playerName.text = currentPlayer.name
        opponentName.text = otherPlayer.name

        // current player's cards show fronts and can be selected
        currentPlayer.hand.forEach { card ->
            val view = cards[card]

            view.showFront()
            view.isDraggable = false

            // reset vertical offset
            view.posY = 0.0

            // if this card is selected, then I move it a bit upwards
            if (card == selectedCard) {
                view.posY = -30.0  // raise by 30
            }

            view.onMouseClicked = {
                onHandCardClicked(card)
            }
            drawButton.isDisabled = currentPlayer.hand.size != 4
            destroyButton.isDisabled = currentPlayer.hand.size != 4
            playerScoreLabel.text = "Score: ${game.getPlayerPoints(currentPlayer)}"
            opponentScoreLabel.text = "Score: ${game.getPlayerPoints(otherPlayer)}"

            playerHand.add(view)
        }


        // opponent’s cards and show backs, no click action
        otherPlayer.hand.forEach { card ->
            val view = cards[card]
            view.showBack()
            view.posY = 0.0
            view.isDraggable = false
            view.onMouseClicked = { }
            opponentHand.add(view)
        }
    }

    /** Handles selecting/deselecting a hand card and updates the hand view. */
    private fun onHandCardClicked(card: Card) {
        val game = rootService.currentGame ?: return
        val currentPlayer =
            if (game.currentplayer == 0) game.player1 else game.player2

        if (!currentPlayer.hand.contains(card)) return

        // toggle selection
        selectedCard = if (selectedCard == card) null else card

        // enable and disable discard button
        discard.isDisabled = selectedCard == null

        // redraw hands so the selected card is lifted
        updateHandsAndNames(game)
    }

    /** Called when i clicks an open staircase card. */
    private fun onOpenStaircaseCardClicked(row: Int, col: Int) {
        val game = rootService.currentGame ?: return

        val handCard = selectedCard
        if (handCard != null) {
            // Hand card selected then try combine
            rootService.playerActionService.combineCard(handCard, row, col)
            return
        }

        // No hand card selected so use this as destroy target (toggle)
        if (selectedStairRow == row && selectedStairCol == col) {
            // clicked again : deselect
            selectedStairRow = null
            selectedStairCol = null
            destroyButton.isDisabled = true
        } else {
            selectedStairRow = row
            selectedStairCol = col
            destroyButton.isDisabled = false
        }

        // redraw staircase so the selected card is lifted
        updateStaircase(game)
    }


    /** Draws the staircase rows from game.staircase. */
    private fun updateStaircase(game: CardStaircaseGame) {
        // Clear all visual rows first
        for (rowView in staircaseRows) {
            rowView.clear()
        }

        // For each row in the model staircase, fill the corresponding cards
        game.staircase.forEachIndexed { rowIndex, rowCards ->
            if (rowIndex < staircaseRows.size) {
                val rowView = staircaseRows[rowIndex]

                rowCards.forEachIndexed { colIndex, card ->
                    val view = cards[card]
                    // reset vertical offset
                    view.posY = 0.0

                    // the top card should be shown and we compare the column of the card with the size of the row above it if it's false then size of row
                    // is smaller than col of that card  then this card is top of the column so show it
                    if (!(rowIndex > 0 && game.staircase[rowIndex - 1].size > colIndex)) {
                        // open card face up and clickable
                        view.showFront()
                        view.onMouseClicked = {
                            onOpenStaircaseCardClicked(rowIndex, colIndex)
                        }

                        if (selectedStairRow == rowIndex && selectedStairCol == colIndex && selectedCard == null) {
                            view.posY = -20.0
                        }
                    } else {
                        // covered card face down plus no action
                        view.showBack()
                        view.onMouseClicked = { }
                    }

                    view.isDraggable = false
                    rowView.add(view)
                }
            }
        }
    }


    /** Shows the top card of the draw stack face-down and updates the card count label. */
    private fun updateDrawStack(game: CardStaircaseGame) {
        drawStackView.clear()

        // update label with current size
        val size = game.drawstack.size
        drawStackCountLabel.text = "Cards: $size"

        val topCard = game.drawstack.lastOrNull() ?: return

        val view = cards[topCard]
        view.showBack()
        view.isDraggable = false
        drawStackView.push(view)
    }


    /** Shows top of dumpstack, or a placeholder label if it is empty. */
    private fun updateDumpStack(game: CardStaircaseGame) {
        dumpStackView.clear()

        if (game.dumpstack.isEmpty()) {
            dumpStackEmptyLabel.isVisible = true
            return
        }

        dumpStackEmptyLabel.isVisible = false

        val topCard = game.dumpstack.last()
        val view = cards[topCard]
        view.showFront()
        view.isDraggable = false
        dumpStackView.push(view)
    }

    /** Shows top of gained stack for the current player, or a placeholder label if it is empty. */
    private fun updateGainedStack(game: CardStaircaseGame) {
        val currentPlayer =
            if (game.currentplayer == 0) game.player1 else game.player2
        gainedStackView.clear()

        if (currentPlayer.gainedStack.isEmpty()) {
            gainedStackEmptyLabel.isVisible = true
            return
        }

        gainedStackEmptyLabel.isVisible = false

        val topCard = currentPlayer.gainedStack.last()
        val view = cards[topCard]
        view.showBack()
        view.isDraggable = false
        gainedStackView.push(view)
    }

    /** Displays the given log lines in the list view with numbering. */
    private fun showLog(lines: List<String>) {
        val items = logListView.items
        items.clear()

        lines.forEachIndexed { index, line ->
            items.add("Action ${index + 1}: $line")
        }
    }

}




