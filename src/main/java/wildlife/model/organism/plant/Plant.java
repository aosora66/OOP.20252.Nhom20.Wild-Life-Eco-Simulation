package wildlife.model.organism.plant;
import wildlife.model.environment.Environment;
import wildlife.model.environment.dto.FoodItem;
import wildlife.model.environment.enums.TerrainType;
import wildlife.model.organism.Organism;
import wildlife.model.organism.component.AdaptabilityComponent;
import wildlife.model.organism.component.GrowthComponent;
import wildlife.model.organism.component.SurvivalStatsComponent;
import wildlife.util.AppConfig;
import wildlife.util.Vector2D;

import java.util.List;

public abstract class Plant extends Organism {

    protected float photosynthesisRate;
    protected float lightLevelToPhotosynthesis;
    protected float nutritionAsorbRadius;
    /**
     * Constructor for Plant entities.
     */
    protected Plant(String id, String speciesName, Vector2D startPos, Environment startEnv, GrowthComponent growth, SurvivalStatsComponent stats, AdaptabilityComponent adaptability) {
        super(id, speciesName, startPos, startEnv, growth, stats, adaptability);
    }

    /**
     * Cây không có khái niệm đói — năng lượng đến từ quang hợp (photosynthesis()),
     * không phải ăn. Chỉ decay mức khát mỗi tick, hungerLevel luôn giữ ở 0.
     */
    @Override
    protected void applyMetabolismDecay(float seasonMultiplier, float thirstMultiplier) {
        stats.applyThirstOnlyDecay(thirstMultiplier);
    }

    /**
     * Mô phỏng quá trình quang hợp của thực vật.
     *
     * Quá trình được thực hiện theo logic sau:
     * 1. Kiểm tra mức ánh sáng từ môi trường có đủ hay không.
     * 2. Tính hệ số độ ẩm (độ ẩm hiện tại / độ ẩm tối đa), đóng vai trò như một chất xúc tác.
     * 3. Tính lượng năng lượng tạo ra dựa trên ánh sáng, hệ số độ ẩm và tốc độ quang hợp riêng của loài cây.
     * 4. Chuyển đổi lượng năng lượng tạo ra thành lượng hồi phục sức khỏe (HP) cho cây.
     */
    public void photosynthesis() {
        Environment currentEnvironment = getEnvironment();
        if (currentEnvironment == null) return;

        // Check if there is enough light for the process
        float lightLevel = currentEnvironment.getLightLevel();
        if (lightLevel < lightLevelToPhotosynthesis) {
            return;
        }

        // Use humidity as a efficiency multiplier
        float humidity = currentEnvironment.getHumidity();
        float humidityFactor = humidity / AppConfig.getFloat("organism.stats.humidityMax");

        // Calculate total energy and restore HP based on nutrition ratio
        float energy = lightLevel * humidityFactor * photosynthesisRate;
        float hpGain = energy * AppConfig.getFloat("organism.stats.nutritionToHpRatio");
        stats.restoreHp(hpGain);
    }

    /**
     * Mô phỏng quá trình hấp thụ chất dinh dưỡng và nước từ môi trường xung quanh.
     *
     * Quá trình được thực hiện theo các bước sau:
     * 1. Truy vấn môi trường để tìm các FoodItem (nguồn dinh dưỡng hoặc nước)
     *    khả dụng trong phạm vi hấp thụ của thực vật.
     * 2. Xác định nguồn tài nguyên gần nhất nhằm tối ưu hiệu quả hấp thụ.
     * 3. Tiêu thụ nguồn tài nguyên đó, đồng thời cập nhật các chỉ số sinh tồn
     *    của cây (mức đói và mức khát).
     * 4. Loại bỏ các tài nguyên hữu hạn đã được sử dụng khỏi bộ quản lý tài nguyên
     *    của môi trường.
     */
    public void absorbNutrients() {
        Environment currentEnvironment = getEnvironment();
        if (currentEnvironment == null) return;

        // Scan environment for nutrients/water in the absorption radius
        List<FoodItem> nearby = currentEnvironment.getResources().getFoodNear(position, nutritionAsorbRadius);

        // Find the closest available resource
        FoodItem nutrients = null;
        float minDistance = Float.MAX_VALUE;
        for (FoodItem item : nearby) {
            float dist = item.position().distanceTo(position);
            if (dist < minDistance) {
                minDistance = dist;
                nutrients = item;
            }
        }

        // If a resource is found, consume it. Water sources are permanent.
        if (nutrients == null) return;

        stats.consume(nutrients.nutritionalValue(), nutrients.isWater());
        if (!nutrients.isWater()) {
            currentEnvironment.getResources().consume(nutrients);
        }
    }

    protected boolean hasReproduced = false;

    @Override
    public void reproduce() {
        if (hasReproduced || environment == null) return;

        // check for maturity
        if (!growth.isAdult()) return;

        // check for health level (using a threshold from config)
        // Cây không có khái niệm đói — dùng mức khát để quyết định có đủ điều kiện sinh sản không
        float thirstThreshold = AppConfig.getFloat("plant.reproduce.thirstThreshold");
        if (stats.getThirstLevel() > thirstThreshold) return;

        // reproduce add number of offspring into enviroment
        // every organism will only reproduce once in their life
        hasReproduced = true;

        // specific number of offspring will get from Appconfig
        int offspringCount = AppConfig.getInt("plant.reproduce.offspringCount");
        float spawnRadius = AppConfig.getFloat("plant.reproduce.spawnRadius");

        for (int i = 0; i < offspringCount; i++) {
            // Random spawn around the current plant
            float offsetX = (float) (Math.random() * 2 - 1) * spawnRadius;
            float offsetY = (float) (Math.random() * 2 - 1) * spawnRadius;

            // Calculate child position manually (Vector2D has no add method)
            Vector2D childPos = new Vector2D(
                    position.getX() + offsetX,
                    position.getY() + offsetY
            );

            // Optional: check if position is passable before adding
            if (environment.getTerrain().isPassable(childPos, this)) {
                addOffspring(childPos);
            }
        }
    }

    /**
     * Subclasses must implement this to create and add their specific instance to the environment.
     */
    protected abstract void addOffspring(Vector2D pos);


}
