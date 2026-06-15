package wildlife.model.brain;

import wildlife.model.environment.Environment;
import wildlife.model.organism.Organism;
import wildlife.model.organism.animal.Animal;
import wildlife.util.AppConfig;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Strategy chạy trốn — kích hoạt khi phát hiện kẻ thù trong tầm nhìn, ưu tiên 30.
 * Kẻ thù bao gồm: (1) named predators theo speciesName, (2) bất kỳ apex predator nào (vd. Voi).
 *
 * Khi HP thấp và kẻ thù áp sát (trong attackRange), có tỉ lệ phản kháng thay vì chỉ chạy.
 * Sát thương phản kháng = combatPower của bản thân.
 */
public class ScaredStrategy extends AbstractSurvivalStrategy {

    private final List<String> predatorSpecies;
    private final int          sprintSteps;
    private final float        counterHpThreshold;
    private final float        counterAttackChance;
    private final int          cooldownTicks;
    private int                counterAttackCooldown = 0;

    /**
     * @param stepSize            bước di chuyển mỗi lần
     * @param fearRadius          tầm phát hiện kẻ thù (sightRadius)
     * @param sprintSteps         số bước chạy mỗi tick
     * @param counterAttackRange  khoảng cách để phản kháng (dùng làm attackRange)
     * @param counterHpThreshold  ngưỡng HP ratio để có thể phản kháng (0–1)
     * @param counterAttackChance xác suất phản kháng mỗi lần cơ hội (0–1)
     * @param predatorSpecies     các loài kẻ thù theo speciesName (có thể để trống —
     *                            khi đó chỉ chạy trốn apex predator như Voi)
     */
    public ScaredStrategy(float stepSize, float fearRadius, int sprintSteps,
                          float counterAttackRange, float counterHpThreshold,
                          float counterAttackChance, String... predatorSpecies) {
        super(stepSize, fearRadius, counterAttackRange);
        this.predatorSpecies     = List.of(predatorSpecies);
        this.sprintSteps         = Math.max(1, sprintSteps);
        this.counterHpThreshold  = counterHpThreshold;
        this.counterAttackChance = counterAttackChance;
        this.cooldownTicks       = AppConfig.getInt("organism.scared.counterAttackCooldown");
    }

    @Override
    public boolean isApplicable(Animal self, Environment env) {
        return findNearestThreat(self, env).isPresent();
    }

    @Override
    public int getPriority() { return 30; }

    @Override
    public void execute(Animal self, Environment env) {
        findNearestThreat(self, env).ifPresent(threat -> {
            if (counterAttackCooldown > 0) {
                counterAttackCooldown--;
                for (int i = 0; i < sprintSteps; i++) {
                    moveAwayFrom(self, threat.getPosition(), env);
                }
                return;
            }

            float dist    = self.getPosition().distanceTo(threat.getPosition());
            float hpRatio = self.getStats().getHp() / self.getStats().getMaxHp();

            if (dist <= attackRange
                    && hpRatio <= counterHpThreshold
                    && RNG.nextFloat() < counterAttackChance) {
                threat.decreaseHp(self.getCombatPower());
                counterAttackCooldown = cooldownTicks;
            } else {
                for (int i = 0; i < sprintSteps; i++) {
                    moveAwayFrom(self, threat.getPosition(), env);
                }
            }
        });
    }

    /**
     * Gộp named predators và apex predators, trả về mối đe dọa gần nhất.
     * Named predators: tìm theo speciesName. Apex: tìm qua isApexPredator().
     */
    private Optional<Organism> findNearestThreat(Animal self, Environment env) {
        Optional<Organism> nearestNamed = predatorSpecies.stream()
                .map(s -> findNearestBySpecies(self, env, s))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .min(Comparator.comparingDouble(o -> o.getPosition().distanceTo(self.getPosition())));

        Optional<Organism> nearestApex = findNearestApex(self, env)
                .map(a -> (Organism) a);

        if (nearestNamed.isEmpty()) return nearestApex;
        if (nearestApex.isEmpty()) return nearestNamed;

        float d1 = nearestNamed.get().getPosition().distanceTo(self.getPosition());
        float d2 = nearestApex.get().getPosition().distanceTo(self.getPosition());
        return d1 <= d2 ? nearestNamed : nearestApex;
    }
}
