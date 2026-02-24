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

public class BoardController { // controller for fxml file

    @FXML private Pane boardPane;

    // connects to labels and dropdown in FXML file
    @FXML private Label titleLabel;
    @FXML private Label turnLabel;
    @FXML private ComboBox<GameMode> modeCombo;

    // board size (11 x 11 octagons)
    private static final int BOARD_SIZE = 11;

    // colours for octagons and rhombi
    private static final Color OCT_FILL = Color.web("#d67a00");
    private static final Color RHO_FILL = Color.web("#f0b000");
    private static final Color STROKE   = Color.web("#4a2a00");

    private static final double OUTER_PADDING = 60; // creates a padding between the board and the pane

    public enum GameMode { // list of game mode: human vs human or human vs bot
        HUMAN_VS_HUMAN("Human vs Human"),
        HUMAN_VS_BOT("Human vs Bot");

        private final String label; // stores the text (human vs human or human vs bot)
        GameMode(String label) { this.label = label; } // constructor for game mode
        @Override public String toString() { return label; } // return text rather than enum name (e.g. display "Human vs Human" rather than "HUMAN_VS_HUMAN")
    }

    private enum Player {
        BLACK, WHITE;

        Player next() {
            return this == BLACK ? WHITE : BLACK;
        }
    }

    private Player currentPlayer = Player.BLACK;

    private Player[][] octOwner = new Player[BOARD_SIZE][BOARD_SIZE];
    private Player[][] rhoOwner = new Player[BOARD_SIZE - 1][BOARD_SIZE - 1];

    private static final Color BLACK_OCTAGON = Color.web("Black");
    private static final Color WHITE_OCTAGON = Color.web("White");
    private static final Color BLACK_RHOMBUS = Color.web("Black");
    private static final Color WHITE_RHOMBUS = Color.web("White");

    private void updateTurnIndicator() {
        turnLabel.setText(currentPlayer + " to play");
    }

    @FXML
    private void initialize() { // javafx runs after fxml loads - used for setting up UI and drawing the board
        setupModeUI(); // fills dropdown and sets title

        Platform.runLater(this::redraw); // run redraw after JavaFX has finished loading fxml file

        // when the pane changes size, we redraw the board. when the observed size changes, we redraw the board using the new size
        boardPane.widthProperty().addListener((obs, oldWidth, newWidth) -> redraw());
        boardPane.heightProperty().addListener((obs, oldHeight, newHeight) -> redraw());
    }

    private void setupModeUI() {
        modeCombo.getItems().setAll(GameMode.HUMAN_VS_HUMAN, GameMode.HUMAN_VS_BOT); // puts different game modes into the dropdown menu
        modeCombo.setValue(GameMode.HUMAN_VS_HUMAN); // default game mode

        applyMode(modeCombo.getValue()); // updates title based on game mode choice

        modeCombo.valueProperty().addListener((obs, oldMode, newMode) -> { // whenever user picks new dropdown option, call applyMode again
            if (newMode != null) applyMode(newMode);
        });

        updateTurnIndicator();
    }

    private void applyMode(GameMode mode) {
        String prettyTitle = "Quax - " + mode; // creates title "Quax" and whatever game mode is chosen (default - human vs human)

        titleLabel.setText(prettyTitle); //updates the title in the display

        Platform.runLater(() -> { // sets window title using the same title displayed in the GUI
            if (boardPane.getScene() == null) return;
            if (boardPane.getScene().getWindow() == null) return;

            Stage stage = (Stage) boardPane.getScene().getWindow();
            stage.setTitle(prettyTitle);
        });
    }

    private void redraw() {
        boardPane.getChildren().clear(); // starts off by clearing the board

        double paneW = boardPane.getWidth(); // gets current available space
        double paneH = boardPane.getHeight();
        if (paneW <= 0 || paneH <= 0) return; // if there is no space, stop the program

        // calculates the amount of space we are allowed to draw in after the padding around the edges
        double usableW = paneW - 2 * OUTER_PADDING;
        double usableH = paneH - 2 * OUTER_PADDING;
        if (usableW <= 0 || usableH <= 0) return;

        // we want 11 tiles. each tile takes 2 x radius from center to center, so we divide space by 11 x 2 to get the biggest radius that fits
        double maxRadiusByWidth  = usableW / (BOARD_SIZE * 2.0);
        double maxRadiusByHeight = usableH / (BOARD_SIZE * 2.0);
        double tileRadius = Math.min(maxRadiusByWidth, maxRadiusByHeight); // use min so it fits in both width and height

        double stepBetweenCenters = 2 * tileRadius; // the center of the tiles are spaced by the diameter

        // 11 centers and between centers there are 10 gaps and add tile edges on each side
        double boardPixelWidth  = (BOARD_SIZE - 1) * stepBetweenCenters + 2 * tileRadius;
        double boardPixelHeight = (BOARD_SIZE - 1) * stepBetweenCenters + 2 * tileRadius;

        // centers the board by splitting whatever extra space remains in half on both sides
        double boardLeftEdge = (paneW - boardPixelWidth) / 2.0;
        double boardTopEdge  = (paneH - boardPixelHeight) / 2.0;

        // calculates the first tile center
        double startCenterX = boardLeftEdge + tileRadius;
        double startCenterY = boardTopEdge + tileRadius;

        // to make an octagon, we make a square and cut each corner equally
        double cornerCut = 0.41421356237 * tileRadius;
        double diamondRadius = tileRadius - cornerCut; // the size of the rhombus that fits in between the gaps of each octagon

        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) { // for loops iterate through the 11x11 positions
                double centerX = startCenterX + col * stepBetweenCenters; // moves right
                double centerY = startCenterY + row * stepBetweenCenters; // moves down

                Polygon oct = makeOctagon(centerX, centerY, tileRadius, cornerCut); // creates the 8 point shape at the center we have calculated
                oct.setId("Octagon_" + row + "_" + col); // assigns an ID for each octagon
                Player owner = octOwner[row][col];
                if(owner == null) {
                    oct.setFill((OCT_FILL));
                } else {
                    oct.setFill(owner ==  Player.BLACK ? BLACK_OCTAGON : WHITE_OCTAGON);
                }
                oct.setStroke(STROKE);
                oct.setOnMouseClicked(this::onCellClicked); // call the click method when an octagon is clicked

                boardPane.getChildren().add(oct); // adds each octagon to the screen
            }
        }

        for (int row = 0; row < BOARD_SIZE - 1; row++) {
            for (int col = 0; col < BOARD_SIZE - 1; col++) { // for loops iterate through the 11x11 positions
                double holeCenterX = startCenterX + col * stepBetweenCenters + tileRadius; // calculates the gap exactly halfway between four octagons
                double holeCenterY = startCenterY + row * stepBetweenCenters + tileRadius;

                Polygon rho = makeDiamond(holeCenterX, holeCenterY, diamondRadius); // creates the 4 point rhombus at the position calculated
                rho.setId("Rhombus_" + row + "_" + col); // assigns an ID for each rhombus
                Player owner = rhoOwner[row][col];
                if(owner == null) {
                    rho.setFill((RHO_FILL));
                } else {
                    rho.setFill(owner == Player.BLACK ? BLACK_RHOMBUS : WHITE_RHOMBUS);
                }
                rho.setStroke(STROKE);
                rho.setOnMouseClicked(this::onCellClicked); // call the click method when a rhombus is clicked

                boardPane.getChildren().add(rho); // adds each rhombus to the screen
            }
        }

        addEdgeLabels(startCenterX, startCenterY, stepBetweenCenters, tileRadius); // adds A - K and 1 - 11 around the board so it aligns with the centers
    }

    private void addEdgeLabels(double startCenterX, double startCenterY, double step, double r) {
        // Board edges (in pixels)
        double leftEdge = startCenterX - r; // finds the leftmost boundary of the first tile
        double rightEdge = startCenterX + (BOARD_SIZE - 1) * step + r; // go to last tile on the board to find right edge of the rightmost tile
        double topEdge = startCenterY - r; // finds the top edge of the board
        double bottomEdge = startCenterY + (BOARD_SIZE - 1) * step + r;

        double fontSize = Math.max(12, r * 0.55); // chooses the font size based on how big the board is
        Font font = Font.font(fontSize);

        double pad = Math.max(10, r * 0.6); // distance from the board edge

        // A–K top and bottom
        for (int col = 0; col < BOARD_SIZE; col++) { // iterates through each column
            char letter = (char) ('A' + col); // starts at character A
            double cx = startCenterX + col * step; // finds the center of the columns tile center and moves right each time

            Text top = new Text(String.valueOf(letter)); // creates the top letter label
            top.setFont(font); // sets font size
            top.setFill(Color.web("#f8fafc")); // sets text colour
            top.setX(cx - fontSize * 0.25); // moves slightly down so text is centered
            top.setY(topEdge - pad); // place above the board edge
            boardPane.getChildren().add(top); // add the letter to the pane

            Text bottom = new Text(String.valueOf(letter)); // creates the bottom letter label
            bottom.setFont(font); // sets font size
            bottom.setFill(Color.web("#f8fafc")); // sets font colour
            bottom.setX(cx - fontSize * 0.25); // moves slightly down so text is centered
            bottom.setY(bottomEdge + pad + fontSize * 0.35); // place below the board edge
            boardPane.getChildren().add(bottom); // add the letter to the pane
        }

        // 1–11 left and right
        for (int row = 0; row < BOARD_SIZE; row++) { // iterates through each row
            String num = String.valueOf(row + 1); // starts at 1
            double cy = startCenterY + row * step; // find center of each rows tile center and moves down each time

            Text left = new Text(num); // creates the left number label
            left.setFont(font); // sets font size
            left.setFill(Color.web("#f8fafc")); // sets font colour
            left.setX(leftEdge - pad - fontSize * 0.35); // centers text
            left.setY(cy + fontSize * 0.35); // places to the left of the board edge
            boardPane.getChildren().add(left); // adds the number to the pane

            Text right = new Text(num); // creates the right number label
            right.setFont(font); // sets the font size
            right.setFill(Color.web("#f8fafc")); // sets the font colour
            right.setX(rightEdge + pad); // centers the text
            right.setY(cy + fontSize * 0.35); // places to the right of the board edge
            boardPane.getChildren().add(right); // adds the number to the pane
        }
    }

    private void onCellClicked(MouseEvent e) { // click function so that when a shape gets clicked, its id is printed to the terminal/console
        if (!(e.getSource() instanceof Polygon clickedShape)) {
            return;
        }

        String id = clickedShape.getId();
        if (id == null) return;
        if(id.startsWith("Octagon_")) {
            String[] parts = id.split("_");
            int row = Integer.parseInt(parts[1]);
            int col = Integer.parseInt(parts[2]);

            if (octOwner[row][col] != null) return;

            octOwner[row][col] = currentPlayer;

            clickedShape.setFill(currentPlayer == Player.BLACK ? BLACK_OCTAGON : WHITE_OCTAGON);

            currentPlayer = currentPlayer.next();
            updateTurnIndicator();
            return;
        }

        if(id.startsWith("Rhombus_")) {
            String[] parts =  id.split("_");
            int row = Integer.parseInt(parts[1]);
            int col = Integer.parseInt(parts[2]);

            if (rhoOwner[row][col] != null) return;

            rhoOwner[row][col] = currentPlayer;
            clickedShape.setFill(currentPlayer == Player.BLACK ? BLACK_RHOMBUS : WHITE_RHOMBUS);
            currentPlayer = currentPlayer.next();
            updateTurnIndicator();
        }
    }

    private Polygon makeOctagon(double centerX, double centerY, double tileRadius, double cornerCut) { // creates an octagon made of 8 points
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

    private Polygon makeDiamond(double centerX, double centerY, double r) { // creates a rhombus made of 4 points
        return new Polygon(
                centerX,     centerY - r, // top point
                centerX + r, centerY, // right side point
                centerX,     centerY + r, // bottom point
                centerX - r, centerY // left side point
        );
    }
}
