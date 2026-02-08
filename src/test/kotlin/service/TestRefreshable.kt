package service

class TestRefreshable(val rootService: RootService): Refreshable {
        val game = rootService.currentGame
        var refreshAfterGameStartCalled = false
            private set


        var refreshAfterDestroyCardCalled = false
            private set

        var refreshAfterDrawCardCalled = false
            private set

        var refreshAfterDiscardCalled = false
            private set

        var refreshAfterCombineCalled = false
            private set

        var refreshAfterGameEndCalled = false
            private set
    var refreshAfterTurnStartCalled = false
        private set

        /** Reset all flags back to false (for next test). */
        fun reset() {
            refreshAfterGameStartCalled = false
            refreshAfterDestroyCardCalled = false
            refreshAfterDrawCardCalled = false
            refreshAfterDiscardCalled = false
            refreshAfterCombineCalled = false
            refreshAfterGameEndCalled = false
            refreshAfterTurnStartCalled=false
        }

        override fun refreshAfterGameStart() {
            refreshAfterGameStartCalled = true
        }



        override fun refreshAfterdestroycard() {
            refreshAfterDestroyCardCalled = true
        }

        override fun refreshAfterdrawcard() {
            refreshAfterDrawCardCalled = true
        }

    override fun refreshAfterTurnStart() {
        refreshAfterTurnStartCalled = true
      }

        override fun refreshAfterdiscard() {
            refreshAfterDiscardCalled = true
        }

        override fun refreshAftercombine() {
            refreshAfterCombineCalled = true
        }

        override fun refreshAfterGameEnd(winner: String) {
            refreshAfterGameEndCalled = true
        }
    }


