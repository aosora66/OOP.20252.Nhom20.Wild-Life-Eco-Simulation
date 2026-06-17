package wildlife.model.organism.animal.hebivores;

import wildlife.model.brain.PassiveStrategy;
import wildlife.model.environment.Environment;
import wildlife.model.environment.enums.FoodType;
import wildlife.model.organism.animal.Animal;
import wildlife.model.organism.component.AdaptabilityComponent;
import wildlife.model.organism.component.GrowthComponent;
import wildlife.model.organism.component.SurvivalStatsComponent;
import wildlife.util.AppConfig;
import wildlife.util.Vector2D;

/**
 * Voi — động vật ăn cỏ đầu bảng. Không săn mồi và không bị thú ăn thịt chọn làm mồi.
 * Chỉ ăn thực vật, máu nhiều, chậm nhưng tầm nhìn xa và sức mạnh rất cao.
 */
public class Elephant extends Animal {

    public Elephant(String id,
                    String speciesName,
                    Vector2D startPos,
                    Environment startEnv,
                    GrowthComponent growth,
                    SurvivalStatsComponent stats,
                    AdaptabilityComponent adaptability,
                    String gender) {
        super(id, speciesName, startPos, startEnv, growth, stats, adaptability, "HERBIVORE");
        this.combatPower       = AppConfig.getFloat("animal.elephant.combatPower");
        this.vision            = AppConfig.getFloat("animal.elephant.vision");
        this.speed             = AppConfig.getFloat("animal.elephant.speed");
        this.interactionRadius = AppConfig.getFloat("animal.elephant.eatRadius");
        this.diet.add(FoodType.APPLE);
        initStrategies();
    }

    /** Voi là apex để HunterStrategy không chọn làm con mồi. */
    @Override
    public boolean isApexPredator() { return true; }

    /** Voi ăn cỏ — gặm trực tiếp cây Grass (ngoài ăn quả rụng). */
    @Override
    public boolean canGraze() { return true; }

    @Override
    protected void addSurvivalStrategies() {
        // Voi không sợ ai — chỉ tìm thức ăn và nước
        addStrategy(new PassiveStrategy(
                this.speed,
                this.vision,
                this.interactionRadius,
                this.defaultHungerSearchThreshold,
                this.defaultThirstSearchThreshold
        ));
    }

    @Override
    protected void onTick(int currentTick) {
        executeStrategy(currentTick);
    }

    @Override
    public void reproduce() {
        reproduceSameSpecies();
    }
}
