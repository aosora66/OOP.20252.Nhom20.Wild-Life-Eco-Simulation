package wildlife.view;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import wildlife.view.renderer.Renderer;
import wildlife.view.ui.UIEventController;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;

/*
 this class init a javafx stage, in which we will put UI layer and a canvas layer behind it for lwjgl embedding (renderer).
 */
public class ApplicationFrame extends Application {
    private static volatile Renderer renderer;
    private static final java.util.concurrent.CountDownLatch latch = new CountDownLatch(1);

    public static Renderer getRendererInstance(){
        try{
            latch.await();
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
        }
        return renderer;
    }
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/wildlife/view/ui/fxml/main_ui.fxml"));
        Parent root = loader.load();

        renderer = loader.<UIEventController>getController().getRenderer();
        latch.countDown();

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
}
