package com.quaxboard.app;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Sprint3Tests {
    @Test
    void pieRuleNotAvailableAtGameStart() {
        GameState state = new GameState(11);
        GameController controller = new GameController(state);

        assertEquals(GameState.Player.BLACK, controller.currentPlayer());
        assertFalse(controller.canUsePieRule());
        assertFalse(controller.activatePieRule().success());

        assertEquals(GameState.HumanPlayer.PLAYER_1, state.getBlackPlayer());
        assertEquals(GameState.HumanPlayer.PLAYER_2, state.getWhitePlayer());
    }

    @Test
    void pieRuleAvailableAfterFirstMove() {
        GameState state = new GameState(11);
        GameController controller = new GameController(state);

        GameController.MoveResult result = controller.place(GameController.CellType.OCTAGON, 0, 0);

        assertTrue(result.success());
        assertEquals(GameState.Player.BLACK, state.getOctagonOwner(0, 0));
        assertEquals(GameState.Player.WHITE, controller.currentPlayer());
        assertTrue(controller.canUsePieRule());
    }

    @Test
    void pieRuleSwapsColours() {
        GameState state = new GameState(11);
        GameController controller = new GameController(state);

        controller.place(GameController.CellType.OCTAGON, 0, 0);

        assertEquals(GameState.HumanPlayer.PLAYER_1, state.getBlackPlayer());
        assertEquals(GameState.HumanPlayer.PLAYER_2, state.getWhitePlayer());

        GameController.MoveResult pieRuleResult = controller.activatePieRule();

        assertTrue(pieRuleResult.success());
        assertEquals(GameState.HumanPlayer.PLAYER_2, state.getBlackPlayer());
        assertEquals(GameState.HumanPlayer.PLAYER_1, state.getWhitePlayer());

        assertEquals(GameState.HumanPlayer.PLAYER_2, state.getPlayerForColour(GameState.Player.BLACK));
        assertEquals(GameState.HumanPlayer.PLAYER_1, state.getPlayerForColour(GameState.Player.WHITE));
    }

    @Test
    void pieRuleCannotBeUsedTwice() {
        GameState state = new GameState(11);
        GameController controller = new GameController(state);

        controller.place(GameController.CellType.OCTAGON, 0, 0);

        assertTrue(controller.activatePieRule().success());
        assertFalse(controller.canUsePieRule());
        assertFalse(controller.activatePieRule().success());
    }
}