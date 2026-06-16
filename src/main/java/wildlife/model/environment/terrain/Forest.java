package wildlife.model.environment.terrain;

import wildlife.model.environment.Environment;
import wildlife.model.environment.component.*;
import wildlife.model.environment.enums.Season;
import wildlife.model.environment.enums.TerrainType;
import wildlife.model.environment.enums.WeatherType;
import wildlife.model.environment.event.EnvironmentEventPublisher;
import wildlife.util.Boundary;
import wildlife.util.Vector2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import wildlife.model.environment.enums.ObstacleType;

/**
 * Môi trường Rừng Rậm (Forest)
 * Đặc trưng: Nhiều rào cản tự nhiên, ánh sáng thấp, có lợi thế tàng hình cực cao.
 * Sinh tài nguyên phụ thuộc chặt chẽ vào gốc cây và thời tiết.
 */
public class Forest extends Environment {

    // ----------------------------------------------------------------
    //  Trạng thái riêng của Rừng Rậm
    // ----------------------------------------------------------------
    private final List<Vector2D> treeLocations; // Lưu vị trí gốc cây cổ thụ
    private final Random random;

    // ----------------------------------------------------------------
    //  Constructor
    // ----------------------------------------------------------------
    public Forest(String id, String name, Boundary boundary) {
        super(
                id, name,
                70.0f, // Độ ẩm cao
                22.0f, // Nhiệt độ trung bình, mát mẻ
                0.5f,  // Ánh sáng thấp do tán lá
                new TimeComponent(240, 2400),
                new TerrainComponent(boundary, TerrainType.FOREST),
                new OrganismRegistry(),
                new ResourceManager(),
                new EnvironmentEventPublisher("sounds/forest_ambient.wav")
        );
        
        this.random = new Random();
        this.treeLocations = new ArrayList<>();

        // Trồng rừng (Khởi tạo vật cản ban đầu)
        initializeFlora();
    }

    /**
     * Khởi tạo các cây cổ thụ và bụi rậm dựa trên kích thước thật của bản đồ.
     */
    private void initializeFlora() {
        // Trồng 3 cây cổ thụ ở các vị trí ngẫu nhiên hợp lệ trong bản đồ
        for (int i = 0; i < 3; i++) {
            Vector2D treePos = terrain.getRandomValidPosition(random);
            treeLocations.add(treePos);
            
            // Cây cổ thụ là vật thể rắn cản đường
            resources.placeObstacle(treePos, ObstacleType.TREE);
        }

        // Rải thêm 5 bụi rậm làm nơi trú ẩn (Tương tác với getStealthModifier)
        for (int i = 0; i < 5; i++) {
            Vector2D bushPos = terrain.getRandomValidPosition(random);
            
            // Chỉ định rõ đây là Bụi rậm (Thỏ/Hươu lách qua được, Sói/Hổ bị chặn)
            resources.placeObstacle(bushPos, ObstacleType.BUSH);
        }
    }

    // ----------------------------------------------------------------
    //  Ghi đè 3 hàm hợp đồng từ lớp cha
    // ----------------------------------------------------------------

    @Override
    protected void applySeasonEffect() {
        // Tán lá rừng giúp điều hòa khí hậu: Nhiệt độ và độ ẩm biến thiên ít hơn
        // Lấy chênh lệch giữa thực tế và nhiệt độ trung bình (22) rồi chia đôi
        float tempBase = 22.0f + (time.getTemperature() - 22.0f) * 0.5f;
        
        // Độ ẩm luôn giữ ở mức cao, không bị khô cạn hoàn toàn
        float humidBase = Math.max(40.0f, time.getHumidity());

        // Ánh sáng luôn bị trừ đi do tán lá cản lại
        float lightBase = Math.max(0.1f, this.lightLevel - 0.3f);

        switch (time.getCurrentSeason()) {
            case BREEDING -> {
                // Mùa sinh sản: thời tiết ôn hòa, nhiệt độ dao động nhỏ, độ ẩm dao động nhẹ
                this.temperature = tempBase + (random.nextFloat() * 2.0f - 1.0f); // +/- 1.0 độ C
                this.humidity = Math.max(0.0f, Math.min(100.0f, humidBase + (random.nextFloat() * 6.0f - 3.0f))); // +/- 3%
                this.lightLevel = Math.max(0.0f, Math.min(1.0f, lightBase + (random.nextFloat() * 0.04f - 0.02f))); // +/- 0.02
            }
            case DROUGHT -> {
                // Mùa hạn hán: nhiệt độ ấm hơn, độ ẩm giảm nhẹ ngẫu nhiên, ánh sáng tăng nhẹ do thưa lá
                this.temperature = tempBase + (random.nextFloat() * 3.0f - 0.5f); // -0.5 đến +2.5 độ C
                this.humidity = Math.max(0.0f, Math.min(100.0f, humidBase - random.nextFloat() * 5.0f)); // giảm thêm tới 5%
                this.lightLevel = Math.max(0.0f, Math.min(1.0f, lightBase + random.nextFloat() * 0.08f)); // tăng thêm tới 0.08
            }
            default -> { // NORMAL
                // Mùa bình thường: biến động rất nhỏ
                this.temperature = tempBase + (random.nextFloat() * 1.0f - 0.5f); // +/- 0.5 độ C
                this.humidity = Math.max(0.0f, Math.min(100.0f, humidBase + (random.nextFloat() * 4.0f - 2.0f))); // +/- 2%
                this.lightLevel = Math.max(0.0f, Math.min(1.0f, lightBase + (random.nextFloat() * 0.02f - 0.01f))); // +/- 0.01
            }
        }
    }

    @Override
    protected void applyWeatherEffect() {
        WeatherType weather = time.getCurrentWeather();

        if (weather == WeatherType.RAIN) {
            this.humidity = 100.0f; // Mưa làm độ ẩm bão hòa
        }
    }

    @Override
    protected void generateNaturalResources() {
        Season currentSeason = time.getCurrentSeason();
        
        // Hệ số rơi rụng cơ bản
        float dropChance = 0.02f; 

        // Phụ thuộc vào mùa (Mùa sinh sản táo rụng nhiều, hạn hán thì hiếm)
        if (currentSeason == Season.BREEDING) {
            dropChance = 0.05f; 
        } else if (currentSeason == Season.DROUGHT) {
            dropChance = 0.005f; 
        }

        // CƠ CHẾ ĐẶC TRƯNG: Rụng quả quanh gốc cây
        if (random.nextFloat() < dropChance && !treeLocations.isEmpty()) {
            Vector2D treePos = treeLocations.get(random.nextInt(treeLocations.size()));
            
            // Quả rơi trong bán kính 10 đơn vị quanh gốc cây (Dùng nextFloat)
            Vector2D applePos = new Vector2D(
                    treePos.getX() + (random.nextFloat() * 20 - 10), // Lệch [-10.0 đến 10.0]
                    treePos.getY() + (random.nextFloat() * 20 - 10)
            );

            if (terrain.containsPosition(applePos)) {
                // Sinh ra quả táo: Dinh dưỡng tốt, tồn tại khá lâu
                resources.spawnFood(applePos, 5.0f, false, 600);
            }
        }
    }
}