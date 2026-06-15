package wildlife.model.organism.plant;

import wildlife.model.environment.Environment;
import wildlife.model.environment.enums.TerrainType;
import wildlife.model.organism.component.AdaptabilityComponent;
import wildlife.model.organism.component.GrowthComponent;
import wildlife.model.organism.component.SurvivalStatsComponent;
import wildlife.util.AppConfig;
import wildlife.util.Vector2D;

/**
 * woodenTree represents a sturdy, long-lived plant species.
 * Compared to Grass, it has a larger nutrient absorption radius and
 * a lower reproduction rate (smaller offspring count).
 */
public class WoodenTree extends Plant {
    public WoodenTree(String id,
                      String speciesName,
                      Vector2D startPos,
                      TerrainType startTer,
                      Environment startEnv,
                      GrowthComponent growth,
                      SurvivalStatsComponent stats,
                      AdaptabilityComponent adaptability) {
        super(id, speciesName, startPos, startTer, startEnv, growth, stats, adaptability);
        
        // Load species-specific biological rates from configuration
        this.photosynthesisRate         = AppConfig.getFloat("plant.woodentree.photosynthesisRate");
        this.lightLevelToPhotosynthesis = AppConfig.getFloat("plant.woodentree.minLightLevel");
        this.nutritionAsorbRadius       = AppConfig.getFloat("plant.woodentree.nutrientsAsorbRadius");
        this.offspringCount             = AppConfig.getInt("plant.woodentree.offspringCount");
    }

    @Override
    public void onTick(int currentTick) {
        // Standard plant metabolism: Generate energy from light and absorb water/nutrients from soil
        photosynthesis();
        absorbNutrients();
    }

    @Override
    protected void addOffspring(Vector2D pos) {
        // Offspring addition logic (to be handled by environment/registry)
    }
}
