package wildlife.view.ui.control;

import javafx.scene.Cursor;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import wildlife.model.environment.CompositeMap;
import wildlife.model.environment.Environment;
import wildlife.model.environment.enums.FoodType;
import wildlife.model.environment.enums.ObstacleType;
import wildlife.model.organism.Organism;
import wildlife.util.AppConfig;
import wildlife.util.Vector2D;
import wildlife.view.renderer.utils.Camera;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class SimulationInteractionController {
    private final AnchorPane sceneCanvas;
    private final Camera camera;
    private final IntSupplier canvasWidth;
    private final IntSupplier canvasHeight;
    private final Supplier<CompositeMap> worldSupplier;
    private final Supplier<Map<String, List<Organism>>> spatialGridSupplier;
    private final BiConsumer<Organism, Vector2D> selectionHandler;
    private final Runnable toggleViewMode;
    private final Runnable togglePause;
    private final int gridCellSize;

    private boolean spacePressed = false;
    private boolean ctrlPressed = false;
    private double lastMouseX;
    private double lastMouseY;
    private ContextMenu activeContextMenu;
    private Class<? extends Organism> activeSpawningClass = null;
    private ObstacleType activeSpawningObstacle = null;
    private FoodType activeSpawningFood = null;
    private boolean killMode = false;

    public SimulationInteractionController(
            AnchorPane sceneCanvas,
            Camera camera,
            IntSupplier canvasWidth,
            IntSupplier canvasHeight,
            Supplier<CompositeMap> worldSupplier,
            Supplier<Map<String, List<Organism>>> spatialGridSupplier,
            int gridCellSize,
            BiConsumer<Organism, Vector2D> selectionHandler,
            Runnable toggleViewMode,
            Runnable togglePause
    ) {
        this.sceneCanvas = sceneCanvas;
        this.camera = camera;
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
        this.worldSupplier = worldSupplier;
        this.spatialGridSupplier = spatialGridSupplier;
        this.gridCellSize = gridCellSize;
        this.selectionHandler = selectionHandler;
        this.toggleViewMode = toggleViewMode;
        this.togglePause = togglePause;
    }

    public void installCanvasHandlers() {
        sceneCanvas.setFocusTraversable(true);
        sceneCanvas.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                sceneCanvas.setCursor(Cursor.DEFAULT);
            }
        });

        sceneCanvas.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                clearActiveTool();
            }
            if (event.getCode() == KeyCode.SPACE) {
                spacePressed = true;
                sceneCanvas.setCursor(Cursor.OPEN_HAND);
            }
            if (event.getCode() == KeyCode.CONTROL) {
                ctrlPressed = true;
                sceneCanvas.setCursor(Cursor.V_RESIZE);
            }
            if (event.getCode() == KeyCode.V) {
                toggleViewMode.run();
            }
            if (event.getCode() == KeyCode.P) {
                togglePause.run();
            }
        });

        sceneCanvas.setOnKeyReleased(event -> {
            if (event.getCode() == KeyCode.SPACE) {
                spacePressed = false;
            }
            if (event.getCode() == KeyCode.CONTROL) {
                ctrlPressed = false;
            }
            sceneCanvas.setCursor(Cursor.DEFAULT);
        });

        sceneCanvas.setOnScroll(event -> {
            if (ctrlPressed) {
                Vector2D beforeZoom = toWorldPosition(event.getX(), event.getY());
                int zoomFactor = (int) (event.getDeltaY() * 2);
                camera.zoom(-zoomFactor);
                Vector2D afterZoom = toWorldPosition(event.getX(), event.getY());
                camera.pan(
                        (int) (beforeZoom.getX() - afterZoom.getX()),
                        (int) (beforeZoom.getY() - afterZoom.getY())
                );
            } else {
                camera.pan(0, -(int) event.getDeltaY());
            }
        });

        sceneCanvas.setOnMousePressed(event -> {
            if (spacePressed) {
                sceneCanvas.requestFocus();
                sceneCanvas.setCursor(Cursor.CLOSED_HAND);
                lastMouseX = event.getX();
                lastMouseY = event.getY();
            }
        });

        sceneCanvas.setOnMouseDragged(event -> {
            if (spacePressed) {
                double currentX = event.getX();
                double currentY = event.getY();
                double deltaX = currentX - lastMouseX;
                double deltaY = currentY - lastMouseY;

                double scale = (double) (camera.getBotRightY() - camera.getTopLeftY()) / canvasHeight.getAsInt();
                camera.pan((int) (-deltaX * scale), (int) (-deltaY * scale));

                lastMouseX = currentX;
                lastMouseY = currentY;
            }
        });

        sceneCanvas.setOnMouseClicked(event -> {
            sceneCanvas.requestFocus();
            hideContextMenu();
            Vector2D worldPos = toWorldPosition(event.getX(), event.getY());

            if (activeSpawningClass != null) {
                spawnOrganism(event.getButton(), worldPos);
            } else if (activeSpawningObstacle != null) {
                placeObstacle(event.getButton(), worldPos);
            } else if (activeSpawningFood != null) {
                spawnFood(event.getButton(), worldPos);
            } else if (killMode) {
                killAt(event.getButton(), event.getX(), event.getY(), worldPos);
            } else if (event.getButton() == MouseButton.PRIMARY && !spacePressed) {
                selectAt(event.getX(), event.getY(), worldPos);
            }
        });
    }

    public void activateOrganismSpawn(String speciesName) {
        activeSpawningObstacle = null;
        activeSpawningFood = null;
        killMode = false;
        activeSpawningClass = InteractionOrganismFactory.speciesClass(speciesName);
        sceneCanvas.setCursor(activeSpawningClass != null ? Cursor.CROSSHAIR : Cursor.DEFAULT);
    }

    public void activateObstacleSpawn(String obstacleName) {
        activeSpawningClass = null;
        activeSpawningFood = null;
        killMode = false;
        activeSpawningObstacle = switch (obstacleName) {
            case "Rock" -> ObstacleType.ROCK;
            case "Bush" -> ObstacleType.BUSH;
            default -> null;
        };
        sceneCanvas.setCursor(activeSpawningObstacle != null ? Cursor.CROSSHAIR : Cursor.DEFAULT);
    }

    public void activateFoodSpawn(String foodName) {
        activeSpawningClass = null;
        activeSpawningObstacle = null;
        killMode = false;
        activeSpawningFood = switch (foodName) {
            case "Meat" -> FoodType.MEAT;
            case "Apple" -> FoodType.APPLE;
            case "Algae" -> FoodType.ALGAE;
            default -> null;
        };
        sceneCanvas.setCursor(activeSpawningFood != null ? Cursor.CROSSHAIR : Cursor.DEFAULT);
    }

    public void activateKillMode() {
        activeSpawningClass = null;
        activeSpawningObstacle = null;
        activeSpawningFood = null;
        killMode = true;
        sceneCanvas.setCursor(Cursor.CROSSHAIR);
    }

    public void destroyAllOrganisms() {
        CompositeMap world = worldSupplier.get();
        if (world == null) return;
        for (Environment sub : world.getSubEnvironments()) {
            List<Organism> allAlive = new ArrayList<>(sub.getRegistry().getAllAlive(Organism.class));
            for (Organism organism : allAlive) {
                organism.decreaseHp(Float.MAX_VALUE);
            }
        }
        System.out.println("Destroyed all organisms in the system.");
    }

    private void clearActiveTool() {
        activeSpawningClass = null;
        activeSpawningObstacle = null;
        activeSpawningFood = null;
        killMode = false;
        sceneCanvas.setCursor(Cursor.DEFAULT);
    }

    private void hideContextMenu() {
        if (activeContextMenu != null && activeContextMenu.isShowing()) {
            activeContextMenu.hide();
        }
    }

    private Vector2D toWorldPosition(double screenX, double screenY) {
        double worldX = camera.getTopLeftX()
                + (screenX / canvasWidth.getAsInt()) * (camera.getBotRightX() - camera.getTopLeftX());
        double worldY = camera.getTopLeftY()
                + (screenY / canvasHeight.getAsInt()) * (camera.getBotRightY() - camera.getTopLeftY());
        return new Vector2D((float) worldX, (float) worldY);
    }

    private Environment findTargetEnvironment(Vector2D position) {
        CompositeMap world = worldSupplier.get();
        if (world == null) return null;
        for (Environment sub : world.getSubEnvironments()) {
            if (sub.getTerrain().containsPosition(position)) {
                return sub;
            }
        }
        return null;
    }

    private void spawnOrganism(MouseButton button, Vector2D spawnPos) {
        if (button != MouseButton.PRIMARY) return;
        Environment targetEnv = findTargetEnvironment(spawnPos);
        if (targetEnv != null) {
            try {
                Organism newOrg = InteractionOrganismFactory.create(activeSpawningClass, spawnPos, targetEnv);
                targetEnv.addOrganism(newOrg);
                System.out.println("Spawned " + activeSpawningClass.getSimpleName()
                        + " at " + spawnPos + " in environment " + targetEnv.getName());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        clearActiveTool();
    }

    private void placeObstacle(MouseButton button, Vector2D spawnPos) {
        if (button != MouseButton.PRIMARY) return;
        Environment targetEnv = findTargetEnvironment(spawnPos);
        if (targetEnv != null) {
            try {
                targetEnv.getResources().placeObstacle(spawnPos, activeSpawningObstacle);
                System.out.println("Placed obstacle " + activeSpawningObstacle.name()
                        + " at " + spawnPos + " in environment " + targetEnv.getName());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        clearActiveTool();
    }

    private void spawnFood(MouseButton button, Vector2D spawnPos) {
        if (button != MouseButton.PRIMARY) return;
        Environment targetEnv = findTargetEnvironment(spawnPos);
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
                System.out.println("Spawned food " + activeSpawningFood.name()
                        + " at " + spawnPos + " in environment " + targetEnv.getName());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        clearActiveTool();
    }

    private void killAt(MouseButton button, double screenX, double screenY, Vector2D worldPos) {
        if (button != MouseButton.PRIMARY) return;
        ArrayList<Organism> selected = findOrganismAt(worldPos);
        if (selected != null) {
            if (selected.size() == 1) {
                selected.get(0).decreaseHp(Float.MAX_VALUE);
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
                activeContextMenu.show(sceneCanvas, screenX, screenY);
            }
        }
        clearActiveTool();
    }

    private void selectAt(double screenX, double screenY, Vector2D worldPos) {
        ArrayList<Organism> selected = findOrganismAt(worldPos);
        if (selected != null) {
            if (selected.size() == 1) {
                selectionHandler.accept(selected.get(0), worldPos);
            } else {
                activeContextMenu = new ContextMenu();
                for (Organism organism : selected) {
                    MenuItem item = new MenuItem(organism.getSpeciesName() + ": " + organism.getId());
                    item.setOnAction(e -> {
                        selectionHandler.accept(organism, worldPos);
                        activeContextMenu.hide();
                    });
                    activeContextMenu.getItems().add(item);
                }
                activeContextMenu.show(sceneCanvas, screenX, screenY);
            }
        } else {
            selectionHandler.accept(null, worldPos);
        }
    }

    private ArrayList<Organism> findOrganismAt(Vector2D position) {
        Map<String, List<Organism>> spatialGrid = spatialGridSupplier.get();
        if (spatialGrid == null || spatialGrid.isEmpty()) return null;

        ArrayList<Organism> result = null;
        int centerCellX = (int) Math.floor(position.getX() / gridCellSize);
        int centerCellY = (int) Math.floor(position.getY() / gridCellSize);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                String key = (centerCellX + dx) + "," + (centerCellY + dy);
                List<Organism> cellOrganisms = spatialGrid.get(key);

                if (cellOrganisms != null) {
                    for (Organism organism : cellOrganisms) {
                        if (organism.isAlive()) {
                            double ox = organism.getPosition().getX();
                            double oy = organism.getPosition().getY();
                            if (position.getX() >= ox - 16 && position.getX() <= ox + 16
                                    && position.getY() >= oy - 16 && position.getY() <= oy + 16) {
                                if (result == null) {
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
}
