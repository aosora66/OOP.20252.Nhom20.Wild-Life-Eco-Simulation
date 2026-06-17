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
import wildlife.util.SoundManager;
import wildlife.model.environment.dto.FoodItem;
import wildlife.model.organism.Organism;

import java.util.List;
import java.util.UUID;

public class Tiger extends Animal {
    public Tiger(String id,
                 String speciesName,
                 Vector2D startPos,
                 Environment startEnv,
                 GrowthComponent growth,
                 SurvivalStatsComponent stats,
                 AdaptabilityComponent adaptability,
                 String gender) {
        super(id, speciesName, startPos, startEnv, growth, stats, adaptability, "CARNIVORE");
        this.combatPower = AppConfig.getFloat("animal.tiger.combatPower");
        this.vision = AppConfig.getFloat("animal.tiger.vision");
        this.speed = AppConfig.getFloat("animal.tiger.speed");
        this.interactionRadius = AppConfig.getFloat("animal.tiger.eatRadius");
        this.diet.add(FoodType.MEAT);
        initStrategies();
    }

    /**
     * Factory method — tạo Tiger với sinh học mặc định.
     */
    public static Tiger create(Vector2D pos, Environment env) {
        return new Tiger(
                UUID.randomUUID().toString(),
                "Tiger",
                pos,
                env,
                new GrowthComponent(1200f, 60f, 0.1f, 0.7f),
                new SurvivalStatsComponent(100f, 30f, 0.15f, 0.15f),
                new AdaptabilityComponent(
                        List.of(TerrainType.FOREST, TerrainType.GRASSLAND),
                        new ValueRange(20f, 35f),
                        new ValueRange(10f, 50f),
                        new ValueRange(-100f, -0.1f)
                ),
                "MALE"
        );
    }

    @Override
    protected void onTick(int currentTick) {
        executeStrategy(currentTick);
    }

    @Override
    protected void preEatAction(FoodItem food) {
        SoundManager.playSoundEffectWithCooldown("Snarl.wav", 1500, 1.0f);
    }

    @Override
    public void performAttack(Organism target, float damage) {
        SoundManager.playSoundEffectWithCooldown("TigerRoar.wav", 2000, 1.0f);
        super.performAttack(target, damage);
    }

    @Override
    protected void addSurvivalStrategies() {
        float huntSpeedMult = AppConfig.getFloat("animal.tiger.hunt.speedMultiplier");
        float huntHungerThreshold = AppConfig.getFloat("animal.tiger.hunt.hungerThreshold");

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

            int offspringCount = AppConfig.getInt("animal.tiger.reproduce.offspringCount");
            float spawnRadius = AppConfig.getFloat("animal.tiger.reproduce.spawnRadius");

            for (int i = 0; i < offspringCount; i++) {
                float offsetX = (float) (Math.random() * 2 - 1) * spawnRadius;
                float offsetY = (float) (Math.random() * 2 - 1) * spawnRadius;

                Vector2D childPos = new Vector2D(
                        position.getX() + offsetX,
                        position.getY() + offsetY
                );

                if (environment.getTerrain().isPassable(childPos, this)) {
                    environment.getRegistry().add(Tiger.create(childPos, environment));
                }
            }
        }
    }
}
