package wildlife.model.organism;

import wildlife.model.environment.dto.FoodItem;
import wildlife.model.environment.enums.TerrainType;
import wildlife.model.organism.component.AdaptabilityComponent;
import wildlife.model.organism.component.GrowthComponent;
import wildlife.model.organism.component.SurvivalStatsComponent;
import wildlife.util.Vector2D;

import java.util.List;

public abstract class Animal extends Organism {
    protected String gender;
    protected float vision;
    protected float combatPower;
    protected List<FoodItem> EdibleFood;

    protected Animal(String id,
                     String speciesName,
                     Vector2D startPos,
                     TerrainType startEnv,
                     GrowthComponent growth,
                     SurvivalStatsComponent stats,
                     AdaptabilityComponent adaptability) {
        super(id, speciesName, startPos, startEnv, growth, stats, adaptability);
    }

    public abstract void wandering();

    public abstract void hunting();

    // hàm được gọi khi đã tìm được ăn (có thể được gọi trong wandering() hoặc moving()
    public abstract void eating(FoodItem food);

    public abstract void drinking();

}
