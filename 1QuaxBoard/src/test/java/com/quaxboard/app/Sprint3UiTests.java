package com.quaxboard.app;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import static org.junit.jupiter.api.Assertions.*;

public class Sprint3UiTests extends ApplicationTest {

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
    void pieRuleButtonShowsAfterFirstMove() {
        Button pieRuleButton = lookup("#pieRuleButton").queryAs(Button.class);

        assertFalse(pieRuleButton.isVisible());

        clickOn("#Octagon_0_0");

        assertTrue(pieRuleButton.isVisible());
        assertTrue(pieRuleButton.isManaged());
    }

    @Test
    void pieRuleButtonDisappearsAfterUse() {
        Button pieRuleButton = lookup("#pieRuleButton").queryAs(Button.class);

        clickOn("#Octagon_0_0");
        assertTrue(pieRuleButton.isVisible());

        clickOn("#pieRuleButton");

        assertFalse(pieRuleButton.isVisible());
        assertFalse(pieRuleButton.isManaged());
    }

    @Test
    void turnIndicatorUpdatesAfterPieRule() {
        Label turnLabel = lookup("#turnLabel").queryAs(Label.class);

        clickOn("#Octagon_0_0");
        assertEquals("WHITE (Player 2) to play", turnLabel.getText());

        clickOn("#pieRuleButton");
        assertEquals("WHITE (Player 1) to play", turnLabel.getText());
    }
}