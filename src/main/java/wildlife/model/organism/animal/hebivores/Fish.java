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
        this.combatPower = AppConfig.getFloat("animal.fish.combatPower");
        this.vision      = AppConfig.getFloat("animal.fish.vision");
        this.speed       = AppConfig.getFloat("animal.fish.speed");
        this.interactionRadius = AppConfig.getFloat("animal.fish.eatRadius");
        this.diet.add(FoodType.ALGAE);
        initStrategies();
    }

    /**
     * Factory method — tạo Fish với sinh học mặc định.
     */
    public static Fish create(Vector2D pos, Environment env) {
        return new Fish(
                UUID.randomUUID().toString(),
                "Fish",
                pos,
                env,
                new GrowthComponent(500f, 20f, 0.3f, 0.85f),
                new SurvivalStatsComponent(20f, 5f, 0.15f, 0.1f),
                new AdaptabilityComponent(
                        List.of(TerrainType.DEEP_WATER),
                        new ValueRange(18f, 28f),
                        new ValueRange(10f, 35f),
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
        float fleeMultiplier = AppConfig.getFloat("animal.fish.flee.speedMultiplier");
        int sprintSteps = AppConfig.getInt("animal.fish.flee.sprintSteps");

        this.addStrategy(new ScaredStrategy(
                speed * fleeMultiplier,
                vision,
                sprintSteps,
                interactionRadius,
                0.25f,
                0.2f,
                "Tiger", "Wolf"
        ));
    }

    @Override
    public void reproduce() {
        if (environment == null) return;
        int currentTick = environment.getTime().getCurrentTick();

        if (canReproduce(currentTick)) {
            lastReproduceTick = currentTick;

            int offspringCount = AppConfig.getInt("animal.fish.reproduce.offspringCount");
            float spawnRadius = AppConfig.getFloat("animal.fish.reproduce.spawnRadius");

            for (int i = 0; i < offspringCount; i++) {
                float offsetX = (float) (Math.random() * 2 - 1) * spawnRadius;
                float offsetY = (float) (Math.random() * 2 - 1) * spawnRadius;

                Vector2D childPos = new Vector2D(
                        position.getX() + offsetX,
                        position.getY() + offsetY
                );

                if (environment.getTerrain().isPassable(childPos, this)) {
                    environment.getRegistry().add(Fish.create(childPos, environment));
                }
            }
        }
    }
}
