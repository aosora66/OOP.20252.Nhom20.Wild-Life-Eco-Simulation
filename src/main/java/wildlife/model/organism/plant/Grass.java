package wildlife.model.organism.plant;

import wildlife.model.environment.Environment;
import wildlife.model.environment.dto.FoodItem;
import wildlife.model.environment.enums.TerrainType;
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
                 TerrainType startTer,
                 Environment startEnv,
                 GrowthComponent growth,
                 SurvivalStatsComponent stats,
                 AdaptabilityComponent adaptability) {
        super(id, speciesName, startPos, startTer, startEnv, growth, stats, adaptability);
        photosynthesisRate         = AppConfig.getFloat("plant.grass.photosynthesisRate");
        lightLevelToPhotosynthesis = AppConfig.getFloat("plant.grass.minLightLevel");
        nutritionAsorbRadius       = AppConfig.getFloat("plant.grass.nutrientsAsorbRadius");
        offspringCount             = AppConfig.getInt("plant.grass.offspringCount");
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
