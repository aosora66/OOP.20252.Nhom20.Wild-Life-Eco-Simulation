package wildlife.model.environment;

import wildlife.model.dto.RenderData;
import wildlife.model.environment.component.OrganismRegistry;
import wildlife.model.environment.component.ResourceManager;
import wildlife.model.environment.component.TerrainComponent;
import wildlife.model.environment.component.TimeComponent;
import wildlife.model.environment.enums.TerrainType;
import wildlife.model.environment.event.EnvironmentEventPublisher;
import wildlife.model.organism.Organism;
import wildlife.util.AppConfig;
import wildlife.util.RectBoundary;
import wildlife.util.Vector2D;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Composite world map.
 *
 * This class is the coordinator for positioned sub-environments. Each child
 * environment owns its own TerrainComponent boundary, and that boundary is used
 * as the child region's footprint on the global map.
 */
public class CompositeMap extends Environment {

    private static final float DEFAULT_WORLD_SCALE = 0.45f;
    private static final float DEFAULT_LOCAL_SCALE = 1.0f;
    private static final float WATER_LOCAL_SCALE = 2.0f;

    private final List<MapRegion> regions;
    private ViewMode viewMode;
    private Environment focusedEnvironment;

    public enum ViewMode {
        WORLD,
        LOCAL
    }

    /**
     * A positioned child environment on the global map.
     *
     * The actual position/shape is still stored in the child's TerrainComponent
     * boundary. The scales here tell ViewLogic how large organisms should be
     * drawn in world view versus local view.
     */
    public static final class MapRegion {
        private final Environment environment;
        private final float worldScale;
        private final float localScale;

        private MapRegion(Environment environment, float worldScale, float localScale) {
            if (environment == null) {
                throw new IllegalArgumentException("Environment cannot be null");
            }
            if (worldScale <= 0 || localScale <= 0) {
                throw new IllegalArgumentException("Render scales must be positive");
            }

            this.environment = environment;
            this.worldScale = worldScale;
            this.localScale = localScale;
        }

        public boolean contains(Vector2D position) {
            return environment.getTerrain().containsPosition(position);
        }

        private float scaleFor(ViewMode mode, Environment focusedEnvironment) {
            if (mode == ViewMode.LOCAL && environment == focusedEnvironment) {
                return localScale;
            }
            return worldScale;
        }

        public Environment getEnvironment() {
            return environment;
        }

        public float getWorldScale() {
            return worldScale;
        }

        public float getLocalScale() {
            return localScale;
        }
    }

    /**
     * Render snapshot enriched with composite-map metadata.
     */
    public static final class MapRenderData {
        private final RenderData renderData;
        private final String environmentId;
        private final String environmentName;
        private final float displayScale;
        private final boolean focused;

        private MapRenderData(RenderData renderData,
                              Environment environment,
                              float displayScale,
                              boolean focused) {
            this.renderData = renderData;
            this.environmentId = environment.getId();
            this.environmentName = environment.getName();
            this.displayScale = displayScale;
            this.focused = focused;
        }

        public RenderData getRenderData() {
            return renderData;
        }

        public String getEnvironmentId() {
            return environmentId;
        }

        public String getEnvironmentName() {
            return environmentName;
        }

        public float getDisplayScale() {
            return displayScale;
        }

        public boolean isFocused() {
            return focused;
        }
    }

    public CompositeMap(String id, String name) {
        super(
                id,
                name,
                0f,
                0f,
                1f,
                new TimeComponent(
                        AppConfig.getInt("environment.time.ticksPerDayCycle"),
                        AppConfig.getInt("environment.time.ticksPerSeason")
                ),
                new TerrainComponent(new RectBoundary(0, 1000, 0, 1000), TerrainType.GRASSLAND),
                new OrganismRegistry(),
                new ResourceManager(),
                new EnvironmentEventPublisher("sounds/world_ambient.wav")
        );

        this.regions = new ArrayList<>();
        this.viewMode = ViewMode.WORLD;
        this.focusedEnvironment = null;
    }

    /**
     * Adds a child environment using default scales. Water-heavy regions get a
     * larger local scale so lake organisms can be inspected more clearly.
     */
    public void addSubEnvironment(Environment env) {
        float localScale = env != null && env.getTerrain().containsTerrain(TerrainType.DEEP_WATER)
                ? WATER_LOCAL_SCALE
                : DEFAULT_LOCAL_SCALE;
        addSubEnvironment(env, DEFAULT_WORLD_SCALE, localScale);
    }

    /**
     * Adds a child environment with explicit render scales.
     */
    public void addSubEnvironment(Environment env, float worldScale, float localScale) {
        if (env == null) {
            throw new IllegalArgumentException("Cannot add null environment");
        }
        if (env == this) {
            throw new IllegalArgumentException("Cannot add CompositeMap to itself");
        }
        if (findSubEnvironment(env.getId()) != null) {
            throw new IllegalArgumentException("Duplicate environment id: " + env.getId());
        }

        regions.add(new MapRegion(env, worldScale, localScale));
    }

    public boolean removeSubEnvironment(String id) {
        if (focusedEnvironment != null && focusedEnvironment.getId().equals(id)) {
            focusedEnvironment = null;
            viewMode = ViewMode.WORLD;
        }
        return regions.removeIf(region -> region.getEnvironment().getId().equals(id));
    }

    /**
     * Updates all positioned child environments.
     */
    @Override
    public final void updateEnvironment(int currentTick) {
        for (MapRegion region : regions) {
            region.getEnvironment().updateEnvironment(currentTick);
        }
    }

    /**
     * Finds the child environment whose terrain boundary contains this position.
     */
    public Environment findEnvironmentAt(Vector2D position) {
        if (position == null) {
            return null;
        }
        for (MapRegion region : regions) {
            if (region.contains(position)) {
                return region.getEnvironment();
            }
        }
        return null;
    }

    /**
     * Moves an organism across the global map and transfers ownership to the
     * target child environment when it crosses a region boundary.
     *
     * @return true when the move is accepted, false when the target is outside
     *         the composite map or blocked by terrain/obstacles.
     */
    public boolean moveOrganism(Organism organism, Vector2D newPosition) {
        if (organism == null || newPosition == null) {
            return false;
        }

        Environment target = findEnvironmentAt(newPosition);
        if (target == null) {
            return false;
        }
        if (!target.isPositionPassable(newPosition, organism.getSpeciesName())) {
            return false;
        }

        Environment current = findEnvironmentContainingOrganism(organism.getId());
        if (current != target) {
            if (current != null) {
                current.getRegistry().remove(organism.getId());
            }
            if (!target.getRegistry().findById(organism.getId()).isPresent()) {
                target.addOrganism(organism);
            }
        }

        organism.setPosition(newPosition);
        organism.setCurrentTerrain(target.getTerrain().getTerrainAt(newPosition));
        return true;
    }

    public void showWorldView() {
        viewMode = ViewMode.WORLD;
        focusedEnvironment = null;
    }

    public boolean focusEnvironment(String id) {
        Environment env = findSubEnvironment(id);
        if (env == null) {
            return false;
        }
        viewMode = ViewMode.LOCAL;
        focusedEnvironment = env;
        return true;
    }

    public boolean focusEnvironmentAt(Vector2D position) {
        Environment env = findEnvironmentAt(position);
        if (env == null) {
            return false;
        }
        viewMode = ViewMode.LOCAL;
        focusedEnvironment = env;
        return true;
    }

    /**
     * Old compatibility API: returns only organism render data.
     */
    public List<RenderData> getAllRenderSnapshots() {
        List<RenderData> all = new ArrayList<>();
        for (MapRegion region : regions) {
            all.addAll(region.getEnvironment().getRenderSnapshot());
        }
        return Collections.unmodifiableList(all);
    }

    /**
     * Preferred API for the GUI: returns organism render data plus region and
     * scale information for world/local drawing modes.
     */
    public List<MapRenderData> getCompositeRenderSnapshot() {
        List<MapRenderData> all = new ArrayList<>();
        for (MapRegion region : regions) {
            Environment env = region.getEnvironment();
            float scale = region.scaleFor(viewMode, focusedEnvironment);
            boolean focused = viewMode == ViewMode.LOCAL && env == focusedEnvironment;
            for (RenderData renderData : env.getRenderSnapshot()) {
                all.add(new MapRenderData(renderData, env, scale, focused));
            }
        }
        return Collections.unmodifiableList(all);
    }

    public Environment findSubEnvironment(String id) {
        for (MapRegion region : regions) {
            Environment env = region.getEnvironment();
            if (env.getId().equals(id)) {
                return env;
            }
        }
        return null;
    }

    public int getTotalOrganismCount() {
        int total = 0;
        for (MapRegion region : regions) {
            total += region.getEnvironment().getRegistry().getAllAlive().size();
        }
        return total;
    }

    @Override
    protected void applySeasonEffect() {
        // CompositeMap delegates season effects to child environments.
    }

    @Override
    protected void applyWeatherEffect() {
        // CompositeMap delegates weather effects to child environments.
    }

    @Override
    protected void generateNaturalResources() {
        // CompositeMap delegates resource generation to child environments.
    }

    public List<Environment> getSubEnvironments() {
        List<Environment> environments = new ArrayList<>();
        for (MapRegion region : regions) {
            environments.add(region.getEnvironment());
        }
        return Collections.unmodifiableList(environments);
    }

    public List<MapRegion> getRegions() {
        return Collections.unmodifiableList(regions);
    }

    public ViewMode getViewMode() {
        return viewMode;
    }

    public Environment getFocusedEnvironment() {
        return focusedEnvironment;
    }

    private Environment findEnvironmentContainingOrganism(String organismId) {
        for (MapRegion region : regions) {
            Environment env = region.getEnvironment();
            if (env.getRegistry().findById(organismId).isPresent()) {
                return env;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("[CompositeMap | id=%s | regions=%d | totalOrganisms=%d | view=%s]",
                getId(), regions.size(), getTotalOrganismCount(), viewMode);
    }
}
