package wildlife.model.environment.terrain;

import wildlife.model.environment.Environment;
import wildlife.model.environment.component.OrganismRegistry;
import wildlife.model.environment.component.ResourceManager;
import wildlife.model.environment.component.TerrainComponent;
import wildlife.model.environment.component.TimeComponent;
import wildlife.model.environment.enums.TerrainType;
import wildlife.model.environment.enums.WeatherType;
import wildlife.model.environment.event.EnvironmentEventPublisher;
import wildlife.util.Boundary;
import wildlife.util.Vector2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Môi trường Đồng Cỏ (Grassland)
 * Đặc trưng: Độ ẩm thấp, nhiệt độ cao, sinh ra vũng bùn khi trời mưa.
 */
public class Grassland extends Environment {

    // ----------------------------------------------------------------
    //  Trạng thái riêng của Đồng Cỏ
    // ----------------------------------------------------------------
    private final List<Vector2D> activeMudPuddles; // Lưu tọa độ các vũng bùn
    private int mudDryingTimer; // Bộ đếm thời gian bùn khô
    private boolean wasRaining; // Đánh dấu trạng thái mưa
    private final Random random;

    // ----------------------------------------------------------------
    //  Constructor
    // ----------------------------------------------------------------
    public Grassland(String id, String name, Boundary boundary) {
        // Cung cấp các tham số tĩnh mặc định cho Đồng Cỏ
        super(
                id, name,
                30.0f, // Độ ẩm thấp
                35.0f, // Nhiệt độ cao
                1.0f,  // Ánh sáng cao
                new TimeComponent(240, 2400), // Chu kỳ thời gian (tùy chỉnh số tick)
                new TerrainComponent(boundary, TerrainType.GRASSLAND), // Phủ toàn bộ bề mặt là Cỏ
                new OrganismRegistry(),
                new ResourceManager(),
                new EnvironmentEventPublisher("sounds/grassland_ambient.wav") // Âm thanh nền đặc trưng
        );

        this.activeMudPuddles = new ArrayList<>();
        this.mudDryingTimer = 0;
        this.wasRaining = false;
        this.random = new Random();

        // Khởi tạo một vài vật cản tĩnh (đá) ban đầu để hỗ trợ sinh vật tương tác
        // (Trong thực tế, bạn có thể truyền danh sách tọa độ hoặc random quanh khu vực quản lý)
        resources.placeObstacle(new Vector2D(50, 50)); 
        resources.placeObstacle(new Vector2D(120, 80));
    }

    // ----------------------------------------------------------------
    //  Ghi đè 3 hàm hợp đồng (Design Contract) từ lớp cha
    // ----------------------------------------------------------------

    @Override
    protected void applySeasonEffect() {
        // Lớp TimeComponent đã tính toán nhiệt độ/độ ẩm cơ bản.
        // Nhưng vì đây là Đồng Cỏ, ta sẽ ép chỉ số để nó khắc nghiệt hơn bình thường:
        this.temperature = time.getTemperature() + 5.0f; // Luôn nóng hơn các vùng khác 5 độ
        this.humidity = Math.max(0, time.getHumidity() - 15.0f); // Luôn khô hơn 15%
    }

    @Override
    protected void applyWeatherEffect() {
        WeatherType currentWeather = time.getCurrentWeather();

        // CƠ CHẾ ĐẶC TRƯNG: Sinh Bùn Lầy khi mưa
        if (currentWeather == WeatherType.RAIN) {
            wasRaining = true;
            mudDryingTimer = 0; // Đang mưa thì bùn luôn ướt

            // Thi thoảng xuất hiện ngẫu nhiên một vũng bùn mới (Giới hạn tối đa 10 vũng để không lag)
            if (activeMudPuddles.size() < 10 && random.nextFloat() < 0.05f) {
                // TODO: Thay tọa độ random (100) bằng tọa độ thực tế từ Boundary của bạn
          // Sửa ở hàm applyWeatherEffect()
        Vector2D mudPos = terrain.getRandomValidPosition(random);

         // Sửa ở hàm generateNaturalResources()
        Vector2D grassPos = terrain.getRandomValidPosition(random); 
                
                // Nếu điểm đó thuộc đồng cỏ, biến nó thành bùn
                if (terrain.containsPosition(mudPos)) {
                    terrain.addCustomTerrain(mudPos, TerrainType.MUD);
                    activeMudPuddles.add(mudPos);
                }
            }
        } else {
            // Khi trời tạnh mưa, bắt đầu đếm giờ để bùn khô đi
            if (wasRaining) {
                mudDryingTimer++;
                int TICKS_TO_DRY = 50; // Sau 50 tick thì bùn khô nứt
                
                if (mudDryingTimer > TICKS_TO_DRY) {
                    for (Vector2D pos : activeMudPuddles) {
                        // Trả lại địa hình cỏ bình thường
                        terrain.addCustomTerrain(pos, TerrainType.GRASSLAND);
                    }
                    activeMudPuddles.clear(); // Xóa sạch danh sách bùn
                    wasRaining = false;
                    mudDryingTimer = 0;
                }
            }
        }
    }

    @Override
    protected void generateNaturalResources() {
        // Tự động mọc thêm cỏ dại (thức ăn cho động vật ăn cỏ) với tỉ lệ thấp
        if (random.nextFloat() < 0.02f) { // 2% cơ hội mỗi vòng lặp
            // TODO: Thay tọa độ random bằng tọa độ Boundary
            Vector2D grassPos = new Vector2D(random.nextInt(100), random.nextInt(100));
            if (terrain.containsPosition(grassPos)) {
                // Sinh ra thức ăn thực vật (nutrition thấp, tồn tại lâu)
                resources.spawnFood(grassPos, 5.0f, false, 500); 
            }
        }
    }
}