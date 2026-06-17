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
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
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
import wildlife.view.renderer.SimpleTexture;
import wildlife.view.renderer.SimpleTextureRegistry;

import java.nio.ByteBuffer;
import java.util.EventListener;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

//Dieu khien cac thao tac tren man hinh UI (va animation) va ghi nhan cac event tac dong vao scene canvas de gui sang lwjgl
public class UIEventController {
    public static boolean sceneModeIsBasic = true;
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

    @FXML
    public ImageView entityImageView;
    @FXML
    public Label entityIdLabel;
    @FXML
    public Region hpBarFill;
    @FXML
    public Label hpValueLabel;
    @FXML
    public Region hungerBarFill;
    @FXML
    public Label hungerValueLabel;
    @FXML
    public Region thirstyBarFill;
    @FXML
    public Label thirstyValueLabel;
    
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
    private boolean isSpacePressed = false;
    private boolean isCtrlPressed = false;
    @FXML
    public StackPane rootStackPane;

    @FXML
    public Group uiGroup;

    private volatile boolean running = true;
    private ImageView renderImageView;
    private WritableImage writableImage;
    private volatile int canvasWidth = 800;
    private volatile int canvasHeight = 600;

    private volatile Renderer renderer;
    private final java.util.concurrent.CountDownLatch rendererLatch = new java.util.concurrent.CountDownLatch(1);

    private static volatile java.util.List<wildlife.model.organism.Organism> activeOrganisms;

    public static void setActiveOrganisms(java.util.List<wildlife.model.organism.Organism> list) {
        activeOrganisms = list;
    }

    private final wildlife.view.renderer.utils.Camera camera = new wildlife.view.renderer.utils.Camera(400, 300, 1080);
    private double lastMouseX;
    private double lastMouseY;
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
    
    private SimpleTextureRegistry buildTextureRegistry() {
        SimpleTextureRegistry registry = new SimpleTextureRegistry();
        registry.register("Wolf",   new SimpleTexture(16, 16, (byte) 220, (byte)  50, (byte)  50, (byte) 255));
        registry.register("Grass",  new SimpleTexture(16, 16, (byte)  50, (byte) 200, (byte)  50, (byte) 255));
        registry.register("Rabbit", new SimpleTexture(16, 16, (byte) 100, (byte) 150, (byte) 255, (byte) 255));
        return registry;
    }

    private void startLWJGLThread() {
        if (System.getProperty("os.name", "").toLowerCase().contains("mac")) {
            System.err.println("[UIEventController] macOS detected — skipping GLFW (JavaFX main thread conflict). Canvas sẽ trắng.");
            this.renderer = null;
            rendererLatch.countDown();
            return;
        }
        Thread renderThread = new Thread(() -> {
            if (!glfwInit()) {
                System.err.println("LWJGL: glfwInit() thất bại");
                return;
            }

            glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

            int width = canvasWidth;
            int height = canvasHeight;

            long window = glfwCreateWindow(width, height, "Offscreen", 0, 0);
            if (window == 0) {
                System.err.println("LWJGL: tạo offscreen window thất bại");
                glfwTerminate();
                return;
            }

            glfwMakeContextCurrent(window);
            GL.createCapabilities();

            SimpleTextureRegistry textureRegistry = buildTextureRegistry();
            SpriteBatch spriteBatch = new SpriteBatch(width, height);
            this.renderer = new Renderer(spriteBatch, textureRegistry);
            rendererLatch.countDown();

            glClearColor(0.15f, 0.15f, 0.15f, 1.0f);

            ByteBuffer pixelBuffer = BufferUtils.createByteBuffer(width * height * 4);
            int[] argbBuffer = new int[width * height];

            while (running) {
                // Chờ coreLoopThread commit frame — tránh spin-loop, chỉ render khi có dữ liệu mới
                try {
                    if (!renderer.awaitFrame(16)) continue;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

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

                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
                spriteBatch.updateProjection(camera);
                renderer.renderAll();
                glfwSwapBuffers(window);

                // Đọc pixel từ OpenGL và chuyển RGBA → ARGB (flip Y vì OpenGL gốc tọa độ bottom-left)
                pixelBuffer.clear();
                glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, pixelBuffer);
                for (int y = 0; y < height; y++) {
                    int srcRow    = height - 1 - y;
                    int srcOffset = srcRow * width * 4;
                    int dstOffset = y * width;
                    for (int x = 0; x < width; x++) {
                        int r = pixelBuffer.get(srcOffset + x * 4)     & 0xFF;
                        int g = pixelBuffer.get(srcOffset + x * 4 + 1) & 0xFF;
                        int b = pixelBuffer.get(srcOffset + x * 4 + 2) & 0xFF;
                        int a = pixelBuffer.get(srcOffset + x * 4 + 3) & 0xFF;
                        argbBuffer[dstOffset + x] = (a << 24) | (r << 16) | (g << 8) | b;
                    }
                }

                final int[] framePixels = argbBuffer.clone();
                final int finalW = width;
                final int finalH = height;
                Platform.runLater(() -> updateJavaFXImage(framePixels, finalW, finalH));
            }

            renderer.stop();
            spriteBatch.dispose();
            textureRegistry.clear();
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

    private void setupCameraEvents() {
        sceneCanvas.setFocusTraversable(true);
        sceneCanvas.setOnKeyPressed(event -> {
            if(event.getCode() == KeyCode.SPACE) {
                isSpacePressed = true;
            }
            if(event.getCode() == KeyCode.CONTROL) {
                isCtrlPressed = true;
            }
        });
        sceneCanvas.setOnKeyReleased(event -> {
            if(event.getCode() == KeyCode.SPACE) {
                isSpacePressed = false;
            }
            if(event.getCode() == KeyCode.CONTROL) {
                isCtrlPressed = false;
            }
        });

        sceneCanvas.setOnScroll(event -> {
            if(isCtrlPressed){
                double deltaY = event.getDeltaY();
                int zoomFactor = (int) (deltaY * 2);
                camera.zoom(-zoomFactor);
            }
        });

        sceneCanvas.setOnMousePressed(event -> {
            if(isSpacePressed){
                sceneCanvas.requestFocus();
                lastMouseX = event.getX();
                lastMouseY = event.getY();
            }
        });

        sceneCanvas.setOnMouseDragged(event -> {
            if(isSpacePressed){
                double currentX = event.getX();
                double currentY = event.getY();
                double deltaX = currentX - lastMouseX;
                double deltaY = currentY - lastMouseY;

                double scale = (double) (camera.getBotRightY() - camera.getTopLeftY()) / canvasHeight;
                camera.pan((int) (-deltaX * scale), (int) (-deltaY * scale));

                lastMouseX = currentX;
                lastMouseY = currentY;
            }
        });

        sceneCanvas.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && !isSpacePressed) {
                double clickX = event.getX();
                double clickY = event.getY();

                // Convert screen space to world space coordinates
                double worldX = camera.getTopLeftX() + (clickX / canvasWidth) * (camera.getBotRightX() - camera.getTopLeftX());
                double worldY = camera.getTopLeftY() + (clickY / canvasHeight) * (camera.getBotRightY() - camera.getTopLeftY());

                wildlife.model.organism.Organism selected = findOrganismAt(worldX, worldY);
                if (selected != null) {
                    showEntityPanel(selected);
                } else {
                    hideEntityPanel();
                }
            }
        });
    }

    private wildlife.model.organism.Organism findOrganismAt(double x, double y) {
        if (activeOrganisms == null) return null;
        synchronized (activeOrganisms) {
            for (wildlife.model.organism.Organism o : activeOrganisms) {
                if (o.isAlive()) {
                    double ox = o.getPosition().getX();
                    double oy = o.getPosition().getY();
                    // Each organism is represented by a 32x32 sprite, so bounding box radius is 16
                    if (x >= ox - 16 && x <= ox + 16 && y >= oy - 16 && y <= oy + 16) {
                        return o;
                    }
                }
            }
        }
        return null;
    }

    private void showEntityPanel(wildlife.model.organism.Organism selected) {
        Platform.runLater(() -> {
            entityIdLabel.setText("ID: " + selected.getId() + " (" + selected.getSpeciesName() + ")");
            
            // Populate stats
            wildlife.model.organism.component.SurvivalStatsComponent stats = selected.getStats();
            double hpPercent = stats.getHp() / stats.getMaxHp();
            hpBarFill.setPrefWidth(500 * hpPercent);
            hpValueLabel.setText(String.format("%.0f / %.0f", stats.getHp(), stats.getMaxHp()));

            // Hunger (0 is full, 100 is starving, so fill is 1.0 - hungerLevel/100.0)
            double hungerPercent = 1.0 - (stats.getHungerLevel() / 100.0);
            hungerPercent = Math.max(0.0, Math.min(1.0, hungerPercent));
            hungerBarFill.setPrefWidth(500 * hungerPercent);
            hungerValueLabel.setText(String.format("%.0f / 100", stats.getHungerLevel()));

            // Thirst (0 is quenched, 100 is dehydrated)
            double thirstPercent = 1.0 - (stats.getThirstLevel() / 100.0);
            thirstPercent = Math.max(0.0, Math.min(1.0, thirstPercent));
            thirstyBarFill.setPrefWidth(500 * thirstPercent);
            thirstyValueLabel.setText(String.format("%.0f / 100", stats.getThirstLevel()));

            // Update Image depending on Species
            String imagePath = "/wildlife/view/ui/assets/images/Fox.png";
            try {
                javafx.scene.image.Image img = new javafx.scene.image.Image(getClass().getResourceAsStream(imagePath));
                entityImageView.setImage(img);
            } catch (Exception e) {
                // Ignore if asset loading fails
            }

            entityPanel.setManaged(true);
            entityPanel.setVisible(true);
            
            entityPanelIsExpanded = true;
            info.setManaged(true);
            info.setVisible(true);
        });
    }

    private void hideEntityPanel() {
        Platform.runLater(() -> {
            entityPanel.setManaged(false);
            entityPanel.setVisible(false);
            entityPanelIsExpanded = false;
            info.setManaged(false);
            info.setVisible(false);
        });
    }

    public Renderer getRenderer(){
        try {
            rendererLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return this.renderer;
    }
    public void initialize() {
        organism_list_load();
        environment_materials_load();
        setupLWJGLCanvas();
        setupScaling();
        setupCameraEvents();
        hideEntityPanel();
        uiGroup.setPickOnBounds(false);
        if(uiGroup.getChildren().get(0) instanceof AnchorPane) {
            ((AnchorPane)uiGroup.getChildren().get(0)).setPickOnBounds(false);
        }
    }
}