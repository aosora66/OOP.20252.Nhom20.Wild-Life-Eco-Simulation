package wildlife.model.environment.envType;

import wildlife.model.environment.Environment;
import wildlife.model.environment.component.OrganismRegistry;
import wildlife.model.environment.component.ResourceManager;
import wildlife.model.environment.component.TerrainComponent;
import wildlife.model.environment.component.TimeComponent;
import wildlife.model.environment.enums.ObstacleType;
import wildlife.model.environment.enums.TerrainType;
import wildlife.model.environment.enums.WeatherType;
import wildlife.model.organism.animal.canivores.Hunter;
import wildlife.model.organism.animal.canivores.Tiger;
import wildlife.model.organism.animal.canivores.Wolf;
import wildlife.model.organism.animal.hebivores.Deer;
import wildlife.model.organism.animal.hebivores.Elephant;
import wildlife.model.organism.animal.hebivores.Rabbit;
import wildlife.model.organism.component.AdaptabilityComponent;
import wildlife.model.organism.component.GrowthComponent;
import wildlife.model.organism.component.SurvivalStatsComponent;
import wildlife.model.organism.plant.TreeForest;
import wildlife.model.organism.plant.AppleTree;
import wildlife.util.Boundary;

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
        // --- 1. THỰC VẬT (PLANTS) ---
        // 30 Cây cổ thụ (Rất dày đặc)
        for (int i = 0; i < 30; i++) {
            registry.add(TreeForest.create(terrain.getRandomValidPosition(), this));
        }
        // 25 Cây táo — rải tuổi ngẫu nhiên để một số đã trưởng thành sẵn (nguồn thức ăn chính cho herbivore)
        for (int i = 0; i < 25; i++) {
            float startAge = random.nextFloat() * 1200f;
            registry.add(AppleTree.create(terrain.getRandomValidPosition(), this, startAge));
        }

        // --- 2. VẬT CẢN (OBSTACLES) ---
        // 25 Bụi rậm (Đặc trưng của rừng, rất nhiều chỗ ẩn nấp)
        for (int i = 0; i < 25; i++) {
            resources.placeObstacle(terrain.getRandomValidPosition(), ObstacleType.BUSH);
        }
        // 15 Tảng đá
        for (int i = 0; i < 15; i++) {
            resources.placeObstacle(terrain.getRandomValidPosition(), ObstacleType.ROCK);
        }

        // --- 3. ĐỘNG VẬT (ANIMALS) ---
        // Tỉ lệ: Nhiều Thú ăn thịt, Ít Thú ăn cỏ, Có động vật đầu bảng (Voi)

        // Sinh Sói (Bầy sói rừng)
        spawnAnimals(Wolf.class, 8);

        // Sinh Hổ (Chúa sơn lâm)
        spawnAnimals(Tiger.class, 6);

        // Sinh Thợ săn (Rừng rậm — ít hơn đồng cỏ)
        spawnAnimals(Hunter.class, 3);

        // Sinh Voi (Động vật đầu bảng)
        spawnAnimals(Elephant.class, 4);

        // Sinh Hươu và Thỏ
        spawnAnimals(Rabbit.class, 15);
        spawnAnimals(Deer.class, 12);
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
