package wildlife.model.environment.envType;

import wildlife.model.environment.Environment;
import wildlife.model.environment.component.OrganismRegistry;
import wildlife.model.environment.component.ResourceManager;
import wildlife.model.environment.component.TerrainComponent;
import wildlife.model.environment.component.TimeComponent;
import wildlife.model.environment.enums.ObstacleType;
import wildlife.model.environment.enums.TerrainType;
import wildlife.model.environment.enums.WeatherType;
import wildlife.util.Boundary;
import wildlife.util.Vector2D;

import java.util.List;
import java.util.Random;

/**
 * Môi trường Đồng Cỏ (Grassland)
 * Đặc trưng: Độ ẩm thấp, nhiệt độ cao.
 */
public class GrassLand extends Environment {
    private final Random random;

    // ----------------------------------------------------------------
    //  Constructor
    // ----------------------------------------------------------------
    public GrassLand(String id, String name, Boundary boundary) {
        // Cung cấp các tham số tĩnh mặc định cho Đồng Cỏ
        super(
                id, name,
                30.0f, // Độ ẩm thấp
                33.0f, // Nhiệt độ cao
                1.0f,  // Ánh sáng cao
                new TimeComponent(), // Chu kỳ thời gian (tùy chỉnh số tick)
                new TerrainComponent(boundary, TerrainType.GRASSLAND), // Phủ toàn bộ bề mặt là Cỏ
                new OrganismRegistry(),
                new ResourceManager()
        );

        initialize();
    }

    // ----------------------------------------------------------------
    //  Ghi đè 3 hàm hợp đồng (Design Contract) từ lớp cha
    // ----------------------------------------------------------------

    @Override
    protected void initialize() {
        // Khởi tạo cỏ
        for (int i = 0; i < 30; i++) {
            Vector2D grassPos = terrain.getRandomValidPosition();

            // Tạo thực thể Plant tương ứng
            registry.add(new Grass(grassPos));
        }

        // Rải bụi rậm làm nơi trú ẩn
        for (int i = 0; i < 5; i++) {
            Vector2D bushPos = terrain.getRandomValidPosition();
            resources.placeObstacle(bushPos, ObstacleType.BUSH);
        }

        for (int i = 0; i < 10; i++) {
            Vector2D rockPos = terrain.getRandomValidPosition();
            resources.placeObstacle(rockPos, ObstacleType.ROCK);
        }
    }

    @Override
    protected void applySeasonEffect() {

        switch (time.getCurrentSeason()) {
            case BREEDING -> {
                // Mùa sinh sản: thời tiết ôn hòa, nhiệt độ dao động nhỏ, độ ẩm dao động nhẹ
                this.currentTemp = this.temperature + (random.nextFloat() * 4.0f - 2.0f); // +/- 2.0 độ C
                this.currentHumidity = Math.max(0.0f, Math.min(100.0f, this.humidity + (random.nextFloat() * 10.0f - 5.0f))); // +/- 5%
                this.currentLight = Math.max(0.0f, Math.min(1.0f, this.lightLevel + (random.nextFloat() * 0.1f - 0.05f))); // +/- 0.05
            }
            case DROUGHT -> {
                // Mùa hạn hán: nhiệt độ cực đoan hơn (tăng thêm), độ ẩm giảm sâu ngẫu nhiên, ánh sáng gắt
                this.currentTemp = this.temperature + (random.nextFloat() * 6.0f - 1.0f); // -1.0 đến +5.0 độ C
                this.currentHumidity = Math.max(0.0f, Math.min(100.0f, this.humidity - random.nextFloat() * 8.0f)); // giảm thêm tới 8%
                this.currentLight = Math.max(0.0f, Math.min(1.0f, this.lightLevel + random.nextFloat() * 0.15f)); // tăng thêm tới 0.15
            }
            default -> { // NORMAL
                // Mùa bình thường: biến động rất nhỏ
                this.currentTemp = this.temperature + (random.nextFloat() * 2.0f - 1.0f); // +/- 1.0 độ C
                this.currentHumidity = Math.max(0.0f, Math.min(100.0f, this.humidity + (random.nextFloat() * 6.0f - 3.0f))); // +/- 3%
                this.currentLight = Math.max(0.0f, Math.min(1.0f, this.lightLevel + (random.nextFloat() * 0.06f - 0.03f))); // +/- 0.03
            }
        }
    }

    @Override
    protected void applyWeatherEffect() {
        // Cập nhật chỉ số môi trường theo thời tiết:
        WeatherType weather = time.getCurrentWeather();
        if (weather == WeatherType.RAIN) {
            this.currentHumidity = Math.min(100.0f, this.currentHumidity + 10.0f);
            this.currentTemp = Math.max(0.0f, this.currentTemp - 2.0f);
        } else if (weather == WeatherType.DROUGHT) {
            this.currentHumidity = Math.max(0.0f, this.currentHumidity - 10.0f);
            this.currentTemp = this.currentTemp + 3.0f;
        }
    }

}
