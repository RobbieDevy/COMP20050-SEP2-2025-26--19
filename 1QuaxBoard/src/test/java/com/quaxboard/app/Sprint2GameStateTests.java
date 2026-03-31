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
        assertNull(state.getOctagonOwner(0, 0));
        assertTrue(state.isOctagonEmpty(0, 0));
    }

    @Test
    void rhoOwnerStartsEmpty() {
        GameState state = new GameState(11);
        assertNull(state.getRhombusOwner(0, 0));
        assertTrue(state.isRhombusEmpty(0, 0));
    }
}