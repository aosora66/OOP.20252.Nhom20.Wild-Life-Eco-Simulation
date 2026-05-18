package wildlife.model.environment.component;

import wildlife.model.environment.enums.TerrainType;
import wildlife.util.AppConfig;
import wildlife.util.Boundary;
import wildlife.util.Vector2D;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Component quản lý địa hình của một môi trường.
 * Đã nâng cấp sử dụng Strategy Pattern (Boundary) và Hybrid Tilemap.
 */
public class TerrainComponent {

    // ----------------------------------------------------------------
    //  Hằng số hệ thống
    // ----------------------------------------------------------------
    private static final float VISIBILITY_REDUCED = AppConfig.getFloat("environment.terrain.visibility.reduced");
    private static final float VISIBILITY_NORMAL  = AppConfig.getFloat("environment.terrain.visibility.normal");
    private static final Set<TerrainType> LOW_VISIBILITY_TERRAINS = Set.of(
            TerrainType.FOREST, TerrainType.DEEP_WATER
    );

    private static final Set<TerrainType> DEFAULT_IMPASSABLE = Set.of(
            TerrainType.DEEP_WATER, TerrainType.CLIFF
    );

    // Kích thước 1 ô vuông ảo để tránh lỗi số thực float khi dùng HashMap
    private record TileIndex(int x, int y) {}
    private static final float TILE_SIZE = AppConfig.getFloat("environment.terrain.tileSize");

    // ----------------------------------------------------------------
    //  Trạng thái nội tại
    // ----------------------------------------------------------------
    private final Boundary boundary; // Ranh giới môi trường (Tròn hoặc Chữ nhật)
    private final TerrainType defaultTerrain; // Loại đất nền trải khắp vùng

    private final Map<TileIndex, TerrainType> customTiles; // Các ô địa hình điểm xuyết
    private final Set<TerrainType> containedTerrains; // Thống kê các loại đất đang có

    // ----------------------------------------------------------------
    //  Constructor
    // ----------------------------------------------------------------
    public TerrainComponent(Boundary boundary, TerrainType defaultTerrain) {
        this.boundary = boundary;
        this.defaultTerrain = defaultTerrain;
        this.customTiles = new HashMap<>();
        this.containedTerrains = new HashSet<>();

        // Thêm nền mặc định vào danh sách thống kê
        this.containedTerrains.add(defaultTerrain);
    }

    // ----------------------------------------------------------------
    //  Phương thức thêm địa hình điểm xuyết
    // ----------------------------------------------------------------
    /**
     * "Vẽ" thêm một loại địa hình đặc biệt vào bên trong môi trường.
     * @param pos Tọa độ của điểm cần vẽ
     * @param type Loại địa hình
     */
    public void addCustomTerrain(Vector2D pos, TerrainType type) {
        if (containsPosition(pos)) {
            int tileX = (int) (pos.getX() / TILE_SIZE);
            int tileY = (int) (pos.getY() / TILE_SIZE);
            customTiles.put(new TileIndex(tileX, tileY), type);
            containedTerrains.add(type);
        }
    }

    // ----------------------------------------------------------------
    //  Truy vấn
    // ----------------------------------------------------------------

    public boolean containsPosition(Vector2D pos) {
        return boundary.contains(pos);
    }

    public TerrainType getTerrainAt(Vector2D pos) {
        // 1. Rớt ra ngoài ranh giới vùng -> Coi như đụng vách núi
        if (!containsPosition(pos)) {
            return TerrainType.CLIFF;
        }

        // 2. Ép tọa độ thực tế thành tọa độ ô vuông
        int tileX = (int) (pos.getX() / TILE_SIZE);
        int tileY = (int) (pos.getY() / TILE_SIZE);

        // 3. Ưu tiên ô điểm xuyết, nếu không có thì trả về nền mặc định
        return customTiles.getOrDefault(new TileIndex(tileX, tileY), defaultTerrain);
    }

    public boolean isPassable(Vector2D pos, String species) {
        TerrainType terrain = getTerrainAt(pos);
        // Sau này bạn có thể thêm logic: nếu species = "Vit" thì DEEP_WATER = passable
        return !DEFAULT_IMPASSABLE.contains(terrain);
    }

    public float getVisibilityModifier(Vector2D pos) {
        TerrainType terrain = getTerrainAt(pos);
        return LOW_VISIBILITY_TERRAINS.contains(terrain)
                ? VISIBILITY_REDUCED
                : VISIBILITY_NORMAL;
    }

    public boolean containsTerrain(TerrainType type) {
        return containedTerrains.contains(type);
    }

    // ----------------------------------------------------------------
    //  Getters
    // ----------------------------------------------------------------
    public Set<TerrainType> getContainedTerrains() {
        return Collections.unmodifiableSet(containedTerrains);
    }
}