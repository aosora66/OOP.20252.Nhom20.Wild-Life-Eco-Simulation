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
import java.util.List;
import java.util.Map;

public class Simulate1000TicksTest {

    record SpeciesStats(int startAlive, int startDead) {}

    public static void main(String[] args) {
        System.out.println("=== SIMULATE 1000 TICKS ===\n");

        CompositeMap world = MapLoader.loadMapFromFile("sim-1000", "Sim Test", "config/map.txt");

        // Snapshot số lượng ban đầu
        Map<String, Integer> startCount = snapshot(world);
        Map<String, Integer> startDead  = deadCount(world);

        System.out.println("--- BAN ĐẦU ---");
        printCounts(startCount);

        // Chạy 1000 tick
        for (int tick = 1; tick <= 1000; tick++) {
            world.updateEnvironment(tick);

            if (tick % 200 == 0) {
                System.out.printf("\n--- TICK %d ---\n", tick);
                printCounts(snapshot(world));
                printDeaths(deadCount(world), startDead);
            }
        }

        // Kết quả cuối
        Map<String, Integer> endCount = snapshot(world);
        Map<String, Integer> endDead  = deadCount(world);

        System.out.println("\n=== KẾT QUẢ SAU 1000 TICK ===");
        System.out.printf("%-12s %8s %8s %8s %10s\n", "Loài", "Đầu", "Cuối", "Sinh", "Chết");
        System.out.println("-".repeat(52));

        for (String species : startCount.keySet()) {
            int start = startCount.getOrDefault(species, 0);
            int end   = endCount.getOrDefault(species, 0);
            int dead  = endDead.getOrDefault(species, 0) - startDead.getOrDefault(species, 0);
            int born  = end - start + dead;
            String flag = "";
            if (end == 0) flag = " ⚠️ TUYỆT CHỦNG";
            else if (end > start * 3) flag = " ⚠️ BÙ PHÁT";
            System.out.printf("%-12s %8d %8d %8d %10d%s\n", species, start, end, Math.max(0, born), dead, flag);
        }
    }

    private static Map<String, Integer> snapshot(CompositeMap world) {
        Map<String, Integer> counts = new HashMap<>();
        for (Environment env : world.getSubEnvironments()) {
            count(env, Rabbit.class,   "Rabbit",   counts);
            count(env, Deer.class,     "Deer",     counts);
            count(env, Elephant.class, "Elephant", counts);
            count(env, Fish.class,     "Fish",     counts);
            count(env, Wolf.class,     "Wolf",     counts);
            count(env, Tiger.class,    "Tiger",    counts);
            count(env, Hunter.class,   "Hunter",   counts);
            count(env, Grass.class,    "Grass",    counts);
            count(env, AppleTree.class,"AppleTree",counts);
        }
        return counts;
    }

    private static Map<String, Integer> deadCount(CompositeMap world) {
        Map<String, Integer> counts = new HashMap<>();
        for (Environment env : world.getSubEnvironments()) {
            countDead(env, Rabbit.class,   "Rabbit",   counts);
            countDead(env, Deer.class,     "Deer",     counts);
            countDead(env, Elephant.class, "Elephant", counts);
            countDead(env, Fish.class,     "Fish",     counts);
            countDead(env, Wolf.class,     "Wolf",     counts);
            countDead(env, Tiger.class,    "Tiger",    counts);
            countDead(env, Hunter.class,   "Hunter",   counts);
        }
        return counts;
    }

    private static <T extends Organism> void count(Environment env, Class<T> cls, String name, Map<String, Integer> map) {
        int n = env.getRegistry().getAllAlive(cls).size();
        map.merge(name, n, Integer::sum);
    }

    private static <T extends Organism> void countDead(Environment env, Class<T> cls, String name, Map<String, Integer> map) {
        long n = env.getRegistry().getAll(cls).stream()
                .filter(o -> o.getState() == OrganismState.DEAD).count();
        map.merge(name, (int) n, Integer::sum);
    }

    private static void printCounts(Map<String, Integer> counts) {
        counts.forEach((k, v) -> System.out.printf("  %-12s: %d\n", k, v));
    }

    private static void printDeaths(Map<String, Integer> endDead, Map<String, Integer> startDead) {
        endDead.forEach((k, v) -> {
            int d = v - startDead.getOrDefault(k, 0);
            if (d > 0) System.out.printf("  %-12s chết thêm: %d\n", k, d);
        });
    }
}
