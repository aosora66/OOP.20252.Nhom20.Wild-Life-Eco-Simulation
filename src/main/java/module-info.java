module wildlife.simulation {
    requires javafx.controls;
    requires javafx.fxml;
    exports wildlife.core;
    exports wildlife.model.brain;
    exports wildlife.view;
    opens wildlife.view.ui to javafx.fxml;
    opens wildlife.view to javafx.graphics;
    opens wildlife.view.ui.fxml;
    opens wildlife.view.ui.css;
    opens wildlife.view.ui.assets.images;
    opens config;
}