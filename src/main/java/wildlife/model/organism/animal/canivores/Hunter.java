package wildlife.model.organism.animal.canivores;

import wildlife.model.brain.HunterStrategy;
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
 * Thợ săn — săn được MỌI loài động vật (Animal.class), sát thương rất cao.
 * Đánh đổi: máu thấp (50 HP) và chịu khát kém (thirstDecayRate cao trong config).
 * Không sợ bất cứ ai, kể cả Voi — nhưng chết nhanh nếu bị phản công.
 */
public class Hunter extends Animal {

    public Hunter(String id,
                  String speciesName,
                  Vector2D startPos,
                  Environment startEnv,
                  GrowthComponent growth,
                  SurvivalStatsComponent stats,
                  AdaptabilityComponent adaptability,
                  String gender) {
        super(id, speciesName, startPos, startEnv, growth, stats, adaptability, "HUNTER");
        this.combatPower       = AppConfig.getFloat("animal.hunter.combatPower");
        this.vision            = AppConfig.getFloat("animal.hunter.vision");
        this.speed             = AppConfig.getFloat("animal.hunter.speed");
        this.interactionRadius = AppConfig.getFloat("animal.hunter.eatRadius");
        this.diet.add(FoodType.MEAT);
        initStrategies();
    }

    @Override
    protected void addSurvivalStrategies() {
        float huntSpeedMult       = AppConfig.getFloat("animal.hunter.hunt.speedMultiplier");
        float huntHungerThreshold = AppConfig.getFloat("animal.hunter.hunt.hungerThreshold");

        // Săn mọi loài động vật — Animal.class bắt tất cả subclass trong registry
        addStrategy(new HunterStrategy(
                this.speed * huntSpeedMult,
                this.vision,
                this.interactionRadius,
                this.combatPower,
                huntHungerThreshold,
                Animal.class
        ));

        // Tìm thịt sẵn / nước khi không săn
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
        // TODO: sinh thợ săn mới
    }
}
