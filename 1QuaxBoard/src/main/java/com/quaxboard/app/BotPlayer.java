package com.quaxboard.app;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

public class BotPlayer {
    private static final long FAR_REACH_WEIGHT = 1_000_000_000L;
    private static final long REACHABLE_SIZE_WEIGHT = 1_000_000L;
    private static final long OPPONENT_DISRUPTION_WEIGHT = 500_000L;

    public record BotStrategy(
            GameController.CellType cellType,
            int row,
            int col,
            String title,
            String description
    ) {}

    private record Move(
            GameController.CellType cellType,
            int row,
            int col
    ) {}

    private record ChainStats(
            int reachableSize,
            int farthestReach
    ) {}

    private record ScoredMove(
            Move move,
            int farthestReachGain,
            int newCellsReached,
            int opponentPathLengthening,
            int opponentPathBefore,
            int forwardProgress,
            long score
    ) {}

    private final GameState gameState;
    private final int boardSize;

    public BotPlayer(GameState gameState) {
        this.gameState = gameState;
        this.boardSize = gameState.getBoardSize();
    }

    public BotStrategy chooseStrategy(GameState.Player player) {
        return chooseBestMove(player);
    }

    public GameController.MoveResult makeMove(GameController gameController, BotStrategy strategy) {
        if (strategy == null) {
            return new GameController.MoveResult(false, null);
        }

        return gameController.place(strategy.cellType(), strategy.row(), strategy.col());
    }

    public GameController.MoveResult makeMove(GameController gameController) {
        BotStrategy chosenMove = chooseBestMove(gameController.currentPlayer());

        if (chosenMove == null) {
            return new GameController.MoveResult(false, null);
        }

        return makeMove(gameController, chosenMove);
    }

    private BotStrategy chooseBestMove(GameState.Player bot) {
        List<Move> allMoves = allEmptyMoves();

        if (allMoves.isEmpty()) {
            return null;
        }

        for (Move move : allMoves) {
            if (winsAfterMove(bot, move)) {
                return strategy(move, "Winning move", "The bot wins immediately.");
            }
        }

        List<Move> blockingMoves = new ArrayList<>();
        for (Move move : allMoves) {
            if (winsAfterMove(bot.next(), move)) {
                blockingMoves.add(move);
            }
        }

        if (!blockingMoves.isEmpty()) {
            ScoredMove scoredMove = pickBestFrom(blockingMoves, bot);
            return strategy(scoredMove.move(), "Blocking move", "Blocks the opponent's winning move.");
        }

        if (!hasStartEdgePiece(bot)) {
            Move move = chooseOpeningMove(bot, allMoves);

            if (move != null) {
                return strategy(move, "Opening move", "Claims the start edge near the centre.");
            }
        }

        ScoredMove scoredMove = pickBestFrom(allMoves, bot);
        return strategy(scoredMove.move(), strategyTitle(scoredMove), strategyDescription(scoredMove));
    }

    private ScoredMove pickBestFrom(List<Move> moves, GameState.Player bot) {
        GameState.Player opponent = bot.next();

        ChainStats myStatsBefore = traverseChain(bot);
        int opponentPathBefore = oppShortestPath(opponent);

        long disruptionWeight = Math.max(1L, (long) (boardSize - safePathLength(opponentPathBefore)));

        ScoredMove bestMove = null;

        for (Move move : moves) {
            place(move, bot);

            ChainStats myStatsAfter = traverseChain(bot);
            int opponentPathAfter = oppShortestPath(opponent);

            unplace(move);

            int farthestReachGain = myStatsAfter.farthestReach() - myStatsBefore.farthestReach();
            int newCellsReached = myStatsAfter.reachableSize() - myStatsBefore.reachableSize();

            int safeOpponentPathBefore = safePathLength(opponentPathBefore);
            int safeOpponentPathAfter = safePathLength(opponentPathAfter);
            int opponentPathLengthening = safeOpponentPathAfter - safeOpponentPathBefore;

            int forwardProgress = forwardProgress(move, bot);

            long score = (long) farthestReachGain * FAR_REACH_WEIGHT
                    + (long) newCellsReached * REACHABLE_SIZE_WEIGHT
                    + disruptionWeight * (long) opponentPathLengthening * OPPONENT_DISRUPTION_WEIGHT
                    + (long) forwardProgress;

            ScoredMove scoredMove = new ScoredMove(
                    move,
                    farthestReachGain,
                    newCellsReached,
                    opponentPathLengthening,
                    opponentPathBefore,
                    forwardProgress,
                    score
            );

            if (bestMove == null || scoredMove.score() > bestMove.score()) {
                bestMove = scoredMove;
            }
        }

        if (bestMove != null) {
            return bestMove;
        }

        return new ScoredMove(moves.get(0), 0, 0, 0, opponentPathBefore, 0, Long.MIN_VALUE);
    }

    private int safePathLength(int pathLength) {
        return pathLength == Integer.MAX_VALUE ? boardSize * 2 : pathLength;
    }

    private String strategyTitle(ScoredMove scoredMove) {
        if (scoredMove.opponentPathLengthening() > 0 && scoredMove.opponentPathBefore() <= 4) {
            return "Blocking threat";
        }

        if (scoredMove.farthestReachGain() > 0) {
            return "Extend chain";
        }

        if (scoredMove.newCellsReached() > 0) {
            return "Connect chain";
        }

        if (scoredMove.opponentPathLengthening() > 0) {
            return "Disrupting opponent";
        }

        return "Forward move";
    }

    private String strategyDescription(ScoredMove scoredMove) {
        if (scoredMove.opponentPathLengthening() > 0 && scoredMove.opponentPathBefore() <= 4) {
            return "Blocks a short opponent path to victory.";
        }

        if (scoredMove.farthestReachGain() > 0) {
            return "Extends the bot's chain closer to its target edge.";
        }

        if (scoredMove.newCellsReached() > 0) {
            return "Connects more of the bot's existing pieces into one chain.";
        }

        if (scoredMove.opponentPathLengthening() > 0) {
            return "Places a piece that makes the opponent's connection path longer.";
        }

        return "Chooses the strongest available move based on board position.";
    }

    private int oppShortestPath(GameState.Player player) {
        return new PathSearch(player).run();
    }

    private final class PathSearch {
        private final GameState.Player player;
        private final int[] distance;
        private final Deque<Integer> deque;

        PathSearch(GameState.Player player) {
            this.player = player;

            int total = boardSize * boardSize + (boardSize - 1) * (boardSize - 1);
            this.distance = new int[total];
            Arrays.fill(this.distance, Integer.MAX_VALUE);

            this.deque = new ArrayDeque<>();
            seedStartEdge();
        }

        int run() {
            while (!deque.isEmpty()) {
                int currentId = deque.pollFirst();
                int currentDistance = distance[currentId];

                if (currentId < boardSize * boardSize) {
                    int row = currentId / boardSize;
                    int col = currentId % boardSize;

                    if (isGoal(row, col)) {
                        return currentDistance;
                    }

                    expandOctagon(row, col, currentDistance);
                } else {
                    int rhombusIndex = currentId - boardSize * boardSize;
                    int row = rhombusIndex / (boardSize - 1);
                    int col = rhombusIndex % (boardSize - 1);

                    expandRhombus(row, col, currentDistance);
                }
            }

            return Integer.MAX_VALUE;
        }

        private void seedStartEdge() {
            if (player == GameState.Player.BLACK) {
                for (int col = 0; col < boardSize; col++) {
                    updateOctagonDistance(0, col, 0);
                }
            } else {
                for (int row = 0; row < boardSize; row++) {
                    updateOctagonDistance(row, 0, 0);
                }
            }
        }

        private boolean isGoal(int row, int col) {
            return (player == GameState.Player.BLACK && row == boardSize - 1)
                    || (player == GameState.Player.WHITE && col == boardSize - 1);
        }

        private void expandOctagon(int row, int col, int currentDistance) {
            updateOctagonDistance(row - 1, col, currentDistance);
            updateOctagonDistance(row + 1, col, currentDistance);
            updateOctagonDistance(row, col - 1, currentDistance);
            updateOctagonDistance(row, col + 1, currentDistance);

            updateRhombusDistance(row - 1, col - 1, currentDistance);
            updateRhombusDistance(row - 1, col, currentDistance);
            updateRhombusDistance(row, col - 1, currentDistance);
            updateRhombusDistance(row, col, currentDistance);
        }

        private void expandRhombus(int row, int col, int currentDistance) {
            updateOctagonDistance(row, col, currentDistance);
            updateOctagonDistance(row, col + 1, currentDistance);
            updateOctagonDistance(row + 1, col, currentDistance);
            updateOctagonDistance(row + 1, col + 1, currentDistance);
        }

        private void updateOctagonDistance(int row, int col, int currentDistance) {
            if (row < 0 || row >= boardSize || col < 0 || col >= boardSize) {
                return;
            }

            if (gameState.getOctagonOwner(row, col) == player.next()) {
                return;
            }

            int id = octagonId(row, col);
            int cost = gameState.getOctagonOwner(row, col) == player ? 0 : 1;
            int newDistance = currentDistance + cost;

            if (newDistance < distance[id]) {
                distance[id] = newDistance;
                enqueue(id, cost);
            }
        }

        private void updateRhombusDistance(int row, int col, int currentDistance) {
            if (row < 0 || row >= boardSize - 1 || col < 0 || col >= boardSize - 1) {
                return;
            }

            if (gameState.getRhombusOwner(row, col) == player.next()) {
                return;
            }

            int id = rhombusId(row, col);
            int cost = gameState.getRhombusOwner(row, col) == player ? 0 : 1;
            int newDistance = currentDistance + cost;

            if (newDistance < distance[id]) {
                distance[id] = newDistance;
                enqueue(id, cost);
            }
        }

        private void enqueue(int id, int cost) {
            if (cost == 0) {
                deque.addFirst(id);
            } else {
                deque.addLast(id);
            }
        }
    }

    private int octagonId(int row, int col) {
        return row * boardSize + col;
    }

    private int rhombusId(int row, int col) {
        return boardSize * boardSize + row * (boardSize - 1) + col;
    }

    private ChainStats traverseChain(GameState.Player player) {
        boolean[][] visitedOctagons = new boolean[boardSize][boardSize];
        boolean[][] visitedRhombuses = new boolean[boardSize - 1][boardSize - 1];
        List<int[]> stack = new ArrayList<>();
        fromStartEdge(player, stack);

        int size = 0;
        int farthest = -1;

        while (!stack.isEmpty()) {
            int[] current = stack.remove(stack.size() - 1);
            int type = current[0];
            int row = current[1];
            int col = current[2];

            if (type == 0) {
                if (row < 0 || row >= boardSize || col < 0 || col >= boardSize) {
                    continue;
                }

                if (visitedOctagons[row][col]) {
                    continue;
                }

                if (gameState.getOctagonOwner(row, col) != player) {
                    continue;
                }

                visitedOctagons[row][col] = true;
                size++;
                farthest = Math.max(farthest, player == GameState.Player.BLACK ? row : col);
                pushOctagonNeighbours(stack, row, col);
            } else {
                if (row < 0 || row >= boardSize - 1 || col < 0 || col >= boardSize - 1) {
                    continue;
                }

                if (visitedRhombuses[row][col]) {
                    continue;
                }

                if (gameState.getRhombusOwner(row, col) != player) {
                    continue;
                }

                visitedRhombuses[row][col] = true;
                size++;
                pushRhombusNeighbours(stack, row, col);
            }
        }

        return new ChainStats(size, farthest);
    }

    private void fromStartEdge(GameState.Player player, List<int[]> stack) {
        if (player == GameState.Player.BLACK) {
            for (int col = 0; col < boardSize; col++) {
                if (gameState.getOctagonOwner(0, col) == player) {
                    stack.add(new int[]{0, 0, col});
                }
            }
        } else {
            for (int row = 0; row < boardSize; row++) {
                if (gameState.getOctagonOwner(row, 0) == player) {
                    stack.add(new int[]{0, row, 0});
                }
            }
        }
    }

    private void pushOctagonNeighbours(List<int[]> stack, int row, int col) {
        stack.add(new int[]{0, row - 1, col});
        stack.add(new int[]{0, row + 1, col});
        stack.add(new int[]{0, row, col - 1});
        stack.add(new int[]{0, row, col + 1});

        stack.add(new int[]{1, row - 1, col - 1});
        stack.add(new int[]{1, row - 1, col});
        stack.add(new int[]{1, row, col - 1});
        stack.add(new int[]{1, row, col});
    }

    private void pushRhombusNeighbours(List<int[]> stack, int row, int col) {
        stack.add(new int[]{0, row, col});
        stack.add(new int[]{0, row, col + 1});
        stack.add(new int[]{0, row + 1, col});
        stack.add(new int[]{0, row + 1, col + 1});
    }

    private boolean hasStartEdgePiece(GameState.Player player) {
        if (player == GameState.Player.BLACK) {
            for (int col = 0; col < boardSize; col++) {
                if (gameState.getOctagonOwner(0, col) == player) {
                    return true;
                }
            }
        } else {
            for (int row = 0; row < boardSize; row++) {
                if (gameState.getOctagonOwner(row, 0) == player) {
                    return true;
                }
            }
        }

        return false;
    }

    private Move chooseOpeningMove(GameState.Player player, List<Move> moves) {
        Move best = null;
        int bestDistance = Integer.MAX_VALUE;
        double centre = (boardSize - 1) / 2.0;

        for (Move move : moves) {
            if (move.cellType() != GameController.CellType.OCTAGON) {
                continue;
            }

            if (player == GameState.Player.BLACK && move.row() != 0) {
                continue;
            }

            if (player == GameState.Player.WHITE && move.col() != 0) {
                continue;
            }

            int coord = player == GameState.Player.BLACK ? move.col() : move.row();
            int distance = (int) Math.round(Math.abs(coord - centre));

            if (distance < bestDistance) {
                bestDistance = distance;
                best = move;
            }
        }

        return best;
    }

    private int forwardProgress(Move move, GameState.Player player) {
        if (player == GameState.Player.BLACK) {
            return move.cellType() == GameController.CellType.RHOMBUS
                    ? move.row() + 1
                    : move.row();
        }

        return move.cellType() == GameController.CellType.RHOMBUS
                ? move.col() + 1
                : move.col();
    }

    private boolean winsAfterMove(GameState.Player player, Move move) {
        place(move, player);
        boolean wins = gameState.hasWinningChain(player);
        unplace(move);
        return wins;
    }

    private void place(Move move, GameState.Player player) {
        if (move.cellType() == GameController.CellType.OCTAGON) {
            gameState.setOctagonOwner(move.row(), move.col(), player);
        } else {
            gameState.setRhombusOwner(move.row(), move.col(), player);
        }
    }

    private void unplace(Move move) {
        if (move.cellType() == GameController.CellType.OCTAGON) {
            gameState.setOctagonOwner(move.row(), move.col(), null);
        } else {
            gameState.setRhombusOwner(move.row(), move.col(), null);
        }
    }

    private List<Move> allEmptyMoves() {
        List<Move> moves = new ArrayList<>();

        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                if (gameState.isOctagonEmpty(row, col)) {
                    moves.add(new Move(GameController.CellType.OCTAGON, row, col));
                }
            }
        }

        for (int row = 0; row < boardSize - 1; row++) {
            for (int col = 0; col < boardSize - 1; col++) {
                if (gameState.isRhombusEmpty(row, col)) {
                    moves.add(new Move(GameController.CellType.RHOMBUS, row, col));
                }
            }
        }

        return moves;
    }

    private BotStrategy strategy(Move move, String title, String description) {
        return new BotStrategy(move.cellType(), move.row(), move.col(), title, description);
    }
}