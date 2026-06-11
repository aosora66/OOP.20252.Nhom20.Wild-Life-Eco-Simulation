package wildlife.model.organism.animal;

import wildlife.model.environment.Environment;
import wildlife.model.environment.enums.FoodType;
import wildlife.model.environment.enums.TerrainType;
import wildlife.model.organism.Organism;
import wildlife.model.organism.component.AdaptabilityComponent;
import wildlife.model.organism.component.GrowthComponent;
import wildlife.model.organism.component.SurvivalStatsComponent;
import wildlife.util.AppConfig;
import wildlife.util.Vector2D;

import java.util.UUID;

public class Rabbit extends Animal{
    public Rabbit(String id,
                  String speciesName,
                  Vector2D startPos,
                  TerrainType startTer,
                  Environment startEnv,
                  GrowthComponent growth,
                  SurvivalStatsComponent stats,
                  AdaptabilityComponent adaptability,
                  String gender) {
        super(id, speciesName, startPos,startTer, startEnv, growth, stats, adaptability);
        this.gender      = gender;
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
        float fleeSpeedMult = AppConfig.getFloat("animal.rabbit.flee.speedMultiplier");
        int fleeSprintSteps = AppConfig.getInt("animal.rabbit.flee.sprintSteps");

        // 1. Chạy trốn khi thấy Tiger hoặc Wolf (Ưu tiên cao nhất: 30)
        addStrategy(new wildlife.model.brain.ScaredStrategy(
                this.speed * fleeSpeedMult,
                this.vision,
                fleeSprintSteps,
                "Tiger", "Wolf"
        ));

        // 2. Tìm thức ăn/nước uống mặc định (Ưu tiên thấp: 10)
        addStrategy(new wildlife.model.brain.PassiveStrategy(
                this.speed,
                this.vision,
                this.interactionRadius,
                this.defaultHungerSearchThreshold,
                this.defaultThirstSearchThreshold
        ));
    }

    @Override
    public Rabbit reproduce() {

        return null;
    }
}
