package wildlife.model.organism.animal.canivores;

import wildlife.model.environment.Environment;
import wildlife.model.environment.enums.FoodType;
import wildlife.model.environment.enums.TerrainType;
import wildlife.model.organism.animal.Animal;
import wildlife.model.organism.animal.hebivores.Deer;
import wildlife.model.organism.animal.hebivores.Rabbit;
import wildlife.model.organism.component.AdaptabilityComponent;
import wildlife.model.organism.component.GrowthComponent;
import wildlife.model.organism.component.SurvivalStatsComponent;
import wildlife.util.AppConfig;
import wildlife.util.Vector2D;

public class Wolf extends Animal {
    public Wolf(String id,
                String speciesName,
                Vector2D startPos,
                Environment startEnv,
                GrowthComponent growth,
                SurvivalStatsComponent stats,
                AdaptabilityComponent adaptability) {
        super(id, speciesName, startPos, startEnv, growth, stats, adaptability, "CANIVORE");
        this.combatPower = AppConfig.getFloat("animal.wolf.combatPower");
        this.vision = AppConfig.getFloat("animal.wolf.vision");
        this.speed = AppConfig.getFloat("animal.wolf.speed");
        this.interactionRadius = AppConfig.getFloat("animal.wolf.eatRadius");
        this.diet.add(FoodType.MEAT);
        initStrategies();
    }

    @Override
    protected void onTick(int currentTick) {
        executeStrategy(currentTick);
    }

    @Override
    protected void addSurvivalStrategies() {
        float huntSpeedMult = AppConfig.getFloat("animal.wolf.hunt.speedMultiplier");
        float huntHungerThreshold = AppConfig.getFloat("animal.wolf.hunt.hungerThreshold");
        int huntSprintSteps = AppConfig.getInt("animal.wolf.hunt.sprintSteps");

        // 1. Predator chủ động đánh mồi khi gặp; Wolf không né Hunter.
        addStrategy(new wildlife.model.brain.HunterStrategy(
                this.speed * huntSpeedMult,
                this.vision,
                this.interactionRadius,
                this.combatPower,
                huntHungerThreshold,
                huntSprintSteps,
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
