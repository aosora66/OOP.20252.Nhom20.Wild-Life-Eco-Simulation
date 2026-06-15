package wildlife.model.organism.animal.hebivores;

import wildlife.model.environment.Environment;
import wildlife.model.environment.enums.FoodType;
import wildlife.model.environment.enums.TerrainType;
import wildlife.model.organism.animal.Animal;
import wildlife.model.organism.animal.AnimalTypes;
import wildlife.model.organism.component.AdaptabilityComponent;
import wildlife.model.organism.component.GrowthComponent;
import wildlife.model.organism.component.SurvivalStatsComponent;
import wildlife.util.AppConfig;
import wildlife.util.Vector2D;

public class Elephant extends Animal {
    public Elephant(String id,
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
        this.animalType = AnimalTypes.HEBIVORE;
        this.combatPower = AppConfig.getFloat("animal.elephant.combatPower");
        this.vision = AppConfig.getFloat("animal.elephant.vision");
        this.speed = AppConfig.getFloat("animal.elephant.speed");
        this.interactionRadius = AppConfig.getFloat("animal.elephant.eatRadius");
        this.diet.add(FoodType.APPLE);
        this.diet.add(FoodType.GRASS);
        initStrategies();
    }

    @Override
    protected void onTick(int currentTick) {
        executeStrategy(currentTick);
    }

    @Override
    protected void addSurvivalStrategies() {
        // Elephant is not afraid of anything, so it only has PassiveStrategy
        // to search for food and water.
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
        // Elephant reproduction logic could be added here if needed
        // For now, it follows the base canReproduce check in Animal.
    }
}
