package wildlife;

import wildlife.model.environment.CompositeMap;
import wildlife.model.environment.Environment;
import wildlife.model.organism.animal.hebivores.Fish;
import wildlife.util.AppConfig;
import wildlife.util.MapLoader;
import wildlife.util.Vector2D;

import java.lang.reflect.Field;
import java.util.Properties;

public class FishSustainabilityRegressionTest {
    public static void main(String[] args) throws Exception {
        int failures = 0;
        failures += run("fishDoesNotLoseHpFromBaselineMetabolism", () -> fishDoesNotLoseHpFromBaselineMetabolism());
        failures += run("fishReproductionIgnoresLegacyPopulationCap", () -> fishReproductionIgnoresLegacyPopulationCap());
        failures += run("fishCannotSwimOutsideLakeBoundary", () -> fishCannotSwimOutsideLakeBoundary());
        if (failures > 0) {
            throw new AssertionError(failures + " fish sustainability checks failed");
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

    private static void fishDoesNotLoseHpFromBaselineMetabolism() throws Exception {
        setConfig("animal.fish.baseHpDrainPerTick", "0.0");
        setConfig("animal.fish.reproduce.chance", "0.0");

        CompositeMap world = MapLoader.loadMapFromFile("fish-metabolism-test", "Fish Metabolism Test", "config/map.txt");
        Environment lake = findEnvironment(world, "lake");
        Fish fish = Fish.create(lake.getTerrain().getRandomValidPosition(), lake, 100f);
        lake.addOrganism(fish);

        float before = fish.getStats().getHp();
        fish.updateOrganism(1);
        float after = fish.getStats().getHp();

        if (after < before - 0.0001f) {
            throw new AssertionError("Expected fish HP not to drop from baseline metabolism; before="
                    + before + ", after=" + after);
        }
    }

    private static void fishReproductionIgnoresLegacyPopulationCap() throws Exception {
        setConfig("animal.fish.baseHpDrainPerTick", "0.0");
        setConfig("animal.fish.reproduce.maxPopulation", "1");
        setConfig("animal.fish.reproduce.chance", "1.0");
        setConfig("animal.fish.reproduce.cooldownTicks", "0");
        setConfig("animal.fish.reproduce.hungerThreshold", "100.0");

        CompositeMap world = MapLoader.loadMapFromFile("fish-repro-test", "Fish Repro Test", "config/map.txt");
        Environment lake = findEnvironment(world, "lake");
        lake.getTime().advance(1000);

        Fish parent = Fish.create(lake.getTerrain().getRandomValidPosition(), lake, 1000f);
        lake.addOrganism(parent);

        int before = lake.getRegistry().getAllAlive(Fish.class).size();
        parent.reproduce();
        int after = lake.getRegistry().getAllAlive(Fish.class).size();

        if (after <= before) {
            throw new AssertionError("Expected fish reproduction to add offspring even above legacy maxPopulation");
        }
    }

    private static void fishCannotSwimOutsideLakeBoundary() {
        CompositeMap world = MapLoader.loadMapFromFile("fish-boundary-test", "Fish Boundary Test", "config/map.txt");
        Environment lake = findEnvironment(world, "lake");
        Fish fish = lake.getRegistry().getAllAlive(Fish.class).stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected initial Fish in lake"));

        Vector2D outsideWorld = new Vector2D(10_000f, 10_000f);
        if (lake.getTerrain().isPassable(outsideWorld, fish)) {
            throw new AssertionError("Expected fish to be blocked outside the lake boundary");
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
