package gui

import service.Refreshable
import service.RootService
import tools.aqua.bgw.core.MenuScene
import tools.aqua.bgw.core.Color
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.util.Font
import tools.aqua.bgw.visual.ColorVisual
/**
 * a "Next turn" popup menu scene using [RootService] to show the upcoming player.
 */
class StartTurnScene(private val rootService: RootService) :
    MenuScene(width = 400, height = 200), Refreshable {

    /** callback set from CardStaircaseApplication to close this popup */
    var onCloseRequested: (() -> Unit)? = null

    private val infoLabel = Label(
        posX = 50,
        posY = 60,
        width = 300,
        height = 40,
        text = "",
        font = Font(22, Color(0x000000), "JetBrains Mono ExtraBold"),
        visual = ColorVisual(Color(0xFFFFFF))
    )

    private val okButton = Button(
        posX = 150,
        posY = 120,
        width = 100,
        height = 40,
        text = "OK",
        font = Font(20, Color(0xFFFFFF), "JetBrains Mono ExtraBold"),
        visual = ColorVisual(Color(0x0C2027))
    ).apply {
        onMouseClicked = {
            onCloseRequested?.invoke()
        }
    }

    init {
        opacity = 1.0
        background = ColorVisual(Color(0xFFFFFF)) // white background
        addComponents(infoLabel, okButton)
    }

    /** Called from services when a new turn starts. */
    override fun refreshAfterTurnStart() {
        val game = rootService.currentGame ?: return
        val currentPlayer =
            if (game.currentplayer == 0) game.player1 else game.player2

        infoLabel.text = "Next turn: ${currentPlayer.name}"
    }
}

