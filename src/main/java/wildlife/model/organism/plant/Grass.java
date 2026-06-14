package wildlife.model.organism.plant;

import wildlife.model.environment.Environment;
import wildlife.model.environment.dto.FoodItem;
import wildlife.model.organism.component.AdaptabilityComponent;
import wildlife.model.organism.component.GrowthComponent;
import wildlife.model.organism.component.SurvivalStatsComponent;
import wildlife.util.AppConfig;
import wildlife.util.Vector2D;

import java.util.List;

public class Grass extends Plant{
    public Grass(String id,
                 String speciesName,
                 Vector2D startPos,
                 Environment startEnv,
                 GrowthComponent growth,
                 SurvivalStatsComponent stats,
                 AdaptabilityComponent adaptability) {
        super(id, speciesName, startPos, startEnv, growth, stats, adaptability);
        photosynthesisRate         = AppConfig.getFloat("plant.grass.photosynthesisRate");
        lightLevelToPhotosynthesis = AppConfig.getFloat("plant.grass.minLightLevel");
        nutritionAsorbRadius       = AppConfig.getFloat("plant.grass.nutrientsAsorbRadius");
    }

    @Override
    public void onTick(int currentTick) {
        // Plants perform photosynthesis and absorb nutrients every tick.
        photosynthesis();
        absorbNutrients();
    }

    @Override
    protected void addOffspring(Vector2D pos) {

    }
}
