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
     * không phải ăn. Thirst tăng mỗi tick nhưng được bù lại bằng hấp thụ độ ẩm không khí/đất.
     * Cây trong môi trường ẩm ướt (Forest) hồi đủ; cây trong đồng cỏ khô phụ thuộc thêm vào nguồn nước gần đó.
     */
    @Override
    protected void applyMetabolismDecay(float seasonMultiplier, float thirstMultiplier) {
        stats.applyThirstOnlyDecay(thirstMultiplier);
        if (environment != null) {
            float humidityFactor = environment.getHumidity()
                    / AppConfig.getFloat("organism.stats.humidityMax");
            float absorption = humidityFactor * AppConfig.getFloat("plant.humidity.absorptionRate");

            // Nguồn nước gần đó (sông/hồ) tăng thêm lượng hấp thụ
            boolean waterNearby = environment.getResources()
                    .getFoodNear(position, nutritionAsorbRadius)
                    .stream().anyMatch(FoodItem::isWater);
            if (waterNearby) {
                absorption += AppConfig.getFloat("plant.water.absorptionBonus");
            }

            stats.absorbMoisture(absorption);
        }
    }

    /**
     * Simulates the biological process of photosynthesis.
     * <p>
     * The process is governed by the following logic:
     * 1. Check for sufficient light level from the environment.
     * 2. Calculate a humidity factor (humidity / max humidity) which acts as a catalyst.
     * 3. Compute energy generated based on light, humidity factor, and the plant's specific rate.
     * 4. Convert the generated energy into health (HP) recovery for the plant.
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
     * Simulates the absorption of nutrients and water from the surrounding environment.
     * <p>
     * The process follows these steps:
     * 1. Query the environment for available FoodItems (nutrients/water) within the absorption radius.
     * 2. Identify the nearest available resource to prioritize efficiency.
     * 3. Consume the resource, which updates the plant's survival statistics (hunger/thirst levels).
     * 4. Remove finite resources from the environment's resource manager.
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

    // -1 lớn để lần đầu luôn sẵn sàng sinh sản (không cần chờ cooldown)
    protected int lastReproduceTick = Integer.MIN_VALUE / 2;

    @Override
    public void reproduce() {
        if (environment == null || !growth.isAdult()) return;

        // Cooldown: mỗi cây có thể sinh nhiều lần, nhưng cần đủ thời gian giữa các lần
        int currentTick = environment.getTime().getCurrentTick();
        String species = getClass().getSimpleName().toLowerCase();
        String cooldownStr = AppConfig.get("plant." + species + ".reproduce.cooldownTicks");
        int cooldown = cooldownStr != null ? Integer.parseInt(cooldownStr.trim())
                : AppConfig.getInt("plant.reproduce.cooldownTicks");
        if (currentTick - lastReproduceTick < cooldown) return;

        // Cây không đói — chỉ dùng ngưỡng khát để quyết định sinh sản
        float thirstThreshold = AppConfig.getFloat("plant.reproduce.thirstThreshold");
        if (stats.getThirstLevel() > thirstThreshold) return;

        // Xác suất sinh sản mỗi lần cooldown hết
        float chance = AppConfig.getFloat("plant.reproduce.chance");

        // Population cap: kiểm tra giới hạn quần thể của loài này
        String maxPopStr = AppConfig.get("plant." + species + ".maxPopulation");
        if (maxPopStr != null) {
            int maxPop = Integer.parseInt(maxPopStr.trim());
            int currentPop = environment.getRegistry().getAllAlive(getClass()).size();
            if (currentPop >= maxPop) return;
            // Soft cap: giảm tỉ lệ sinh tuyến tính khi vượt 70% mức tối đa
            float ratio = (float) currentPop / maxPop;
            if (ratio > 0.7f) {
                chance *= (1f - (ratio - 0.7f) / 0.3f);
            }
        }

        if (Math.random() >= chance) return;

        // Đặt cooldown ngay khi chance pass — kể cả khi terrain không hợp lệ,
        // tránh vòng lặp thử lại mỗi tick (gây chậm simulation).
        lastReproduceTick = currentTick;

        int offspringCount = AppConfig.getInt("plant.reproduce.offspringCount");
        float spawnRadius = AppConfig.getFloat("plant.reproduce.spawnRadius");

        for (int i = 0; i < offspringCount; i++) {
            float offsetX = (float) (Math.random() * 2 - 1) * spawnRadius;
            float offsetY = (float) (Math.random() * 2 - 1) * spawnRadius;
            Vector2D childPos = new Vector2D(
                    position.getX() + offsetX,
                    position.getY() + offsetY
            );
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
