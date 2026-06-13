package wildlife.model.environment.component;

import wildlife.model.environment.enums.TerrainType;
import wildlife.model.organism.animal.Animal;
import wildlife.util.AppConfig;
import wildlife.util.Boundary;
import wildlife.util.Vector2D;

import java.util.*;

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

    // Kích thước 1 ô vuông ảo để tránh lỗi số thực float khi dùng HashMap
    private record TileIndex(int x, int y) {}
    private static final float TILE_SIZE = AppConfig.getFloat("environment.terrain.tileSize");
    private final Map<TileIndex, TerrainType> customTiles; // Các ô địa hình điểm xuyết
    // ----------------------------------------------------------------
    //  Trạng thái nội tại
    // ----------------------------------------------------------------
    private final Boundary boundary; // Ranh giới môi trường (Tròn hoặc Chữ nhật)
    private final TerrainType defaultTerrain; // Loại đất nền trải khắp vùng
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
     * Thêm loại địa hình vào 1 ô .
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

    public TerrainType getTerrainAt(Vector2D pos) {
        // 1. Rớt ra ngoài ranh giới vùng -> Coi như đụng vách núi
        if (!containsPosition(pos)) {
            return TerrainType.MOUNTAIN;
        }

        // 2. Ép tọa độ thực tế thành tọa độ ô vuông
        int tileX = (int) (pos.getX() / TILE_SIZE);
        int tileY = (int) (pos.getY() / TILE_SIZE);

        // 3. Ưu tiên ô điểm xuyết, nếu không có thì trả về nền mặc định
        return customTiles.getOrDefault(new TileIndex(tileX, tileY), defaultTerrain);
    }

    public float getVisibility(Vector2D pos) {
        TerrainType terrain = getTerrainAt(pos);
        return LOW_VISIBILITY_TERRAINS.contains(terrain)
                ? VISIBILITY_REDUCED
                : VISIBILITY_NORMAL;
    }

    /** Check địa hình cản trở
     *
     * @param pos
     * @param self
     * @return true nếu không có cản trở
     */
    public boolean isPassable(Vector2D pos, Animal self) {
        TerrainType terrain = getTerrainAt(pos);
        if (self == null) {
            return terrain != TerrainType.DEEP_WATER && terrain != TerrainType.MOUNTAIN;
        }

        boolean isAquatic = self instanceof fish;
        // --- NHÁNH DÀNH CHO CÁ ---
        if (isAquatic) {
            // Cá chỉ bơi được ở nước sâu và nước nông
            return terrain == TerrainType.DEEP_WATER;
        }

        // --- NHÁNH DÀNH CHO ĐỘNG VẬT TRÊN CẠN ---
        switch (terrain) {
            case DEEP_WATER:
            case MOUNTAIN:
                return false; // Cấm tuyệt đối xuống nước sâu và leo vách đá vách núi

            case GRASSLAND:
            case FOREST:
            case MUD:
                return true;  // Trên cạn và lội nước nông thì thoải mái

            default:
                return false;
        }
    }

    public boolean containsPosition(Vector2D pos) {
        return boundary.contains(pos);
    }
    public Vector2D getRandomValidPosition() {
        return boundary.getRandomPoint(new Random());
    }


    public boolean containsTerrain(TerrainType type) {
        return containedTerrains.contains(type);
    }

    /**
     * Trả về lượng tốc độ BỊ GIẢM dựa trên địa hình và sinh vật.
     * @param pos Tọa độ sinh vật đang đứng
     * @param self Đối tượng sinh vật đang di chuyển
     * @return Lượng tốc độ bị trừ đi (0.0 = không bị trừ, di chuyển bình thường)
     */
    public float getSpeedPenalty(Vector2D pos, Animal self) { // Truyền thẳng animal vào
        TerrainType terrain = getTerrainAt(pos);

        // Lấy tên loài từ object animal (Giả định Organism có hàm getSpecies())
        String species = self.getSpeciesName();

        // Voi (Động vật đầu bảng) càn lướt mọi địa hình, không bị trừ tốc độ
        if (species.equalsIgnoreCase("Elephant")) {
            return 0.0f;
        }

        switch (terrain) {
            case MUD:
                // Trả về lượng x lấy từ file config
                return AppConfig.getFloat("environment.terrain.penalty.shallow_water");

            case FOREST:
                // Rừng rậm: Sói bị trừ ít tốc độ hơn nhờ bản năng
                if (species.equalsIgnoreCase("Wolf")) {
                    return AppConfig.getFloat("environment.terrain.penalty.forest.wolf");
                }
                return AppConfig.getFloat("environment.terrain.penalty.forest");

            default:
                return 0.0f;
        }
    }



    // ----------------------------------------------------------------
    //  Getters
    // ----------------------------------------------------------------
    public Set<TerrainType> getContainedTerrains() {
        return Collections.unmodifiableSet(containedTerrains);
    }
    public Boundary getBoundary()                  { return boundary; }
}