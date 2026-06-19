package wildlife;

import wildlife.model.environment.CompositeMap;
import wildlife.model.environment.Environment;
import wildlife.model.organism.Organism;
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

        Map<String, Integer> endCount   = snapshot(world);
        Map<String, int[]>   tally      = mergedDeathTally(world);
        Map<String, Integer> births     = mergedBirthTally(world);

        long totalMs = System.currentTimeMillis() - wallStart;
        System.out.printf("\n=== KET QUA SAU 10000 TICK [tong %.1fs wall] ===\n", totalMs / 1000.0);
        System.out.printf("%-12s %6s %6s %8s %8s %8s %8s %8s\n",
                "Loai", "Dau", "Cuoi", "BiGiet", "Gia", "Doi", "Khat", "Sinh");
        System.out.println("-".repeat(70));

        // Loài không dùng reproduceSameSpecies → hiện "X" thay vì số
        java.util.Set<String> noReproduceTracking = java.util.Set.of("Fish", "Grass", "AppleTree");

        String[] order = {"Rabbit","Deer","Elephant","Fish","Wolf","Tiger","Hunter","Grass","AppleTree"};
        for (String species : order) {
            int   start  = startCount.getOrDefault(species, 0);
            int   end    = endCount.getOrDefault(species, 0);
            int[] t      = tally.getOrDefault(species, new int[5]);
            String flag  = "";
            if (end == 0 && start > 0) flag = "  << TUYET CHUNG";
            else if (end > start * 5)  flag = "  << BU PHAT";
            // t: [0]=killed [1]=oldAge [2]=starvation [3]=dehydration [4]=other
            if (noReproduceTracking.contains(species)) {
                System.out.printf("%-12s %6d %6d %8d %8d %8d %8d %8s%s\n",
                        species, start, end, t[0], t[1], t[2], t[3], "X", flag);
            } else {
                int born = births.getOrDefault(species, 0);
                System.out.printf("%-12s %6d %6d %8d %8d %8d %8d %8d%s\n",
                        species, start, end, t[0], t[1], t[2], t[3], born, flag);
            }
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

    private static Map<String, int[]> mergedDeathTally(CompositeMap world) {
        Map<String, int[]> result = new HashMap<>();
        for (Environment env : world.getSubEnvironments()) {
            env.getDeathTally().forEach((species, t) ->
                result.merge(species, t.clone(),
                    (a, b) -> new int[]{a[0]+b[0], a[1]+b[1], a[2]+b[2], a[3]+b[3], a[4]+b[4]})
            );
        }
        return result;
    }

    private static Map<String, Integer> mergedBirthTally(CompositeMap world) {
        Map<String, Integer> result = new HashMap<>();
        for (Environment env : world.getSubEnvironments()) {
            env.getBirthTally().forEach((species, n) -> result.merge(species, n, Integer::sum));
        }
        return result;
    }

    private static <T extends Organism> void merge(Environment env, Class<T> cls, String name, Map<String, Integer> map) {
        map.merge(name, env.getRegistry().getAllAlive(cls).size(), Integer::sum);
    }

    private static void printCounts(Map<String, Integer> counts) {
        String[] order = {"Rabbit","Deer","Elephant","Fish","Wolf","Tiger","Hunter","Grass","AppleTree"};
        for (String k : order) {
            int v = counts.getOrDefault(k, 0);
            System.out.printf("  %-12s: %d\n", k, v);
        }
    }
}
