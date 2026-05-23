module wildlife.simulation {
    // Declare dependencies on required JavaFX modules
    requires javafx.controls;
    requires javafx.fxml;

    // Export packages containing public APIs that other modules might need to access.
    // This includes your main application entry point and model/view components.
    exports wildlife.core;        // Contains the initial Main class
    exports wildlife.model.brain; // Contains strategy interfaces and implementations
    exports wildlife.view;        // Contains the JavaFX Application subclass (entry_point) and Mobs

    // Open packages for reflective access by the JavaFX runtime and FXMLLoader.
    // The package containing your FXML controller (MainController) needs to be opened to javafx.fxml.
    opens wildlife.view.ui to javafx.fxml;

    // The package containing your JavaFX Application subclass (entry_point) needs to be opened to javafx.graphics
    // for the JavaFX runtime to reflectively instantiate it.
    opens wildlife.view to javafx.graphics;

    // Open packages containing resources to ensure they are accessible at runtime via Class::getResource.
    // While getClass().getResource() generally works, explicitly opening resource packages
    // helps prevent issues, especially with jlink/jpackage.
    opens wildlife.view.ui.fxml;         // Contains FXML files like main_ui.fxml
    opens wildlife.view.ui.css;          // Contains CSS files like style.css
    opens wildlife.view.ui.assets.images; // Contains image assets like Fox.png
//    opens wildlife.view.ui.assets.fonts;  // Contains font files
    opens config;                         // Contains setting.properties
}
