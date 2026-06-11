package wildlife.model.environment.component;

import wildlife.model.environment.enums.TerrainType;
import wildlife.model.organism.Organism;
import wildlife.util.AppConfig;
import wildlife.util.Boundary;
import wildlife.util.Vector2D;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
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

        // 1. Nếu không xác định được loài (phòng hờ lỗi null), áp dụng luật mặc định:
        if (species == null) {
            return terrain != TerrainType.DEEP_WATER && terrain != TerrainType.CLIFF;
        }

        // 2. Xác định xem sinh vật có phải là hệ dưới nước không
        boolean isAquatic = species.equalsIgnoreCase("Fish") || species.equalsIgnoreCase("Cá");

        // --- NHÁNH DÀNH CHO CÁ ---
        if (isAquatic) {
            // Cá chỉ bơi được ở nước sâu và nước nông
            return terrain == TerrainType.DEEP_WATER || terrain == TerrainType.SHALLOW_WATER;
        }

        // --- NHÁNH DÀNH CHO ĐỘNG VẬT TRÊN CẠN ---
        switch (terrain) {
            case DEEP_WATER:
            case CLIFF:
                return false; // Cấm tuyệt đối xuống nước sâu và leo vách đá vách núi
                
            case GRASSLAND:
            case FOREST:
            case ICE:
            case SHALLOW_WATER:
                return true;  // Trên cạn và lội nước nông thì thoải mái
                
            default:
                return false;
        }
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
/**
     * Tìm tọa độ của ô địa hình thuộc loại chỉ định gần với vị trí hiện tại nhất.
     */
    public Vector2D findNearestTile(Vector2D currentPos, wildlife.model.environment.enums.TerrainType targetType) {
        Vector2D nearestPos = null;
        float minDistance = Float.MAX_VALUE;

        // --- BƯỚC 1: KIỂM TRA NỀN MẶC ĐỊNH (DEFAULT TERRAIN) ---
        if (this.defaultTerrain == targetType) {
            if (containsPosition(currentPos) && getTerrainAt(currentPos) == targetType) {
                // Nếu sinh vật đang đứng ngay trên loại đất/nước đó rồi thì khỏi tìm đâu xa
                return currentPos; 
            } else {
                // Lấy tọa độ trung tâm thực tế của ranh giới vùng làm đích đến (tổng quát cho mọi hình dạng)
                nearestPos = boundary.getCenter();
                minDistance = currentPos.distanceTo(nearestPos);
            }
        }

        // --- BƯỚC 2: TÌM TRONG CÁC Ô ĐIỂM XUYẾT (CUSTOM TILES) NHƯ CŨ ---
        for (Map.Entry<TileIndex, wildlife.model.environment.enums.TerrainType> entry : customTiles.entrySet()) {
            if (entry.getValue() == targetType) {
                float tileCenterX = entry.getKey().x() * TILE_SIZE + (TILE_SIZE / 2);
                float tileCenterY = entry.getKey().y() * TILE_SIZE + (TILE_SIZE / 2);
                Vector2D tilePos = new Vector2D(tileCenterX, tileCenterY);

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
     * Trả về lượng tốc độ BỊ GIẢM (lượng x) dựa trên địa hình và sinh vật.
     * @param pos Tọa độ sinh vật đang đứng
     * @param animal Đối tượng sinh vật đang di chuyển
     * @return Lượng tốc độ bị trừ đi (0.0 = không bị trừ, di chuyển bình thường)
     */
    public float getSpeedPenalty(Vector2D pos, Organism animal) { // Truyền thẳng animal vào
        TerrainType terrain = getTerrainAt(pos);
        
        // Lấy tên loài từ object animal (Giả định Organism có hàm getSpecies())
        String species = animal.getSpeciesName();

        // Voi (Động vật đầu bảng) càn lướt mọi địa hình, không bị trừ tốc độ
        if (species.equalsIgnoreCase("Elephant")) {
            return 0.0f; 
        }

        switch (terrain) {
            case SHALLOW_WATER:
                // Trả về lượng x lấy từ file config
                return AppConfig.getFloat("environment.terrain.penalty.shallow_water");
                
            case FOREST:
                // Rừng rậm: Sói bị trừ ít tốc độ hơn nhờ bản năng
                if (species.equalsIgnoreCase("Wolf")) {
                    return AppConfig.getFloat("environment.terrain.penalty.forest.wolf");
                }
                return AppConfig.getFloat("environment.terrain.penalty.forest");
                
            default: 
                // GRASSLAND hoặc các địa hình bình thường: Không bị trừ tốc (lượng giảm = 0)
                return 0.0f; 
        }
    }
    public Vector2D getRandomValidPosition(Random random) {
        return boundary.getRandomPoint(random);
    }
}