package wildlife.view.ui;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.util.Duration;
import wildlife.view.renderer.Renderer;
import wildlife.view.renderer.SpriteBatch;
import wildlife.view.renderer.TextureRegistry;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Canvas;

//Dieu khien cac thao tac tren man hinh UI (va animation) va ghi nhan cac event tac dong vao scene canvas de gui sang lwjgl
public class uiEventController {
    private static boolean sceneModeIsBasic = true;
    private Timeline timelineForAnimation;
    
    //view button
    @FXML
    public HBox viewButton;
    @FXML
    public Label sceneMode_label;
    
    @FXML
    public void viewButtonExpand() {
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
    public void viewButtonCollapse() {
        sceneMode_label.setManaged(false);
        sceneMode_label.setVisible(false);
    }
    
    @FXML
    public void viewButtonOnPressed() {
        viewButton.setStyle("-fx-background-color: #B0C7DD");
    }
    
    @FXML
    public void ViewButtonOnReleased() {
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
    
//    public void entityPanelShow(boolean anAnimalFocused){
//        entityPanel.setManaged(anAnimalFocused);
//        entityPanel.setVisible(anAnimalFocused);
//    }
    
    public void entityPanelFormChange() {
        entityPanelIsExpanded = !entityPanelIsExpanded;
        info.setManaged(entityPanelIsExpanded);
        info.setVisible(entityPanelIsExpanded);
    }
    
    public void entityPanelSolid() {
        entityPanel.setOpacity(1);
    }
    
    public void entityPanelTransparent() {
        if(!entityPanelIsExpanded){
            entityPanel.setOpacity(0.5);
        }
    }

    //toolBox
    @FXML
    public HBox organismToolset;
    private void organism_list_load() {
    }
    
    @FXML
    public HBox envTool;
    private void environment_materials_load() {
    }

    //Pane nay de render scene
    @FXML
    public AnchorPane sceneCanvas;
    

    private void setupLWJGLCanvas() {
        // dung swingnode de nhung vao javafx (nhung lwjgl vao swingnode)
        SwingNode swingNode = new SwingNode();
        
        AnchorPane.setTopAnchor(swingNode, 0.0);
        AnchorPane.setBottomAnchor(swingNode, 0.0);
        AnchorPane.setLeftAnchor(swingNode, 0.0);
        AnchorPane.setRightAnchor(swingNode, 0.0);
        
        // nhung swingnode vao sceneCanvas (anchor pane danh cho renderer)
        sceneCanvas.getChildren().add(swingNode);

        SwingUtilities.invokeLater(() -> {
            JPanel panel = new JPanel(new BorderLayout());

            Canvas canvas = new Canvas();
            canvas.setBackground(java.awt.Color.BLACK);
            canvas.setIgnoreRepaint(true);

            //Wrap lai vao1 cai JPanel vi swingNode yeu cau dau vao la 1 JComponent
            panel.add(canvas, BorderLayout.CENTER);
            swingNode.setContent(panel);
            
            // bat dau vong lap render
            Platform.runLater(() -> startLWJGLThread(canvas));
        });
    }
    
    private void startLWJGLThread(Canvas canvas) {
        Thread renderThread = new Thread(() -> {
            // TODO: Initialize OpenGL context from canvas, then create/inject dependencies:
            //   SpriteBatch      spriteBatch     = new SpriteBatch(canvas.getWidth(), canvas.getHeight());
            //   TextureRegistry  textureRegistry = /* your implementation */;
            //   Renderer         renderer        = new Renderer(spriteBatch, textureRegistry);
            //
            // Then, in the render loop:
            //   List<RenderData> snapshot = environment.getRenderSnapshot();
            //   renderer.render(snapshot);
        }, "LWJGL-Render-Thread");
        
        renderThread.setDaemon(true);
        renderThread.start();
    }

    
    //responsive
    public double getImageHeight() {
        Screen screen = Screen.getPrimary();
        Rectangle2D bounds = screen.getVisualBounds();
        return bounds.getHeight() * 0.2;
    }

    public void initialize() {
        organism_list_load();
        environment_materials_load();
        setupLWJGLCanvas();
    }
}