package gui

import service.Refreshable
import service.RootService
import tools.aqua.bgw.core.BoardGameApplication


/**
 * Main BGW application for the game "Card Staircase".
 */
class CardStaircaseApplication : BoardGameApplication("Card Staircase"), Refreshable {

    // Central service from which all others are created/accessed
    // also holds the currently active game
    private val rootService = RootService()

    // Scenes
    private val gamefineshed= Gamefineshed(rootService)
    private val mainMenuScene = MainMenuScene(rootService)
    private val resultScene = ResultScene(rootService)
    // "Next turn" pops up
    private val startTurnScene = StartTurnScene(rootService).apply {
        // what should happen when the user clicks on the popup
        onCloseRequested = {
            hideMenuScene()   // closes the StartTurnScene overlay
        }
    }
    // This menu scene is shown after application start and if the "new game" button
    // is clicked in the gameFinishedMenuScene
    private val newGameMenuScene = MainMenuScene(rootService).apply {
        quitButton.onMouseClicked = {
            exit()
        }
    }
    // This is where the actual game takes place
    private val gameScene = GameScene(rootService)

    init {
        // all scenes are refreshables
        rootService.addRefreshables(
            this,
            gameScene,
            mainMenuScene,
            gamefineshed,
            startTurnScene,
            resultScene,
            newGameMenuScene
        )

        // to show main menu on top of the game scene
        showGameScene(gameScene)

        showMenuScene(mainMenuScene)
        gamefineshed.onShowResult = {
            hideMenuScene()
            resultScene.showresult()
            showGameScene(resultScene)
        }
        this.showMenuScene(newGameMenuScene, 0)
    }

    /**
     * Called by GameService after a game has started.
     * Hides the main menu after a short delay.
     */
    override fun refreshAfterGameStart() {
        hideMenuScene(500)
    }

    /**
     * Called when the game ends: show result screen.
     */
    override fun refreshAfterGameEnd(winner: String) {
        showMenuScene(gamefineshed)
    }

    /**
     * Called after the active player changes (after draw).
     * Shows the "Next turn: <name>" popup.
     */
    override fun refreshAfterTurnStart() {
        showMenuScene(startTurnScene)
    }



}

