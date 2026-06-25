package wildlife.view.ui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.*;
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
import wildlife.model.environment.CompositeMap;
import wildlife.model.environment.Environment;
import wildlife.model.environment.enums.Season;
import wildlife.model.environment.enums.WeatherType;
import wildlife.model.environment.enums.ObstacleType;
import wildlife.model.environment.enums.FoodType;
import wildlife.model.environment.component.TimeComponent;
import wildlife.model.organism.Organism;
import wildlife.model.organism.animal.Animal;
import wildlife.model.organism.animal.canivores.Tiger;
import wildlife.model.organism.animal.canivores.Wolf;
import wildlife.model.organism.animal.canivores.Hunter;
import wildlife.model.organism.animal.hebivores.Deer;
import wildlife.model.organism.animal.hebivores.Elephant;
import wildlife.model.organism.animal.hebivores.Fish;
import wildlife.model.organism.animal.hebivores.Rabbit;
import wildlife.model.organism.plant.AppleTree;
import wildlife.model.organism.plant.Grass;
import wildlife.model.organism.plant.TreeForest;
import wildlife.model.organism.component.GrowthComponent;
import wildlife.model.organism.component.SurvivalStatsComponent;
import wildlife.model.organism.component.AdaptabilityComponent;
import wildlife.model.environment.enums.TerrainType;
import wildlife.util.AppConfig;
import wildlife.util.Vector2D;
import wildlife.util.ValueRange;
import wildlife.model.organism.plant.Plant;
import wildlife.view.renderer.AtlasTexture;
import wildlife.view.renderer.Renderer;
import wildlife.view.renderer.SpriteBatch;
import wildlife.view.renderer.SimpleTexture;
import wildlife.view.renderer.SimpleTextureRegistry;
import wildlife.view.renderer.utils.Camera;

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
    public Button viewButton;
    @FXML
    public void ViewButtonClicked() {
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

    public static final java.util.concurrent.locks.ReentrantReadWriteLock worldLock = new java.util.concurrent.locks.ReentrantReadWriteLock();
    private static volatile CompositeMap world;

    public static void setWorld(CompositeMap w) {
        world = w;
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
        // Food — fallback khi resources_atlas.png chưa có
        registry.register("MEAT",  new SimpleTexture(16, 16, (byte) 200, (byte)  60, (byte)  60, (byte) 255)); // đỏ
        registry.register("APPLE", new SimpleTexture(16, 16, (byte) 220, (byte)  80, (byte)  30, (byte) 255)); // cam đỏ
        registry.register("ALGAE", new SimpleTexture(16, 16, (byte)  60, (byte) 190, (byte)  90, (byte) 255)); // xanh lá
        // registry.register("WATER", new SimpleTexture(16, 16, (byte)  80, (byte) 160, (byte) 240, (byte) 255)); // xanh nước
        // Obstacles — fallback khi resources_atlas.png chưa có
        registry.register("ROCK",  new SimpleTexture(16, 16, (byte) 140, (byte) 140, (byte) 140, (byte) 255)); // xám
        registry.register("BUSH",  new SimpleTexture(16, 16, (byte)  40, (byte) 140, (byte)  50, (byte) 255)); // xanh đậm
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


    private final Camera camera = new Camera(800, 800, 2500);
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
            if(event.getCode() == KeyCode.ESCAPE) {
                activeSpawningClass = null;
                activeSpawningObstacle = null;
                activeSpawningFood = null;
                isInKillMode = false;
                sceneCanvas.setCursor(Cursor.DEFAULT);
            }
            if(event.getCode() == KeyCode.SPACE) {
                isSpacePressed = true;
                sceneCanvas.setCursor(Cursor.OPEN_HAND);
            }
            if(event.getCode() == KeyCode.CONTROL) {
                isCtrlPressed = true;
                sceneCanvas.setCursor(Cursor.V_RESIZE);
            }
            if(event.getCode() == KeyCode.V) {
                ViewButtonClicked();
            }
            if(event.getCode() == KeyCode.P){
                pauseStatusChange();
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
            sceneCanvas.requestFocus();
            if(activeContextMenu != null && activeContextMenu.isShowing()) {
                activeContextMenu.hide();
            }
            if (activeSpawningClass != null) {
                if (event.getButton() == MouseButton.PRIMARY) {
                    double clickX = event.getX();
                    double clickY = event.getY();

                    // Convert screen space to world space coordinates
                    double worldX = camera.getTopLeftX() + (clickX / canvasWidth) * (camera.getBotRightX() - camera.getTopLeftX());
                    double worldY = camera.getTopLeftY() + (clickY / canvasHeight) * (camera.getBotRightY() - camera.getTopLeftY());

                    Vector2D spawnPos = new Vector2D((float) worldX, (float) worldY);

                    // Find target sub-environment containing this position
                    Environment targetEnv = null;
                    if (world != null) {
                        for (Environment sub : world.getSubEnvironments()) {
                            if (sub.getTerrain().containsPosition(spawnPos)) {
                                targetEnv = sub;
                                break;
                            }
                        }
                    }

                    if (targetEnv != null) {
                        try {
                            Organism newOrg = createOrganism(activeSpawningClass, spawnPos, targetEnv);
                            targetEnv.addOrganism(newOrg);
                            System.out.println("Spawned " + activeSpawningClass.getSimpleName() + " at " + spawnPos + " in environment " + targetEnv.getName());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }

                    activeSpawningClass = null;
                    sceneCanvas.setCursor(Cursor.DEFAULT);
                }
            } else if (activeSpawningObstacle != null) {
                if (event.getButton() == MouseButton.PRIMARY) {
                    double clickX = event.getX();
                    double clickY = event.getY();

                    // Convert screen space to world space coordinates
                    double worldX = camera.getTopLeftX() + (clickX / canvasWidth) * (camera.getBotRightX() - camera.getTopLeftX());
                    double worldY = camera.getTopLeftY() + (clickY / canvasHeight) * (camera.getBotRightY() - camera.getTopLeftY());

                    Vector2D spawnPos = new Vector2D((float) worldX, (float) worldY);

                    // Find target sub-environment containing this position
                    Environment targetEnv = null;
                    if (world != null) {
                        for (Environment sub : world.getSubEnvironments()) {
                            if (sub.getTerrain().containsPosition(spawnPos)) {
                                targetEnv = sub;
                                break;
                            }
                        }
                    }

                    if (targetEnv != null) {
                        try {
                            targetEnv.getResources().placeObstacle(spawnPos, activeSpawningObstacle);
                            System.out.println("Placed obstacle " + activeSpawningObstacle.name() + " at " + spawnPos + " in environment " + targetEnv.getName());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }

                    activeSpawningObstacle = null;
                    sceneCanvas.setCursor(Cursor.DEFAULT);
                }
            } else if (activeSpawningFood != null) {
                if (event.getButton() == MouseButton.PRIMARY) {
                    double clickX = event.getX();
                    double clickY = event.getY();

                    // Convert screen space to world space coordinates
                    double worldX = camera.getTopLeftX() + (clickX / canvasWidth) * (camera.getBotRightX() - camera.getTopLeftX());
                    double worldY = camera.getTopLeftY() + (clickY / canvasHeight) * (camera.getBotRightY() - camera.getTopLeftY());

                    Vector2D spawnPos = new Vector2D((float) worldX, (float) worldY);

                    // Find target sub-environment containing this position
                    Environment targetEnv = null;
                    if (world != null) {
                        for (Environment sub : world.getSubEnvironments()) {
                            if (sub.getTerrain().containsPosition(spawnPos)) {
                                targetEnv = sub;
                                break;
                            }
                        }
                    }

                    if (targetEnv != null) {
                        try {
                            float nutrition = 0f;
                            int expiry = 100;
                            if (activeSpawningFood == FoodType.MEAT) {
                                nutrition = AppConfig.getFloat("food.meat.nutritionalValue");
                                String expStr = AppConfig.get("food.meat.expiryTicks");
                                expiry = expStr != null ? Integer.parseInt(expStr.trim()) : 120;
                            } else if (activeSpawningFood == FoodType.APPLE) {
                                nutrition = AppConfig.getFloat("food.apple.nutritionalValue");
                                String expStr = AppConfig.get("food.apple.expiryTicks");
                                expiry = expStr != null ? Integer.parseInt(expStr.trim()) : 100;
                            } else if (activeSpawningFood == FoodType.ALGAE) {
                                nutrition = AppConfig.getFloat("food.algae.nutritionalValue");
                                String expStr = AppConfig.get("food.algae.expiryTicks");
                                expiry = expStr != null ? Integer.parseInt(expStr.trim()) : 90;
                            }
                            targetEnv.getResources().spawnFood(spawnPos, nutrition, activeSpawningFood, expiry);
                            System.out.println("Spawned food " + activeSpawningFood.name() + " at " + spawnPos + " in environment " + targetEnv.getName());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }

                    activeSpawningFood = null;
                    sceneCanvas.setCursor(Cursor.DEFAULT);
                }
            } else if (isInKillMode) {
                if (event.getButton() == MouseButton.PRIMARY) {
                    double clickX = event.getX();
                    double clickY = event.getY();

                    // Convert screen space to world space coordinates
                    double worldX = camera.getTopLeftX() + (clickX / canvasWidth) * (camera.getBotRightX() - camera.getTopLeftX());
                    double worldY = camera.getTopLeftY() + (clickY / canvasHeight) * (camera.getBotRightY() - camera.getTopLeftY());

                    ArrayList<Organism> selected = findOrganismAt(worldX, worldY);
                    if (selected != null) {
                        if (selected.size() == 1) {
                            selected.getFirst().decreaseHp(Float.MAX_VALUE);
                        } else {
                            activeContextMenu = new ContextMenu();
                            for (Organism organism : selected) {
                                MenuItem item = new MenuItem("Kill " + organism.getSpeciesName() + ": " + organism.getId());
                                item.setOnAction(e -> {
                                    organism.decreaseHp(Float.MAX_VALUE);
                                    activeContextMenu.hide();
                                });
                                activeContextMenu.getItems().add(item);
                            }
                            activeContextMenu.show(sceneCanvas, clickX, clickY);
                        }
                    }
                    isInKillMode = false;
                    sceneCanvas.setCursor(Cursor.DEFAULT);
                }
            } else if (event.getButton() == MouseButton.PRIMARY && !isSpacePressed) {

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
        if (world != null) {
            worldLock.readLock().lock();
            String seasonText;
            String weatherText;
            String daytimeText;
            int aliveCount = 0;
            try {
                seasonText = world.getTime().getCurrentSeason().name();
                weatherText = world.getTime().getCurrentWeather().name();
                daytimeText = world.getTime().isDaytime() ? "DAY" : "NIGHT";
                for (Environment sub : world.getSubEnvironments()) {
                    aliveCount += sub.getRegistry().getAllAlive(Organism.class).size();
                }
            } finally {
                worldLock.readLock().unlock();
            }
            final int count = aliveCount;
            Platform.runLater(() -> {
                if (SeasonLabel != null) SeasonLabel.setText(seasonText);
                if (WeatherLabel != null) WeatherLabel.setText(weatherText);
                if (DaytimeLabel != null) DaytimeLabel.setText(daytimeText);
                if (OrganismCount != null) OrganismCount.setText(String.valueOf(count));
            });
        }

        tickCount++;
        if (tickCount >= N_TICKS) {
            tickCount = 0;
            Organism selected = selectedOrganism;
            showEntityPanel(selected);
        }
    }

    @FXML
    public Label OrganismCount;
    @FXML
    public Slider simulationSpeed;
    private static volatile int tickRate = 5;
    public static int getTickRate() { return tickRate; }

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

    // Season
    @FXML
    public SVGPath PrevSeason;
    public Label SeasonLabel;
    public SVGPath NextSeason;
    public void prevSeasonClick(ActionEvent event) {
        changeSeason(false);
    }
    public void nextSeasonClick(ActionEvent event) {
        changeSeason(true);
    }
    private void changeSeason(boolean next) {
        if (world == null) return;
        worldLock.writeLock().lock();
        try {
            TimeComponent timeComp = world.getTime();
            int ticksPerSeason = timeComp.getTicksPerSeason();
            int cycleLength = ticksPerSeason * 3;
            int currentTick = timeComp.getCurrentTick();
            int rawTick = currentTick - TimeComponent.getSeasonOffset();
            Season currentSeason = timeComp.getCurrentSeason();
            int currentSeasonIdx = switch (currentSeason) {
                case NORMAL -> 0;
                case BREEDING -> 1;
                case DROUGHT -> 2;
            };
            int targetSeasonIdx;
            if (next) {
                targetSeasonIdx = (currentSeasonIdx + 1) % 3;
            } else {
                targetSeasonIdx = (currentSeasonIdx + 2) % 3;
            }
            int posInCycle = currentTick % cycleLength;
            int targetStartTick = targetSeasonIdx * ticksPerSeason;
            int delta = targetStartTick - posInCycle;
            if (delta <= 0) {
                delta += cycleLength;
            }
            TimeComponent.setSeasonOffset(TimeComponent.getSeasonOffset() + delta);
            int newTick = rawTick;
            timeComp.advance(newTick);
            for (Environment sub : world.getSubEnvironments()) {
                sub.getTime().advance(newTick);
            }
            String seasonText = timeComp.getCurrentSeason().name();
            Platform.runLater(() -> {
                if (SeasonLabel != null) {
                    SeasonLabel.setText(seasonText);
                }
            });
        } finally {
            worldLock.writeLock().unlock();
        }
    }

    // Weather
    @FXML
    public SVGPath PrevWeather;
    public Label WeatherLabel;
    public SVGPath NextWeather;
    public void prevWeatherClick(MouseEvent mouseEvent) {
        changeWeather(false);
    }
    public void nextWeatherClick(MouseEvent mouseEvent) {
        changeWeather(true);
    }
    private void changeWeather(boolean next) {
        if (world == null) return;
        worldLock.writeLock().lock();
        try {
            WeatherType[] values = WeatherType.values();
            WeatherType current = world.getTime().getCurrentWeather();
            int idx = 0;
            for (int i = 0; i < values.length; i++) {
                if (values[i] == current) { idx = i; break; }
            }
            int targetIdx = next ? (idx + 1) % values.length : (idx + values.length - 1) % values.length;
            WeatherType newWeather = values[targetIdx];

            TimeComponent.setWeatherOverride(newWeather);
            world.getTime().setCurrentWeather(newWeather);
            for (Environment sub : world.getSubEnvironments()) {
                sub.getTime().setCurrentWeather(newWeather);
            }

            Platform.runLater(() -> {
                if (WeatherLabel != null) WeatherLabel.setText(newWeather.name());
            });
        } finally {
            worldLock.writeLock().unlock();
        }
    }

    // Daytime
    @FXML
    public Label DaytimeLabel;
    public SVGPath NextDaytime;
    public void nextDaytimeClick(MouseEvent mouseEvent) {
        changeDaytime();
    }
    private void changeDaytime() {
        if (world == null) return;
        worldLock.writeLock().lock();
        try {
            TimeComponent timeComp = world.getTime();
            int ticksPerDayCycle = timeComp.getTicksPerDayCycle();
            int currentTick = timeComp.getCurrentTick();
            int rawTick = currentTick - TimeComponent.getSeasonOffset();
            int posInCycle = currentTick % ticksPerDayCycle;

            // Day → jump to night start (half-cycle mark); Night → jump to next day start
            int delta = timeComp.isDaytime()
                    ? ticksPerDayCycle / 2 - posInCycle
                    : ticksPerDayCycle - posInCycle;

            TimeComponent.setSeasonOffset(TimeComponent.getSeasonOffset() + delta);
            int newTick = rawTick;
            timeComp.advance(newTick);
            for (Environment sub : world.getSubEnvironments()) {
                sub.getTime().advance(newTick);
            }

            String daytimeText = timeComp.isDaytime() ? "DAY" : "NIGHT";
            Platform.runLater(() -> {
                if (DaytimeLabel != null) DaytimeLabel.setText(daytimeText);
            });
        } finally {
            worldLock.writeLock().unlock();
        }
    }

    // Khoi tao
    public void initialize() {
        instance = this;
        setupLWJGLCanvas();
        setupScaling();
        setupCameraEvents();
        hideEntityPanel();
        uiGroup.setPickOnBounds(false);
        if (simulationSpeed != null) {
            simulationSpeed.setMin(1);
            simulationSpeed.setMax(10);
            simulationSpeed.setValue(tickRate);
            simulationSpeed.setBlockIncrement(0.25);
            simulationSpeed.valueProperty().addListener((obs, oldVal, newVal) ->
                    tickRate = newVal.intValue());
        }
        if(uiGroup.getChildren().getFirst() instanceof AnchorPane) {
            ((AnchorPane)uiGroup.getChildren().getFirst()).setPickOnBounds(false);
        }
    }

    private Class<? extends Organism> activeSpawningClass = null;
    private ObstacleType activeSpawningObstacle = null;
    private FoodType activeSpawningFood = null;

    @FXML
    public void handleOrganismSelection(javafx.event.ActionEvent event) {
        activeSpawningObstacle = null;
        activeSpawningFood = null;
        isInKillMode = false;
        Button btn = (Button) event.getSource();
        String speciesName = btn.getText();
        switch (speciesName) {
            case "Tiger" -> activeSpawningClass = Tiger.class;
            case "Wolf" -> activeSpawningClass = Wolf.class;
            case "Hunter" -> activeSpawningClass = Hunter.class;
            case "Deer" -> activeSpawningClass = Deer.class;
            case "Elephant" -> activeSpawningClass = Elephant.class;
            case "Fish" -> activeSpawningClass = Fish.class;
            case "Rabbit" -> activeSpawningClass = Rabbit.class;
            case "AppleTree" -> activeSpawningClass = AppleTree.class;
            case "Grass" -> activeSpawningClass = Grass.class;
            case "TreeForest" -> activeSpawningClass = TreeForest.class;
            default -> activeSpawningClass = null;
        }
        if (activeSpawningClass != null) {
            sceneCanvas.setCursor(Cursor.CROSSHAIR);
        } else {
            sceneCanvas.setCursor(Cursor.DEFAULT);
        }
    }

    @FXML
    public void handleObstacleSelection(javafx.event.ActionEvent event) {
        activeSpawningClass = null;
        activeSpawningFood = null;
        isInKillMode = false;
        Button btn = (Button) event.getSource();
        String obstacleName = btn.getText();
        switch (obstacleName) {
            case "Rock" -> activeSpawningObstacle = ObstacleType.ROCK;
            case "Bush" -> activeSpawningObstacle = ObstacleType.BUSH;
            default -> activeSpawningObstacle = null;
        }
        if (activeSpawningObstacle != null) {
            sceneCanvas.setCursor(Cursor.CROSSHAIR);
        } else {
            sceneCanvas.setCursor(Cursor.DEFAULT);
        }
    }

    @FXML
    public void handleFoodsSelection(javafx.event.ActionEvent event) {
        activeSpawningClass = null;
        activeSpawningObstacle = null;
        isInKillMode = false;
        Button btn = (Button) event.getSource();
        String foodName = btn.getText();
        switch (foodName) {
            case "Meat" -> activeSpawningFood = FoodType.MEAT;
            case "Apple" -> activeSpawningFood = FoodType.APPLE;
            case "Algae" -> activeSpawningFood = FoodType.ALGAE;
            default -> activeSpawningFood = null;
        }
        if (activeSpawningFood != null) {
            sceneCanvas.setCursor(Cursor.CROSSHAIR);
        } else {
            sceneCanvas.setCursor(Cursor.DEFAULT);
        }
    }

    private boolean isInKillMode = false;

    @FXML
    public void handleKillButton(javafx.event.ActionEvent event) {
        activeSpawningClass = null;
        activeSpawningObstacle = null;
        activeSpawningFood = null;
        isInKillMode = true;
        sceneCanvas.setCursor(Cursor.CROSSHAIR);
    }

    @FXML
    public void handleDestroyButton(javafx.event.ActionEvent event) {
        if (world != null) {
            for (Environment sub : world.getSubEnvironments()) {
                List<Organism> allAlive = new ArrayList<>(sub.getRegistry().getAllAlive(Organism.class));
                for (Organism o : allAlive) {
                    o.decreaseHp(Float.MAX_VALUE);
                }
            }
            System.out.println("Destroyed all organisms in the system.");
        }
    }

    private Organism createOrganism(Class<?> clazz, Vector2D pos, Environment env) throws Exception {
        if (clazz == AppleTree.class) {
            return AppleTree.create(pos, env);
        } else if (clazz == Grass.class) {
            return Grass.create(pos, env);
        } else if (clazz == TreeForest.class) {
            return TreeForest.create(pos, env);
        } else {
            String species = clazz.getSimpleName().toLowerCase();
            Random random = new Random();
            float baseHp = AppConfig.getFloat("animal." + species + ".maxHp");
            float baseNutrition = AppConfig.getFloat("animal." + species + ".nutrition");
            float hungerDecay = AppConfig.getFloat("animal." + species + ".hungerDecay");
            float thirstDecay = AppConfig.getFloat("animal." + species + ".thirstDecay");

            float baseAge = AppConfig.getFloat("animal." + species + ".maxAge");
            float baseSize = AppConfig.getFloat("animal." + species + ".maxSize");

            float randomFactorHp = 0.85f + (random.nextFloat() * 0.3f);
            float randomFactorAge = 0.85f + (random.nextFloat() * 0.3f);
            float randomFactorSize = 0.85f + (random.nextFloat() * 0.3f);

            float finalHp = baseHp * randomFactorHp;
            float finalMaxAge = baseAge * randomFactorAge;
            float finalMaxSize = baseSize * randomFactorSize;

            float startAge = finalMaxAge * 0.4f;
            GrowthComponent growth = new GrowthComponent(finalMaxAge, finalMaxSize, 0.2f, 0.7f, startAge);
            SurvivalStatsComponent stats = new SurvivalStatsComponent(finalHp, baseNutrition, hungerDecay, thirstDecay);

            List<TerrainType> survivableTerrains = species.equals("fish")
                    ? List.of(TerrainType.DEEP_WATER)
                    : List.of(TerrainType.GRASSLAND, TerrainType.FOREST, TerrainType.MUD);

            AdaptabilityComponent adapt = new AdaptabilityComponent(
                    survivableTerrains,
                    new ValueRange(15f, 35f),
                    new ValueRange(0f, 45f),
                    new ValueRange(-60f, -10f)
            );

            String id = clazz.getSimpleName().toUpperCase() + "_" + System.nanoTime();

            return (Organism) clazz.getDeclaredConstructor(
                    String.class, String.class, Vector2D.class, Environment.class,
                    GrowthComponent.class, SurvivalStatsComponent.class, AdaptabilityComponent.class
            ).newInstance(id, clazz.getSimpleName(), pos, env, growth, stats, adapt);
        }
    }

}
