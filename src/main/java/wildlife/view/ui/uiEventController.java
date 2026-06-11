package wildlife.view.ui;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.util.Duration;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import wildlife.model.dto.RenderData;
import wildlife.model.organism.OrganismState;
import wildlife.view.renderer.Renderer;
import wildlife.view.renderer.SpriteBatch;
import wildlife.view.renderer.testWindow;

import java.nio.ByteBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

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

    @FXML
    public StackPane rootStackPane;

    @FXML
    public Group uiGroup;

    private volatile boolean running = true;
    private ImageView renderImageView;
    private WritableImage writableImage;
    private volatile int canvasWidth = 800;
    private volatile int canvasHeight = 600;

    private void setupLWJGLCanvas() {
        // Create the ImageView for rendering
        renderImageView = new ImageView();
        sceneCanvas.getChildren().add(renderImageView);
        
        // Make the ImageView resize dynamically with the AnchorPane
        renderImageView.fitWidthProperty().bind(sceneCanvas.widthProperty());
        renderImageView.fitHeightProperty().bind(sceneCanvas.heightProperty());

        // Listen to width and height changes of the AnchorPane
        sceneCanvas.widthProperty().addListener((obs, oldVal, newVal) -> {
            canvasWidth = Math.max(100, newVal.intValue());
        });
        sceneCanvas.heightProperty().addListener((obs, oldVal, newVal) -> {
            canvasHeight = Math.max(100, newVal.intValue());
        });
        
        // Set initial sizes if already laid out
        if (sceneCanvas.getWidth() > 0) {
            canvasWidth = (int) sceneCanvas.getWidth();
        }
        if (sceneCanvas.getHeight() > 0) {
            canvasHeight = (int) sceneCanvas.getHeight();
        }

        // Listen for window close to stop the thread
        sceneCanvas.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((obsW, oldWindow, newWindow) -> {
                    if (newWindow != null) {
                        newWindow.setOnCloseRequest(event -> {
                            running = false;
                        });
                    }
                });
            }
        });

        // Start the render loop on a background thread
        startLWJGLThread();
    }
    
    private void startLWJGLThread() {
        Thread renderThread = new Thread(() -> {
            // Initialize GLFW
            if (!glfwInit()) {
                System.err.println("Failed to initialize GLFW");
                return;
            }

            // Configure GLFW for offscreen/hidden rendering
            glfwDefaultWindowHints();
            glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

            int width = canvasWidth;
            int height = canvasHeight;

            // Create offscreen GLFW window
            long window = glfwCreateWindow(width, height, "Offscreen Window", 0, 0);
            if (window == 0) {
                System.err.println("Failed to create offscreen GLFW window");
                glfwTerminate();
                return;
            }

            // Make context current
            glfwMakeContextCurrent(window);
            GL.createCapabilities();

            // Create mock textures (16x16 pixels with solid colors)
            testWindow.MockTexture wolfTexture = new testWindow.MockTexture(16, 16, (byte) 255, (byte) 50, (byte) 50, (byte) 255);
            testWindow.MockTexture grassTexture = new testWindow.MockTexture(16, 16, (byte) 50, (byte) 200, (byte) 50, (byte) 255);
            testWindow.MockTexture rabbitTexture = new testWindow.MockTexture(16, 16, (byte) 100, (byte) 150, (byte) 255, (byte) 255);

            // Create registry and register our textures
            testWindow.MockTextureRegistry textureRegistry = new testWindow.MockTextureRegistry();
            textureRegistry.register("Wolf", wolfTexture);
            textureRegistry.register("Grass", grassTexture);
            textureRegistry.register("Rabbit", rabbitTexture);

            // Initialize SpriteBatch and Renderer
            SpriteBatch spriteBatch = new SpriteBatch(width, height);
            Renderer renderer = new Renderer(spriteBatch, textureRegistry);

            // Set the clear color
            glClearColor(0.15f, 0.15f, 0.15f, 1.0f);

            // Buffer to hold pixel data read from OpenGL
            ByteBuffer pixelBuffer = BufferUtils.createByteBuffer(width * height * 4);
            int[] argbBuffer = new int[width * height];

            while (running) {
                // Check if JavaFX container size has changed
                int targetW = canvasWidth;
                int targetH = canvasHeight;

                if (targetW != width || targetH != height) {
                    width = targetW;
                    height = targetH;
                    glfwSetWindowSize(window, width, height);
                    glViewport(0, 0, width, height);
                    spriteBatch.resize(width, height);
                    pixelBuffer = BufferUtils.createByteBuffer(width * height * 4);
                    argbBuffer = new int[width * height];
                }

                // Clear the framebuffer
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

                // Submit Mock RenderData for demo purposes (matching testWindow)
                renderer.submit(new RenderData("g1", "Grass", 400, 300, OrganismState.ALIVE));
                renderer.submit(new RenderData("g2", "Grass", 432, 300, OrganismState.ALIVE));
                renderer.submit(new RenderData("g3", "Grass", 400, 332, OrganismState.ALIVE));

                double time = glfwGetTime();
                float wolfX = 400f + (float) Math.cos(time) * 150f;
                float wolfY = 300f + (float) Math.sin(time) * 150f;
                renderer.submit(new RenderData("w1", "Wolf", wolfX, wolfY, OrganismState.ALIVE));

                float rabbitX = 200f + (float) (time * 50 % 400);
                float rabbitY = 150f + (float) Math.abs(Math.sin(time * 3)) * 80f;
                renderer.submit(new RenderData("r1", "Rabbit", rabbitX, rabbitY, OrganismState.ALIVE));

                // Draw all submitted organisms
                renderer.renderAll();

                // Swap buffers (mandatory for GLFW double buffering even if offscreen)
                glfwSwapBuffers(window);

                // Read pixels from OpenGL backbuffer
                pixelBuffer.clear();
                glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, pixelBuffer);

                // Convert RGBA to ARGB and flip Y-axis (since OpenGL 0,0 is bottom-left, but JavaFX is top-left)
                for (int y = 0; y < height; y++) {
                    int srcRow = height - 1 - y;
                    int srcOffset = srcRow * width * 4;
                    int dstOffset = y * width;
                    for (int x = 0; x < width; x++) {
                        int r = pixelBuffer.get(srcOffset + x * 4) & 0xFF;
                        int g = pixelBuffer.get(srcOffset + x * 4 + 1) & 0xFF;
                        int b = pixelBuffer.get(srcOffset + x * 4 + 2) & 0xFF;
                        int a = pixelBuffer.get(srcOffset + x * 4 + 3) & 0xFF;
                        argbBuffer[dstOffset + x] = (a << 24) | (r << 16) | (g << 8) | b;
                    }
                }

                // Copy the pixel buffer and size for thread-safety in Platform.runLater
                final int[] framePixels = argbBuffer.clone();
                final int finalW = width;
                final int finalH = height;

                Platform.runLater(() -> {
                    updateJavaFXImage(framePixels, finalW, finalH);
                });

                // Maintain a stable ~60 FPS
                try {
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // Clean up resources inside the render thread
            renderer.stop();
            spriteBatch.dispose();
            wolfTexture.dispose();
            grassTexture.dispose();
            rabbitTexture.dispose();

            glfwDestroyWindow(window);
            glfwTerminate();
        }, "LWJGL-Render-Thread");

        renderThread.setDaemon(true);
        renderThread.start();
    }

    private void updateJavaFXImage(int[] pixels, int w, int h) {
        if (writableImage == null || (int) writableImage.getWidth() != w || (int) writableImage.getHeight() != h) {
            writableImage = new WritableImage(w, h);
            renderImageView.setImage(writableImage);
        }
        writableImage.getPixelWriter().setPixels(
                0, 0, w, h,
                javafx.scene.image.PixelFormat.getIntArgbInstance(),
                pixels, 0, w
        );
    }

    
    //responsive scaling
    private void setupScaling() {
        if (rootStackPane == null || uiGroup == null) return;

        javafx.beans.value.ChangeListener<Number> resizeListener = (obs, oldVal, newVal) -> {
            double windowWidth = rootStackPane.getWidth();
            double windowHeight = rootStackPane.getHeight();

            if (windowWidth <= 0 || windowHeight <= 0) return;

            // Reference resolution is 1920x1080
            double scaleX = windowWidth / 1920.0;
            double scaleY = windowHeight / 1080.0;

            // Preserve aspect ratio (uniform scaling)
            double scale = Math.min(scaleX, scaleY);

            uiGroup.setScaleX(scale);
            uiGroup.setScaleY(scale);
        };

        rootStackPane.widthProperty().addListener(resizeListener);
        rootStackPane.heightProperty().addListener(resizeListener);

        // Run once for initial scaling if layout is already calculated
        if (rootStackPane.getWidth() > 0 && rootStackPane.getHeight() > 0) {
            double scaleX = rootStackPane.getWidth() / 1920.0;
            double scaleY = rootStackPane.getHeight() / 1080.0;
            double scale = Math.min(scaleX, scaleY);
            uiGroup.setScaleX(scale);
            uiGroup.setScaleY(scale);
        }
    }

    public void initialize() {
        organism_list_load();
        environment_materials_load();
        setupLWJGLCanvas();
        setupScaling();
    }
}