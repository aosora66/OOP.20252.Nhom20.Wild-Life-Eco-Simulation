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
 * Voi — apex predator thực vật. Không săn mồi, nhưng khiến MỌI động vật khác phải bỏ chạy.
 * Cơ chế: isApexPredator() = true → ScaredStrategy của mọi loài tự động phát hiện và né tránh.
 * Chỉ ăn thực vật, máu nhiều, chậm nhưng tầm nhìn xa và sức mạnh rất cao (khi bị tấn công).
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

    /** Voi là apex predator — mọi ScaredStrategy tự động nhận diện và bỏ chạy. */
    @Override
    public boolean isApexPredator() { return true; }

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
        // TODO: sinh voi con
    }
}
