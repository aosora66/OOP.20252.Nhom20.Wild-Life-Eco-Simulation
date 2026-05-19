package wildlife.model.organism;

import wildlife.model.environment.enums.TerrainType;
import wildlife.model.organism.component.AdaptabilityComponent;
import wildlife.model.organism.component.GrowthComponent;
import wildlife.model.organism.component.SurvivalStatsComponent;
import wildlife.util.Vector2D;

public abstract class Plant extends Organism{

    protected Plant(String id,
                    String speciesName,
                    Vector2D startPos,
                    TerrainType startEnv,
                    GrowthComponent growth,
                    SurvivalStatsComponent stats,
                    AdaptabilityComponent adaptability) {
        super(id, speciesName, startPos, startEnv, growth, stats, adaptability);
    }

    public abstract void photosynthesis();

    public abstract void absorbNutrients();

}
