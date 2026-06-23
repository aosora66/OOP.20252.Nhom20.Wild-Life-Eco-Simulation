package wildlife.view.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.shape.SVGPath;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import wildlife.model.organism.Organism;
import wildlife.model.organism.plant.Plant;
import wildlife.view.renderer.AtlasTexture;
import wildlife.view.renderer.Renderer;
import wildlife.view.renderer.SpriteBatch;
import wildlife.view.renderer.SimpleTexture;
import wildlife.view.renderer.SimpleTextureRegistry;
import wildlife.view.renderer.utils.Camera;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.List;
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
    private void showEntityPanel(Organism selected) {
        if(selected == null){
            hideEntityPanel();
            return;
        }
        Platform.runLater(() -> {
            // chống trường hợp chuaw kịp vẽ lại thì người dùng chọn sinh vật kh
            if (selected != selectedOrganism) {
                return;
            }

            // Cập nhật ảnh đại diện của loài (áp dụng cho cả sinh vật sống và chết)
            String imagePath = "/wildlife/view/ui/assets/images/pfp/" + selected.getSpeciesName() + ".png";
            try {
                java.io.InputStream stream = getClass().getResourceAsStream(imagePath);
                if (stream != null) {
                    javafx.scene.image.Image img = new javafx.scene.image.Image(stream);
                    entityImageView.setImage(img);
                }
            } catch (Exception e) {
                // Ignore if asset loading fails
            }

            wildlife.model.organism.component.SurvivalStatsComponent stats = selected.getStats();
            entityIdLabel.setText("ID: " + selected.getId() + " (" + selected.getSpeciesName() + ")");
            entityIdLabel.setTextFill(Paint.valueOf("#2d3436"));
            if(!selected.isAlive()){
                entityIdLabel.setText("[DEAD]   " + entityIdLabel.getText());
                entityIdLabel.setTextFill(Paint.valueOf("#f28c8c"));
                hpBarFill.setPrefWidth(0);
                hpValueLabel.setText(String.format("0 / %.0f", stats.getMaxHp()));
                hungerBarFill.setPrefWidth(0);
                hungerValueLabel.setText("Unspecified");
                hungerValueLabel.setDisable(true);
                thirstyBarFill.setPrefWidth(0);
                thirstyValueLabel.setText("Unspecified");
                thirstyValueLabel.setDisable(true);
                entityPanel.setManaged(true);
                entityPanel.setVisible(true);
                return;
            }

            // Populate stats
            double hpPercent = stats.getHp() / stats.getMaxHp();
            hpBarFill.setPrefWidth(500 * hpPercent);
            hpValueLabel.setText(String.format("%.0f / %.0f", stats.getHp(), stats.getMaxHp()));

            if(selected instanceof Plant){
                HungerBar.setDisable(true);
                hungerValueLabel.setText("Unspecified");
                hungerBarFill.setPrefWidth(0);
            }else{
                // 0 la no, 100 la doi
                double hungerPercent = (stats.getHungerLevel() / 100.0);
                hungerBarFill.setPrefWidth(500 * (1 - hungerPercent));
                hungerValueLabel.setText(String.format("%.0f / 100", 100-stats.getHungerLevel()));
                HungerBar.setDisable(false);
            }

            // Thirst (0 is quenched, 100 is dehydrated)
            double thirstPercent = 1- (stats.getThirstLevel() / 100.0);
            thirstyBarFill.setPrefWidth(500 * (thirstPercent));
            thirstyValueLabel.setText(String.format("%.0f / 100", 100-stats.getThirstLevel()));

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

    // Basic mode: solid-color textures per species.
    // Warm tones for predators (Hunter, Tiger, Wolf), cool tones for prey (Deer, Elephant, Fish, Rabbit).
    private SimpleTextureRegistry buildTextureRegistry() {
        SimpleTextureRegistry registry = new SimpleTextureRegistry();
        // Predators — warm tones
        registry.register("Hunter",   new SimpleTexture(16, 16, (byte) 230, (byte)  60, (byte)  30, (byte) 255)); // deep red-orange
        registry.register("Tiger",    new SimpleTexture(16, 16, (byte) 240, (byte) 140, (byte)  20, (byte) 255)); // amber
        registry.register("Wolf",     new SimpleTexture(16, 16, (byte) 210, (byte)  40, (byte)  70, (byte) 255)); // crimson
        // Herbivores — cool tones
        registry.register("Deer",     new SimpleTexture(16, 16, (byte)  60, (byte) 160, (byte) 140, (byte) 255)); // teal
        registry.register("Elephant", new SimpleTexture(16, 16, (byte)  70, (byte) 110, (byte) 190, (byte) 255)); // steel blue
        registry.register("Fish",     new SimpleTexture(16, 16, (byte)  30, (byte) 170, (byte) 230, (byte) 255)); // cyan
        registry.register("Rabbit",   new SimpleTexture(16, 16, (byte) 130, (byte) 100, (byte) 210, (byte) 255)); // lavender
        // Plants
        registry.register("Grass",    new SimpleTexture(16, 16, (byte)  50, (byte) 200, (byte)  50, (byte) 255)); // green
        registry.register("AppleTree", new SimpleTexture(16, 16, (byte) 50, (byte) 210, (byte) 20, (byte) 255));
        registry.register("TreeForest", new SimpleTexture(16, 16, (byte) 20, (byte) 255, (byte) 10, (byte) 255));
        // env
        registry.register("DEEP_WATER", new SimpleTexture(16, 16, (byte) 29, (byte) 77, (byte) 253, (byte) 255));
        registry.register("GRASSLAND", new SimpleTexture(16, 16, (byte) 115, (byte) 186, (byte) 103, (byte) 255));
        registry.register("FOREST", new SimpleTexture(16, 16, (byte) 34, (byte) 94, (byte) 53, (byte) 255));
        registry.register("MOUNTAIN", new SimpleTexture(16, 16, (byte) 120, (byte) 120, (byte) 120, (byte) 255));
        registry.register("MUD", new SimpleTexture(16, 16, (byte) 128, (byte) 85, (byte) 50, (byte) 255));
        return registry;
    }

    private AtlasTexture buildAtlasTexture() {
        AtlasTexture atlas;
        try (var stream = getClass().getResourceAsStream(
                "/wildlife/view/ui/assets/texture_sheet/atlas.png")) {
            if (stream == null) {
                System.err.println("[UIEventController] atlas.png not found — sprite mode will be unavailable");
                return null;
            }
            atlas = new AtlasTexture(stream);
        } catch (Exception e) {
            System.err.println("[UIEventController] Failed to load atlas: " + e.getMessage());
            return null;
        }

        // Terrain atlas (env_atlas.png có thể chưa có — bỏ qua nếu null)
        try (var png  = getClass().getResourceAsStream(
                    "/wildlife/view/ui/assets/texture_sheet/env_atlas.png");
             var json = getClass().getResourceAsStream(
                    "/wildlife/view/ui/assets/texture_sheet/env_atlas_coordinates.json")) {
            atlas.loadEnvAtlas(png, json);
        } catch (Exception e) {
            System.err.println("[UIEventController] Failed to load env atlas: " + e.getMessage());
        }

        // Resources atlas (resources_atlas.png có thể chưa có — bỏ qua nếu null)
        try (var png  = getClass().getResourceAsStream(
                    "/wildlife/view/ui/assets/texture_sheet/resources_atlas.png");
             var json = getClass().getResourceAsStream(
                    "/wildlife/view/ui/assets/texture_sheet/resources_atlas_coordinates.json")) {
            atlas.loadResourcesAtlas(png, json);
        } catch (Exception e) {
            System.err.println("[UIEventController] Failed to load resources atlas: " + e.getMessage());
        }

        return atlas;
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
            AtlasTexture atlasTexture = buildAtlasTexture();
            SpriteBatch spriteBatch = new SpriteBatch(width, height);

            this.renderer = new Renderer(spriteBatch, textureRegistry, atlasTexture, camera);
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
            if (atlasTexture != null) atlasTexture.dispose();
            glfwDestroyWindow(window);
            glfwTerminate();
        }, "LWJGL-Render-Thread");

        renderThread.setDaemon(true);
        renderThread.start();
    }

    private static final int N_TICKS = 15;
    private static volatile UIEventController instance;
    private int tickCount = 0;


    private final Camera camera = new Camera(500, 500, 1000);
    private boolean isSpacePressed = false;
    private boolean isCtrlPressed = false;
    private double lastMouseX;
    private double lastMouseY;
    private volatile Organism selectedOrganism = null;
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
                sceneCanvas.setCursor(Cursor.V_RESIZE);
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
                camera.pan((int)(currentX_OnMap-newX_OnMap), (int)(currentY_OnMap-newY_OnMap));
            }else{
                double deltaY =  event.getDeltaY();
                camera.pan(0, -(int)deltaY);
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
                        selectedOrganism = selected.getFirst();
                    }else{
                        activeContextMenu = new ContextMenu();
                        for(Organism organism : selected){
                            MenuItem item = new MenuItem(organism.getSpeciesName() + ": " + organism.getId());
                            item.setOnAction(e ->{
                                selectedOrganism = organism;
                                activeContextMenu.hide();
                                showEntityPanel(selectedOrganism);
                            });
                            activeContextMenu.getItems().add(item);
                        }
                        activeContextMenu.show(sceneCanvas, clickX, clickY);
                    }
                } else {
                    selectedOrganism = null;
                }
                showEntityPanel(selectedOrganism);
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

    public static void tickUpdate() {
        if (instance != null) {
            instance.onTick();
        }
    }

    private void onTick() {
        tickCount++;
        if (tickCount >= N_TICKS) {
            tickCount = 0;
            Organism selected = selectedOrganism;
            showEntityPanel(selected);
        }
    }

    // Pause Button
    @FXML
    public Button pauseButton;
    @FXML
    private SVGPath pauseIcon;
    public void pauseStatusChange() {
        synchronized (lock) {
            paused = !paused;
            if (pauseIcon != null) {
                pauseIcon.setContent(paused ? "M3 2l7 4-7 4V2z" : "M3 2h2v8H3V2zm4 0h2v8H7V2z");
            }
        }
    }
    private static volatile boolean paused = false;
    private static final Object lock = new Object();
    public static boolean isPaused() { return paused; }

    // Khoi tao
    public void initialize() {
        instance = this;
        setupLWJGLCanvas();
        setupScaling();
        setupCameraEvents();
        hideEntityPanel();
        uiGroup.setPickOnBounds(false);
        pauseButton.setFocusTraversable(false);
        if(uiGroup.getChildren().getFirst() instanceof AnchorPane) {
            ((AnchorPane)uiGroup.getChildren().getFirst()).setPickOnBounds(false);
        }
    }

}