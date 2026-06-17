package wildlife.model.organism.animal.hebivores;

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
 * Voi — apex predator thực vật. Không săn mồi, nhưng khiến MỌI động vật khác phải bỏ chạy.
 * Cơ chế: isApexPredator() = true → ScaredStrategy của mọi loài tự động phát hiện và né tránh.
 * Chỉ ăn thực vật, máu nhiều, chậm nhưng tầm nhìn xa và sức mạnh rất cao (khi bị tấn công).
 */
public class Elephant extends Animal {

    public Elephant(String id,
                    String speciesName,
                    Vector2D startPos,
                    Environment startEnv,
                    GrowthComponent growth,
                    SurvivalStatsComponent stats,
                    AdaptabilityComponent adaptability,
                    String gender) {
        super(id, speciesName, startPos, startEnv, growth, stats, adaptability, "HERBIVORE");
        this.combatPower       = AppConfig.getFloat("animal.elephant.combatPower");
        this.vision            = AppConfig.getFloat("animal.elephant.vision");
        this.speed             = AppConfig.getFloat("animal.elephant.speed");
        this.interactionRadius = AppConfig.getFloat("animal.elephant.eatRadius");
        this.diet.add(FoodType.APPLE);
        initStrategies();
    }

    /**
     * Factory method — tạo Elephant với sinh học mặc định.
     */
    public static Elephant create(Vector2D pos, Environment env) {
        return new Elephant(
                UUID.randomUUID().toString(),
                "Elephant",
                pos,
                env,
                new GrowthComponent(3000f, 100f, 0.05f, 0.85f),
                new SurvivalStatsComponent(250f, 100f, 0.05f, 0.05f),
                new AdaptabilityComponent(
                        List.of(TerrainType.FOREST, TerrainType.GRASSLAND),
                        new ValueRange(18f, 32f),
                        new ValueRange(10f, 40f),
                        new ValueRange(-100f, -0.1f)
                ),
                "MALE"
        );
    }

    /** Voi là apex predator — mọi ScaredStrategy tự động nhận diện và bỏ chạy. */
    @Override
    public boolean isApexPredator() { return true; }

    @Override
    protected void addSurvivalStrategies() {
        // Voi không sợ ai — chỉ tìm thức ăn và nước
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

            int offspringCount = AppConfig.getInt("animal.elephant.reproduce.offspringCount");
            float spawnRadius = AppConfig.getFloat("animal.elephant.reproduce.spawnRadius");

            for (int i = 0; i < offspringCount; i++) {
                float offsetX = (float) (Math.random() * 2 - 1) * spawnRadius;
                float offsetY = (float) (Math.random() * 2 - 1) * spawnRadius;

                Vector2D childPos = new Vector2D(
                        position.getX() + offsetX,
                        position.getY() + offsetY
                );

                if (environment.getTerrain().isPassable(childPos, this)) {
                    environment.getRegistry().add(Elephant.create(childPos, environment));
                }
            }
        }
    }
}
