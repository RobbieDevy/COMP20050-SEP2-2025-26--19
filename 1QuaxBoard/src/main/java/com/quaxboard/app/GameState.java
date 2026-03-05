package com.quaxboard.app;

/*
GameState is the data holder for the game.
Stores board size, who owns each cell, and whose turn it is.
Has methods to get/set cell owners, check if a cell is empty, and get current player
Switches turn between BLACK AND WHITE.
 */
public class GameState {
    public enum Player { // enum for black and white player
        BLACK, WHITE;

        public Player next() { // returns which player comes next
            return this == BLACK ? WHITE : BLACK; // if the current player is black, return white, otherwise return black
        }
    }

    private final int boardSize; // stores board size
    private final Player[][] octOwner; // octOwner is a 2D array for who owns each octagon
    private final Player[][] rhoOwner; // rhoOwner is a 2D array for who owns each rhombus
    private Player currentPlayer = Player.BLACK; // current player is black

    public GameState(int boardSize) { // constructor for GameState
        this.boardSize = boardSize; // saves board size
        this.octOwner = new Player[boardSize][boardSize]; // creates octagon owner grid
        this.rhoOwner = new Player[boardSize - 1][boardSize - 1]; // creates rhombus owner grid
    }

    public int getBoardSize() {
        return boardSize;
    } // returns board size

    public Player getCurrentPlayer() {
        return currentPlayer;
    } // returns current player

    public void switchTurn() {
        currentPlayer = currentPlayer.next();
    } // switches player turn

    public Player getOctOwner(int row, int col) {
        return octOwner[row][col];
    } // returns who owns octagon cell

    public boolean isOctEmpty(int row, int col) {
        return octOwner[row][col] == null;
    } // returns if octagon cell is empty or not

    public void setOctOwner(int row, int col, Player player) {
        octOwner[row][col] = player;
    } // sets who owns octagon cell

    public Player getRhoOwner(int row, int col) {
        return rhoOwner[row][col];
    } // returns who owns rhombus cell

    public boolean isRhoEmpty(int row, int col) {
        return rhoOwner[row][col] == null;
    } // returns if rhombus cell is empty or not
    public void setRhoOwner(int row, int col, Player player) {
        rhoOwner[row][col] = player;
    } // sets who owns rhombus cell
}