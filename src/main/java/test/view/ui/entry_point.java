package test.view.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class entry_point extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/wildlife/view/ui/fxml/main_ui.fxml"));
        Parent root = loader.load();
        Screen screen = Screen.getPrimary();
        Rectangle2D bounds = screen.getVisualBounds();

        primaryStage.setX(bounds.getMinX());
        primaryStage.setY(bounds.getMinY());
        primaryStage.setWidth(bounds.getWidth());
        primaryStage.setHeight(bounds.getHeight());

        Scene scene = new Scene(root);
        scene.getStylesheets().add(String.valueOf(getClass().getResource("/wildlife/view/ui/css/style.css")));

        primaryStage.setTitle("Wild-life eco simulation");
        Image logo = new Image(getClass().getResourceAsStream("/wildlife/view/ui/assets/images/App_Icon.svg"));
        primaryStage.getIcons().add(logo);

        primaryStage.setScene(scene);
        primaryStage.show();

    }

    public static void main(String[] args) {
        launch(args);
    }
}
