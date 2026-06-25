package wildlife.view.ui.control;

import wildlife.model.environment.Environment;
import wildlife.model.environment.enums.TerrainType;
import wildlife.model.organism.Organism;
import wildlife.model.organism.animal.canivores.Hunter;
import wildlife.model.organism.animal.canivores.Tiger;
import wildlife.model.organism.animal.canivores.Wolf;
import wildlife.model.organism.animal.hebivores.Deer;
import wildlife.model.organism.animal.hebivores.Elephant;
import wildlife.model.organism.animal.hebivores.Fish;
import wildlife.model.organism.animal.hebivores.Rabbit;
import wildlife.model.organism.component.AdaptabilityComponent;
import wildlife.model.organism.component.GrowthComponent;
import wildlife.model.organism.component.SurvivalStatsComponent;
import wildlife.model.organism.plant.AppleTree;
import wildlife.model.organism.plant.Grass;
import wildlife.model.organism.plant.TreeForest;
import wildlife.util.AppConfig;
import wildlife.util.ValueRange;
import wildlife.util.Vector2D;

import java.util.List;
import java.util.Random;

final class InteractionOrganismFactory {
    private InteractionOrganismFactory() {
    }

    static Class<? extends Organism> speciesClass(String speciesName) {
        return switch (speciesName) {
            case "Tiger" -> Tiger.class;
            case "Wolf" -> Wolf.class;
            case "Hunter" -> Hunter.class;
            case "Deer" -> Deer.class;
            case "Elephant" -> Elephant.class;
            case "Fish" -> Fish.class;
            case "Rabbit" -> Rabbit.class;
            case "AppleTree" -> AppleTree.class;
            case "Grass" -> Grass.class;
            case "TreeForest" -> TreeForest.class;
            default -> null;
        };
    }

    static Organism create(Class<?> clazz, Vector2D pos, Environment env) throws Exception {
        if (clazz == AppleTree.class) {
            return AppleTree.create(pos, env);
        } else if (clazz == Grass.class) {
            return Grass.create(pos, env);
        } else if (clazz == TreeForest.class) {
            return TreeForest.create(pos, env);
        }

        String species = clazz.getSimpleName().toLowerCase();
        Random random = new Random();
        float baseHp = AppConfig.getFloat("animal." + species + ".maxHp");
        float baseNutrition = AppConfig.getFloat("animal." + species + ".nutrition");
        float hungerDecay = AppConfig.getFloat("animal." + species + ".hungerDecay");
        float thirstDecay = AppConfig.getFloat("animal." + species + ".thirstDecay");

        float baseAge = AppConfig.getFloat("animal." + species + ".maxAge");
        float baseSize = AppConfig.getFloat("animal." + species + ".maxSize");

        float randomFactorHp = 0.85f + (random.nextFloat() * 0.3f);
        float randomFactorAge = 0.85f + (random.nextFloat() * 0.3f);
        float randomFactorSize = 0.85f + (random.nextFloat() * 0.3f);

        float finalHp = baseHp * randomFactorHp;
        float finalMaxAge = baseAge * randomFactorAge;
        float finalMaxSize = baseSize * randomFactorSize;

        float startAge = finalMaxAge * 0.4f;
        GrowthComponent growth = new GrowthComponent(finalMaxAge, finalMaxSize, 0.2f, 0.7f, startAge);
        SurvivalStatsComponent stats = new SurvivalStatsComponent(finalHp, baseNutrition, hungerDecay, thirstDecay);

        List<TerrainType> survivableTerrains = species.equals("fish")
                ? List.of(TerrainType.DEEP_WATER)
                : List.of(TerrainType.GRASSLAND, TerrainType.FOREST, TerrainType.MUD);

        AdaptabilityComponent adapt = new AdaptabilityComponent(
                survivableTerrains,
                new ValueRange(15f, 35f),
                new ValueRange(0f, 45f),
                new ValueRange(-60f, -10f)
        );

        String id = clazz.getSimpleName().toUpperCase() + "_" + System.nanoTime();

        return (Organism) clazz.getDeclaredConstructor(
                String.class, String.class, Vector2D.class, Environment.class,
                GrowthComponent.class, SurvivalStatsComponent.class, AdaptabilityComponent.class
        ).newInstance(id, clazz.getSimpleName(), pos, env, growth, stats, adapt);
    }
}
