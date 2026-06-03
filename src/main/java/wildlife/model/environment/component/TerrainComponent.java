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
    /**
     * Tìm tọa độ của ô địa hình thuộc loại chỉ định gần với vị trí hiện tại nhất.
     * * @param currentPos Vị trí hiện tại
     * @param targetType Loại địa hình cần tìm (VD: Nước nông)
     * @return Tọa độ Vector2D của ô gần nhất, hoặc null nếu không tìm thấy
     */
    public Vector2D findNearestTile(Vector2D currentPos, wildlife.model.environment.enums.TerrainType targetType) {
        Vector2D nearestPos = null;
        float minDistance = Float.MAX_VALUE;

        // Duyệt qua danh sách các ô địa hình điểm xuyết để tìm ô phù hợp
        for (Map.Entry<TileIndex, wildlife.model.environment.enums.TerrainType> entry : customTiles.entrySet()) {
            if (entry.getValue() == targetType) {
                // Tính toạ độ thực của ô đó (lấy tâm ô)
                float tileCenterX = entry.getKey().x() * TILE_SIZE + (TILE_SIZE / 2);
                float tileCenterY = entry.getKey().y() * TILE_SIZE + (TILE_SIZE / 2);
                Vector2D tilePos = new Vector2D(tileCenterX, tileCenterY);

                // Giả định class Vector2D của cậu có hàm distanceTo()
                float dist = currentPos.distanceTo(tilePos);
                if (dist < minDistance) {
                    minDistance = dist;
                    nearestPos = tilePos;
                }
            }
        }
        return nearestPos;
    }
    /**
     * Trả về hệ số tốc độ di chuyển dựa trên địa hình và loài sinh vật.
     * @param pos Tọa độ sinh vật đang đứng
     * @param species Phân loại sinh vật (Ví dụ: "Wolf", "Deer", "Elephant")
     * @return Hệ số tốc độ (1.0 = bình thường, 0.5 = giảm nửa tốc độ)
     */
    public float getMovementSpeedModifier(Vector2D pos, String species) {
        TerrainType terrain = getTerrainAt(pos);

        // Voi (Động vật đầu bảng) càn lướt mọi địa hình, không bị giảm tốc
        if (species.equalsIgnoreCase("Elephant")) {
            return 1.0f; 
        }

        switch (terrain) {
            case MUD:
                // Bùn lầy làm chậm Hươu và Sói đáng kể
                return 0.5f; 
                
            case SHALLOW_WATER:
                // Nước nông làm chậm vừa phải
                return 0.7f;
                
            case FOREST:
                // Rừng rậm: Sói di chuyển nhanh hơn Hươu một chút nhờ bản năng
                if (species.equalsIgnoreCase("Wolf")) return 0.9f;
                return 0.8f;
                
            default: // GRASSLAND
                return 1.0f; // Tốc độ tối đa trên đồng cỏ
        }
    }
}