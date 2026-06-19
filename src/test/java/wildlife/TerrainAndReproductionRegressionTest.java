package wildlife;

import wildlife.model.environment.CompositeMap;
import wildlife.model.environment.Environment;
import wildlife.model.environment.enums.TerrainType;
import wildlife.model.organism.Organism;
import wildlife.model.organism.animal.hebivores.Rabbit;
import wildlife.model.organism.component.AdaptabilityComponent;
import wildlife.model.organism.component.GrowthComponent;
import wildlife.model.organism.component.SurvivalStatsComponent;
import wildlife.util.AppConfig;
import wildlife.util.MapLoader;
import wildlife.util.ValueRange;
import wildlife.util.Vector2D;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Properties;

public class TerrainAndReproductionRegressionTest {
    public static void main(String[] args) throws Exception {
        int failures = 0;
        failures += run("currentTerrainIsAssignedDuringTick", () -> currentTerrainIsAssignedDuringTick());
        failures += run("landAnimalReproductionAddsOffspring", () -> landAnimalReproductionAddsOffspring());
        if (failures > 0) {
            throw new AssertionError(failures + " regression checks failed");
        }
    }

    private static int run(String name, Check check) throws Exception {
        try {
            check.run();
            return 0;
        } catch (AssertionError error) {
            System.err.println(name + ": " + error.getMessage());
            return 1;
        }
    }

    private interface Check {
        void run() throws Exception;
    }

    private static void currentTerrainIsAssignedDuringTick() {
        CompositeMap world = MapLoader.loadMapFromFile("terrain-test", "Terrain Test", "config/map.txt");
        Environment grass = findEnvironment(world, "grass");
        Organism organism = grass.getRegistry().getAllAlive(Organism.class).stream()
                .filter(o -> o instanceof Rabbit)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected an initial Rabbit in grassland"));

        if (organism.getCurrentTerrain() != null) {
            throw new AssertionError("Precondition failed: terrain should start unset before first tick");
        }

        world.updateEnvironment(1);

        TerrainType expected = grass.getTerrain().getTerrainAt(organism.getPosition());
        if (organism.getCurrentTerrain() != expected) {
            throw new AssertionError("Expected currentTerrain " + expected
                    + " but was " + organism.getCurrentTerrain());
        }
    }

    private static void landAnimalReproductionAddsOffspring() throws Exception {
        setConfig("animal.reproduce.chance", "1.0");
        setConfig("animal.reproduce.cooldownTicks", "0");
        setConfig("animal.rabbit.reproduce.chance", "1.0");
        setConfig("animal.rabbit.reproduce.cooldownTicks", "0");

        CompositeMap world = MapLoader.loadMapFromFile("reproduction-test", "Reproduction Test", "config/map.txt");
        Environment grass = findEnvironment(world, "grass");
        grass.getTime().advance(1000);

        Rabbit parent = new Rabbit(
                "RABBIT_REPRO_TEST",
                "Rabbit",
                new Vector2D(120f, 120f),
                grass,
                new GrowthComponent(100f, 5f, 0.2f, 0.7f, 30f),
                new SurvivalStatsComponent(40f, 15f, 0.8f, 1.0f),
                new AdaptabilityComponent(
                        List.of(TerrainType.GRASSLAND, TerrainType.FOREST, TerrainType.MUD),
                        new ValueRange(15f, 35f),
                        new ValueRange(0f, 45f),
                        new ValueRange(-60f, -10f)
                )
        );
        grass.addOrganism(parent);

        int before = grass.getRegistry().getAllAlive(Rabbit.class).size();
        parent.reproduce();
        int after = grass.getRegistry().getAllAlive(Rabbit.class).size();

        if (after <= before) {
            throw new AssertionError("Expected land animal reproduction to add an offspring");
        }
    }

    private static Environment findEnvironment(CompositeMap world, String id) {
        return world.getSubEnvironments().stream()
                .filter(env -> id.equals(env.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing environment: " + id));
    }

    private static void setConfig(String key, String value) throws Exception {
        Field propsField = AppConfig.class.getDeclaredField("props");
        propsField.setAccessible(true);
        Properties props = (Properties) propsField.get(null);
        props.setProperty(key, value);
    }
}
