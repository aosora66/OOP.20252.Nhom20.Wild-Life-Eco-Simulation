package wildlife.model.environment.envType;

import wildlife.model.environment.Environment;
import wildlife.model.environment.component.OrganismRegistry;
import wildlife.model.environment.component.ResourceManager;
import wildlife.model.environment.component.TerrainComponent;
import wildlife.model.environment.component.TimeComponent;
import wildlife.model.environment.enums.ObstacleType;
import wildlife.model.environment.enums.TerrainType;
import wildlife.model.environment.enums.WeatherType;
import wildlife.model.organism.plant.TreeForest;
import wildlife.util.Boundary;
import wildlife.util.Vector2D;

import java.util.List;
import java.util.Random;

/**
 * Môi trường Rừng Rậm (Forest)
 * Đặc trưng: Nhiều rào cản tự nhiên, ánh sáng thấp, có lợi thế tàng hình cực cao.
 */
public class Forest extends Environment {
    private final Random random;

    // ----------------------------------------------------------------
    //  Constructor
    // ----------------------------------------------------------------
    public Forest(String id, String name, Boundary boundary) {
        super(
                id, name,
                70.0f, // Độ ẩm cao
                24.0f, // Nhiệt độ trung bình, mát mẻ
                0.5f,  // Ánh sáng thấp do tán lá
                new TimeComponent(),
                new TerrainComponent(boundary, TerrainType.FOREST),
                new OrganismRegistry(),
                new ResourceManager()
        );
        this.random = new Random();  // fix: final field phải khởi tạo trước khi dùng
        initialize();
    }

    // ----------------------------------------------------------------
    //  Ghi đè 3 hàm từ lớp cha
    // ----------------------------------------------------------------

    @Override
    protected void initialize() {
        // Khởi tạo 20 cây cổ thụ
        for (int i = 0; i < 20; i++) {
            Vector2D treePos = terrain.getRandomValidPosition();
            registry.add(TreeForest.create(treePos, this));
        }

        // Rải thêm 10 bụi rậm làm nơi trú ẩn
        for (int i = 0; i < 10; i++) {
            Vector2D bushPos = terrain.getRandomValidPosition();
            // Chỉ định rõ đây là Bụi rậm (Thỏ/Hươu lách qua được, Sói/Hổ bị chặn)
            resources.placeObstacle(bushPos, ObstacleType.BUSH);
        }
    }

    @Override
    protected void applySeasonEffect() {

        switch (time.getCurrentSeason()) {
            case BREEDING -> {
                this.currentTemp = this.temperature + (random.nextFloat() * 2.0f - 1.0f); // +/- 1.0 độ C
                this.currentHumidity = Math.max(0.0f, Math.min(100.0f, this.humidity + (random.nextFloat() * 6.0f - 3.0f))); // +/- 3%
                this.currentLight = Math.max(0.0f, Math.min(1.0f, this.lightLevel + (random.nextFloat() * 0.04f - 0.02f))); // +/- 0.02
            }
            case DROUGHT -> {
                // Mùa hạn hán: nhiệt độ ấm hơn, độ ẩm giảm nhẹ ngẫu nhiên, ánh sáng tăng nhẹ do thưa lá
                this.currentTemp = this.temperature + (random.nextFloat() * 3.0f - 0.5f); // -0.5 đến +2.5 độ C
                this.currentHumidity = Math.max(0.0f, Math.min(100.0f, this.humidity - random.nextFloat() * 5.0f)); // giảm thêm tới 5%
                this.currentLight = Math.max(0.0f, Math.min(1.0f, this.lightLevel + random.nextFloat() * 0.08f)); // tăng thêm tới 0.08
            }
            default -> { // NORMAL
                // Mùa bình thường: biến động rất nhỏ
                this.currentTemp = this.temperature + (random.nextFloat() * 1.0f - 0.5f); // +/- 0.5 độ C
                this.currentHumidity = Math.max(0.0f, Math.min(100.0f, this.humidity + (random.nextFloat() * 4.0f - 2.0f))); // +/- 2%
                this.currentLight = Math.max(0.0f, Math.min(1.0f, this.lightLevel + (random.nextFloat() * 0.02f - 0.01f))); // +/- 0.01
            }
        }
    }

    @Override
    protected void applyWeatherEffect() {
        WeatherType weather = time.getCurrentWeather();

        if (weather == WeatherType.RAIN) {
            this.currentHumidity = 100.0f; // Mưa làm độ ẩm bão hòa
        }
    }

}
