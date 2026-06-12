package wildlife.model.environment.terrain;

import wildlife.model.environment.Environment; 
import wildlife.model.environment.component.*;
import wildlife.model.environment.enums.TerrainType;
import wildlife.model.environment.enums.WeatherType;
import wildlife.model.environment.enums.FoodType;
import wildlife.model.environment.event.EnvironmentEventPublisher;
import wildlife.model.organism.Organism;
import wildlife.model.organism.OrganismState;
import wildlife.util.Boundary;
import wildlife.util.Vector2D;

import java.util.Random;

/**
 * Môi trường Hồ Nước (Lake)
/**
 * Môi trường Hồ Nước (Lake)

 * Đặc trưng: Mực nước thay đổi theo thời tiết, đóng băng khi < 0 độ C, 
 * và cung cấp API tính tổng dinh dưỡng sinh khối.
 */
public class Lake extends Environment {

    // ----------------------------------------------------------------
    //  Trạng thái riêng của Hồ Nước
    // ----------------------------------------------------------------
    private final float maxWaterLevel;
    private float currentWaterLevel;
    private boolean isFrozen;
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
                new TimeComponent(240, 2400),
                new TerrainComponent(boundary, TerrainType.DEEP_WATER), // Phủ mặt định là nước sâu
                new OrganismRegistry(),
                new ResourceManager(),
                new EnvironmentEventPublisher("sounds/lake_ambient.wav")
        );

        this.maxWaterLevel = maxWaterLevel;
        this.currentWaterLevel = maxWaterLevel * 0.8f; // Bắt đầu với 80% thể tích
        this.isFrozen = false;
        this.random = new Random();

    }

    // ----------------------------------------------------------------
    //  Ghi đè 3 hàm hợp đồng từ lớp cha
    // ----------------------------------------------------------------

    @Override
    protected void applySeasonEffect() {
        // Hồ nước có "nhiệt dung" lớn, nên nhiệt độ ổn định hơn đồng cỏ
        // Không quá nóng vào mùa Hạn, không quá lạnh vào ban đêm
        float tempBase = time.getTemperature() * 0.9f; 
        float humidBase = Math.min(100.0f, time.getHumidity() + 20.0f); // Luôn ẩm ướt
        float lightBase = this.lightLevel;

        switch (time.getCurrentSeason()) {
            case BREEDING -> {
                // Mùa sinh sản: thời tiết ôn hòa, nhiệt độ dao động nhỏ, độ ẩm dao động nhẹ
                this.temperature = tempBase + (random.nextFloat() * 1.5f - 0.75f); // +/- 0.75 độ C
                this.humidity = Math.max(0.0f, Math.min(100.0f, humidBase + (random.nextFloat() * 4.0f - 2.0f))); // +/- 2%
                this.lightLevel = Math.max(0.0f, Math.min(1.0f, lightBase + (random.nextFloat() * 0.04f - 0.02f))); // +/- 0.02
            }
            case DROUGHT -> {
                // Mùa hạn hán: nhiệt độ ấm hơn, độ ẩm giảm nhẹ ngẫu nhiên, ánh sáng phản chiếu mạnh hơn
                this.temperature = tempBase + (random.nextFloat() * 2.5f - 0.5f); // -0.5 đến +2.0 độ C
                this.humidity = Math.max(0.0f, Math.min(100.0f, humidBase - random.nextFloat() * 4.0f)); // giảm thêm tới 4%
                this.lightLevel = Math.max(0.0f, Math.min(1.0f, lightBase + random.nextFloat() * 0.06f)); // tăng thêm tới 0.06
            }
            default -> { // NORMAL
                // Mùa bình thường: biến động rất nhỏ
                this.temperature = tempBase + (random.nextFloat() * 1.0f - 0.5f); // +/- 0.5 độ C
                this.humidity = Math.max(0.0f, Math.min(100.0f, humidBase + (random.nextFloat() * 2.0f - 1.0f))); // +/- 1%
                this.lightLevel = Math.max(0.0f, Math.min(1.0f, lightBase + (random.nextFloat() * 0.02f - 0.01f))); // +/- 0.01
            }
        }
    }

    @Override
    protected void applyWeatherEffect() {
        WeatherType weather = time.getCurrentWeather();

        // 1. CẬP NHẬT MỰC NƯỚC: Mưa thì đầy, hạn thì cạn
        if (weather == WeatherType.RAIN) {
            currentWaterLevel = Math.min(maxWaterLevel, currentWaterLevel + 0.5f);
        } else if (weather == WeatherType.DROUGHT) {
            currentWaterLevel = Math.max(0, currentWaterLevel - 0.2f);
        }
    }

    @Override
    protected void postClimateUpdate() {
        // 2. CƠ CHẾ ĐẶC TRƯNG: Đóng băng mặt hồ dựa trên nhiệt độ đã được làm mượt
        if (this.temperature < 0 && !isFrozen) {
            freezeLake();
        } else if (this.temperature >= 0 && isFrozen) {
            unfreezeLake();
        }
    }

    @Override
    protected void generateNaturalResources() {
        // Nếu hồ đang đóng băng thì không sinh tài nguyên
        if (isFrozen) return;

        // Sinh ra rong rêu dưới đáy hồ (có thể đóng vai trò vừa là thức ăn, vừa là vật cản)
        if (random.nextFloat() < 0.03f) {
            // TODO: Lấy tọa độ random hợp lệ từ Boundary
            Vector2D weedPos = new Vector2D(random.nextInt(100), random.nextInt(100));
            if (terrain.containsPosition(weedPos)) {
                // Rong rêu: Sinh dưỡng thấp, tồn tại lâu trong môi trường nước
                resources.spawnFood(weedPos, 2.0f, FoodType.APPLE, 800);
            }
        }
    }

    // ----------------------------------------------------------------
    //  Các phương thức phụ trợ đặc thù của Hồ Nước
    // ----------------------------------------------------------------

    /**
     * Kích hoạt trạng thái đóng băng: 
     * Đổi địa hình thành BĂNG và tiêu diệt toàn bộ sinh vật thủy sinh.
     */
    private void freezeLake() {
        this.isFrozen = true;

        // Ép tử toàn bộ sinh vật hiện tại trong hồ
        for (Organism o : registry.getAllAlive()) {
            // Chuyển sang trạng thái DEAD (Environment lớp cha sẽ tự quét 
            // biến chúng thành thịt và phát âm thanh ORGANISM_DIED)
            o.setState(OrganismState.DEAD);
        }

        // TODO: (Mở rộng) Có thể yêu cầu TerrainComponent chuyển hàng loạt 
        // ô DEEP_WATER thành ICE để sinh vật trên cạn có thể đi lên trên.
        
        // Báo cho UI phát âm thanh "Rắc rắc" của băng kết
        // events.publish("EVENT_LAKE_FROZEN"); 
    }

    private void unfreezeLake() {
        this.isFrozen = false;
        // Băng tan, bề mặt trở lại thành DEEP_WATER
        // events.publish("EVENT_LAKE_UNFROZEN");
    }

    /**
     * Yêu cầu từ thiết kế: Tính tổng giá trị dinh dưỡng của sinh quyển trong hồ.
     * @return Tổng dinh dưỡng của tất cả sinh vật còn sống.
     */
    public float getTotalBiosphereNutrition() {
        float totalNutrition = 0f;
        for (Organism o : registry.getAllAlive()) {
            // Giả định đối tượng Stats của sinh vật có hàm getNutritionalValue()
            totalNutrition += o.getStats().getNutritionalValue();
        }
        return totalNutrition;
    }

    // --- Getters ---
    public float getCurrentWaterLevel() { return currentWaterLevel; }
    public boolean isFrozen() { return isFrozen; }
}