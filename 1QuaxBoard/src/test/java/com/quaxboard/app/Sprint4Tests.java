package com.quaxboard.app;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Sprint4Tests {

    @Test
    void blackWins() {
        GameState state = new GameState(11);
        GameController controller = new GameController(state);

        for (int row = 0; row < 10; row++) {
            state.setOctagonOwner(row, 0, GameState.Player.BLACK);
        }

        var result = controller.place(GameController.CellType.OCTAGON, 10, 0);

        assertTrue(result.success());
        assertEquals(GameState.Player.BLACK, result.winner());
        assertEquals(GameState.Player.BLACK, state.getWinner());
        assertTrue(state.isGameOver());
    }

    @Test
    void whiteWins() {
        GameState state = new GameState(11);
        GameController controller = new GameController(state);

        state.switchTurn(); // make it WHITE's turn

        for (int col = 0; col < 10; col++) {
            state.setOctagonOwner(0, col, GameState.Player.WHITE);
        }

        var result = controller.place(GameController.CellType.OCTAGON, 0, 10);

        assertTrue(result.success());
        assertEquals(GameState.Player.WHITE, result.winner());
        assertEquals(GameState.Player.WHITE, state.getWinner());
        assertTrue(state.isGameOver());
    }

    @Test
    void botMakesValidMove() {
        GameState state = new GameState(11);
        GameController controller = new GameController(state);
        BotPlayer bot = new BotPlayer(state);

        assertTrue(controller.place(GameController.CellType.OCTAGON, 5, 5).success());
        assertEquals(GameState.Player.WHITE, controller.currentPlayer());

        var result = bot.makeMove(controller);

        assertTrue(result.success());
        assertEquals(GameState.Player.BLACK, controller.currentPlayer());
        assertEquals(1, countPiecesOwnedBy(state, GameState.Player.WHITE));
        assertEquals(2, countOccupiedCells(state));
    }

    private int countPiecesOwnedBy(GameState state, GameState.Player player) {
        int count = 0;

        for (int row = 0; row < state.getBoardSize(); row++) {
            for (int col = 0; col < state.getBoardSize(); col++) {
                if (state.getOctagonOwner(row, col) == player) {
                    count++;
                }
            }
        }

        for (int row = 0; row < state.getBoardSize() - 1; row++) {
            for (int col = 0; col < state.getBoardSize() - 1; col++) {
                if (state.getRhombusOwner(row, col) == player) {
                    count++;
                }
            }
        }

        return count;
    }

    private int countOccupiedCells(GameState state) {
        return countPiecesOwnedBy(state, GameState.Player.BLACK)
                + countPiecesOwnedBy(state, GameState.Player.WHITE);
    }
}
