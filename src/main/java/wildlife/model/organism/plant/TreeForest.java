package wildlife.model.organism.plant;

import wildlife.model.environment.Environment;
import wildlife.model.environment.enums.FoodType;
import wildlife.model.environment.enums.TerrainType;
import wildlife.model.organism.component.AdaptabilityComponent;
import wildlife.model.organism.component.GrowthComponent;
import wildlife.model.organism.component.SurvivalStatsComponent;
import wildlife.util.AppConfig;
import wildlife.util.ValueRange;
import wildlife.util.Vector2D;

import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Cây rừng — cây cổ thụ sống trong môi trường Forest, định kỳ rụng quả để động vật ăn.
 * Dùng FoodType.APPLE như quả chung (quả rừng), các động vật ăn thực vật đều tiêu thụ được.
 */
public class TreeForest extends Plant {

    private final float fruitDropRadius;
    private static final Random RNG = new Random();

    public TreeForest(String id,
                      String speciesName,
                      Vector2D startPos,
                      Environment startEnv,
                      GrowthComponent growth,
                      SurvivalStatsComponent stats,
                      AdaptabilityComponent adaptability) {
        super(id, speciesName, startPos, startEnv, growth, stats, adaptability);
        this.photosynthesisRate         = AppConfig.getFloat("plant.treeforest.photosynthesisRate");
        this.lightLevelToPhotosynthesis = AppConfig.getFloat("plant.treeforest.minLightLevel");
        this.nutritionAsorbRadius       = AppConfig.getFloat("plant.treeforest.nutrientsAbsorbRadius");
        this.fruitDropRadius            = AppConfig.getFloat("plant.treeforest.fruitDropRadius");
    }

    /**
     * Factory method — tạo TreeForest với sinh học mặc định, dùng trong Forest.initialize().
     * Dùng factory thay vì 7 param để Forest không cần biết chi tiết component.
     */
    public static TreeForest create(Vector2D pos, Environment env) {
        return new TreeForest(
                UUID.randomUUID().toString(),
                "TreeForest",
                pos,
                env,
                new GrowthComponent(3000f, 60f, 0.1f, 0.75f),
                new SurvivalStatsComponent(120f, 10f, 0.04f, 0.08f),
                new AdaptabilityComponent(
                        List.of(TerrainType.FOREST),
                        new ValueRange(15f, 28f),
                        new ValueRange(0f, 40f),
                        new ValueRange(-100f, -0.1f)
                )
        );
    }

    @Override
    public void onTick(int currentTick) {
        photosynthesis();
        absorbNutrients();

        int interval = AppConfig.getInt("plant.treeforest.fruitDropInterval");
        if (growth.isAdult() && currentTick % interval == 0) {
            dropFruit();
        }
    }

    private void dropFruit() {
        if (environment == null) return;

        float nutrition = AppConfig.getFloat("food.treeforest.nutritionalValue");
        int   expiry    = AppConfig.getInt("food.treeforest.expiryTicks");

        float offsetX = (RNG.nextFloat() * 2 - 1) * fruitDropRadius;
        float offsetY = (RNG.nextFloat() * 2 - 1) * fruitDropRadius;

        Vector2D fruitPos = new Vector2D(
                position.getX() + offsetX,
                position.getY() + offsetY
        );

        environment.getResources().spawnFood(fruitPos, nutrition, FoodType.APPLE, expiry);
    }

    @Override
    protected void addOffspring(Vector2D pos) {
        environment.getRegistry().add(TreeForest.create(pos, environment));
    }
}
