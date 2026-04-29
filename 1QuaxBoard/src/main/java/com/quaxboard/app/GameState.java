package com.quaxboard.app;
import java.util.Random;

public class GameState {
    private static final Random RANDOM = new Random();
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
    private Player winner = null;

    private HumanPlayer blackPlayer;
    private HumanPlayer whitePlayer;

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
        this(boardSize, RANDOM.nextBoolean());
    }

    public GameState(int boardSize, boolean botPlaysBlack) {
        this.boardSize = boardSize;
        this.octagonOwners = new Player[boardSize][boardSize];
        this.rhombusOwners = new Player[boardSize - 1][boardSize - 1];

        if (botPlaysBlack) {
            this.blackPlayer = HumanPlayer.PLAYER_2;
            this.whitePlayer = HumanPlayer.PLAYER_1;
        } else {
            this.blackPlayer = HumanPlayer.PLAYER_1;
            this.whitePlayer = HumanPlayer.PLAYER_2;
        }
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

    public Player getWinner() {
        return winner;
    }

    public boolean isGameOver() {
        return winner != null;
    }

    public void setWinner(Player winner) {
        this.winner = winner;
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

    public boolean hasWinningChain(Player player) {
        boolean[][] visitedOctagons = new boolean[boardSize][boardSize];
        boolean[][] visitedRhombuses = new boolean[boardSize - 1][boardSize - 1];

        if (player == Player.BLACK) {
            for (int col = 0; col < boardSize; col++) {
                if (dfsOctagon(player, 0, col, visitedOctagons, visitedRhombuses)) {
                    return true;
                }
            }
        } else {
            for (int row = 0; row < boardSize; row++) {
                if (dfsOctagon(player, row, 0, visitedOctagons, visitedRhombuses)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean dfsOctagon(
            Player player,
            int row,
            int col,
            boolean[][] visitedOctagons,
            boolean[][] visitedRhombuses
    ) {
        if (row < 0 || row >= boardSize || col < 0 || col >= boardSize) return false;
        if (visitedOctagons[row][col]) return false;
        if (getOctagonOwner(row, col) != player) return false;

        visitedOctagons[row][col] = true;

        if (player == Player.BLACK && row == boardSize - 1) return true;
        if (player == Player.WHITE && col == boardSize - 1) return true;

        return
                dfsOctagon(player, row - 1, col, visitedOctagons, visitedRhombuses) ||
                        dfsOctagon(player, row + 1, col, visitedOctagons, visitedRhombuses) ||
                        dfsOctagon(player, row, col - 1, visitedOctagons, visitedRhombuses) ||
                        dfsOctagon(player, row, col + 1, visitedOctagons, visitedRhombuses) ||
                        dfsRhombus(player, row - 1, col - 1, visitedOctagons, visitedRhombuses) ||
                        dfsRhombus(player, row - 1, col, visitedOctagons, visitedRhombuses) ||
                        dfsRhombus(player, row, col - 1, visitedOctagons, visitedRhombuses) ||
                        dfsRhombus(player, row, col, visitedOctagons, visitedRhombuses);
    }

    private boolean dfsRhombus(
            Player player,
            int row,
            int col,
            boolean[][] visitedOctagons,
            boolean[][] visitedRhombuses
    ) {
        if (row < 0 || row >= boardSize - 1 || col < 0 || col >= boardSize - 1) return false;
        if (visitedRhombuses[row][col]) return false;
        if (getRhombusOwner(row, col) != player) return false;

        visitedRhombuses[row][col] = true;

        return
                dfsOctagon(player, row, col, visitedOctagons, visitedRhombuses) ||
                        dfsOctagon(player, row, col + 1, visitedOctagons, visitedRhombuses) ||
                        dfsOctagon(player, row + 1, col, visitedOctagons, visitedRhombuses) ||
                        dfsOctagon(player, row + 1, col + 1, visitedOctagons, visitedRhombuses);
    }
}