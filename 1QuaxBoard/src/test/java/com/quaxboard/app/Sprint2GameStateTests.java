package com.quaxboard.app;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Sprint2GameStateTests {

    @Test
    void currentPlayerStartsAsBlack() {
        GameState state = new GameState(11);
        assertEquals(GameState.Player.BLACK, state.getCurrentPlayer());
    }

    @Test
    void switchTurnChangesPlayer() {
        GameState state = new GameState(11);

        state.switchTurn();
        assertEquals(GameState.Player.WHITE, state.getCurrentPlayer());

        state.switchTurn();
        assertEquals(GameState.Player.BLACK, state.getCurrentPlayer());
    }

    @Test
    void octOwnerStartsEmpty() {
        GameState state = new GameState(11);
        assertNull(state.getOctOwner(0, 0));
        assertTrue(state.isOctEmpty(0, 0));
    }

    @Test
    void rhoOwnerStartsEmpty() {
        GameState state = new GameState(11);
        assertNull(state.getRhoOwner(0, 0));
        assertTrue(state.isRhoEmpty(0, 0));
    }
}