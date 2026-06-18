package wildlife;

import wildlife.model.environment.CompositeMap;
import wildlife.model.environment.Environment;
import wildlife.model.organism.Organism;
import wildlife.model.organism.OrganismState;
import wildlife.model.organism.animal.canivores.Hunter;
import wildlife.model.organism.animal.canivores.Tiger;
import wildlife.model.organism.animal.canivores.Wolf;
import wildlife.model.organism.animal.hebivores.Deer;
import wildlife.model.organism.animal.hebivores.Elephant;
import wildlife.model.organism.animal.hebivores.Fish;
import wildlife.model.organism.animal.hebivores.Rabbit;
import wildlife.model.organism.plant.AppleTree;
import wildlife.model.organism.plant.Grass;
import wildlife.util.MapLoader;

import java.util.HashMap;
import java.util.Map;

public class Simulate10000TicksTest {

    public static void main(String[] args) {
        System.out.println("=== SIMULATE 10000 TICKS (~ 5.5 phut thuc te @ 30 tick/s) ===\n");

        CompositeMap world = MapLoader.loadMapFromFile("sim-10000", "Sim 10000", "config/map.txt");

        Map<String, Integer> startCount = snapshot(world);
        Map<String, Integer> startDead  = deadCount(world);

        System.out.println("--- BAN DAU ---");
        printCounts(startCount);

        long wallStart = System.currentTimeMillis();

        for (int tick = 1; tick <= 10000; tick++) {
            world.updateEnvironment(tick);

            if (tick % 1000 == 0) {
                long elapsed = System.currentTimeMillis() - wallStart;
                System.out.printf("\n--- TICK %d [%.1fs wall] ---\n", tick, elapsed / 1000.0);
                printCounts(snapshot(world));
            }
        }

        Map<String, Integer> endCount = snapshot(world);
        Map<String, Integer> endDead  = deadCount(world);

        long totalMs = System.currentTimeMillis() - wallStart;
        System.out.printf("\n=== KET QUA SAU 10000 TICK [tong %.1fs wall] ===\n", totalMs / 1000.0);
        System.out.printf("%-12s %8s %8s %8s %10s\n", "Loai", "Dau", "Cuoi", "Sinh", "Chet");
        System.out.println("-".repeat(52));

        String[] order = {"Rabbit","Deer","Elephant","Fish","Wolf","Tiger","Hunter","Grass","AppleTree"};
        for (String species : order) {
            int start = startCount.getOrDefault(species, 0);
            int end   = endCount.getOrDefault(species, 0);
            int dead  = endDead.getOrDefault(species, 0) - startDead.getOrDefault(species, 0);
            int born  = end - start + dead;
            String flag = "";
            if (end == 0 && start > 0) flag = "  << TUYET CHUNG";
            else if (end > start * 5)  flag = "  << BU PHAT";
            System.out.printf("%-12s %8d %8d %8d %10d%s\n", species, start, end, Math.max(0, born), dead, flag);
        }
    }

    private static Map<String, Integer> snapshot(CompositeMap world) {
        Map<String, Integer> counts = new HashMap<>();
        for (Environment env : world.getSubEnvironments()) {
            merge(env, Rabbit.class,    "Rabbit",    counts);
            merge(env, Deer.class,      "Deer",      counts);
            merge(env, Elephant.class,  "Elephant",  counts);
            merge(env, Fish.class,      "Fish",      counts);
            merge(env, Wolf.class,      "Wolf",      counts);
            merge(env, Tiger.class,     "Tiger",     counts);
            merge(env, Hunter.class,    "Hunter",    counts);
            merge(env, Grass.class,     "Grass",     counts);
            merge(env, AppleTree.class, "AppleTree", counts);
        }
        return counts;
    }

    private static Map<String, Integer> deadCount(CompositeMap world) {
        Map<String, Integer> counts = new HashMap<>();
        for (Environment env : world.getSubEnvironments()) {
            mergeDead(env, Rabbit.class,   "Rabbit",   counts);
            mergeDead(env, Deer.class,     "Deer",     counts);
            mergeDead(env, Elephant.class, "Elephant", counts);
            mergeDead(env, Fish.class,     "Fish",     counts);
            mergeDead(env, Wolf.class,     "Wolf",     counts);
            mergeDead(env, Tiger.class,    "Tiger",    counts);
            mergeDead(env, Hunter.class,   "Hunter",   counts);
        }
        return counts;
    }

    private static <T extends Organism> void merge(Environment env, Class<T> cls, String name, Map<String, Integer> map) {
        map.merge(name, env.getRegistry().getAllAlive(cls).size(), Integer::sum);
    }

    private static <T extends Organism> void mergeDead(Environment env, Class<T> cls, String name, Map<String, Integer> map) {
        long n = env.getRegistry().getAll(cls).stream()
                .filter(o -> o.getState() == OrganismState.DEAD).count();
        map.merge(name, (int) n, Integer::sum);
    }

    private static void printCounts(Map<String, Integer> counts) {
        String[] order = {"Rabbit","Deer","Elephant","Fish","Wolf","Tiger","Hunter","Grass","AppleTree"};
        for (String k : order) {
            int v = counts.getOrDefault(k, 0);
            System.out.printf("  %-12s: %d\n", k, v);
        }
    }
}
