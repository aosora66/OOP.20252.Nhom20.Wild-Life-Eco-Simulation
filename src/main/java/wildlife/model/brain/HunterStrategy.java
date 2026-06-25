package wildlife.model.brain;

import wildlife.model.environment.Environment;
import wildlife.model.environment.dto.FoodItem;
import wildlife.model.environment.enums.FoodType;
import wildlife.model.environment.enums.TerrainType;
import wildlife.model.organism.Organism;
import wildlife.model.organism.animal.Animal;
import wildlife.util.AppConfig;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Strategy săn mồi — kích hoạt khi đói đủ ngưỡng, ưu tiên 20.
 * Nếu không tìm thấy con mồi trong tầm nhìn thì wander, chờ tick sau tìm lại.
 */
public class HunterStrategy extends AbstractSurvivalStrategy {

    private final List<Class<? extends Animal>> preySpecies;
    private final float  attackDamage;
    private final int chaseSteps;

    // Mức đói tối thiểu để bắt đầu săn (0–100)
    private final float  hungerSearchThreshold;
    private final boolean attackNearbyPreyWhenNotHungry;

    public HunterStrategy(float stepSize, float sightRadius, float attackRange,
                          float attackDamage, float hungerSearchThreshold,
                          Class<? extends Animal>... preySpecies) {
        this(stepSize, sightRadius, attackRange, attackDamage, hungerSearchThreshold, 1, preySpecies);
    }

    public HunterStrategy(float stepSize, float sightRadius, float attackRange,
                          float attackDamage, float hungerSearchThreshold,
                          int chaseSteps,
                          Class<? extends Animal>... preySpecies) {
        this(stepSize, sightRadius, attackRange, attackDamage, hungerSearchThreshold,
                chaseSteps, false, preySpecies);
    }

    public HunterStrategy(float stepSize, float sightRadius, float attackRange,
                          float attackDamage, float hungerSearchThreshold,
                          int chaseSteps,
                          boolean attackNearbyPreyWhenNotHungry,
                          Class<? extends Animal>... preySpecies) {
        super(stepSize, sightRadius, attackRange);
        this.attackDamage          = attackDamage;
        this.hungerSearchThreshold = hungerSearchThreshold;
        this.chaseSteps            = Math.max(1, chaseSteps);
        this.attackNearbyPreyWhenNotHungry = attackNearbyPreyWhenNotHungry;
        this.preySpecies           = List.of(preySpecies);
    }

    /** Chỉ săn khi đói đủ ngưỡng; predator có thể cắn mồi đã lọt sát tầm đánh. */
    @Override
    public boolean isApplicable(Animal self, Environment env) {
        if (!self.getGrowth().isAdult()
                || self.getStats().getThirstLevel() >= AppConfig.getFloat("organism.stats.thirstHpThreshold")) {
            return false;
        }
        if (self.getStats().getHungerLevel() >= hungerSearchThreshold) {
            return true;
        }
        return attackNearbyPreyWhenNotHungry
                && findNearestAttackablePreyInRange(self, env).isPresent();
    }

    @Override
    public int getPriority() { return 20; }

    /**
     * Thực thi chiến lược săn mồi:
     * 1. Ưu tiên ăn nếu có thịt sẵn trong phạm vi attackRange
     * 2. Bản năng săn mồi sống: tìm kiếm con mồi gần nhất.
     *    - Trong tầm đánh: Cắn mục tiêu (trừ HP). Nếu mục tiêu chết, Environment
     *      sẽ tự động dọn xác và sinh ra thịt (FoodItem) để nhặt vào tick tiếp theo.
     *    - Ngoài tầm đánh: Di chuyển bám theo con mồi.
     * 3. Trạng thái nghỉ (Wander): Nếu không có thịt và cũng không tìm thấy mồi,
     *    di chuyển lang thang ngẫu nhiên.
     *
     * @param self Thực thể động vật đang thực hiện chiến lược này.
     * @param env  Môi trường sống hiện tại chứa các danh sách thực thể và tài nguyên.
     */
    @Override
    public void execute(Animal self, Environment env) {
        boolean hungryEnoughToHunt = self.getStats().getHungerLevel() >= hungerSearchThreshold;
        if (!hungryEnoughToHunt && attackNearbyPreyWhenNotHungry) {
            findNearestAttackablePreyInRange(self, env)
                    .ifPresent(target -> attackAndEatIfKilled(self, env, target));
            return;
        }

        // =================================================================
        // ƯU TIÊN 1: Quét tìm thịt rớt xung quanh (trong phạm vi attackRange)
        // =================================================================
        List<FoodItem> nearbyFood = env.getResources().getFoodNear(self.getPosition(), attackRange);

        for (FoodItem f : nearbyFood) {
            if (f.type() == FoodType.MEAT) {
                self.eating(f);
                return;
            }
        }
        // Tìm con mồi gần nhất — trước tiên trong tầm nhìn, sau đó fallback toàn môi trường.
        Optional<? extends Animal> prey = findNearestPrey(self, env);
        // =================================================================
        // ƯU TIÊN 2: Kích hoạt bản năng săn mồi sống
        // =================================================================
        prey.ifPresentOrElse(
                target -> {
                    float dist = self.getPosition().distanceTo(target.getPosition());
                    if (dist <= attackRange) {
                        // Mồi trong tầm -> Cắn
                        attackAndEatIfKilled(self, env, target);
                    } else {
                        // Mồi ngoài tầm -> Đuổi theo
                        for (int i = 0; i < chaseSteps; i++) {
                            moveToward(self, target.getPosition(), env);
                            if (self.getPosition().distanceTo(target.getPosition()) <= attackRange) {
                                attackAndEatIfKilled(self, env, target);
                                break;
                            }
                        }
                    }
                },
                // =================================================================
                // ƯU TIÊN 3: Không có mồi, không có thịt -> Đi dạo
                // =================================================================
                () -> wander(self, env)
        );
    }

    /**
     * Tìm con mồi trong tầm nhìn trước; nếu không có thì tìm con mồi gần nhất trong toàn
     * môi trường (tương tự cách PassiveStrategy tìm food items toàn cục).
     */
    private Optional<? extends Animal> findNearestPrey(Animal self, Environment env) {
        Optional<? extends Animal> inSight = preySpecies.stream()
                .flatMap(species -> findNearestBySpecies(self, env, species).stream())
                .filter(a -> isValidPrey(self, env, a))
                .max(Comparator.comparingDouble(o -> detectability(o, self, env)));
        if (inSight.isPresent()) return inSight;

        return preySpecies.stream()
                .flatMap(species -> env.getRegistry().getAllAlive(species).stream())
                .filter(a -> isValidPrey(self, env, a))
                .min(Comparator.comparingDouble(a -> a.getPosition().distanceTo(self.getPosition())));
    }

    private Optional<? extends Animal> findNearestAttackablePreyInRange(Animal self, Environment env) {
        return preySpecies.stream()
                .flatMap(species -> env.getRegistry().findNear(self.getPosition(), attackRange, species).stream())
                .filter(a -> isValidPrey(self, env, a))
                .min(Comparator.comparingDouble(a -> a.getPosition().distanceTo(self.getPosition())));
    }

    private boolean isValidPrey(Animal self, Environment env, Animal prey) {
        return !prey.isApexPredator()
                && !prey.getSpeciesName().equals(self.getSpeciesName())
                && env.getTerrain().getTerrainAt(prey.getPosition()) != TerrainType.DEEP_WATER;
    }

    private void attackAndEatIfKilled(Animal self, Environment env, Animal target) {
        target.decreaseHp(attackDamage);
        if (!target.isAlive()) {
            self.getStats().consume(target.getStats().getNutritionalValue(), false);
            env.getRegistry().remove(target.getId());
        }
    }
}
