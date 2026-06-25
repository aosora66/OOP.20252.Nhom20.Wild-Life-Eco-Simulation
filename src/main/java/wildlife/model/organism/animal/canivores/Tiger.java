package wildlife.model.organism.animal.canivores;

import wildlife.model.environment.Environment;
import wildlife.model.environment.enums.FoodType;
import wildlife.model.organism.animal.Animal;
import wildlife.model.organism.animal.hebivores.Deer;
import wildlife.model.organism.animal.hebivores.Rabbit;
import wildlife.model.organism.component.AdaptabilityComponent;
import wildlife.model.organism.component.GrowthComponent;
import wildlife.model.organism.component.SurvivalStatsComponent;
import wildlife.util.AppConfig;
import wildlife.util.Vector2D;

public class Tiger extends Animal {
    public Tiger(String id,
                 String speciesName,
                 Vector2D startPos,
                 Environment startEnv,
                 GrowthComponent growth,
                 SurvivalStatsComponent stats,
                 AdaptabilityComponent adaptability) {
        super(id, speciesName, startPos, startEnv, growth, stats, adaptability, "CARNIVORE");
        this.combatPower = AppConfig.getFloat("animal.tiger.combatPower");
        this.vision = AppConfig.getFloat("animal.tiger.vision");
        this.speed = AppConfig.getFloat("animal.tiger.speed");
        this.interactionRadius = AppConfig.getFloat("animal.tiger.eatRadius");
        this.diet.add(FoodType.MEAT);
        initStrategies();
    }

    @Override
    protected void onTick(int currentTick) {
        executeStrategy(currentTick);
    }

    @Override
    protected void addSurvivalStrategies() {
        float huntSpeedMult = AppConfig.getFloat("animal.tiger.hunt.speedMultiplier");
        float huntHungerThreshold = AppConfig.getFloat("animal.tiger.hunt.hungerThreshold");
        int huntSprintSteps = AppConfig.getInt("animal.tiger.hunt.sprintSteps");

        // 1. Khi đói thì săn đuổi; khi no vẫn cắn con mồi đã lọt sát tầm đánh.
        addStrategy(new wildlife.model.brain.HunterStrategy(
                this.speed * huntSpeedMult,
                this.vision,
                this.interactionRadius,
                this.combatPower,
                huntHungerThreshold,
                huntSprintSteps,
                true,
                Rabbit.class, Deer.class, Hunter.class
        ));

        // 2. Tìm nước hoặc đi dạo khi không săn (Ưu tiên thấp: 10)
        addStrategy(new wildlife.model.brain.PassiveStrategy(
                this.speed,
                this.vision,
                this.interactionRadius,
                this.defaultHungerSearchThreshold,
                this.defaultThirstSearchThreshold
        ));
    }

    @Override
    public void reproduce() {
        reproduceSameSpecies();
    }
}
