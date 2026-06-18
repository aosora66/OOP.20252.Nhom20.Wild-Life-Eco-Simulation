package wildlife.view.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import wildlife.model.organism.Organism;
import wildlife.model.organism.plant.Plant;
import wildlife.view.renderer.Renderer;
import wildlife.view.renderer.SpriteBatch;
import wildlife.view.renderer.SimpleTexture;
import wildlife.view.renderer.SimpleTextureRegistry;
import wildlife.view.renderer.utils.Camera;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class UIEventController {
    //Root
    @FXML
    public StackPane rootStackPane;

    // Auto-scaling
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

    //UI panel
    @FXML
    public Group uiGroup;
    private ContextMenu activeContextMenu;

    //view button
    public static boolean sceneModeIsBasic = true;
    @FXML
    public HBox viewButton;
    @FXML
    public void viewButtonOnPressed() {
        viewButton.setStyle("-fx-background-color: #B0C7DD");
    }
    @FXML
    public void ViewButtonOnReleased() {
        viewButton.setStyle("-fx-background-color: #F8FAFC");
        sceneModeIsBasic = !sceneModeIsBasic;
        int mode = sceneModeIsBasic ? 0 : 1;
        Renderer r = Renderer.getInstance();
        if (r != null) r.setRenderMode(mode);
    }

    //entity panel
    public VBox HungerBar;
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
    private void showEntityPanel(wildlife.model.organism.Organism selected) {
        Platform.runLater(() -> {
            entityIdLabel.setText("ID: " + selected.getId() + " (" + selected.getSpeciesName() + ")");

            // Populate stats
            wildlife.model.organism.component.SurvivalStatsComponent stats = selected.getStats();
            double hpPercent = stats.getHp() / stats.getMaxHp();
            hpBarFill.setPrefWidth(500 * hpPercent);
            hpValueLabel.setText(String.format("%.0f / %.0f", stats.getHp(), stats.getMaxHp()));

            if(selected instanceof Plant){
                HungerBar.setDisable(true);
                hungerValueLabel.setText("Unspecified");
                hungerBarFill.setPrefWidth(0);
            }else{
                // 0 = no, 100 = doi kiet — bar day len khi doi tang
                double hungerPercent = stats.getHungerLevel() / 100.0;
                hungerBarFill.setPrefWidth(500 * hungerPercent);
                hungerValueLabel.setText(String.format("%.0f / 100", stats.getHungerLevel()));
                HungerBar.setDisable(false);
            }

            // Thirst: 0 = du nuoc, 100 = khat kiet — bar day len khi khat tang
            double thirstPercent = stats.getThirstLevel() / 100.0;
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
        });
    }
    private void hideEntityPanel() {
        Platform.runLater(() -> {
            entityPanel.setManaged(false);
            entityPanel.setVisible(false);
        });
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


    //Render Scene
    @FXML
    public AnchorPane sceneCanvas;

    //Grid
    private static final int CELL_SIZE = 128;
    private static volatile Map<String, List<Organism>> spatialGrid = new HashMap<>();

    //Config de nhung vao JavaFX
    private volatile boolean running = true;
    private ImageView renderImageView;
    private WritableImage writableImage;
    private volatile int canvasWidth = 800;
    private volatile int canvasHeight = 600;
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

    private volatile Renderer renderer;
    private final CountDownLatch rendererLatch = new CountDownLatch(1);
    public Renderer getRenderer(){
        try {
            rendererLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return this.renderer;
    }

    private static volatile List<Organism> activeOrganisms;
    public static void setActiveOrganisms(List<Organism> list) {
        activeOrganisms = list;
        Map<String, List<Organism>> newGrid = new HashMap<>();
        for(Organism org : list) {
            if(org.isAlive()){
                int cellX = (int) Math.floor(org.getPosition().getX() / CELL_SIZE);
                int cellY = (int) Math.floor(org.getPosition().getY() / CELL_SIZE);

                String key = cellX + "," + cellY;
                newGrid.computeIfAbsent(key, k -> new ArrayList<>()).add(org);
            }
        }
        spatialGrid = newGrid;
    }

    private SimpleTextureRegistry buildTextureRegistry() {
        SimpleTextureRegistry registry = new SimpleTextureRegistry();
        registry.register("Wolf",   new SimpleTexture(16, 16, (byte) 220, (byte)  50, (byte)  50, (byte) 255));
        registry.register("Grass",  new SimpleTexture(16, 16, (byte)  50, (byte) 200, (byte)  50, (byte) 255));
        registry.register("Rabbit", new SimpleTexture(16, 16, (byte) 100, (byte) 150, (byte) 255, (byte) 255));
        return registry;
    }
    private void setupLWJGLCanvas() {
        // tạo ImageView để nhúng LWJGL vào
        renderImageView = new ImageView();
        sceneCanvas.getChildren().add(renderImageView);
        
        // AutoScale cho vừa khung AnchorPane
        renderImageView.fitWidthProperty().bind(sceneCanvas.widthProperty());
        renderImageView.fitHeightProperty().bind(sceneCanvas.heightProperty());
        sceneCanvas.widthProperty().addListener((obs, oldVal, newVal) -> canvasWidth = Math.max(100, newVal.intValue()));
        sceneCanvas.heightProperty().addListener((obs, oldVal, newVal) ->canvasHeight = Math.max(100, newVal.intValue()));
        // Set initial sizes if already laid out
        if (sceneCanvas.getWidth() > 0) {
            canvasWidth = (int) sceneCanvas.getWidth();
        }
        if (sceneCanvas.getHeight() > 0) {
            canvasHeight = (int) sceneCanvas.getHeight();
        }
        // lắng nghe xem JavaFX có tắt không -> nếu tắt, set cờ running = false và LWJGL sẽ dừng lại
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

        startLWJGLThread();
    }
    private void startLWJGLThread() {
        //MacOS khong dung duoc GLFW
        if (System.getProperty("os.name", "").toLowerCase().contains("mac")) {
            System.err.println("[UIEventController] macOS detected — skipping GLFW (JavaFX main thread conflict). Canvas sẽ trắng.");
            this.renderer = null;
            rendererLatch.countDown();
            return;
        }
        // Luong render LWJGL nam trong JavaFX
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

            this.renderer = new Renderer(spriteBatch, textureRegistry, camera);
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

    private final Camera camera = new Camera(400, 300, 1080);
    private boolean isSpacePressed = false;
    private boolean isCtrlPressed = false;
    private double lastMouseX;
    private double lastMouseY;
    private void setupCameraEvents() {
        sceneCanvas.setFocusTraversable(true);
        sceneCanvas.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if(!newVal){
                sceneCanvas.setCursor(Cursor.DEFAULT);
            }
        });
        sceneCanvas.setOnKeyPressed(event -> {
            if(event.getCode() == KeyCode.SPACE) {
                isSpacePressed = true;
                sceneCanvas.setCursor(Cursor.OPEN_HAND);
            }
            if(event.getCode() == KeyCode.CONTROL) {
                isCtrlPressed = true;
                sceneCanvas.setCursor(Cursor.NONE);
            }
        });
        sceneCanvas.setOnKeyReleased(event -> {
            if(event.getCode() == KeyCode.SPACE) {
                isSpacePressed = false;
            }
            if(event.getCode() == KeyCode.CONTROL) {
                isCtrlPressed = false;
            }
            sceneCanvas.setCursor(Cursor.DEFAULT);
        });

        sceneCanvas.setOnScroll(event -> {
            if(isCtrlPressed){
                double currentX_OnScreen = event.getX();
                double currentY_OnScreen = event.getY();
                double currentX_OnMap = camera.getTopLeftX() + (currentX_OnScreen / canvasWidth) * (camera.getBotRightX()-camera.getTopLeftX());
                double currentY_OnMap = camera.getTopLeftY() + (currentY_OnScreen / canvasHeight) * (camera.getBotRightY()-camera.getTopLeftY());
                double deltaY = event.getDeltaY();
                int zoomFactor = (int) (deltaY * 2);
                camera.zoom(-zoomFactor);
                double newX_OnMap = camera.getTopLeftX() + (currentX_OnScreen / canvasWidth) * (camera.getBotRightX()-camera.getTopLeftX());
                double newY_OnMap = camera.getTopLeftY() + (currentY_OnScreen / canvasHeight) * (camera.getBotRightY()-camera.getTopLeftY());
                camera.pan(-(int)(newX_OnMap-currentX_OnMap), -(int)(newY_OnMap-currentY_OnMap));
            }
        });

        sceneCanvas.setOnMousePressed(event -> {
            if(isSpacePressed){
                sceneCanvas.requestFocus();
                sceneCanvas.setCursor(Cursor.CLOSED_HAND);
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
            if(activeContextMenu != null && activeContextMenu.isShowing()) {
                activeContextMenu.hide();
            }
            if (event.getButton() == MouseButton.PRIMARY && !isSpacePressed) {

                double clickX = event.getX();
                double clickY = event.getY();

                // Convert screen space to world space coordinates
                double worldX = camera.getTopLeftX() + (clickX / canvasWidth) * (camera.getBotRightX() - camera.getTopLeftX());
                double worldY = camera.getTopLeftY() + (clickY / canvasHeight) * (camera.getBotRightY() - camera.getTopLeftY());

                ArrayList<Organism> selected = findOrganismAt(worldX, worldY);
                if (selected != null) {
                    if(selected.size() == 1){
                        showEntityPanel(selected.get(0));
                    }else{
                        activeContextMenu = new ContextMenu();
                        for(Organism organism : selected){
                            MenuItem item = new MenuItem(organism.getSpeciesName() + ": " + organism.getId());
                            item.setOnAction(e ->{
                                showEntityPanel(organism);
                                activeContextMenu.hide();
                            });
                            activeContextMenu.getItems().add(item);
                        }
                        activeContextMenu.show(sceneCanvas, clickX, clickY);
                    }


                } else {
                    hideEntityPanel();
                }
            }
        });
    }

    private ArrayList<Organism> findOrganismAt(double x, double y) {
        if(spatialGrid == null || spatialGrid.isEmpty()) return null;

        ArrayList<Organism> result = null;

        int centerCellX = (int) Math.floor(x / CELL_SIZE);
        int centerCellY = (int) Math.floor(y / CELL_SIZE);
        for(int dx = -1; dx <= 1; dx++){
            for(int dy = -1; dy <= 1; dy++){
                String key = (centerCellX + dx) + "," + (centerCellY + dy);
                List<Organism> cellOrganisms = spatialGrid.get(key);

                if(cellOrganisms != null){
                    for(Organism organism : cellOrganisms){
                        if(organism.isAlive()){
                            double ox = organism.getPosition().getX();
                            double oy = organism.getPosition().getY();
                            if(x >= ox - 16 && x <= ox + 16 && y >= oy - 16 && y <= oy + 16){
                                if(result == null){
                                    result = new ArrayList<>();
                                }
                                result.add(organism);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    // Khoi tao
    public void initialize() {
        organism_list_load();
        environment_materials_load();
        setupLWJGLCanvas();
        setupScaling();
        setupCameraEvents();
        hideEntityPanel();
        uiGroup.setPickOnBounds(false);
        if(uiGroup.getChildren().getFirst() instanceof AnchorPane) {
            ((AnchorPane)uiGroup.getChildren().getFirst()).setPickOnBounds(false);
        }
    }
}