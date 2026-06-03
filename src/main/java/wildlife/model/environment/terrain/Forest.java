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

/**
 * Môi trường Rừng Rậm (Forest)
 * Đặc trưng: Nhiều rào cản tự nhiên, ánh sáng thấp, có lợi thế tàng hình cực cao.
 * Sinh tài nguyên (quả, nấm) phụ thuộc chặt chẽ vào gốc cây và thời tiết.
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
     * Khởi tạo các cây cổ thụ và bụi rậm.
     */
    private void initializeFlora() {
        // Trồng một vài cây cổ thụ làm gốc sinh thái
        // (Trong thực tế bạn có thể dùng vòng lặp để sinh ngẫu nhiên theo diện tích)
        Vector2D tree1 = new Vector2D(30, 40);
        Vector2D tree2 = new Vector2D(70, 60);
        
        treeLocations.add(tree1);
        treeLocations.add(tree2);

        resources.placeObstacle(tree1); 
        resources.placeObstacle(tree2);

        // Rải thêm bụi rậm làm nơi trú ẩn (Tương tác với getStealthModifier)
        resources.placeObstacle(new Vector2D(35, 45));
        resources.placeObstacle(new Vector2D(80, 20));
    }

    // ----------------------------------------------------------------
    //  Ghi đè 3 hàm hợp đồng từ lớp cha
    // ----------------------------------------------------------------

    @Override
    protected void applySeasonEffect() {
        // Tán lá rừng giúp điều hòa khí hậu: Nhiệt độ và độ ẩm biến thiên ít hơn
        // Lấy chênh lệch giữa thực tế và nhiệt độ trung bình (22) rồi chia đôi
        this.temperature = 22.0f + (time.getTemperature() - 22.0f) * 0.5f;
        
        // Độ ẩm luôn giữ ở mức cao, không bị khô cạn hoàn toàn
        this.humidity = Math.max(40.0f, time.getHumidity());

        // Ánh sáng luôn bị trừ đi do tán lá cản lại
        this.lightLevel = Math.max(0.1f, time.getLightLevel() - 0.3f);
    }

    @Override
    protected void applyWeatherEffect() {
        WeatherType weather = time.getCurrentWeather();

        if (weather == WeatherType.RAIN) {
            this.humidity = 100.0f; // Mưa làm độ ẩm bão hòa

            // CƠ CHẾ ĐẶC TRƯNG: Nấm mọc sau mưa dưới gốc cây
            if (random.nextFloat() < 0.05f && !treeLocations.isEmpty()) {
                // Lấy ngẫu nhiên một cây cổ thụ
                Vector2D treePos = treeLocations.get(random.nextInt(treeLocations.size()));
                
                // Mọc nấm cách gốc cây một khoảng nhỏ
                Vector2D mushroomPos = new Vector2D(
                        treePos.getX() + (random.nextInt(10) - 5), 
                        treePos.getY() + (random.nextInt(10) - 5)
                );

                if (terrain.containsPosition(mushroomPos)) {
                    // Sinh ra nấm: Tồn tại ngắn hạn (ví dụ: 200 tick) rồi héo
                    resources.spawnFood(mushroomPos, 3.0f, false, 200);
                }
            }
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
            
            // Quả rơi trong bán kính 10 đơn vị quanh gốc cây
            Vector2D applePos = new Vector2D(
                    treePos.getX() + (random.nextInt(20) - 10), 
                    treePos.getY() + (random.nextInt(20) - 10)
            );

            if (terrain.containsPosition(applePos)) {
                // Sinh ra quả táo: Dinh dưỡng tốt, tồn tại khá lâu
                resources.spawnFood(applePos, 5.0f, false, 600);
            }
        }
    }
}