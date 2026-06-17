package wildlife.model.environment.envType;

import wildlife.model.environment.Environment;
import wildlife.model.environment.component.OrganismRegistry;
import wildlife.model.environment.component.ResourceManager;
import wildlife.model.environment.component.TerrainComponent;
import wildlife.model.environment.component.TimeComponent;
import wildlife.model.environment.enums.FoodType;
import wildlife.model.environment.enums.TerrainType;
import wildlife.model.environment.enums.WeatherType;
import wildlife.model.organism.Organism;
import wildlife.model.organism.animal.hebivores.Fish;
import wildlife.util.AppConfig;
import wildlife.util.Boundary;
import wildlife.util.Vector2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Môi trường Hồ Nước (Lake)
 * Đặc trưng: Mực nước thay đổi theo thời tiết
 */
public class Lake extends Environment {

    // ----------------------------------------------------------------
    //  Trạng thái riêng của Hồ Nước
    // ----------------------------------------------------------------
    // Thêm biến này ở đầu class Lake
    private ResourceManager mainWaterSource;
    private final float maxWaterLevel;
    private float currentWaterLevel;
    private final Random random;

    // ----------------------------------------------------------------
    //  Constructor
    // ----------------------------------------------------------------
    public Lake(String id, String name, Boundary boundary, float maxWaterLevel) {
        super(
                id, name,
                80.0f, // Độ ẩm mặc định rất cao
                22.0f, // Nhiệt độ trung bình, mát mẻ
                0.8f,  // Ánh sáng phản chiếu mặt nước
                new TimeComponent(),
                new TerrainComponent(boundary, TerrainType.DEEP_WATER), // Phủ mặt định là nước sâu
                new OrganismRegistry(),
                new ResourceManager()
        );

        this.maxWaterLevel = maxWaterLevel;
        this.currentWaterLevel = maxWaterLevel * 0.8f; // Bắt đầu với 80% thể tích
        this.random = new Random(); // fix: final field phải khởi tạo trước khi dùng

        initialize();
    }

    // ----------------------------------------------------------------
    //  Ghi đè 3 hàm hợp đồng từ lớp cha
    // ----------------------------------------------------------------

    @Override
    protected void initialize() {
        int numberOfWaterNodes = 16;
        List<Vector2D> shorelinePoints = generateShorelinePoints(numberOfWaterNodes);

        // --- 1. TÀI NGUYÊN NƯỚC UỐNG ---
        for (Vector2D pos : shorelinePoints) {
            this.resources.spawnFood(pos, AppConfig.getFloat("food.water.nutritionalValue"), FoodType.WATER, Integer.MAX_VALUE);
        }

        // --- 2. THỰC VẬT ĐẦU VÀO (TẢO) ---
        // Rải sẵn 15 cụm tảo ngay khi bắt đầu để Cá có đồ ăn ngay, tránh chết đói ở những tick đầu
        float algaeNutrition = AppConfig.getFloat("food.algae.nutritionalValue");
        int algaeExpiry = AppConfig.getInt("food.algae.expiryTicks");
        for (int i = 0; i < 15; i++) {
            resources.spawnFood(terrain.getRandomValidPosition(), algaeNutrition, FoodType.ALGAE, algaeExpiry);
        }

        // --- 3. ĐỘNG VẬT DƯỚI NƯỚC (CÁ) ---
        spawnAnimals(Fish.class, 15);
    }

    @Override
    protected void applySeasonEffect() {
        // Hồ nước có "nhiệt dung" lớn, nên nhiệt độ ổn định hơn đồng cỏ
        switch (time.getCurrentSeason()) {
            case BREEDING -> {
                // Mùa sinh sản: thời tiết ôn hòa, nhiệt độ dao động nhỏ, độ ẩm dao động nhẹ
                this.currentTemp = this.temperature + (random.nextFloat() * 1.5f - 0.75f); // +/- 0.75 độ C
                this.currentHumidity = Math.max(0.0f, Math.min(100.0f, this.humidity + (random.nextFloat() * 4.0f - 2.0f))); // +/- 2%
                this.currentLight = Math.max(0.0f, Math.min(1.0f, this.lightLevel + (random.nextFloat() * 0.04f - 0.02f))); // +/- 0.02
                this.currentWaterLevel = Math.min(maxWaterLevel, this.currentWaterLevel + 0.1f);
            }
            case DROUGHT -> {
                // Mùa hạn hán: nhiệt độ ấm hơn, độ ẩm giảm nhẹ ngẫu nhiên, ánh sáng phản chiếu mạnh hơn
                this.currentTemp = this.temperature + (random.nextFloat() * 2.5f - 0.5f); // -0.5 đến +2.0 độ C
                this.currentHumidity = Math.max(0.0f, Math.min(100.0f, this.humidity - random.nextFloat() * 4.0f)); // giảm thêm tới 4%
                this.currentLight = Math.max(0.0f, Math.min(1.0f, this.lightLevel + random.nextFloat() * 0.06f)); // tăng thêm tới 0.06
                this.currentWaterLevel = Math.max(0.0f, this.currentWaterLevel - 0.3f);
            }
            default -> { // NORMAL
                // Mùa bình thường: biến động rất nhỏ
                this.currentTemp = this.temperature + (random.nextFloat() * 1.0f - 0.5f); // +/- 0.5 độ C
                this.currentHumidity = Math.max(0.0f, Math.min(100.0f, this.humidity + (random.nextFloat() * 2.0f - 1.0f))); // +/- 1%
                this.currentLight = Math.max(0.0f, Math.min(1.0f, this.lightLevel + (random.nextFloat() * 0.02f - 0.01f))); // +/- 0.01
            }
        }
    }

    @Override
    protected void applyWeatherEffect() {
        WeatherType weather = time.getCurrentWeather();

        if (weather == WeatherType.RAIN) {
            currentWaterLevel = Math.min(maxWaterLevel, currentWaterLevel + 0.5f);
        } else if (weather == WeatherType.DROUGHT) {
            currentWaterLevel = Math.max(0, currentWaterLevel - 0.2f);
        }
    }

    /**
     * Mỗi tick: chạy cập nhật chuẩn của Environment, sau đó rải thêm tảo (Algae)
     * ngẫu nhiên trong hồ — nguồn thức ăn liên tục cho Cá, tránh bị tuyệt chủng vì đói.
     */
    @Override
    public void updateEnvironment(int currentTick) {
        super.updateEnvironment(currentTick);
        trySpawnAlgae(currentTick);
    }

    private void trySpawnAlgae(int currentTick) {
        int interval = AppConfig.getInt("plant.algae.spawnInterval");
        if (currentTick % interval != 0) return;

        int spawnCount   = AppConfig.getInt("plant.algae.spawnCount");
        float nutrition  = AppConfig.getFloat("food.algae.nutritionalValue");
        int expiryTicks  = AppConfig.getInt("food.algae.expiryTicks");

        for (int i = 0; i < spawnCount; i++) {
            Vector2D pos = terrain.getRandomValidPosition();
            resources.spawnFood(pos, nutrition, FoodType.ALGAE, expiryTicks);
        }
    }

    // ----------------------------------------------------------------
    //  Các phương thức phụ trợ đặc thù của Hồ Nước
    // ----------------------------------------------------------------

    /**
     * Yêu cầu từ thiết kế: Tính tổng giá trị dinh dưỡng của sinh quyển trong hồ.
     * @return Tổng dinh dưỡng của tất cả sinh vật còn sống.
     */
    public float getTotalBiosphereNutrition() {
        float totalNutrition = 0f;
        for (Organism o : registry.getAllAlive(Organism.class)) {
            // Giả định đối tượng Stats của sinh vật có hàm getNutritionalValue()
            totalNutrition += o.getStats().getNutritionalValue();
        }
        return totalNutrition;
    }

    /**
     * Sinh các điểm ngẫu nhiên hợp lệ trong hồ để rải nước uống.
     * (Đơn giản hóa: Boundary hiện chỉ hỗ trợ random point trong vùng,
     * chưa có API lấy điểm trên đường viền — toàn bộ mặt hồ là DEEP_WATER
     * nên rải ngẫu nhiên trong vùng vẫn hợp lý.)
     */
    private List<Vector2D> generateShorelinePoints(int count) {
        List<Vector2D> points = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            points.add(terrain.getRandomValidPosition());
        }
        return points;
    }

    // --- Getters ---
    public float getCurrentWaterLevel() { return currentWaterLevel; }
}
