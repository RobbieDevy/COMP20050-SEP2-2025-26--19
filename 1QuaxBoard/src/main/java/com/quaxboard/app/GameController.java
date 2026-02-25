package com.quaxboard.app;

public class GameController {
    public enum cellType {
        OCTAGON, RHOMBUS;
    }

    public record MoveResult(boolean success) {}

    private final GameState state;

    public GameController(GameState state) {
        this.state = state;
    }

    public GameState.Player currentPlayer() {
        return state.getCurrentPlayer();
    }

    public MoveResult place(cellType type, int row, int col) {
        if(type == cellType.OCTAGON) {
            if (row < 0 || row >= state.getBoardSize() || col < 0 || col >= state.getBoardSize()) {
                return new MoveResult(false);
            }

            if(!state.isOctEmpty(row, col)) {
                return new MoveResult(false);
            }

            state.setOctOwner(row, col, state.getCurrentPlayer());
            state.switchTurn();
            return new MoveResult(true);
        }
        if(type == cellType.RHOMBUS) {
            int n = state.getBoardSize() - 1;
            if (row < 0 || row >= n || col < 0 || col >= n) {
                return new MoveResult(false);
            }
            if(!state.isRhoEmpty(row, col)) {
                return new MoveResult(false);
            }

            state.setRhoOwner(row, col, state.getCurrentPlayer());
            state.switchTurn();
            return new MoveResult(true);
        }
        return null;
    }
}
