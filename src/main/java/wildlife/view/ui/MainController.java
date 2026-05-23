package wildlife.view.ui;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.util.Duration;
import wildlife.view.organism;
import wildlife.view.environment;

public class MainController {
    private static boolean sceneModeIsBasic = true;
    private Timeline timelineForAnimation;
    //view button
    @FXML
    public HBox viewButton;
    @FXML
    public Label sceneMode_label;
    @FXML
    public void viewButtonExpand(MouseEvent mouseEvent) {
        sceneMode_label.setManaged(true);
        sceneMode_label.setVisible(true);
        double targetWidth = sceneMode_label.prefWidth(-1);
        timelineForAnimation = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(sceneMode_label.maxWidthProperty(), 0)),
                new KeyFrame(Duration.millis(50), new KeyValue(sceneMode_label.maxWidthProperty(), targetWidth))
        );
        timelineForAnimation.play();
    }
    @FXML
    public void viewButtonCollapse(MouseEvent mouseEvent) {
        sceneMode_label.setManaged(false);
        sceneMode_label.setVisible(false);
    }
    @FXML
    public void viewButtonOnPressed(MouseEvent mouseEvent) {
        viewButton.setStyle("-fx-background-color: #B0C7DD");
    }
    @FXML
    public void ViewButtonOnReleased(MouseEvent mouseEvent) {
        sceneModeIsBasic = !sceneModeIsBasic;
        double currentWidth = sceneMode_label.getWidth();
        sceneMode_label.setText((sceneModeIsBasic)?"Basic":"Sprite");
        timelineForAnimation = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(sceneMode_label.maxWidthProperty(), currentWidth)),
                new KeyFrame(Duration.millis(10), new KeyValue(sceneMode_label.maxWidthProperty(), sceneMode_label.prefWidth(-1)))
        );
        timelineForAnimation.play();
        viewButton.setStyle("-fx-background-color: #F8FAFC");
    }

    //entity panel
    private boolean entityPanelIsExpanded = false;
    @FXML
    public HBox entityPanel;
    @FXML
    public VBox info;
    public void entityPanelShow(boolean anAnimalFocused){
        entityPanel.setManaged(anAnimalFocused);
        entityPanel.setVisible(anAnimalFocused);
    }
    public void entityPanelFormChange(MouseEvent mouseEvent) {
        entityPanelIsExpanded = !entityPanelIsExpanded;
        info.setManaged(entityPanelIsExpanded);
        info.setVisible(entityPanelIsExpanded);
    }
    public void entityPanelSolid(MouseEvent mouseEvent) {
            entityPanel.setOpacity(1);
    }
    public void entityPanelTransparent(MouseEvent mouseEvent) {
        if(!entityPanelIsExpanded){
            entityPanel.setOpacity(0.5);
        }
    }

    //toolBox
    @FXML
    public HBox organismToolset;
    private void organism_list_load(){
        if(!organism.mobsList.isEmpty()){
            int id = 1;
            for(String b: organism.mobsList){
                Button button = new Button(b);
                button.getStyleClass().add("tool-item");
                button.setStyle("-fx-background-color: #D9B65D");

                button.setId("animalButton" + id);
                id++;
                organismToolset.getChildren().add(button);
            }
        }
        if(!organism.plantsList.isEmpty()){
            int id = 1;
            for(String b: organism.plantsList){
                Button button = new Button(b);
                button.getStyleClass().add("tool-item");
                button.setId("plantsButton" + id);
                button.setStyle("-fx-background-color: #6FA37C");
                id++;
                organismToolset.getChildren().add(button);
            }
        }
    }
    @FXML
    public HBox envTool;
    private void environment_materials_load(){
        if(environment.materialLists.isEmpty()){
            return;
        }
        int id = 1;
        for(String b: environment.materialLists){
            Button button = new Button(b);
            button.getStyleClass().add("tool-item");
            button.setId("envButton" + id);
            button.setStyle("-fx-background-color: #A8907A");
            id++;
            envTool.getChildren().add(button);
        }
    }

    //Pane nay de render scene
    @FXML
    public AnchorPane sceneCanvas;

    public void initialize() {
        organism_list_load();
        environment_materials_load();
    }

    public double getImageHeight() {
        Screen screen = Screen.getPrimary();
        Rectangle2D bounds = screen.getVisualBounds();
        return bounds.getHeight() * 0.2;
    }
}