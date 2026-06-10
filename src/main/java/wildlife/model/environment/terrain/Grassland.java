package wildlife.model.environment.terrain;

import wildlife.model.environment.Environment;
import wildlife.model.environment.component.OrganismRegistry;
import wildlife.model.environment.component.ResourceManager;
import wildlife.model.environment.component.TerrainComponent;
import wildlife.model.environment.component.TimeComponent;
import wildlife.model.environment.enums.Season;
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
 * Đặc trưng: Độ ẩm thấp, nhiệt độ cao.
 */
public class Grassland extends Environment {

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
        // Nhưng vì đây là Đồng Cỏ, ta sẽ ép chỉ số để nó khắc nghiệt hơn bình thường
        // và thêm dao động ngẫu nhiên (random) theo đặc tính các mùa đã cài:
        float tempBase = time.getTemperature() + 5.0f; // Luôn nóng hơn các vùng khác 5 độ
        float humidBase = Math.max(0.0f, time.getHumidity() - 15.0f); // Luôn khô hơn 15%
        float lightBase = time.getLightLevel();

        switch (time.getCurrentSeason()) {
            case BREEDING -> {
                // Mùa sinh sản: thời tiết ôn hòa, nhiệt độ dao động nhỏ, độ ẩm dao động nhẹ
                this.temperature = tempBase + (random.nextFloat() * 4.0f - 2.0f); // +/- 2.0 độ C
                this.humidity = Math.max(0.0f, Math.min(100.0f, humidBase + (random.nextFloat() * 10.0f - 5.0f))); // +/- 5%
                this.lightLevel = Math.max(0.0f, Math.min(1.0f, lightBase + (random.nextFloat() * 0.1f - 0.05f))); // +/- 0.05
            }
            case DROUGHT -> {
                // Mùa hạn hán: nhiệt độ cực đoan hơn (tăng thêm), độ ẩm giảm sâu ngẫu nhiên, ánh sáng gắt
                this.temperature = tempBase + (random.nextFloat() * 6.0f - 1.0f); // -1.0 đến +5.0 độ C
                this.humidity = Math.max(0.0f, Math.min(100.0f, humidBase - random.nextFloat() * 8.0f)); // giảm thêm tới 8%
                this.lightLevel = Math.max(0.0f, Math.min(1.0f, lightBase + random.nextFloat() * 0.15f)); // tăng thêm tới 0.15
            }
            default -> { // NORMAL
                // Mùa bình thường: biến động rất nhỏ
                this.temperature = tempBase + (random.nextFloat() * 2.0f - 1.0f); // +/- 1.0 độ C
                this.humidity = Math.max(0.0f, Math.min(100.0f, humidBase + (random.nextFloat() * 6.0f - 3.0f))); // +/- 3%
                this.lightLevel = Math.max(0.0f, Math.min(1.0f, lightBase + (random.nextFloat() * 0.06f - 0.03f))); // +/- 0.03
            }
        }
    }

    @Override
    protected void applyWeatherEffect() {
    //cập nhật chỉ số môi trường theo thời tiết:
        WeatherType weather = time.getCurrentWeather();
        if (weather == WeatherType.RAIN) {
            this.humidity = Math.min(100.0f, this.humidity + 10.0f);
            this.temperature = Math.max(0.0f, this.temperature - 2.0f);
        } else if (weather == WeatherType.DROUGHT) {
            this.humidity = Math.max(0.0f, this.humidity - 10.0f);
            this.temperature = this.temperature + 3.0f;
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