package com.quaxboard.app;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

public class BotPlayer {

    public record BotStrategy(GameController.CellType cellType, int row, int col, String title, String description) {}

    private record Move(GameController.CellType cellType, int row, int col) {}

    private final GameState gameState;
    private final int boardSize;

    public BotPlayer(GameState gameState) {
        this.gameState = gameState;
        this.boardSize = gameState.getBoardSize();
    }

    public GameController.MoveResult makeMove(GameController gameController) {
        BotStrategy chosenMove = chooseBestMove(gameController.currentPlayer());

        if (chosenMove == null) return new GameController.MoveResult(false, null);

        return gameController.place(chosenMove.cellType(), chosenMove.row(), chosenMove.col());
    }

    public BotStrategy peekStrategy() {
        return chooseBestMove(gameState.getCurrentPlayer());
    }

    private BotStrategy chooseBestMove(GameState.Player bot) {
        List<Move> allMoves = allEmptyMoves();
        if (allMoves.isEmpty()) return null;

        for (Move move : allMoves) {
            if (winsAfterMove(bot, move)) {
                return strategy(move, "Winning move", "The bot wins immediately.");
            }
        }

        List<Move> blockingMoves = new ArrayList<>();
        for (Move move : allMoves) {
            if (winsAfterMove(bot.next(), move)) blockingMoves.add(move);
        }
        if (!blockingMoves.isEmpty()) {
            Move move = pickBestFrom(blockingMoves, bot);
            return strategy(move, "Blocking move", "Blocks the opponent's winning move.");
        }

        if (!hasStartEdgePiece(bot)) {
            Move move = chooseOpeningMove(bot, allMoves);
            if (move != null) return strategy(move, "Opening move", "Claims the start edge near the centre.");
        }

        Move move = pickBestFrom(allMoves, bot);

        int oppPath = oppShortestPath(bot.next());
        String title = oppPath <= 4 ? "Blocking threat" : "Extend chain";
        String desc = oppPath <= 4
                ? "Opponent only needs " + oppPath + " more pieces to win — prioritising disruption."
                : "Extends the bot's chain toward its target edge.";
        return strategy(move, title, desc);
    }

    private Move pickBestFrom(List<Move> moves, GameState.Player bot) {
        GameState.Player opponent = bot.next();

        int myFrontierBefore = frontier(gameState, bot);
        int mySizeBefore = reachableSize(gameState, bot);

        int oppPathBefore = oppShortestPath(opponent);

        long disruptionWeight = Math.max(1L, (long)(boardSize - oppPathBefore));

        Move bestMove = null;
        long bestScore = Long.MIN_VALUE;

        for (Move move : moves) {
            place(move, bot);

            int myFrontierAfter = frontier(gameState, bot);
            int mySizeAfter = reachableSize(gameState, bot);
            int oppPathAfter = oppShortestPath(opponent);

            unplace(move);

            int frontierGain = myFrontierAfter - myFrontierBefore;
            int newCellsReached = mySizeAfter - mySizeBefore;
            int pathLengthening = (oppPathAfter == Integer.MAX_VALUE ? boardSize * 2 : oppPathAfter)
                    - (oppPathBefore == Integer.MAX_VALUE ? boardSize * 2 : oppPathBefore);
            int forward = forwardCoord(move, bot);

            long score = (long) frontierGain * 1_000_000_000L
                    + (long) newCellsReached * 1_000_000L
                    + disruptionWeight * (long) pathLengthening * 500_000L
                    + (long) forward;

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }

        return bestMove != null ? bestMove : moves.get(0);
    }

    private int oppShortestPath(GameState.Player player) {
        int total = boardSize * boardSize + (boardSize - 1) * (boardSize - 1);
        int[] distance = new int[total];
        Arrays.fill(distance, Integer.MAX_VALUE);
        Deque<Integer> deque = new ArrayDeque<>();

        if (player == GameState.Player.BLACK) {
            for (int col = 0; col < boardSize; col++) {
                if (gameState.getOctagonOwner(0, col) == player.next()) continue;
                int id = octagonId(0, col);
                int cost = gameState.getOctagonOwner(0, col) == player ? 0 : 1;
                if (cost < distance[id]) {
                    distance[id] = cost;
                    enqueue(deque, id, cost);
                }
            }
        } else {
            for (int row = 0; row < boardSize; row++) {
                if (gameState.getOctagonOwner(row, 0) == player.next()) continue;
                int id = octagonId(row, 0);
                int cost = gameState.getOctagonOwner(row, 0) == player ? 0 : 1;
                if (cost < distance[id]) {
                    distance[id] = cost;
                    enqueue(deque, id, cost);
                }
            }
        }

        while (!deque.isEmpty()) {
            int currentId = deque.pollFirst();
            int currentDistance = distance[currentId];

            if (currentId < boardSize * boardSize) {
                int row = currentId / boardSize;
                int col = currentId % boardSize;

                if (player == GameState.Player.BLACK && row == boardSize - 1) return currentDistance;
                if (player == GameState.Player.WHITE && col == boardSize - 1) return currentDistance;

                visitOctagonNeighbour(player, row - 1, col, currentDistance, distance, deque);
                visitOctagonNeighbour(player, row + 1, col, currentDistance, distance, deque);
                visitOctagonNeighbour(player, row, col - 1, currentDistance, distance, deque);
                visitOctagonNeighbour(player, row, col + 1, currentDistance, distance, deque);

                visitRhombusNeighbour(player, row - 1, col - 1, currentDistance, distance, deque);
                visitRhombusNeighbour(player, row - 1, col, currentDistance, distance, deque);
                visitRhombusNeighbour(player, row, col - 1, currentDistance, distance, deque);
                visitRhombusNeighbour(player, row, col, currentDistance, distance, deque);

            } else {
                int rhombusIndex = currentId - boardSize * boardSize;
                int row = rhombusIndex / (boardSize - 1);
                int col = rhombusIndex % (boardSize - 1);

                visitOctagonNeighbour(player, row, col, currentDistance, distance, deque);
                visitOctagonNeighbour(player, row, col + 1, currentDistance, distance, deque);
                visitOctagonNeighbour(player, row + 1, col, currentDistance, distance, deque);
                visitOctagonNeighbour(player, row + 1, col + 1, currentDistance, distance, deque);
            }
        }

        return Integer.MAX_VALUE;
    }

    private void visitOctagonNeighbour(GameState.Player player, int row, int col,
                                       int currentDistance, int[] distance, Deque<Integer> deque) {
        if (row < 0 || row >= boardSize || col < 0 || col >= boardSize) return;
        if (gameState.getOctagonOwner(row, col) == player.next()) return;
        int id = octagonId(row, col);
        int cost = gameState.getOctagonOwner(row, col) == player ? 0 : 1;
        int newDistance = currentDistance + cost;
        if (newDistance < distance[id]) {
            distance[id] = newDistance;
            enqueue(deque, id, cost);
        }
    }

    private void visitRhombusNeighbour(GameState.Player player, int row, int col,
                                       int currentDistance, int[] distance, Deque<Integer> deque) {
        if (row < 0 || row >= boardSize - 1 || col < 0 || col >= boardSize - 1) return;
        if (gameState.getRhombusOwner(row, col) == player.next()) return;
        int id = rhombusId(row, col);
        int cost = gameState.getRhombusOwner(row, col) == player ? 0 : 1;
        int newDistance = currentDistance + cost;
        if (newDistance < distance[id]) {
            distance[id] = newDistance;
            enqueue(deque, id, cost);
        }
    }

    private void enqueue(Deque<Integer> deque, int id, int cost) {
        if (cost == 0) deque.addFirst(id);
        else deque.addLast(id);
    }

    private int octagonId(int row, int col) {
        return row * boardSize + col;
    }

    private int rhombusId(int row, int col) {
        return boardSize * boardSize + row * (boardSize - 1) + col;
    }

    private int frontier(GameState state, GameState.Player player) {
        boolean[][] visitedOctagons = new boolean[boardSize][boardSize];
        boolean[][] visitedRhombuses = new boolean[boardSize - 1][boardSize - 1];
        List<int[]> stack = new ArrayList<>();

        if (player == GameState.Player.BLACK) {
            for (int col = 0; col < boardSize; col++)
                if (state.getOctagonOwner(0, col) == player) stack.add(new int[]{0, 0, col});
        } else {
            for (int row = 0; row < boardSize; row++)
                if (state.getOctagonOwner(row, 0) == player) stack.add(new int[]{0, row, 0});
        }

        int farthest = -1;

        while (!stack.isEmpty()) {
            int[] current = stack.remove(stack.size() - 1);
            int type = current[0];
            int row = current[1];
            int col = current[2];

            if (type == 0) {
                if (row < 0 || row >= boardSize || col < 0 || col >= boardSize) continue;
                if (visitedOctagons[row][col]) continue;
                if (state.getOctagonOwner(row, col) != player) continue;
                visitedOctagons[row][col] = true;
                farthest = Math.max(farthest, player == GameState.Player.BLACK ? row : col);
                stack.add(new int[]{0, row - 1, col});
                stack.add(new int[]{0, row + 1, col});
                stack.add(new int[]{0, row, col - 1});
                stack.add(new int[]{0, row, col + 1});
                stack.add(new int[]{1, row - 1, col - 1});
                stack.add(new int[]{1, row - 1, col});
                stack.add(new int[]{1, row, col - 1});
                stack.add(new int[]{1, row, col});
            } else {
                if (row < 0 || row >= boardSize - 1 || col < 0 || col >= boardSize - 1) continue;
                if (visitedRhombuses[row][col]) continue;
                if (state.getRhombusOwner(row, col) != player) continue;
                visitedRhombuses[row][col] = true;
                stack.add(new int[]{0, row, col});
                stack.add(new int[]{0, row, col + 1});
                stack.add(new int[]{0, row + 1, col});
                stack.add(new int[]{0, row + 1, col + 1});
            }
        }

        return farthest;
    }

    private int reachableSize(GameState state, GameState.Player player) {
        boolean[][] visitedOctagons = new boolean[boardSize][boardSize];
        boolean[][] visitedRhombuses = new boolean[boardSize - 1][boardSize - 1];
        List<int[]> stack = new ArrayList<>();

        if (player == GameState.Player.BLACK) {
            for (int col = 0; col < boardSize; col++)
                if (state.getOctagonOwner(0, col) == player) stack.add(new int[]{0, 0, col});
        } else {
            for (int row = 0; row < boardSize; row++)
                if (state.getOctagonOwner(row, 0) == player) stack.add(new int[]{0, row, 0});
        }

        int size = 0;

        while (!stack.isEmpty()) {
            int[] current = stack.remove(stack.size() - 1);
            int type = current[0];
            int row = current[1];
            int col = current[2];

            if (type == 0) {
                if (row < 0 || row >= boardSize || col < 0 || col >= boardSize) continue;
                if (visitedOctagons[row][col]) continue;
                if (state.getOctagonOwner(row, col) != player) continue;
                visitedOctagons[row][col] = true;
                size++;
                stack.add(new int[]{0, row - 1, col});
                stack.add(new int[]{0, row + 1, col});
                stack.add(new int[]{0, row, col - 1});
                stack.add(new int[]{0, row, col + 1});
                stack.add(new int[]{1, row - 1, col - 1});
                stack.add(new int[]{1, row - 1, col});
                stack.add(new int[]{1, row, col - 1});
                stack.add(new int[]{1, row, col});
            } else {
                if (row < 0 || row >= boardSize - 1 || col < 0 || col >= boardSize - 1) continue;
                if (visitedRhombuses[row][col]) continue;
                if (state.getRhombusOwner(row, col) != player) continue;
                visitedRhombuses[row][col] = true;
                size++;
                stack.add(new int[]{0, row, col});
                stack.add(new int[]{0, row, col + 1});
                stack.add(new int[]{0, row + 1, col});
                stack.add(new int[]{0, row + 1, col + 1});
            }
        }

        return size;
    }

    private boolean hasStartEdgePiece(GameState.Player player) {
        if (player == GameState.Player.BLACK) {
            for (int col = 0; col < boardSize; col++)
                if (gameState.getOctagonOwner(0, col) == player) return true;
        } else {
            for (int row = 0; row < boardSize; row++)
                if (gameState.getOctagonOwner(row, 0) == player) return true;
        }
        return false;
    }

    private Move chooseOpeningMove(GameState.Player player, List<Move> moves) {
        Move best = null;
        int bestDistance = Integer.MAX_VALUE;
        double centre = (boardSize - 1) / 2.0;

        for (Move move : moves) {
            if (move.cellType() != GameController.CellType.OCTAGON) continue;
            if (player == GameState.Player.BLACK && move.row() != 0) continue;
            if (player == GameState.Player.WHITE && move.col() != 0) continue;
            int coord = player == GameState.Player.BLACK ? move.col() : move.row();
            int distance = (int) Math.round(Math.abs(coord - centre));
            if (distance < bestDistance) {
                bestDistance = distance;
                best = move;
            }
        }
        return best;
    }

    private int forwardCoord(Move move, GameState.Player player) {
        if (player == GameState.Player.BLACK)
            return move.cellType() == GameController.CellType.RHOMBUS ? move.row() + 1 : move.row();
        else
            return move.cellType() == GameController.CellType.RHOMBUS ? move.col() + 1 : move.col();
    }

    private boolean winsAfterMove(GameState.Player player, Move move) {
        place(move, player);
        boolean wins = gameState.hasWinningChain(player);
        unplace(move);
        return wins;
    }

    private void place(Move move, GameState.Player player) {
        if (move.cellType() == GameController.CellType.OCTAGON)
            gameState.setOctagonOwner(move.row(), move.col(), player);
        else
            gameState.setRhombusOwner(move.row(), move.col(), player);
    }

    private void unplace(Move move) {
        if (move.cellType() == GameController.CellType.OCTAGON)
            gameState.setOctagonOwner(move.row(), move.col(), null);
        else
            gameState.setRhombusOwner(move.row(), move.col(), null);
    }

    private List<Move> allEmptyMoves() {
        List<Move> moves = new ArrayList<>();
        for (int row = 0; row < boardSize; row++)
            for (int col = 0; col < boardSize; col++)
                if (gameState.isOctagonEmpty(row, col))
                    moves.add(new Move(GameController.CellType.OCTAGON, row, col));
        for (int row = 0; row < boardSize - 1; row++)
            for (int col = 0; col < boardSize - 1; col++)
                if (gameState.isRhombusEmpty(row, col))
                    moves.add(new Move(GameController.CellType.RHOMBUS, row, col));
        return moves;
    }

    private BotStrategy strategy(Move move, String title, String description) {
        return new BotStrategy(move.cellType(), move.row(), move.col(), title, description);
    }
}