package wildlife.model.organism.animal.hebivores;

import wildlife.model.brain.PassiveStrategy;
import wildlife.model.brain.ScaredStrategy;
import wildlife.model.environment.Environment;
import wildlife.model.environment.enums.FoodType;
import wildlife.model.environment.enums.TerrainType;
import wildlife.model.organism.animal.Animal;
import wildlife.model.organism.component.AdaptabilityComponent;
import wildlife.model.organism.component.GrowthComponent;
import wildlife.model.organism.component.SurvivalStatsComponent;
import wildlife.util.AppConfig;
import wildlife.util.Vector2D;

public class Fish extends Animal {
    public Fish(String id,
                String speciesName,
                Vector2D startPos,
                Environment startEnv,
                GrowthComponent growth,
                SurvivalStatsComponent stats,
                AdaptabilityComponent adaptability,
                String gender) {
        super(id, speciesName, startPos, startEnv, growth, stats, adaptability, "HEBIVORE");
        this.combatPower = AppConfig.getFloat("animal.rabbit.combatPower");
        this.vision      = AppConfig.getFloat("animal.rabbit.vision");
        this.speed       = AppConfig.getFloat("animal.rabbit.speed");
        this.interactionRadius = AppConfig.getFloat("animal.rabbit.eatRadius");
        this.diet.add(FoodType.APPLE);
        initStrategies();
    }

    @Override
    protected void onTick(int currentTick) {
        executeStrategy(currentTick);
    }

    @Override
    protected void addSurvivalStrategies() {
        // Passive: Default behavior (search for water/food or wander)
        this.addStrategy(new PassiveStrategy(
                speed,
                vision,
                interactionRadius,
                defaultHungerSearchThreshold,
                defaultThirstSearchThreshold
        ));

        // Scared: Flee from predators (Tiger, Wolf)
        // Borrowing rabbit flee config as default
        float fleeMultiplier = AppConfig.getFloat("animal.rabbit.flee.speedMultiplier");
        int sprintSteps = AppConfig.getInt("animal.rabbit.flee.sprintSteps");

        this.addStrategy(new ScaredStrategy(
                speed * fleeMultiplier,
                vision,
                sprintSteps,
                "Tiger", "Wolf"
        ));
    }

    @Override
    public void reproduce() {

    }
}
