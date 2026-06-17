package wildlife.model.organism.animal.hebivores;

import wildlife.model.brain.PassiveStrategy;
import wildlife.model.brain.ScaredStrategy;
import wildlife.model.environment.Environment;
import wildlife.model.environment.enums.FoodType;
import wildlife.model.environment.enums.TerrainType;
import wildlife.model.organism.animal.Animal;
import wildlife.model.organism.animal.canivores.Tiger;
import wildlife.model.organism.animal.canivores.Wolf;
import wildlife.model.organism.component.AdaptabilityComponent;
import wildlife.model.organism.component.GrowthComponent;
import wildlife.model.organism.component.SurvivalStatsComponent;
import wildlife.util.AppConfig;
import wildlife.util.Vector2D;

public class Deer extends Animal {
    public Deer(String id,
                String speciesName,
                Vector2D startPos,
                Environment startEnv,
                GrowthComponent growth,
                SurvivalStatsComponent stats,
                AdaptabilityComponent adaptability,
                String gender) {
        super(id, speciesName, startPos, startEnv, growth, stats, adaptability, "HEBIVORE");
        this.combatPower = AppConfig.getFloat("animal.deer.combatPower");
        this.vision = AppConfig.getFloat("animal.deer.vision");
        this.speed = AppConfig.getFloat("animal.deer.speed");
        this.interactionRadius = AppConfig.getFloat("animal.deer.eatRadius");
        this.diet.add(FoodType.APPLE);
        initStrategies();
    }

    @Override
    protected void onTick(int currentTick) {
        executeStrategy(currentTick);
    }

    /** Hươu ăn cỏ — gặm trực tiếp cây Grass. */
    @Override
    public boolean canGraze() { return true; }

    @Override
    protected void addSurvivalStrategies() {
        float fleeSpeedMult = AppConfig.getFloat("animal.deer.flee.speedMultiplier");
        int fleeSprintSteps = AppConfig.getInt("animal.deer.flee.sprintSteps");

        // 1. Chạy trốn khi thấy Tiger hoặc Wolf (Ưu tiên cao nhất: 30)
        // phản kháng khi HP <= 25% với 20% xác suất
        addStrategy(new ScaredStrategy(
                this.speed * fleeSpeedMult,
                this.vision,
                fleeSprintSteps,
                this.interactionRadius,
                0.25f,
                0.2f,
                Tiger.class, Wolf.class
        ));

        // 2. Tìm thức ăn/nước uống mặc định (Ưu tiên thấp: 10)
        addStrategy(new PassiveStrategy(
                this.speed,
                this.vision,
                this.interactionRadius,
                this.defaultHungerSearchThreshold,
                this.defaultThirstSearchThreshold
        ));
    }

}
