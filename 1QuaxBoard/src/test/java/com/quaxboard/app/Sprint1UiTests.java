package com.quaxboard.app;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Polygon;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class Sprint1UiTests extends ApplicationTest {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/quaxboard/app/board-view.fxml")
        );
        Parent root = loader.load();

        stage.setScene(new Scene(root, 917, 665));
        stage.show();
    }

    // board test
    @Test
    void SR1_appLaunchesAndBoardPaneExists() {
        Pane boardPane = lookup("#boardPane").queryAs(Pane.class);
        assertNotNull(boardPane);
    }

    // correct titles based on game mode tests
    @Test
    void SR1_feature2_titleChangesWhenModeChanges() {
        Label titleLabel = lookup("#titleLabel").queryAs(Label.class);
        ComboBox<?> modeCombo = lookup("#modeCombo").queryAs(ComboBox.class);

        // default should be Human vs Human
        assertTrue(titleLabel.getText().contains("Quax"));
        assertTrue(titleLabel.getText().contains("Human vs Human"));

        // when the game mode is changed the title should change
        interact(() -> modeCombo.getSelectionModel().select(1));
        assertTrue(titleLabel.getText().contains("Human vs Bot"));
    }

    //test to ensure board has 11x11 layout of octagons and rhombi
    @Test
    void SR1_feature3_boardHasCorrectNumberOfCells() {
        Pane boardPane = lookup("#boardPane").queryAs(Pane.class);

        Set<Polygon> polygons = boardPane.getChildren()
                .stream()
                .filter(n -> n instanceof Polygon)
                .map(n -> (Polygon) n)
                .collect(Collectors.toSet());

        long octCount = polygons.stream()
                .filter(p -> p.getId() != null && p.getId().startsWith("Octagon_"))
                .count();

        long rhoCount = polygons.stream()
                .filter(p -> p.getId() != null && p.getId().startsWith("Rhombus_"))
                .count();

        assertEquals(121, octCount, "Should be 11x11 = 121 octagons");
        assertEquals(100, rhoCount, "Should be 10x10 = 100 rhombi");
    }

    // test to ensure A - K and 1 - 11 appears on the board
    @Test
    void SR1_feature4_edgeLabelsExist() {
        Pane boardPane = lookup("#boardPane").queryAs(Pane.class);

        long textCount = boardPane.getChildren()
                .stream()
                .filter(n -> n instanceof javafx.scene.text.Text)
                .count();

        assertEquals(44, textCount, "Should be 44 labels total (A-K top/bottom + 1-11 left/right)");
    }

    // test to ensure correct user turn is displayed (for now just black to play)
    @Test
    void SR1_turnIndicatorShowsBlackToPlay() {
        Label turnLabel = lookup("#turnLabel").queryAs(Label.class);
        assertEquals("BLACK (Player 1) to play", turnLabel.getText());
    }
}