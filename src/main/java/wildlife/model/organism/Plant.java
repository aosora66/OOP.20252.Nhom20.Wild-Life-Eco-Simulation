package wildlife.model.organism;

import wildlife.model.environment.Environment;
import wildlife.model.environment.enums.TerrainType;
import wildlife.model.organism.component.AdaptabilityComponent;
import wildlife.model.organism.component.GrowthComponent;
import wildlife.model.organism.component.SurvivalStatsComponent;
import wildlife.util.Vector2D;

public abstract class Plant extends Organism{
    protected float photosynthesisRate;
    protected float lightLevelToPhotosynthesis;
    protected float nutritionAsorbRadius;


    protected Plant(String id,
                    String speciesName,
                    Vector2D startPos,
                    Environment startEnv,
                    GrowthComponent growth,
                    SurvivalStatsComponent stats,
                    AdaptabilityComponent adaptability) {
        super(id, speciesName, startPos, startEnv, growth, stats, adaptability);
    }

    // Quang hợp
    public abstract void photosynthesis();

    // hấp thu dưỡng chất
    public abstract void absorbNutrients();

}
