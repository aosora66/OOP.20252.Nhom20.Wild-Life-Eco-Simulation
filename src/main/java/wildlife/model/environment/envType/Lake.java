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
import wildlife.util.RegionBoundary;
import wildlife.util.Vector2D;

import java.util.ArrayList;
import java.util.Collections;
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
    private final float maxWaterLevel;
    private float currentWaterLevel;
    private final Random random;
    private final List<Vector2D> algaeSpawnTiles;
    private int algaeSpawnCursor;

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
        this.algaeSpawnTiles = collectAlgaeSpawnTiles(boundary);
        Collections.shuffle(this.algaeSpawnTiles, random);

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
        // Rải sẵn 25 cụm tảo theo vòng qua các tile nước để Cá có đồ ăn đều hơn ngay từ đầu.
        float algaeNutrition = AppConfig.getFloat("food.algae.nutritionalValue");
        int algaeExpiry = AppConfig.getInt("food.algae.expiryTicks");
        spawnAlgaeEvenly(25, algaeNutrition, algaeExpiry);

        // --- 3. ĐỘNG VẬT DƯỚI NƯỚC (CÁ) ---
        // Rải đều TUỔI ban đầu (0..60% đời) để đàn không cùng già chết một lúc gây tuyệt chủng.
        float fishMaxAge = AppConfig.getFloat("animal.fish.maxAge");
        for (int i = 0; i < 15; i++) {
            float startAge = random.nextFloat() * fishMaxAge * 0.6f;
            addOrganism(Fish.create(terrain.getRandomValidPosition(), this, startAge));
        }
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
        } else if (weather == WeatherType.DRY) {
            currentWaterLevel = Math.max(0, currentWaterLevel - 0.2f);
        }
    }

    /**
     * Mỗi tick: chạy cập nhật chuẩn của Environment, sau đó rải thêm tảo (Algae)
     * theo vòng qua các tile hồ — nguồn thức ăn liên tục và đều hơn cho Cá.
     */
    @Override
    public void updateEnvironment(int currentTick) {
        super.updateEnvironment(currentTick);
        trySpawnAlgae(currentTick);
    }

    private void trySpawnAlgae(int currentTick) {
        int interval = AppConfig.getInt("plant.algae.spawnInterval");
        if (currentTick % interval != 0) return;

        // Tảo ít đi khi nước cạn
        float waterRatio = currentWaterLevel / maxWaterLevel;
        int spawnCount = Math.max(1, (int)(AppConfig.getInt("plant.algae.spawnCount") * waterRatio));
        float nutrition  = AppConfig.getFloat("food.algae.nutritionalValue");
        int expiryTicks  = AppConfig.getInt("food.algae.expiryTicks");

        spawnAlgaeEvenly(spawnCount, nutrition, expiryTicks);
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

    private List<Vector2D> collectAlgaeSpawnTiles(Boundary boundary) {
        if (boundary instanceof RegionBoundary regionBoundary) {
            return new ArrayList<>(regionBoundary.getTileOrigins());
        }
        return new ArrayList<>();
    }

    private void spawnAlgaeEvenly(int count, float nutrition, int expiryTicks) {
        for (int i = 0; i < count; i++) {
            resources.spawnFood(nextAlgaeSpawnPosition(), nutrition, FoodType.ALGAE, expiryTicks);
        }
    }

    private Vector2D nextAlgaeSpawnPosition() {
        if (algaeSpawnTiles.isEmpty()) {
            return terrain.getRandomValidPosition();
        }

        Vector2D tile = algaeSpawnTiles.get(algaeSpawnCursor);
        algaeSpawnCursor = (algaeSpawnCursor + 1) % algaeSpawnTiles.size();

        float tileSize = AppConfig.getFloat("environment.terrain.tileSize");
        float x = tile.getX() + random.nextFloat() * tileSize;
        float y = tile.getY() + random.nextFloat() * tileSize;
        return new Vector2D(x, y);
    }

    // --- Getters ---
    public float getCurrentWaterLevel() { return currentWaterLevel; }
}
