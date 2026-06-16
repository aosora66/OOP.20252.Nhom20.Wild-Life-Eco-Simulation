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
     * 4. Remove the consumed resource from the environment's resource manager.
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

        // If a resource is found, consume it and remove it from the map
        if (nutrients == null) return;

        stats.consume(nutrients.nutritionalValue(), nutrients.isWater());
        currentEnvironment.getResources().consume(nutrients);
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