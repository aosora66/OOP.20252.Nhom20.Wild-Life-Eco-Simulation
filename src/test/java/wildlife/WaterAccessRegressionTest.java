package wildlife;

import wildlife.model.environment.CompositeMap;
import wildlife.model.environment.Environment;
import wildlife.model.environment.dto.FoodItem;
import wildlife.model.environment.enums.FoodType;
import wildlife.model.environment.enums.TerrainType;
import wildlife.model.organism.animal.hebivores.Rabbit;
import wildlife.model.organism.component.AdaptabilityComponent;
import wildlife.model.organism.component.GrowthComponent;
import wildlife.model.organism.component.SurvivalStatsComponent;
import wildlife.model.organism.plant.Grass;
import wildlife.util.MapLoader;
import wildlife.util.ValueRange;
import wildlife.util.Vector2D;

import java.util.List;

public class WaterAccessRegressionTest {
    public static void main(String[] args) {
        shorelineWaterIsAvailableToLandEnvironment();
        forestAlsoGetsNearbyLakeWaterSources();
        drinkingWaterDoesNotRemoveTheWaterSource();
        plantAbsorbingWaterDoesNotRemoveTheWaterSource();
    }

    private static void plantAbsorbingWaterDoesNotRemoveTheWaterSource() {
        CompositeMap world = MapLoader.loadMapFromFile("test-world", "Test World", "config/map.txt");
        Environment grass = findEnvironment(world, "grass");
        Vector2D waterPos = new Vector2D(150f, 150f);
        grass.getResources().spawnFood(waterPos, 20f, FoodType.WATER, Integer.MAX_VALUE);

        Grass plant = Grass.create(waterPos, grass);
        plant.absorbNutrients();

        boolean stillPresent = grass.getResources().getFoodNear(waterPos, 1f).stream()
                .anyMatch(FoodItem::isWater);
        if (!stillPresent) {
            throw new AssertionError("Expected WATER source to remain after plant absorption");
        }
    }

    private static void shorelineWaterIsAvailableToLandEnvironment() {
        CompositeMap world = MapLoader.loadMapFromFile("test-world", "Test World", "config/map.txt");
        Environment grass = findEnvironment(world, "grass");

        long grassWaterSources = grass.getResources()
                .getFoodNear(new Vector2D(500f, 500f), 2000f)
                .stream()
                .filter(FoodItem::isWater)
                .count();

        if (grassWaterSources == 0) {
            throw new AssertionError("Expected grassland shoreline WATER sources from map water tiles");
        }
    }

    private static void forestAlsoGetsNearbyLakeWaterSources() {
        CompositeMap world = MapLoader.loadMapFromFile("test-world", "Test World", "config/map.txt");
        Environment forest = findEnvironment(world, "forest");

        long forestWaterSources = forest.getResources()
                .getFoodNear(new Vector2D(500f, 500f), 2000f)
                .stream()
                .filter(FoodItem::isWater)
                .count();

        if (forestWaterSources < 5) {
            throw new AssertionError("Expected forest land near the lake to receive several WATER sources, got "
                    + forestWaterSources);
        }
    }

    private static void drinkingWaterDoesNotRemoveTheWaterSource() {
        CompositeMap world = MapLoader.loadMapFromFile("test-world", "Test World", "config/map.txt");
        Environment grass = findEnvironment(world, "grass");
        Vector2D waterPos = new Vector2D(50f, 50f);
        grass.getResources().spawnFood(waterPos, 20f, FoodType.WATER, Integer.MAX_VALUE);

        FoodItem water = grass.getResources().getFoodNear(waterPos, 1f).stream()
                .filter(FoodItem::isWater)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected manually spawned WATER source"));

        Rabbit rabbit = new Rabbit(
                "RABBIT_TEST",
                "Rabbit",
                waterPos,
                grass,
                new GrowthComponent(100f, 2f, 0.2f, 0.7f),
                new SurvivalStatsComponent(40f, 5f, 1f, 1f),
                new AdaptabilityComponent(
                        List.of(TerrainType.GRASSLAND, TerrainType.FOREST, TerrainType.MUD),
                        new ValueRange(15f, 35f),
                        new ValueRange(0f, 45f),
                        new ValueRange(-60f, -10f)
                )
        );

        rabbit.eating(water);

        boolean stillPresent = grass.getResources().getFoodNear(waterPos, 1f).stream()
                .anyMatch(FoodItem::isWater);
        if (!stillPresent) {
            throw new AssertionError("Expected WATER source to remain after drinking");
        }
    }

    private static Environment findEnvironment(CompositeMap world, String id) {
        return world.getSubEnvironments().stream()
                .filter(env -> id.equals(env.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing environment: " + id));
    }
}
