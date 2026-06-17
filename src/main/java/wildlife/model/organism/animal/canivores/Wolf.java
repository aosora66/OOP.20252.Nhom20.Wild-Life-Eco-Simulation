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
import wildlife.util.ValueRange;
import wildlife.util.Vector2D;

import java.util.List;
import java.util.UUID;

public class Wolf extends Animal {
    public Wolf(String id,
                String speciesName,
                Vector2D startPos,
                Environment startEnv,
                GrowthComponent growth,
                SurvivalStatsComponent stats,
                AdaptabilityComponent adaptability,
                String gender) {
        super(id, speciesName, startPos, startEnv, growth, stats, adaptability, "CANIVORE");
        this.combatPower = AppConfig.getFloat("animal.wolf.combatPower");
        this.vision = AppConfig.getFloat("animal.wolf.vision");
        this.speed = AppConfig.getFloat("animal.wolf.speed");
        this.interactionRadius = AppConfig.getFloat("animal.wolf.eatRadius");
        this.diet.add(FoodType.MEAT);
        initStrategies();
    }

    /**
     * Factory method — tạo Wolf với sinh học mặc định.
     */
    public static Wolf create(Vector2D pos, Environment env) {
        return new Wolf(
                UUID.randomUUID().toString(),
                "Wolf",
                pos,
                env,
                new GrowthComponent(1000f, 50f, 0.12f, 0.75f),
                new SurvivalStatsComponent(70f, 20f, 0.12f, 0.12f),
                new AdaptabilityComponent(
                        List.of(TerrainType.FOREST, TerrainType.GRASSLAND),
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
    protected void addSurvivalStrategies() {
        float huntSpeedMult = AppConfig.getFloat("animal.wolf.hunt.speedMultiplier");
        float huntHungerThreshold = AppConfig.getFloat("animal.wolf.hunt.hungerThreshold");

        // 1. Tránh apex predator (vd. Voi) — ưu tiên cao nhất: 30, không có named predator
        addStrategy(new wildlife.model.brain.ScaredStrategy(
                this.speed * 1.3f,
                this.vision,
                2,
                this.interactionRadius,
                0.4f,
                0.4f
        ));

        // 2. Săn mồi khi đói (Ưu tiên trung bình: 20)
        addStrategy(new wildlife.model.brain.HunterStrategy(
                this.speed * huntSpeedMult,
                this.vision,
                this.interactionRadius,
                this.combatPower,
                huntHungerThreshold,
                Rabbit.class, Deer.class
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
        if (environment == null) return;
        int currentTick = environment.getTime().getCurrentTick();

        if (canReproduce(currentTick)) {
            lastReproduceTick = currentTick;

            int offspringCount = AppConfig.getInt("animal.wolf.reproduce.offspringCount");
            float spawnRadius = AppConfig.getFloat("animal.wolf.reproduce.spawnRadius");

            for (int i = 0; i < offspringCount; i++) {
                float offsetX = (float) (Math.random() * 2 - 1) * spawnRadius;
                float offsetY = (float) (Math.random() * 2 - 1) * spawnRadius;

                Vector2D childPos = new Vector2D(
                        position.getX() + offsetX,
                        position.getY() + offsetY
                );

                if (environment.getTerrain().isPassable(childPos, this)) {
                    environment.getRegistry().add(Wolf.create(childPos, environment));
                }
            }
        }
    }
}
