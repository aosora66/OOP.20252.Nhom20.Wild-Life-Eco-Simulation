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
 * Kẻ thù là các loài săn mồi được khai báo rõ bằng speciesName.
 *
 * Khi HP thấp và kẻ thù áp sát (trong attackRange), có tỉ lệ phản kháng thay vì chỉ chạy.
 * Sát thương phản kháng = combatPower của bản thân.
 */
public class ScaredStrategy extends AbstractSurvivalStrategy {

    private final List<Class<? extends Animal>> predatorSpecies;
    private final int          sprintSteps;
    private final float        counterHpThreshold;
    private final float        counterAttackChance;
    private final float        fleeOnlyBelowHungerThreshold;
    private final int          cooldownTicks;
    private int                counterAttackCooldown = 0;

    /**
     * @param stepSize            bước di chuyển mỗi lần
     * @param fearRadius          tầm phát hiện kẻ thù (sightRadius)
     * @param sprintSteps         số bước chạy mỗi tick
     * @param counterAttackRange  khoảng cách để phản kháng (dùng làm attackRange)
     * @param counterHpThreshold  ngưỡng HP ratio để có thể phản kháng (0–1)
     * @param counterAttackChance xác suất phản kháng mỗi lần cơ hội (0–1)
     * @param predatorSpecies     các loài kẻ thù theo speciesName
     */
    public ScaredStrategy(float stepSize, float fearRadius, int sprintSteps,
                          float counterAttackRange, float counterHpThreshold,
                          float counterAttackChance, Class<? extends Animal>... predatorSpecies) {
        this(stepSize, fearRadius, sprintSteps, counterAttackRange, counterHpThreshold,
                counterAttackChance, Float.POSITIVE_INFINITY, predatorSpecies);
    }

    /**
     * Variant dùng cho predator: chỉ chạy khi chưa đói tới ngưỡng săn. Khi đói đủ ngưỡng,
     * ScaredStrategy tắt để HunterStrategy có thể lao vào cắn lại.
     */
    public ScaredStrategy(float stepSize, float fearRadius, int sprintSteps,
                          float counterAttackRange, float counterHpThreshold,
                          float counterAttackChance, float fleeOnlyBelowHungerThreshold,
                          Class<? extends Animal>... predatorSpecies) {
        super(stepSize, fearRadius, counterAttackRange);
        this.predatorSpecies     = List.of(predatorSpecies);
        this.sprintSteps         = Math.max(1, sprintSteps);
        this.counterHpThreshold  = counterHpThreshold;
        this.counterAttackChance = counterAttackChance;
        this.fleeOnlyBelowHungerThreshold = fleeOnlyBelowHungerThreshold;
        this.cooldownTicks       = AppConfig.getInt("organism.scared.counterAttackCooldown");
    }

    @Override
    public boolean isApplicable(Animal self, Environment env) {
        if (self.getStats().getHungerLevel() >= fleeOnlyBelowHungerThreshold) {
            return false;
        }
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
                self.performAttack(threat, self.getCombatPower());
                counterAttackCooldown = cooldownTicks;
            } else {
                for (int i = 0; i < sprintSteps; i++) {
                    moveAwayFrom(self, threat.getPosition(), env);
                }
            }
        });
    }

    /**
     * Trả về mối đe dọa gần nhất trong các loài săn mồi được khai báo rõ.
     * Apex herbivore như Voi không được coi là nguồn sợ hãi ở đây; Voi được xử lý
     * như vật cản sinh học trong Environment.isPositionPassable().
     */
    private Optional<? extends Animal> findNearestThreat(Animal self, Environment env) {
        return predatorSpecies.stream()
                .map(s -> findNearestBySpecies(self, env, s))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .max(Comparator.comparingDouble(o -> detectability(o, self, env)));
    }
}
