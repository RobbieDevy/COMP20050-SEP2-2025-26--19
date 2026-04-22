package com.quaxboard.app;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import static org.junit.jupiter.api.Assertions.*;

public class Sprint4UiTests extends ApplicationTest {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/quaxboard/app/board-view.fxml")
        );
        Parent root = loader.load();

        stage.setScene(new Scene(root, 917, 665));
        stage.show();
    }

    @Test
    void botAutomaticallyMoves() throws Exception {
        Pane boardPane = lookup("#boardPane").queryAs(Pane.class);
        Label turnLabel = lookup("#turnLabel").queryAs(Label.class);

        assertEquals(0, countPlayedPolygons(boardPane));

        clickOn("#Octagon_5_5");
        Thread.sleep(700);

        assertEquals(2, countPlayedPolygons(boardPane));
        assertEquals("BLACK (Human) to play", turnLabel.getText());
    }

    @Test
    void showStrategyDisplaysExplanation() {
        Pane boardPane = lookup("#boardPane").queryAs(Pane.class);
        Button strategyButton = lookup("#strategyButton").queryAs(Button.class);
        Label strategyLabel = lookup("#strategyLabel").queryAs(Label.class);

        long polygonCountBefore = countAllPolygons(boardPane);

        clickOn("#strategyButton");

        long polygonCountAfter = countAllPolygons(boardPane);

        assertEquals("Hide Strategy", strategyButton.getText());
        assertFalse(strategyLabel.getText().isBlank());
        assertEquals(polygonCountBefore + 1, polygonCountAfter);
    }

    @Test
    void hideStrategyTurnsOffExplanation() {
        Pane boardPane = lookup("#boardPane").queryAs(Pane.class);
        Button strategyButton = lookup("#strategyButton").queryAs(Button.class);
        Label strategyLabel = lookup("#strategyLabel").queryAs(Label.class);

        clickOn("#strategyButton");
        clickOn("#strategyButton");

        assertEquals("Show Strategy", strategyButton.getText());
        assertEquals("", strategyLabel.getText());
        assertEquals(221, countAllPolygons(boardPane));
    }

    private long countPlayedPolygons(Pane boardPane) {
        long count = 0;

        for (var node : boardPane.getChildren()) {
            if (node instanceof Polygon polygon) {
                if (Color.BLACK.equals(polygon.getFill()) || Color.WHITE.equals(polygon.getFill())) {
                    count++;
                }
            }
        }

        return count;
    }

    private long countAllPolygons(Pane boardPane) {
        long count = 0;

        for (var node : boardPane.getChildren()) {
            if (node instanceof Polygon) {
                count++;
            }
        }

        return count;
    }
}
