package com.quaxboard.app;

public class GameController {
    public enum CellType {
        OCTAGON,
        RHOMBUS
    }


    public record MoveResult(boolean success, GameState.Player winner) {}

    public record BotMove(CellType cellType, int row, int col, String strategy) {}

    private final GameState state;

    public GameController(GameState state) {
        this.state = state;
    }

    public GameState.Player currentPlayer() {
        return state.getCurrentPlayer();
    }

    public MoveResult place(CellType type, int row, int col) {
        return switch (type) {
            case OCTAGON -> placeOctagon(row, col);
            case RHOMBUS -> placeRhombus(row, col);
        };
    }

    public boolean canUsePieRule() {
        return state.canUsePieRule();
    }

    public MoveResult activatePieRule() {
        if (!canUsePieRule()) {
            return new MoveResult(false, null);
        }

        state.usePieRule();
        return new MoveResult(true, null);
    }

    private MoveResult placeOctagon(int row, int col) {
        if (isOutsideOctagonBoard(row, col) || !state.isOctagonEmpty(row, col)) {
            return new MoveResult(false, null);
        }

        GameState.Player mover = state.getCurrentPlayer();
        state.setOctagonOwner(row, col, mover);
        return completeSuccessfulMove(mover);
    }

    private MoveResult placeRhombus(int row, int col) {
        if (isOutsideRhombusBoard(row, col) || !state.isRhombusEmpty(row, col)) {
            return new MoveResult(false, null);
        }

        GameState.Player mover = state.getCurrentPlayer();
        state.setRhombusOwner(row, col, mover);
        return completeSuccessfulMove(mover);
    }

    private boolean isOutsideOctagonBoard(int row, int col) {
        int boardSize = state.getBoardSize();
        return row < 0 || row >= boardSize || col < 0 || col >= boardSize;
    }

    private boolean isOutsideRhombusBoard(int row, int col) {
        int rhombusGridSize = state.getBoardSize() - 1;
        return row < 0 || row >= rhombusGridSize || col < 0 || col >= rhombusGridSize;
    }

    private MoveResult completeSuccessfulMove(GameState.Player mover) {
        state.recordSuccessfulMove();

        if (state.hasWinningChain(mover)) {
            state.setWinner(mover);
            return new MoveResult(true, mover);
        }

        state.switchTurn();
        return new MoveResult(true, null);
    }
}