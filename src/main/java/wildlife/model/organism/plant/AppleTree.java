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
 * AppleTree represents a fruit-bearing plant in the ecosystem.
 * It inherits basic biological processes (photosynthesis, nutrient absorption) from Plant
 * and introduces a specialized behavior: periodic fruit (Apple) generation.
 */
public class AppleTree extends Plant {

    private float appleDropRadius;
    /** Random number generator for fruit placement offsets. */
    private static final Random RNG = new Random();

    /**
     * Constructs a new AppleTree instance with species-specific configuration.
     *
     * @param id           Unique identifier for the tree.
     * @param speciesName  The name of the species ("AppleTree").
     * @param startPos     The initial location in the environment.
     * @param startEnv     The environment this tree belongs to.
     * @param growth       Component managing age and size.
     * @param stats        Component managing health and hunger.
     * @param adaptability Component managing environmental tolerances.
     */
    public AppleTree(String id,
                     String speciesName,
                     Vector2D startPos,
                     Environment startEnv,
                     GrowthComponent growth,
                     SurvivalStatsComponent stats,
                     AdaptabilityComponent adaptability) {
        super(id, speciesName, startPos, startEnv, growth, stats, adaptability);

        // Load species-specific biological rates from configuration
        this.photosynthesisRate         = AppConfig.getFloat("plant.appletree.photosynthesisRate");
        this.lightLevelToPhotosynthesis = AppConfig.getFloat("plant.appletree.minLightLevel");
        this.nutritionAsorbRadius       = AppConfig.getFloat("plant.appletree.nutrientsAsorbRadius");
        this.appleDropRadius            = AppConfig.getFloat("plant.appletree.appleDropRadius");
    }

    public static AppleTree create(Vector2D pos, Environment env) {
        return create(pos, env, 0f);
    }

    public static AppleTree create(Vector2D pos, Environment env, float startAge) {
        float maxAge      = AppConfig.getFloat("plant.appletree.maxAge") * (0.8f + RNG.nextFloat() * 0.4f);
        float maxHp       = AppConfig.getFloat("plant.appletree.maxHp");
        float thirstDecay = AppConfig.getFloat("plant.appletree.thirstDecay");
        return new AppleTree(
                "APPLETREE_" + System.nanoTime() + "_" + RNG.nextInt(9999),
                "AppleTree",
                pos,
                env,
                new GrowthComponent(maxAge, 25f, 0.2f, 0.7f, startAge),
                new SurvivalStatsComponent(maxHp, 15f, 0.0f, thirstDecay),
                new AdaptabilityComponent(
                        List.of(TerrainType.FOREST, TerrainType.GRASSLAND),
                        new ValueRange(18f, 30f),
                        new ValueRange(5f, 40f),
                        new ValueRange(-50f, 0f)
                )
        );
    }

    /**
     * Updates the AppleTree state during each simulation tick.
     * Performs basic plant metabolism and triggers fruit dropping if conditions are met.
     *
     * @param currentTick The current step count of the simulation.
     */
    @Override
    public void onTick(int currentTick) {
        // Standard plant metabolism: Generate energy from light and absorb water/nutrients from soil
        photosynthesis();
        absorbNutrients();

        // Fruit Production Logic:
        // Only mature (Adult) trees can drop fruit.
        // Fruit is dropped at fixed intervals defined in the configuration.
        int interval = AppConfig.getInt("plant.appletree.appleDropInterval");
        if (growth.isAdult() && currentTick % interval == 0) {
            dropApple();
        }
    }

    /**
     * Generates a new Apple (FoodItem) and places it into the environment.
     * The apple is spawned at a random location within the configured appleDropRadius.
     */
    private void dropApple() {
        if (environment == null) return;

        // Load fruit properties from configuration
        float nutrition = AppConfig.getFloat("food.apple.nutritionalValue");
        int expiry      = AppConfig.getInt("food.apple.expiryTicks");

        // Calculate a random displacement within the drop radius
        // nextFloat() * 2 - 1 provides a range between [-1, 1]
        float offsetX = (RNG.nextFloat() * 2 - 1) * appleDropRadius;
        float offsetY = (RNG.nextFloat() * 2 - 1) * appleDropRadius;

        // Create the final position for the dropped fruit
        Vector2D applePos = new Vector2D(
                position.getX() + offsetX,
                position.getY() + offsetY
        );

        // Add the fruit to the environment's resource pool
        environment.getResources().spawnFood(applePos, nutrition, FoodType.APPLE, expiry);
    }

    @Override
    protected void addOffspring(Vector2D pos) {
        environment.addOrganism(AppleTree.create(pos, environment));
    }
}