package wildlife.model.organism.plant;

import wildlife.model.environment.Environment;
import wildlife.model.environment.enums.TerrainType;
import wildlife.model.organism.Organism;
import wildlife.model.organism.component.AdaptabilityComponent;
import wildlife.model.organism.component.GrowthComponent;
import wildlife.model.organism.component.SurvivalStatsComponent;
import wildlife.util.Vector2D;

public abstract class Plant extends Organism {
    // viết tạm để tạo lớp
    protected Plant(String id, String speciesName, Vector2D startPos, TerrainType startTer, Environment startEnv, GrowthComponent growth, SurvivalStatsComponent stats, AdaptabilityComponent adaptability) {
        super(id, speciesName, startPos, startTer, startEnv, growth, stats, adaptability);
    }
}
