package com.quaxboard.app;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Sprint2GameControllerTests {

    @Test
    void placeOctagonOnEmptyCellWorksAndSwitchesTurn() {
        GameState state = new GameState(11);
        GameController controller = new GameController(state);

        var result = controller.place(GameController.cellType.OCTAGON, 3, 4);

        assertTrue(result.success(), "Expected move to succeed");
        assertEquals(GameState.Player.BLACK, state.getOctOwner(3, 4), "Cell should be owned by BLACK");
        assertEquals(GameState.Player.WHITE, state.getCurrentPlayer(), "Turn should switch to WHITE");
    }

    @Test
    void cannotPlaceOctagonOnOccupiedCell() {
        GameState state = new GameState(11);
        GameController controller = new GameController(state);

        assertTrue(controller.place(GameController.cellType.OCTAGON, 0, 0).success());

        var result = controller.place(GameController.cellType.OCTAGON, 0, 0);
        assertFalse(result.success(), "Expected move to fail on occupied cell");

        assertEquals(GameState.Player.BLACK, state.getOctOwner(0, 0));
    }

    @Test
    void placeRhombusOnEmptyCellWorksAndSwitchesTurn() {
        GameState state = new GameState(11);
        GameController controller = new GameController(state);

        var result = controller.place(GameController.cellType.RHOMBUS, 2, 2);

        assertTrue(result.success(), "Expected move to succeed");
        assertEquals(GameState.Player.BLACK, state.getRhoOwner(2, 2), "Rhombus should be owned by BLACK");
        assertEquals(GameState.Player.WHITE, state.getCurrentPlayer(), "Turn should switch to WHITE");
    }

    @Test
    void cannotPlaceRhombusOnOccupiedCell() {
        GameState state = new GameState(11);
        GameController controller = new GameController(state);

        assertTrue(controller.place(GameController.cellType.RHOMBUS, 1, 1).success());

        var result = controller.place(GameController.cellType.RHOMBUS, 1, 1);
        assertFalse(result.success(), "Expected move to fail on occupied rhombus");

        assertEquals(GameState.Player.BLACK, state.getRhoOwner(1, 1));
    }

    @Test
    void rhombusHasSmallerGridThanOctagon() {
        GameState state = new GameState(11);
        GameController controller = new GameController(state);

        var result = controller.place(GameController.cellType.RHOMBUS, 10, 0);
        assertFalse(result.success(), "Expected out-of-bounds rhombus move to fail");
    }

    @Test
    void octagonBoundsAre11By11() {
        GameState state = new GameState(11);
        GameController controller = new GameController(state);

        assertTrue(controller.place(GameController.cellType.OCTAGON, 10, 10).success());

        var result = controller.place(GameController.cellType.OCTAGON, 11, 0);
        assertFalse(result.success(), "Expected out-of-bounds octagon move to fail");
    }
}