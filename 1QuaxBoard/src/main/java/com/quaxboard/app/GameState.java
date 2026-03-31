package com.quaxboard.app;

public class GameState {
    public enum Player {
        BLACK, WHITE;

        public Player next() {
            return this == BLACK ? WHITE : BLACK;
        }
    }

    // Player represents colour of who is to play, HumanPlayer represents the player that owns that colour.
    // this was implemented to ensure the pie rule works correctly.
    public enum HumanPlayer {
        PLAYER_1, PLAYER_2
    }

    private final int boardSize;
    private final Player[][] octagonOwners;
    private final Player[][] rhombusOwners;
    private Player currentPlayer = Player.BLACK;
    private int moveCount = 0;
    private boolean pieRuleUsed = false;

    private HumanPlayer blackPlayer = HumanPlayer.PLAYER_1;
    private HumanPlayer whitePlayer = HumanPlayer.PLAYER_2;

    public HumanPlayer getBlackPlayer() {
        return blackPlayer;
    }

    public HumanPlayer getWhitePlayer() {
        return whitePlayer;
    }

    public HumanPlayer getPlayerForColour(Player colour) {
        return colour == Player.BLACK ? blackPlayer : whitePlayer;
    }

    public GameState(int boardSize) {
        this.boardSize = boardSize;
        this.octagonOwners = new Player[boardSize][boardSize];
        this.rhombusOwners = new Player[boardSize - 1][boardSize - 1];
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

    public Player getOctagonOwner(int row, int col) {
        return octagonOwners[row][col];
    }

    public boolean isOctagonEmpty(int row, int col) {
        return octagonOwners[row][col] == null;
    }

    public void setOctagonOwner(int row, int col, Player player) {
        octagonOwners[row][col] = player;
    }

    public Player getRhombusOwner(int row, int col) {
        return rhombusOwners[row][col];
    }

    public boolean isRhombusEmpty(int row, int col) {
        return rhombusOwners[row][col] == null;
    }

    public void setRhombusOwner(int row, int col, Player player) {
        rhombusOwners[row][col] = player;
    }

    public void recordSuccessfulMove() {
        moveCount++;
    }

    // Pie rule is only available after first move, when White is to play.
    public boolean canUsePieRule() {
        return moveCount == 1 && !pieRuleUsed && currentPlayer == Player.WHITE;
    }

    public void usePieRule() {
        if (!canUsePieRule()) {
            throw new IllegalStateException("Pie rule not available");
        }

        HumanPlayer originalOwner = blackPlayer;
        blackPlayer = whitePlayer;
        whitePlayer = originalOwner;

        pieRuleUsed = true;
    }
}