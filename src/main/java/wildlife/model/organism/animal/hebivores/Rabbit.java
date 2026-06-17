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
import wildlife.util.ValueRange;
import wildlife.util.Vector2D;

import java.util.List;
import java.util.UUID;

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

    /**
     * Factory method — tạo Rabbit với sinh học mặc định.
     */
    public static Rabbit create(Vector2D pos, Environment env) {
        return new Rabbit(
                UUID.randomUUID().toString(),
                "Rabbit",
                pos,
                env,
                new GrowthComponent(1000f, 50f, 0.2f, 0.8f),
                new SurvivalStatsComponent(40f, 10f, 0.1f, 0.1f),
                new AdaptabilityComponent(
                        List.of(TerrainType.GRASSLAND, TerrainType.FOREST),
                        new ValueRange(15f, 35f),
                        new ValueRange(5f, 45f),
                        new ValueRange(-100f, 0f)
                ),
                "MALE"
        );
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
        if (environment == null) return;
        int currentTick = environment.getTime().getCurrentTick();

        if (canReproduce(currentTick)) {
            lastReproduceTick = currentTick;

            int offspringCount = AppConfig.getInt("animal.rabbit.reproduce.offspringCount");
            float spawnRadius = AppConfig.getFloat("animal.rabbit.reproduce.spawnRadius");

            for (int i = 0; i < offspringCount; i++) {
                // Random spawn around the parent
                float offsetX = (float) (Math.random() * 2 - 1) * spawnRadius;
                float offsetY = (float) (Math.random() * 2 - 1) * spawnRadius;

                Vector2D childPos = new Vector2D(
                        position.getX() + offsetX,
                        position.getY() + offsetY
                );

                // Check if position is passable before adding offspring
                if (environment.getTerrain().isPassable(childPos, this)) {
                    environment.getRegistry().add(Rabbit.create(childPos, environment));
                }
            }
        }
    }
}
