package service

import entity.CardStaircaseGame


/**
 * The root service class is responsible for managing services and the entity layer reference.
 * This class acts as a central hub for every other service within the application.
 *
 */

class RootService {

    val gameService = GameService(this)
    val playerActionService = PlayerActionService(this)

    /**
     * The currently active game. Can be `null`, if no game has started yet.
     */
    var currentGame: CardStaircaseGame? = null

    /**
     * Adds the provided [newRefreshable] to all services connected
     * to this root service
     */
    fun addRefreshable(newRefreshable: Refreshable) {
        gameService.addRefreshable(newRefreshable)
        playerActionService.addRefreshable(newRefreshable)
    }

    /**
     * Adds each of the provided [newRefreshables] to all services
     * connected to this root service
     */
    fun addRefreshables(vararg newRefreshables: Refreshable) {
        newRefreshables.forEach { addRefreshable(it) }
    }
}
