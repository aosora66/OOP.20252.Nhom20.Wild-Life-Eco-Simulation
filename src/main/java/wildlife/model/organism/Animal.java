package wildlife.model.organism;

import wildlife.model.environment.Environment;
import wildlife.model.environment.dto.FoodItem;
import wildlife.model.organism.component.AdaptabilityComponent;
import wildlife.model.organism.component.GrowthComponent;
import wildlife.model.organism.component.SurvivalStatsComponent;
import wildlife.util.AppConfig;
import wildlife.util.SurvivalStrategy;
import wildlife.util.Vector2D;

public abstract class Animal extends Organism {

    protected String gender;
    protected float vision;
    protected float combatPower;
    protected float speed;
    protected float interactionRadius;      // eating/drinking/attacking radius
    protected float defaultHungerSearchThreshold;
    protected float defaultThirstSearchThreshold;

    protected Animal(String id,
                     String speciesName,
                     Vector2D startPos,
                     Environment startEnv,
                     GrowthComponent growth,
                     SurvivalStatsComponent stats,
                     AdaptabilityComponent adaptability) {
        super(id, speciesName, startPos, startEnv, growth, stats, adaptability);
        this.defaultHungerSearchThreshold = AppConfig.getFloat("organism.stats.hungerHpThreshold");
        this.defaultThirstSearchThreshold = AppConfig.getFloat("organism.stats.thirstHpThreshold");
    }

    /**
     * Mỗi loài chọn strategy phù hợp (Passive / Scared / Hunter).
     * Gọi {@link #initSurvivalStrategy()} ở cuối constructor subclass sau khi gán speed, vision...
     */
    protected abstract SurvivalStrategy createSurvivalStrategy();

    protected final void initSurvivalStrategy() {
        setStrategy(createSurvivalStrategy());
    }


    public void eating(FoodItem food) {
        if (food == null || currentEnvironment == null) return;
        stats.consume(food.nutritionalValue(), food.isWater());
        currentEnvironment.getResources().consume(food);
    }
}
