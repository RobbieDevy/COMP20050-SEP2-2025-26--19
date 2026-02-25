package com.quaxboard.app;

public class GameState {
    public enum Player {
        BLACK, WHITE;

        public Player next() {
            return this == BLACK ? WHITE : BLACK;
        }
    }

    private final int boardSize;
    private final Player[][] octOwner;
    private final Player[][] rhoOwner;
    private Player currentPlayer = Player.BLACK;

    public GameState(int boardSize) {
        this.boardSize = boardSize;
        this.octOwner = new Player[boardSize][boardSize];
        this.rhoOwner = new Player[boardSize - 1][boardSize - 1];
    }

    public int getBoardSize() {
        return boardSize;
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public void switchTurn() {
        currentPlayer = currentPlayer.next();
    }

    public Player getOctOwner(int row, int col) {
        return octOwner[row][col];
    }

    public boolean isOctEmpty(int row, int col) {
        return octOwner[row][col] == null;
    }

    public void setOctOwner(int row, int col, Player player) {
        octOwner[row][col] = player;
    }

    public Player getRhoOwner(int row, int col) {
        return rhoOwner[row][col];
    }

    public boolean isRhoEmpty(int row, int col) {
        return rhoOwner[row][col] == null;
    }
    public void setRhoOwner(int row, int col, Player player) {
        rhoOwner[row][col] = player;
    }
}