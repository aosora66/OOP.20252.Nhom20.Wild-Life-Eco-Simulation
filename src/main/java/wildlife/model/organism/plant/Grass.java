package wildlife.model.organism.plant;

import wildlife.model.environment.Environment;
import wildlife.model.environment.dto.FoodItem;
import wildlife.model.environment.enums.TerrainType;
import wildlife.model.organism.component.AdaptabilityComponent;
import wildlife.model.organism.component.GrowthComponent;
import wildlife.model.organism.component.SurvivalStatsComponent;
import wildlife.util.AppConfig;
import wildlife.util.ValueRange;
import wildlife.util.Vector2D;

import java.util.List;
import java.util.UUID;

public class Grass extends Plant{
    public Grass(String id,
                 String speciesName,
                 Vector2D startPos,
                 Environment startEnv,
                 GrowthComponent growth,
                 SurvivalStatsComponent stats,
                 AdaptabilityComponent adaptability) {
        super(id, speciesName, startPos, startEnv, growth, stats, adaptability);
        photosynthesisRate         = AppConfig.getFloat("plant.grass.photosynthesisRate");
        lightLevelToPhotosynthesis = AppConfig.getFloat("plant.grass.minLightLevel");
        nutritionAsorbRadius       = AppConfig.getFloat("plant.grass.nutrientsAsorbRadius");
    }

    /**
     * Factory method — tạo Grass với sinh học mặc định, dùng trong GrassLand.initialize().
     * Dùng factory thay vì 7 param để GrassLand không cần biết chi tiết component.
     */
    public static Grass create(Vector2D pos, Environment env) {
        return new Grass(
                UUID.randomUUID().toString(),
                "Grass",
                pos,
                env,
                new GrowthComponent(200f, 3f, 0.2f, 0.8f),
                new SurvivalStatsComponent(30f, 2f, 0.05f, 0.07f),
                new AdaptabilityComponent(
                        List.of(TerrainType.GRASSLAND),
                        new ValueRange(25f, 38f),
                        new ValueRange(5f, 45f),
                        new ValueRange(-100f, 0f)
                )
        );
    }

    @Override
    public void onTick(int currentTick) {
        photosynthesis();
        absorbNutrients();
    }

    /** Cỏ không đói, không khát — năng lượng từ quang hợp và đất. */
    @Override
    protected void applyMetabolismDecay(float seasonMultiplier, float thirstMultiplier) {}

    /** Cỏ không mất HP thụ động — chỉ chết khi bị ăn hết HP. */
    @Override
    protected float getBaseHpDrainPerTick() { return 0f; }

    @Override
    protected void addOffspring(Vector2D pos) {
        environment.getRegistry().add(Grass.create(pos, environment));
    }
}
