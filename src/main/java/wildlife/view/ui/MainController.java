package wildlife.view.ui;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.embed.swing.SwingNode;
import javax.swing.SwingUtilities;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.backends.lwjgl.LwjglAWTCanvas;
import wildlife.view.Mobs;

public class MainController {

    private static boolean isBasic = true;
    public HBox entityPanel;
    
    @FXML
    public AnchorPane libgdxContainer;
    
    //View Button
        private Timeline timeline;
        //timeline for animation handling
        @FXML
        public Label hiddenLabel_view;
        @FXML
        public HBox viewButton;
        @FXML
        public void viewButtonFullForm(MouseEvent mouseEvent) {
            hiddenLabel_view.setManaged(true);
            hiddenLabel_view.setVisible(true);
            double targetWidth = hiddenLabel_view.prefWidth(-1);
            timeline = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(hiddenLabel_view.maxWidthProperty(), 0)),
                    new KeyFrame(Duration.millis(50), new KeyValue(hiddenLabel_view.maxWidthProperty(), targetWidth))
            );
            timeline.play();
        }
        @FXML
        public void viewButtonDefault(MouseEvent mouseEvent) {
            hiddenLabel_view.setManaged(false);
            hiddenLabel_view.setVisible(false);
        }
        @FXML
        public void viewButtonOnPressed(MouseEvent mouseEvent) {
            viewButton.setStyle("    -fx-background-color: #B0C7DD;\n" +
                    "    -fx-effect: dropshadow(gaussian, rgba(45, 52, 54, 0.15), 12, 0, 0, 0);");
        }
        @FXML
        public void ViewButtonOnReleased(MouseEvent mouseEvent) {
            isBasic = !isBasic;
            double currentWidth = hiddenLabel_view.getWidth();
            hiddenLabel_view.setText((isBasic)?"Basic":"Sprite");
            timeline = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(hiddenLabel_view.maxWidthProperty(), currentWidth)),
                    new KeyFrame(Duration.millis(50), new KeyValue(hiddenLabel_view.maxWidthProperty(), hiddenLabel_view.prefWidth(-1)))
            );
            timeline.play();
            viewButton.setStyle("-fx-background-color: #F8FAFC;\n" +
                    "    -fx-effect: dropshadow(gaussian, rgba(45, 52, 54, 0.15), 12, 0, 0, 4);");
        }

    //Entity
        @FXML
        private boolean fullForm = false;
        public VBox info;
        public void update_visiblity(boolean isFocused) {
                entityPanel.setVisible(isFocused);
        }
        public void entityFormChange(MouseEvent mouseEvent) {
            fullForm = !fullForm;
            info.setManaged(fullForm);
            info.setVisible(fullForm);
        }
        public void entityShow(MouseEvent mouseEvent) {
            entityPanel.setOpacity(1);
        }

        public void entityHide(MouseEvent mouseEvent) {
            entityPanel.setOpacity(0.5);
            fullForm = false;
            info.setManaged(fullForm);
            info.setVisible(fullForm);
        }

    //Tool panel
        @FXML
        public HBox mobsTool;

        private void mobs_list_loader(){
            if(Mobs.MobsList.isEmpty()){
                return;
            }
            int N = 1;
            for(String b: Mobs.MobsList){
                Button button = new Button(b);
                button.getStyleClass().add("tool-item");
                button.setId("mobsButton" + N);
                N++;
                mobsTool.getChildren().add(button);
            }
        }



    //Init
        public void initialize() {
            mobs_list_loader();
            SwingNode swingNode = new SwingNode();
            AnchorPane.setTopAnchor(swingNode, 0.0);
            AnchorPane.setBottomAnchor(swingNode, 0.0);
            AnchorPane.setLeftAnchor(swingNode, 0.0);
            AnchorPane.setRightAnchor(swingNode, 0.0);
            libgdxContainer.getChildren().add(swingNode);
            SwingUtilities.invokeLater(() ->{

            });

        }

}