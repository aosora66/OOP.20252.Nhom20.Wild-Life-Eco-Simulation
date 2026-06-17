package wildlife.model.organism.animal.hebivores;

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

public class Deer extends Animal {
    public Deer(String id,
                String speciesName,
                Vector2D startPos,
                Environment startEnv,
                GrowthComponent growth,
                SurvivalStatsComponent stats,
                AdaptabilityComponent adaptability) {
        super(id, speciesName, startPos, startEnv, growth, stats, adaptability, "HEBIVORE");
        this.combatPower = AppConfig.getFloat("animal.deer.combatPower");
        this.vision = AppConfig.getFloat("animal.deer.vision");
        this.speed = AppConfig.getFloat("animal.deer.speed");
        this.interactionRadius = AppConfig.getFloat("animal.deer.eatRadius");
        this.diet.add(FoodType.APPLE);
        initStrategies();
    }

    /**
     * Factory method — tạo Deer với sinh học mặc định.
     */
    public static Deer create(Vector2D pos, Environment env) {
        return new Deer(
                UUID.randomUUID().toString(),
                "Deer",
                pos,
                env,
                new GrowthComponent(1000f, 45f, 0.15f, 0.8f),
                new SurvivalStatsComponent(60f, 15f, 0.08f, 0.08f),
                new AdaptabilityComponent(
                        List.of(TerrainType.GRASSLAND, TerrainType.FOREST),
                        new ValueRange(15f, 35f),
                        new ValueRange(5f, 45f),
                        new ValueRange(-100f, -0.1f)
                ),
                "FEMALE"
        );
    }

    @Override
    protected void onTick(int currentTick) {
        executeStrategy(currentTick);
    }

    @Override
    public void reproduce() { reproduceSameSpecies(); }

    @Override
    protected void addSurvivalStrategies() {
        float fleeSpeedMult = AppConfig.getFloat("animal.deer.flee.speedMultiplier");
        int fleeSprintSteps = AppConfig.getInt("animal.deer.flee.sprintSteps");

        // 1. Chạy trốn khi thấy Tiger hoặc Wolf (Ưu tiên cao nhất: 30)
        // phản kháng khi HP <= 25% với 20% xác suất
        addStrategy(new wildlife.model.brain.ScaredStrategy(
                this.speed * fleeSpeedMult,
                this.vision,
                fleeSprintSteps,
                this.interactionRadius,
                0.25f,
                0.2f,
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
    public void reproduce() {
        if (environment == null) return;
        int currentTick = environment.getTime().getCurrentTick();

        if (canReproduce(currentTick)) {
            lastReproduceTick = currentTick;

            int offspringCount = AppConfig.getInt("animal.deer.reproduce.offspringCount");
            float spawnRadius = AppConfig.getFloat("animal.deer.reproduce.spawnRadius");

            for (int i = 0; i < offspringCount; i++) {
                float offsetX = (float) (Math.random() * 2 - 1) * spawnRadius;
                float offsetY = (float) (Math.random() * 2 - 1) * spawnRadius;

                Vector2D childPos = new Vector2D(
                        position.getX() + offsetX,
                        position.getY() + offsetY
                );

                if (environment.getTerrain().isPassable(childPos, this)) {
                    environment.getRegistry().add(Deer.create(childPos, environment));
                }
            }
        }
    }
}
