package gui

import entity.Player
import service.Refreshable
import service.RootService
import tools.aqua.bgw.components.layoutviews.Pane
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.components.uicomponents.UIComponent
import tools.aqua.bgw.core.Alignment
import tools.aqua.bgw.core.Color
import tools.aqua.bgw.core.MenuScene
import tools.aqua.bgw.util.Font
import tools.aqua.bgw.visual.ColorVisual

/**
 * Winner popup shown when the game is finished.
 */
class Gamefineshed(private val rootService: RootService) :
    MenuScene(1920, 1080), Refreshable {

    /** callback set from the application to open the result scene */
    var onShowResult: (() -> Unit)? = null

    private val contentPane = Pane<UIComponent>(
        width = 700,
        height = 500,
        posX = 1920 / 2 - 700 / 2,
        posY = 1080 / 2 - 500 / 2,
        visual = ColorVisual(Color(0x0C2027))
    )

    private val titleLabel = Label(
        text = "RESULT",
        width = 700,
        height = 100,
        posX = 0,
        posY = 30,
        alignment = Alignment.CENTER,
        font = Font(30, Color(0xFFFFFF), "JetBrains Mono ExtraBold")
    )

    private val winnerLabel = Label(
        text = "",
        width = 600,
        height = 200,
        posX = 50,
        posY = 150,
        alignment = Alignment.CENTER,
        font = Font(45, Color(0xFFFFFF), "JetBrains Mono ExtraBold"),
        visual = ColorVisual(Color(0x49585D))
    )

    private val showResultButton = Button(
        text = "Show Stats",
        width = 200,
        height = 50,
        posX = 250,
        posY = 380,
        font = Font(22, Color(0xFFFFFF), "JetBrains Mono ExtraBold"),
        visual = ColorVisual(Color(0x49585D))
    ).apply {
        onMouseClicked = {
            onShowResult?.invoke()
        }
    }

    init {
        background = ColorVisual(Color(12, 32, 39, 240))
        contentPane.addAll(titleLabel, winnerLabel, showResultButton)
        addComponents(contentPane)
    }

    override fun refreshAfterGameEnd(winner: String) {
        winnerLabel.text = winner
    }
}
