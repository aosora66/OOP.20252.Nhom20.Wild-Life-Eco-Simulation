package wildlife;

import wildlife.model.environment.CompositeMap;
import wildlife.model.environment.Environment;
import wildlife.model.environment.enums.TerrainType;
import wildlife.model.brain.HunterStrategy;
import wildlife.model.brain.PassiveStrategy;
import wildlife.model.brain.ScaredStrategy;
import wildlife.model.organism.Organism;
import wildlife.model.organism.animal.Animal;
import wildlife.model.organism.animal.canivores.Tiger;
import wildlife.model.organism.animal.canivores.Wolf;
import wildlife.model.organism.animal.hebivores.Elephant;
import wildlife.model.organism.animal.hebivores.Rabbit;
import wildlife.model.organism.component.AdaptabilityComponent;
import wildlife.model.organism.component.GrowthComponent;
import wildlife.model.organism.component.SurvivalStatsComponent;
import wildlife.model.organism.plant.Grass;
import wildlife.util.AppConfig;
import wildlife.util.MapLoader;
import wildlife.util.ValueRange;
import wildlife.util.Vector2D;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Properties;

public class LandEcosystemRegressionTest {
    public static void main(String[] args) throws Exception {
        int failures = 0;
        failures += run("elephantsBlockMovementButDoNotTriggerFear",
                () -> elephantsBlockMovementButDoNotTriggerFear());
        failures += run("juvenilePredatorDoesNotHunt", () -> juvenilePredatorDoesNotHunt());
        failures += run("predatorDamageCanBringDownPreyBeforeStarving",
                () -> predatorDamageCanBringDownPreyBeforeStarving());
        failures += run("predatorReproductionIsSlowerThanDefaultHerbivores",
                () -> predatorReproductionIsSlowerThanDefaultHerbivores());
        failures += run("predatorChaseCanUseMultipleSprintSteps", () -> predatorChaseCanUseMultipleSprintSteps());
        failures += run("predatorConsumesPreyImmediatelyAfterKill",
                () -> predatorConsumesPreyImmediatelyAfterKill());
        failures += run("thirstyPredatorDoesNotHuntBeforeDrinking", () -> thirstyPredatorDoesNotHuntBeforeDrinking());
        failures += run("thirstyHerbivoreStillEatsNearbyGrassWhenNoWaterIsKnown",
                () -> thirstyHerbivoreStillEatsNearbyGrassWhenNoWaterIsKnown());
        failures += run("initialAdultPreyCanReproduceWithoutWaitingFullCooldown",
                () -> initialAdultPreyCanReproduceWithoutWaitingFullCooldown());
        if (failures > 0) {
            throw new AssertionError(failures + " land ecosystem checks failed");
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

    private static void elephantsBlockMovementButDoNotTriggerFear() {
        CompositeMap world = MapLoader.loadMapFromFile("elephant-fear-test", "Elephant Fear Test", "config/map.txt");
        Environment grass = findEnvironment(world, "grass");
        removePredators(grass);

        Rabbit rabbit = rabbitAt(new Vector2D(120f, 120f), grass, 100f);
        Elephant elephant = new Elephant(
                "ELEPHANT_FEAR_TEST",
                "Elephant",
                new Vector2D(130f, 120f),
                grass,
                new GrowthComponent(2500f, 50f, 0.2f, 0.7f, 1000f),
                new SurvivalStatsComponent(500f, 150f, 0.3f, 0.5f),
                landAdaptability(),
                "FEMALE"
        );
        grass.addOrganism(rabbit);
        grass.addOrganism(elephant);

        ScaredStrategy fearTigerOrWolf = new ScaredStrategy(
                3f,
                100f,
                2,
                5f,
                0.25f,
                0.3f,
                Tiger.class, Wolf.class
        );

        if (fearTigerOrWolf.isApplicable(rabbit, grass)) {
            throw new AssertionError("Expected animal not to fear Elephant");
        }

        if (grass.isPositionPassable(elephant.getPosition(), rabbit)) {
            throw new AssertionError("Expected Elephant body to block movement");
        }
    }

    private static void juvenilePredatorDoesNotHunt() throws Exception {
        setConfig("animal.tiger.hunt.hungerThreshold", "0.0");
        CompositeMap world = MapLoader.loadMapFromFile("juvenile-hunt-test", "Juvenile Hunt Test", "config/map.txt");
        Environment grass = findEnvironment(world, "grass");

        Rabbit rabbit = rabbitAt(new Vector2D(120f, 120f), grass, 100f);
        Tiger juvenileTiger = new Tiger(
                "TIGER_JUVENILE_TEST",
                "Tiger",
                new Vector2D(121f, 120f),
                grass,
                new GrowthComponent(1000f, 20f, 0.2f, 0.7f, 0f),
                new SurvivalStatsComponent(150f, 50f, 0.4f, 0.6f),
                landAdaptability(),
                "MALE"
        );

        grass.addOrganism(rabbit);
        grass.addOrganism(juvenileTiger);

        float before = rabbit.getStats().getHp();
        juvenileTiger.updateOrganism(1);
        float after = rabbit.getStats().getHp();

        if (after < before) {
            throw new AssertionError("Expected juvenile predator not to damage prey");
        }
    }

    private static void predatorDamageCanBringDownPreyBeforeStarving() {
        float rabbitHp = AppConfig.getFloat("animal.rabbit.maxHp");
        float deerHp = AppConfig.getFloat("animal.deer.maxHp");
        float wolfDamage = AppConfig.getFloat("animal.wolf.combatPower");
        float tigerDamage = AppConfig.getFloat("animal.tiger.combatPower");

        if (wolfDamage < rabbitHp / 2f) {
            throw new AssertionError("Expected Wolf to kill Rabbit in about two bites");
        }
        if (tigerDamage < deerHp / 2f) {
            throw new AssertionError("Expected Tiger to kill Deer in about two bites");
        }
    }

    private static void predatorReproductionIsSlowerThanDefaultHerbivores() {
        float defaultChance = AppConfig.getFloat("animal.reproduce.chance");
        int defaultCooldown = AppConfig.getInt("animal.reproduce.cooldownTicks");

        if (AppConfig.getFloat("animal.wolf.reproduce.chance") >= defaultChance) {
            throw new AssertionError("Expected Wolf reproduction chance below herbivore default");
        }
        if (AppConfig.getFloat("animal.tiger.reproduce.chance") >= defaultChance) {
            throw new AssertionError("Expected Tiger reproduction chance below herbivore default");
        }
        if (AppConfig.getInt("animal.wolf.reproduce.cooldownTicks") <= defaultCooldown) {
            throw new AssertionError("Expected Wolf reproduction cooldown above herbivore default");
        }
        if (AppConfig.getInt("animal.tiger.reproduce.cooldownTicks") <= defaultCooldown) {
            throw new AssertionError("Expected Tiger reproduction cooldown above herbivore default");
        }
    }

    private static void predatorChaseCanUseMultipleSprintSteps() throws Exception {
        CompositeMap world = MapLoader.loadMapFromFile("predator-sprint-test", "Predator Sprint Test", "config/map.txt");
        Environment grass = findEnvironment(world, "grass");
        removeOrganisms(grass);

        Rabbit rabbit = rabbitAt(new Vector2D(150f, 220f), grass, 100f);
        Tiger tiger = new Tiger(
                "TIGER_SPRINT_TEST",
                "Tiger",
                new Vector2D(100f, 220f),
                grass,
                new GrowthComponent(1000f, 20f, 0.2f, 0.7f, 500f),
                new SurvivalStatsComponent(150f, 50f, 0.4f, 0.6f),
                landAdaptability(),
                "MALE"
        );
        grass.addOrganism(rabbit);
        grass.addOrganism(tiger);

        HunterStrategy hunter = new HunterStrategy(
                5f,
                150f,
                7f,
                10f,
                0f,
                3,
                Rabbit.class
        );

        hunter.execute(tiger, grass);

        if (tiger.getPosition().getX() < 114.9f) {
            throw new AssertionError("Expected predator to use multiple chase steps in one tick");
        }
    }

    private static void predatorConsumesPreyImmediatelyAfterKill() throws Exception {
        CompositeMap world = MapLoader.loadMapFromFile("predator-kill-test", "Predator Kill Test", "config/map.txt");
        Environment grass = findEnvironment(world, "grass");
        removeOrganisms(grass);

        Rabbit rabbit = rabbitAt(new Vector2D(106f, 220f), grass, 100f);
        Tiger tiger = new Tiger(
                "TIGER_KILL_TEST",
                "Tiger",
                new Vector2D(100f, 220f),
                grass,
                new GrowthComponent(1000f, 20f, 0.2f, 0.7f, 500f),
                new SurvivalStatsComponent(150f, 50f, 0.4f, 0.6f),
                landAdaptability(),
                "MALE"
        );
        grass.addOrganism(rabbit);
        grass.addOrganism(tiger);
        setStat(tiger.getStats(), "hungerLevel", 100f);

        HunterStrategy hunter = new HunterStrategy(
                5f,
                150f,
                7f,
                100f,
                0f,
                1,
                Rabbit.class
        );

        hunter.execute(tiger, grass);

        if (tiger.getStats().getHungerLevel() >= 100f) {
            throw new AssertionError("Expected predator hunger to drop immediately after killing prey");
        }
        if (!grass.getRegistry().getAllAlive(Rabbit.class).isEmpty()) {
            throw new AssertionError("Expected killed prey to be removed from alive registry");
        }
    }

    private static void thirstyPredatorDoesNotHuntBeforeDrinking() throws Exception {
        setConfig("animal.tiger.hunt.hungerThreshold", "0.0");
        CompositeMap world = MapLoader.loadMapFromFile("thirsty-predator-test", "Thirsty Predator Test", "config/map.txt");
        Environment grass = findEnvironment(world, "grass");

        Rabbit rabbit = rabbitAt(new Vector2D(120f, 120f), grass, 100f);
        Tiger tiger = new Tiger(
                "TIGER_THIRST_TEST",
                "Tiger",
                new Vector2D(121f, 120f),
                grass,
                new GrowthComponent(1000f, 20f, 0.2f, 0.7f, 500f),
                new SurvivalStatsComponent(150f, 50f, 0.4f, 0.6f),
                landAdaptability(),
                "MALE"
        );
        setStat(tiger.getStats(), "hungerLevel", 100f);
        setStat(tiger.getStats(), "thirstLevel", 100f);

        grass.addOrganism(rabbit);
        grass.addOrganism(tiger);

        float before = rabbit.getStats().getHp();
        tiger.updateOrganism(1);
        float after = rabbit.getStats().getHp();

        if (after < before) {
            throw new AssertionError("Expected thirsty predator not to attack before drinking");
        }
    }

    private static void thirstyHerbivoreStillEatsNearbyGrassWhenNoWaterIsKnown() throws Exception {
        CompositeMap world = MapLoader.loadMapFromFile("thirsty-herbivore-food-test", "Thirsty Herbivore Food Test", "config/map.txt");
        Environment grassEnv = findEnvironment(world, "grass");

        Rabbit rabbit = rabbitAt(new Vector2D(120f, 120f), grassEnv, 100f);
        Grass grass = Grass.create(rabbit.getPosition(), grassEnv);
        grassEnv.addOrganism(rabbit);
        grassEnv.addOrganism(grass);
        setStat(rabbit.getStats(), "hungerLevel", 100f);
        setStat(rabbit.getStats(), "thirstLevel", 100f);

        PassiveStrategy passive = new PassiveStrategy(
                2f,
                0f,
                5f,
                80f,
                85f
        );

        float hungerBefore = rabbit.getStats().getHungerLevel();
        passive.execute(rabbit, grassEnv);
        float hungerAfter = rabbit.getStats().getHungerLevel();

        if (hungerAfter >= hungerBefore) {
            throw new AssertionError("Expected thirsty herbivore to eat nearby grass when no water is visible");
        }
    }

    private static void initialAdultPreyCanReproduceWithoutWaitingFullCooldown() throws Exception {
        setConfig("animal.reproduce.chance", "1.0");
        setConfig("animal.reproduce.cooldownTicks", "80");

        CompositeMap world = MapLoader.loadMapFromFile("initial-adult-repro-test", "Initial Adult Repro Test", "config/map.txt");
        Environment grass = findEnvironment(world, "grass");

        Rabbit parent = rabbitAt(new Vector2D(120f, 120f), grass, 100f);
        grass.addOrganism(parent);

        int before = grass.getRegistry().getAllAlive(Rabbit.class).size();
        parent.reproduce();
        int after = grass.getRegistry().getAllAlive(Rabbit.class).size();

        if (after <= before) {
            throw new AssertionError("Expected initial adult prey to reproduce without waiting a full cooldown");
        }
    }

    private static Rabbit rabbitAt(Vector2D pos, Environment env, float startAge) {
        return new Rabbit(
                "RABBIT_LAND_TEST_" + System.nanoTime(),
                "Rabbit",
                pos,
                env,
                new GrowthComponent(400f, 5f, 0.2f, 0.7f, startAge),
                new SurvivalStatsComponent(40f, 15f, 0.8f, 1.0f),
                landAdaptability(),
                "FEMALE"
        );
    }

    private static void removePredators(Environment env) {
        for (Tiger tiger : env.getRegistry().getAllAlive(Tiger.class)) {
            env.getRegistry().remove(tiger.getId());
        }
        for (Wolf wolf : env.getRegistry().getAllAlive(Wolf.class)) {
            env.getRegistry().remove(wolf.getId());
        }
    }

    private static void removeAnimals(Environment env) {
        for (Animal animal : env.getRegistry().getAllAlive(Animal.class)) {
            env.getRegistry().remove(animal.getId());
        }
    }

    private static void removeOrganisms(Environment env) {
        for (Organism organism : env.getRegistry().getAllAlive(Organism.class)) {
            env.getRegistry().remove(organism.getId());
        }
    }

    private static AdaptabilityComponent landAdaptability() {
        return new AdaptabilityComponent(
                List.of(TerrainType.GRASSLAND, TerrainType.FOREST, TerrainType.MUD),
                new ValueRange(15f, 35f),
                new ValueRange(0f, 45f),
                new ValueRange(-60f, -10f)
        );
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

    private static void setStat(Object stats, String fieldName, float value) throws Exception {
        Field field = stats.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setFloat(stats, value);
    }
}
