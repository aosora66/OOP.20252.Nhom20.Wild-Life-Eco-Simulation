package wildlife.model.organism.animal.canivores;

import wildlife.model.brain.HunterStrategy;
import wildlife.model.brain.PassiveStrategy;
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

/**
 * Thợ săn — săn được MỌI loài động vật (Animal.class) trừ apex predator, sát thương rất cao.
 * Đánh đổi: máu thấp (50 HP) và chịu khát kém (thirstDecayRate cao trong config).
 * Vẫn phải tránh xa Voi (apex predator) như mọi sinh vật khác — không săn được, không miễn nhiễm.
 */
public class Hunter extends Animal {

    public Hunter(String id,
                  String speciesName,
                  Vector2D startPos,
                  Environment startEnv,
                  GrowthComponent growth,
                  SurvivalStatsComponent stats,
                  AdaptabilityComponent adaptability,
                  String gender) {
        super(id, speciesName, startPos, startEnv, growth, stats, adaptability, "HUNTER");
        this.combatPower       = AppConfig.getFloat("animal.hunter.combatPower");
        this.vision            = AppConfig.getFloat("animal.hunter.vision");
        this.speed             = AppConfig.getFloat("animal.hunter.speed");
        this.interactionRadius = AppConfig.getFloat("animal.hunter.eatRadius");
        this.diet.add(FoodType.MEAT);
        initStrategies();
    }

    /**
     * Factory method — tạo Hunter với sinh học mặc định.
     */
    public static Hunter create(Vector2D pos, Environment env) {
        return new Hunter(
                UUID.randomUUID().toString(),
                "Hunter",
                pos,
                env,
                new GrowthComponent(1500f, 40f, 0.15f, 0.8f),
                new SurvivalStatsComponent(50f, 15f, 0.1f, 0.2f),
                new AdaptabilityComponent(
                        List.of(TerrainType.FOREST, TerrainType.GRASSLAND),
                        new ValueRange(18f, 35f),
                        new ValueRange(10f, 45f),
                        new ValueRange(-100f, -0.1f)
                ),
                "MALE"
        );
    }

    @Override
    protected void addSurvivalStrategies() {
        float huntSpeedMult       = AppConfig.getFloat("animal.hunter.hunt.speedMultiplier");
        float huntHungerThreshold = AppConfig.getFloat("animal.hunter.hunt.hungerThreshold");

        // 1. Tránh apex predator (vd. Voi) — ưu tiên cao nhất, không có named predator
        addStrategy(new wildlife.model.brain.ScaredStrategy(
                this.speed * 1.3f,
                this.vision,
                2,
                this.interactionRadius,
                0.4f,
                0.4f
        ));

        // 2. Săn mọi loài động vật — Animal.class bắt tất cả subclass trong registry
        //    (HunterStrategy tự loại apex predator khỏi danh sách con mồi)
        addStrategy(new HunterStrategy(
                this.speed * huntSpeedMult,
                this.vision,
                this.interactionRadius,
                this.combatPower,
                huntHungerThreshold,
                Animal.class
        ));

        // Tìm thịt sẵn / nước khi không săn
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

            int offspringCount = AppConfig.getInt("animal.hunter.reproduce.offspringCount");
            float spawnRadius = AppConfig.getFloat("animal.hunter.reproduce.spawnRadius");

            for (int i = 0; i < offspringCount; i++) {
                float offsetX = (float) (Math.random() * 2 - 1) * spawnRadius;
                float offsetY = (float) (Math.random() * 2 - 1) * spawnRadius;

                Vector2D childPos = new Vector2D(
                        position.getX() + offsetX,
                        position.getY() + offsetY
                );

                if (environment.getTerrain().isPassable(childPos, this)) {
                    environment.getRegistry().add(Hunter.create(childPos, environment));
                }
            }
        }
    }
}
