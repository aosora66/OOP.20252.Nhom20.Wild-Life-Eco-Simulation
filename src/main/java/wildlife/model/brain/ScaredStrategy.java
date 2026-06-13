package wildlife.model.brain;

import wildlife.model.environment.Environment;
import wildlife.model.organism.Animal;
import wildlife.util.AppConfig;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Strategy chạy trốn — kích hoạt ngay khi phát hiện kẻ thù trong tầm nhìn, ưu tiên 30.
 * Ưu tiên cao nhất để đảm bảo sinh tồn luôn được đặt trước săn mồi hay ăn uống.
 * Hỗ trợ nhiều loài kẻ thù — chạy khỏi con gần nhất trong tất cả các loài nguy hiểm.
 *
 * Khi HP thấp và kẻ thù áp sát (trong attackRange), có tỉ lệ phản kháng thay vì chỉ chạy.
 * Sát thương phản kháng = combatPower của bản thân.
 */
public class ScaredStrategy extends AbstractSurvivalStrategy {

    private final List<String> predatorSpecies;
    private final int          sprintSteps;
    // Ngưỡng HP (0–1) để kích hoạt phản kháng, vd. 0.3 = còn 30% HP
    private final float        counterHpThreshold;
    // Xác suất phản kháng mỗi lần cơ hội xuất hiện (0–1), không phải mỗi tick
    private final float        counterAttackChance;
    // Số tick phải chờ giữa hai lần phản kháng — đọc từ config, tránh spam mỗi tick
    private final int          cooldownTicks;
    // Đếm ngược tick còn lại trước khi được phản kháng tiếp
    private int                counterAttackCooldown = 0;

    /**
     * @param stepSize            bước di chuyển mỗi lần
     * @param fearRadius          tầm phát hiện kẻ thù (sightRadius)
     * @param sprintSteps         số bước chạy mỗi tick
     * @param counterAttackRange  khoảng cách để phản kháng (dùng làm attackRange)
     * @param counterHpThreshold  ngưỡng HP ratio để có thể phản kháng (0–1)
     * @param counterAttackChance xác suất phản kháng mỗi lần cơ hội (0–1)
     * @param predatorSpecies     các loài kẻ thù
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

    /** Kích hoạt khi có BẤT KỲ loài kẻ thù nào xuất hiện trong fearRadius. */
    @Override
    public boolean isApplicable(Animal self, Environment env) {
        return predatorSpecies.stream()
                .anyMatch(s -> findNearestBySpecies(self, env, s).isPresent());
    }

    @Override
    public int getPriority() { return 30; }

    /**
     * Tìm kẻ thù gần nhất (theo loài) rồi quyết định hành động:
     * - Nếu còn trong cooldown → chỉ chạy, bỏ qua cơ hội phản kháng
     * - Nếu kẻ thù áp sát (≤ attackRange) + HP thấp + may mắn → đánh lại, reset cooldown
     * - Ngược lại → chạy sprintSteps bước ra xa
     */
    @Override
    public void execute(Animal self, Environment env) {
        predatorSpecies.stream()
                .map(s -> findNearestBySpecies(self, env, s))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .min(Comparator.comparingDouble(o -> o.getPosition().distanceTo(self.getPosition())))
                .ifPresent(threat -> {
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
}