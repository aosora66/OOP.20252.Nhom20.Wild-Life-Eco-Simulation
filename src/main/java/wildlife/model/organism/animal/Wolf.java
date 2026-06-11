package wildlife.model.organism.animal;

import wildlife.model.environment.Environment;
import wildlife.model.environment.enums.FoodType;
import wildlife.model.environment.enums.TerrainType;
import wildlife.model.organism.component.AdaptabilityComponent;
import wildlife.model.organism.component.GrowthComponent;
import wildlife.model.organism.component.SurvivalStatsComponent;
import wildlife.util.AppConfig;
import wildlife.util.Vector2D;

public class Wolf extends Animal {
    public Wolf(String id,
                String speciesName,
                Vector2D startPos,
                TerrainType startTer,
                Environment startEnv,
                GrowthComponent growth,
                SurvivalStatsComponent stats,
                AdaptabilityComponent adaptability,
                String gender) {
        super(id, speciesName, startPos, startTer, startEnv, growth, stats, adaptability);
        this.gender = gender;
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

        // 1. Săn mồi khi đói (Ưu tiên trung bình: 20)
        addStrategy(new wildlife.model.brain.HunterStrategy(
                this.speed * huntSpeedMult,
                this.vision,
                this.interactionRadius,
                this.combatPower,
                huntHungerThreshold,
                "Rabbit", "Deer"
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
        // Implementation for reproduction
    }
}
