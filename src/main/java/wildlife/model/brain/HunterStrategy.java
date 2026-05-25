package wildlife.model.brain;

import wildlife.model.environment.Environment;
import wildlife.model.environment.dto.FoodItem;
import wildlife.model.organism.Organism;

import java.util.List;
import java.util.Optional;

/**
 * Strategy săn mồi — kích hoạt khi đói đủ ngưỡng, ưu tiên 20.
 * Nếu không tìm thấy con mồi trong tầm nhìn thì wander, chờ tick sau tìm lại.
 */
public class HunterStrategy extends AbstractSurvivalStrategy {

    private final String preySpecies;
    private final float  attackDamage;
    // Mức đói tối thiểu để bắt đầu săn (0–100)
    private final float  hungerSearchThreshold;

    public HunterStrategy(float stepSize, float sightRadius, float attackRange,
                          float attackDamage, float hungerSearchThreshold,
                          String preySpecies) {
        super(stepSize, sightRadius, attackRange);
        this.attackDamage          = attackDamage;
        this.hungerSearchThreshold = hungerSearchThreshold;
        this.preySpecies           = preySpecies;
    }

    /** Chỉ săn khi đói đủ ngưỡng — khi no, nhường cho PassiveStrategy xử lý. */
    @Override
    public boolean isApplicable(Organism self, Environment env) {
        return self.getStats().getHungerLevel() >= hungerSearchThreshold;
    }

    @Override
    public int getPriority() { return 20; }

    /**
     * Nếu con mồi trong tầm tấn công thì đánh, ngược lại tiến lại gần.
     * Khi con mồi chết: chuyển xác thành thịt, xóa khỏi registry, rồi ăn ngay —
     * đảm bảo hungerLevel giảm trong cùng tick săn được mồi.
     * Nếu không tìm thấy con mồi nào thì wander chờ tick sau.
     */
    @Override
    public void execute(Organism self, Environment env) {
        Optional<Organism> prey = findNearestBySpecies(self, env, preySpecies);
        prey.ifPresentOrElse(
            target -> {
                float dist = self.getPosition().distanceTo(target.getPosition());
                if (dist <= attackRange) {
                    target.decreaseHp(attackDamage);
                    if (!target.isAlive()) {
                        eatCorpse(self, target, env);
                    }
                } else {
                    moveToward(self, target.getPosition(), env);
                }
            },
            () -> wander(self, env)
        );
    }

    /** Chuyển xác con mồi thành FoodItem, xóa khỏi registry, ăn ngay nếu tìm thấy thịt. */
    private void eatCorpse(Organism self, Organism target, Environment env) {
        float nutrition = target.getStats().getNutritionalValue();
        env.getResources().convertDeadToMeat(target.getPosition(), nutrition);
        env.getRegistry().remove(target.getId());

        // Tìm miếng thịt vừa spawn gần nhất và ăn
        List<FoodItem> nearby = env.getResources().getFoodNear(self.getPosition(), attackRange);
        FoodItem meat = null;
        float minDist = Float.MAX_VALUE;
        for (FoodItem f : nearby) {
            if (f.isWater()) continue;
            float d = f.position().distanceTo(self.getPosition());
            if (d < minDist) { minDist = d; meat = f; }
        }
        if (meat != null) {
            self.eating(meat);
        }
    }
}