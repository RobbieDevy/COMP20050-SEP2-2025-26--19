package com.quaxboard.app;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.scene.control.Button;

public class BoardController {
    private static final int BOARD_SIZE = 11;

    private static final Color EMPTY_OCTAGON_FILL = Color.web("#d67a00");
    private static final Color EMPTY_RHOMBUS_FILL = Color.web("#f0b000");
    private static final Color PIECE_BLACK_FILL = Color.BLACK;
    private static final Color PIECE_WHITE_FILL = Color.WHITE;
    private static final Color STROKE = Color.web("#4a2a00");
    private static final Color LABEL_COLOR = Color.web("#f8fafc");

    private static final double OUTER_PADDING = 60;
    private static final double OCTAGON_CORNER_CUT_RATIO = 0.41421356237;
    private static final double MIN_LABEL_FONT_SIZE = 12;
    private static final double LABEL_FONT_SCALE = 0.55;
    private static final double MIN_LABEL_PADDING = 10;
    private static final double LABEL_PADDING_SCALE = 0.6;
    private static final double HORIZONTAL_LABEL_OFFSET_SCALE = 0.25;
    private static final double VERTICAL_LABEL_OFFSET_SCALE = 0.35;

    public enum GameMode {
        HUMAN_VS_HUMAN("Human vs Human"),
        HUMAN_VS_BOT("Human vs Bot");

        private final String label;
        GameMode(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    private record BoardLayout(
            double startCenterX,
            double startCenterY,
            double stepBetweenCenters,
            double tileRadius,
            double cornerCut,
            double diamondRadius
    ) {}

    @FXML private Pane boardPane;
    @FXML private Label titleLabel;
    @FXML private Label turnLabel;
    @FXML private ComboBox<GameMode> modeCombo;
    @FXML private Button pieRuleButton;

    private final GameState gameState = new GameState(BOARD_SIZE);
    private final GameController gameController = new GameController(gameState);

    private void updateTurnIndicator() {
        GameState.Player colour = gameController.currentPlayer();
        GameState.HumanPlayer human = gameState.getPlayerForColour(colour);
        String playerText = (human == GameState.HumanPlayer.PLAYER_1) ? "Player 1" : "Player 2";
        turnLabel.setText(colour + " (" + playerText + ") to play");
    }

    @FXML
    private void initialize() { // javafx runs after fxml loads - used for setting up UI and drawing the board
        setupModeUI();
        updatePieRuleButton();
        Platform.runLater(this::redraw);
        resizer();
    }

    private void setupModeUI() {
        modeCombo.getItems().setAll(GameMode.HUMAN_VS_HUMAN, GameMode.HUMAN_VS_BOT);
        modeCombo.setValue(GameMode.HUMAN_VS_HUMAN);
        applyMode(modeCombo.getValue());

        modeCombo.valueProperty().addListener((obs, oldMode, newMode) -> { // whenever user picks new dropdown option, call applyMode again
            if (newMode != null) applyMode(newMode);
        });

        updateTurnIndicator();
        updatePieRuleButton();
    }

    private void resizer() {
        boardPane.widthProperty().addListener((obs, oldWidth, newWidth) -> redraw());
        boardPane.heightProperty().addListener((obs, oldHeight, newHeight) -> redraw());
    }

    private void applyMode(GameMode mode) {
        String prettyTitle = "Quax - " + mode;
        titleLabel.setText(prettyTitle);

        Platform.runLater(() -> { // sets window title using the same title displayed in the GUI
            if (boardPane.getScene() == null) return;
            if (boardPane.getScene().getWindow() == null) return;

            Stage stage = (Stage) boardPane.getScene().getWindow();
            stage.setTitle(prettyTitle);
        });
    }

    private void redraw() {
        boardPane.getChildren().clear();

        BoardLayout layout = calculateBoardLayout();
        if (layout == null) {
            return;
        }

        drawOctagons(layout);
        drawRhombuses(layout);
        addColumnLabels(layout);
        addRowLabels(layout);
    }

    private BoardLayout calculateBoardLayout() {
        double paneWidth = boardPane.getWidth();
        double paneHeight = boardPane.getHeight();

        if (paneWidth <= 0 || paneHeight <= 0) {
            return null;
        }

        double usableWidth = paneWidth - 2 * OUTER_PADDING;
        double usableHeight = paneHeight - 2 * OUTER_PADDING;
        if (usableWidth <= 0 || usableHeight <= 0) {
            return null;
        }

        double maxRadiusByWidth = usableWidth / (BOARD_SIZE * 2.0);
        double maxRadiusByHeight = usableHeight / (BOARD_SIZE * 2.0);
        double tileRadius = Math.min(maxRadiusByWidth, maxRadiusByHeight);

        double stepBetweenCenters = 2 * tileRadius;
        double boardPixelWidth = (BOARD_SIZE - 1) * stepBetweenCenters + 2 * tileRadius;
        double boardPixelHeight = (BOARD_SIZE - 1) * stepBetweenCenters + 2 * tileRadius;

        double boardLeftEdge = (paneWidth - boardPixelWidth) / 2.0;
        double boardTopEdge = (paneHeight - boardPixelHeight) / 2.0;

        double startCenterX = boardLeftEdge + tileRadius;
        double startCenterY = boardTopEdge + tileRadius;

        double cornerCut = OCTAGON_CORNER_CUT_RATIO * tileRadius;
        double diamondRadius = tileRadius - cornerCut;

        return new BoardLayout(
                startCenterX,
                startCenterY,
                stepBetweenCenters,
                tileRadius,
                cornerCut,
                diamondRadius
        );
    }

    private void drawOctagons(BoardLayout layout) {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                double centerX = layout.startCenterX() + col * layout.stepBetweenCenters();
                double centerY = layout.startCenterY() + row * layout.stepBetweenCenters();

                Polygon octagon = makeOctagon(centerX, centerY, layout.tileRadius(), layout.cornerCut());
                octagon.setId("Octagon_" + row + "_" + col);
                GameState.Player owner = gameState.getOctagonOwner(row, col);
                octagon.setFill(owner == null ? EMPTY_OCTAGON_FILL : owner == GameState.Player.BLACK ? PIECE_BLACK_FILL : PIECE_WHITE_FILL);
                octagon.setStroke(STROKE);
                octagon.setOnMouseClicked(this::onCellClicked);

                boardPane.getChildren().add(octagon);
            }
        }
    }

    private void drawRhombuses(BoardLayout layout) {
        for (int row = 0; row < BOARD_SIZE - 1; row++) {
            for (int col = 0; col < BOARD_SIZE - 1; col++) {
                double centerX = layout.startCenterX() + col * layout.stepBetweenCenters() + layout.tileRadius();
                double centerY = layout.startCenterY() + row * layout.stepBetweenCenters() + layout.tileRadius();

                Polygon rhombus = makeDiamond(centerX, centerY, layout.diamondRadius());
                rhombus.setId("Rhombus_" + row + "_" + col);
                GameState.Player owner = gameState.getRhombusOwner(row, col);
                rhombus.setFill(owner == null ? EMPTY_RHOMBUS_FILL : owner == GameState.Player.BLACK ? PIECE_BLACK_FILL : PIECE_WHITE_FILL);
                rhombus.setStroke(STROKE);
                rhombus.setOnMouseClicked(this::onCellClicked);

                boardPane.getChildren().add(rhombus);
            }
        }
    }

    private void addColumnLabels(BoardLayout layout) {
        double fontSize = Math.max(MIN_LABEL_FONT_SIZE, layout.tileRadius() * LABEL_FONT_SCALE);
        double labelPadding = Math.max(MIN_LABEL_PADDING, layout.tileRadius() * LABEL_PADDING_SCALE);
        Font font = Font.font(fontSize);

        double topEdge = layout.startCenterY() - layout.tileRadius();
        double bottomEdge = layout.startCenterY() + (BOARD_SIZE - 1) * layout.stepBetweenCenters() + layout.tileRadius();

        for (int col = 0; col < BOARD_SIZE; col++) {
            char letter = (char) ('A' + col);
            double centerX = layout.startCenterX() + col * layout.stepBetweenCenters();

            Text topLabel = new Text(String.valueOf(letter));
            topLabel.setFont(font);
            topLabel.setFill(LABEL_COLOR);
            topLabel.setX(centerX - fontSize * HORIZONTAL_LABEL_OFFSET_SCALE);
            topLabel.setY(topEdge - labelPadding);
            boardPane.getChildren().add(topLabel);

            Text bottomLabel = new Text(String.valueOf(letter));
            bottomLabel.setFont(font);
            bottomLabel.setFill(LABEL_COLOR);
            bottomLabel.setX(centerX - fontSize * HORIZONTAL_LABEL_OFFSET_SCALE);
            bottomLabel.setY(bottomEdge + labelPadding + fontSize * VERTICAL_LABEL_OFFSET_SCALE);
            boardPane.getChildren().add(bottomLabel);
        }
    }

    private void addRowLabels(BoardLayout layout) {
        double fontSize = Math.max(MIN_LABEL_FONT_SIZE, layout.tileRadius() * LABEL_FONT_SCALE);
        double labelPadding = Math.max(MIN_LABEL_PADDING, layout.tileRadius() * LABEL_PADDING_SCALE);
        Font font = Font.font(fontSize);

        double leftEdge = layout.startCenterX() - layout.tileRadius();
        double rightEdge = layout.startCenterX() + (BOARD_SIZE - 1) * layout.stepBetweenCenters() + layout.tileRadius();

        for (int row = 0; row < BOARD_SIZE; row++) {
            String number = String.valueOf(row + 1);
            double centerY = layout.startCenterY() + row * layout.stepBetweenCenters();

            Text leftLabel = new Text(number);
            leftLabel.setFont(font);
            leftLabel.setFill(LABEL_COLOR);
            leftLabel.setX(leftEdge - labelPadding - fontSize * VERTICAL_LABEL_OFFSET_SCALE);
            leftLabel.setY(centerY + fontSize * VERTICAL_LABEL_OFFSET_SCALE);
            boardPane.getChildren().add(leftLabel);

            Text rightLabel = new Text(number);
            rightLabel.setFont(font);
            rightLabel.setFill(LABEL_COLOR);
            rightLabel.setX(rightEdge + labelPadding);
            rightLabel.setY(centerY + fontSize * VERTICAL_LABEL_OFFSET_SCALE);
            boardPane.getChildren().add(rightLabel);
        }
    }

    private void onCellClicked(MouseEvent e) {
        if (!(e.getSource() instanceof Polygon clickedShape)) { // ensures that the thing clicked is a polygon (octagon or rhombus)
            return;
        }

        String id = clickedShape.getId();
        if (id == null) return;

        if(id.startsWith("Octagon_")) {
            String[] parts = id.split("_"); // we split the id by the underscores
            int row = Integer.parseInt(parts[1]); // grabs the second piece as row number
            int col = Integer.parseInt(parts[2]); // grabs the third piece as column number

           var result = gameController.place(GameController.CellType.OCTAGON, row, col);

            if(!result.success()) return;
            redraw();
            updateTurnIndicator();
            updatePieRuleButton();
            return;
        }

        if(id.startsWith("Rhombus_")) {
            String[] parts =  id.split("_");
            int row = Integer.parseInt(parts[1]);
            int col = Integer.parseInt(parts[2]);

            var result = gameController.place(GameController.CellType.RHOMBUS, row, col);
            if (!result.success()) return;
            redraw();
            updateTurnIndicator();
            updatePieRuleButton();
        }
    }

    private Polygon makeOctagon(double centerX, double centerY, double tileRadius, double cornerCut) {
        return new Polygon(
                centerX - cornerCut, centerY - tileRadius, // top left point
                centerX + cornerCut, centerY - tileRadius, // top right point
                centerX + tileRadius, centerY - cornerCut, // right top point
                centerX + tileRadius, centerY + cornerCut, // right bottom point
                centerX + cornerCut, centerY + tileRadius, // bottom right point
                centerX - cornerCut, centerY + tileRadius, // bottom left point
                centerX - tileRadius, centerY + cornerCut, // left bottom
                centerX - tileRadius, centerY - cornerCut // left top point
                // essentially, starting from the center, we create the octagon starting from the top left point, moving around in a clockwise direction
        );
    }

    private Polygon makeDiamond(double centerX, double centerY, double diamondRadius) {
        return new Polygon(
                centerX,     centerY - diamondRadius, // top point
                centerX + diamondRadius, centerY, // right side point
                centerX,     centerY + diamondRadius, // bottom point
                centerX - diamondRadius, centerY // left side point
        );
    }

    private void updatePieRuleButton() {
        boolean canUse = gameController.canUsePieRule();
        pieRuleButton.setVisible(canUse);
        pieRuleButton.setManaged(canUse);
    }

    @FXML
    private void onPieRuleClicked() {
        var result = gameController.activatePieRule();
        if (!result.success()) return;

        updateTurnIndicator();
        updatePieRuleButton();
        redraw();
    }
}
