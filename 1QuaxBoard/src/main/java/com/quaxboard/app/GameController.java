package com.quaxboard.app;

/*
GameController is the rules manager.
It receives a move request to place a piece.
Uses GameState to ensure the clicked object is on the board and is not empty.
 */
public class GameController {
    public enum cellType { // an enum for each shape
        OCTAGON, RHOMBUS;
    }

    public record MoveResult(boolean success) {} // defines move result

    private final GameState state; // stores game state

    public GameController(GameState state) {
        this.state = state;
    }

    public GameState.Player currentPlayer() {
        return state.getCurrentPlayer();
    } // returns current player

    public MoveResult place(cellType type, int row, int col) {
        if(type == cellType.OCTAGON) { // if the cell type is OCTAGON
            // we check if row and column are oustide the board boundaries
            if (row < 0 || row >= state.getBoardSize() || col < 0 || col >= state.getBoardSize()) {
                return new MoveResult(false); // if they are, return false for moveResult
            }

            if(!state.isOctEmpty(row, col)) { // if the octagon cell is not empty
                return new MoveResult(false); // return false for moveResult
            }

            state.setOctOwner(row, col, state.getCurrentPlayer()); // set octagon cell owner to current player
            state.switchTurn(); // switch turn to other player
            return new MoveResult(true); // return true for moveResult
        }
        if(type == cellType.RHOMBUS) { // if the cell type is RHOMBUS
            int n = state.getBoardSize() - 1; // board size minus 1 for the rhombus grid
            // if row and column are outside the the board boundaries
            if (row < 0 || row >= n || col < 0 || col >= n) {
                return new MoveResult(false); // return false for moveResult
            }
            if(!state.isRhoEmpty(row, col)) { // if the rhombus cell is not empty
                return new MoveResult(false); // return false for moveResult
            }

            state.setRhoOwner(row, col, state.getCurrentPlayer()); // set rhombus cell owner to current player
            state.switchTurn(); // switch turn to other player
            return new MoveResult(true); // return true for moveResult
        }
        return null;
    }
}
