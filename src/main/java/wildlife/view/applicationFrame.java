package wildlife.view;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;

public class applicationFrame extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/wildlife/view/ui/fxml/main_ui.fxml"));
        Parent root = loader.load();
        primaryStage.setMaximized(true);
        primaryStage.setResizable(false);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(String.valueOf(getClass().getResource("/wildlife/view/ui/css/style.css")));

        primaryStage.setTitle("Wild-life eco simulation");
        Image logo = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/wildlife/view/ui/assets/images/Fox.png")));
        primaryStage.getIcons().add(logo);

        primaryStage.setScene(scene);
        primaryStage.show();

    }

    public void show(String[] args){
        launch(args);
    }
}
