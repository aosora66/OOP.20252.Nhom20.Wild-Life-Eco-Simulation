package wildlife.model.organism.animal.canivores;

import wildlife.model.brain.HunterStrategy;
import wildlife.model.brain.PassiveStrategy;
import wildlife.model.brain.ScaredStrategy;
import wildlife.model.environment.Environment;
import wildlife.model.environment.enums.FoodType;
import wildlife.model.organism.Organism;
import wildlife.model.organism.animal.Animal;
import wildlife.model.organism.component.AdaptabilityComponent;
import wildlife.model.organism.component.GrowthComponent;
import wildlife.model.organism.component.SurvivalStatsComponent;
import wildlife.util.AppConfig;
import wildlife.util.SoundManager;
import wildlife.util.Vector2D;

/**
 * Thợ săn — săn được MỌI loài động vật (Animal.class) trừ apex predator, sát thương rất cao.
 * Đánh đổi: máu thấp (50 HP) và chịu khát kém (thirstDecayRate cao trong config).
 * Khi chưa đói thì tránh thú săn mồi lớn; khi đói đủ ngưỡng thì lao vào combat.
 */
public class Hunter extends Animal {

    public Hunter(String id,
                  String speciesName,
                  Vector2D startPos,
                  Environment startEnv,
                  GrowthComponent growth,
                  SurvivalStatsComponent stats,
                  AdaptabilityComponent adaptability) {
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
        int huntSprintSteps       = AppConfig.getInt("animal.hunter.hunt.sprintSteps");

        // 1. Khi chưa đói thì con người né Wolf/Tiger; khi đói đủ ngưỡng thì lao vào săn/đánh.
        addStrategy(new ScaredStrategy(
                this.speed * 1.3f,
                this.vision,
                2,
                this.interactionRadius,
                0.4f,
                0.4f,
                huntHungerThreshold,
                Wolf.class, Tiger.class
        ));

        // 2. Săn mọi loài động vật — Animal.class bắt tất cả subclass trong registry
        //    (HunterStrategy tự loại apex predator khỏi danh sách con mồi)
        addStrategy(new HunterStrategy(
                this.speed * huntSpeedMult,
                this.vision,
                this.interactionRadius,
                this.combatPower,
                huntHungerThreshold,
                huntSprintSteps,
                Animal.class
        ));

        // 3. Tìm thịt sẵn / nước khi không săn
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

    @Override
    public void performAttack(Organism target, float damage) {
        SoundManager.playSequentialSoundEffects("GunLoad.wav", "GunFire.wav");
        super.performAttack(target, damage);
    }
}
