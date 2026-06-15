package wildlife.model.organism.animal.hebivores;

import wildlife.model.brain.PassiveStrategy;
import wildlife.model.brain.ScaredStrategy;
import wildlife.model.environment.Environment;
import wildlife.model.environment.enums.FoodType;
import wildlife.model.organism.animal.Animal;
import wildlife.model.organism.component.AdaptabilityComponent;
import wildlife.model.organism.component.GrowthComponent;
import wildlife.model.organism.component.SurvivalStatsComponent;
import wildlife.util.AppConfig;
import wildlife.util.Vector2D;

public class Rabbit extends Animal {

    public Rabbit(String id,
                  String speciesName,
                  Vector2D startPos,
                  Environment startEnv,
                  GrowthComponent growth,
                  SurvivalStatsComponent stats,
                  AdaptabilityComponent adaptability,
                  String gender) {
        super(id, speciesName, startPos, startEnv, growth, stats, adaptability, "HEBIVORE");
        this.combatPower       = AppConfig.getFloat("animal.rabbit.combatPower");
        this.vision            = AppConfig.getFloat("animal.rabbit.vision");
        this.speed             = AppConfig.getFloat("animal.rabbit.speed");
        this.interactionRadius = AppConfig.getFloat("animal.rabbit.eatRadius");
        this.diet.add(FoodType.APPLE);
        initStrategies();
    }

    @Override
    protected void addSurvivalStrategies() {
        float fleeSpeedMult = AppConfig.getFloat("animal.rabbit.flee.speedMultiplier");
        int   fleeSprintSteps = AppConfig.getInt("animal.rabbit.flee.sprintSteps");

        // Chạy trốn khi thấy Tiger hoặc Wolf, phản kháng khi HP <= 25% với 30% xác suất
        addStrategy(new ScaredStrategy(
                this.speed * fleeSpeedMult,
                this.vision,
                fleeSprintSteps,
                this.interactionRadius,
                0.25f,
                0.3f,
                "Tiger", "Wolf"
        ));

        // Tìm thức ăn/nước uống khi không có kẻ thù
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
        // TODO: tạo Rabbit con và thêm vào environment
    }
}
