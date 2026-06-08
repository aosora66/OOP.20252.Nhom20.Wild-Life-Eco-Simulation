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
    protected Plant(String id, String speciesName, Vector2D startPos, TerrainType startTer, Environment startEnv, GrowthComponent growth, SurvivalStatsComponent stats, AdaptabilityComponent adaptability) {
        super(id, speciesName, startPos, startTer, startEnv, growth, stats, adaptability);
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
}
