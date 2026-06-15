package wildlife.model.brain;

import wildlife.model.environment.Environment;
import wildlife.model.environment.dto.FoodItem;
import wildlife.model.environment.enums.FoodType;
import wildlife.model.organism.Organism;
import wildlife.model.organism.animal.Animal;

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

    // Mức đói tối thiểu để bắt đầu săn (0–100)
    private final float  hungerSearchThreshold;

    public HunterStrategy(float stepSize, float sightRadius, float attackRange,
                          float attackDamage, float hungerSearchThreshold,
                          Class<? extends Animal>... preySpecies) {
        super(stepSize, sightRadius, attackRange);
        this.attackDamage          = attackDamage;
        this.hungerSearchThreshold = hungerSearchThreshold;
        this.preySpecies           = List.of(preySpecies);
    }

    /** Chỉ săn khi đói đủ ngưỡng — khi no, nhường cho PassiveStrategy xử lý. */
    @Override
    public boolean isApplicable(Animal self, Environment env) {
        return self.getStats().getHungerLevel() >= hungerSearchThreshold;
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
        // Tìm con mồi gần nhất trong tất cả các loài có thể săn được
        Optional<? extends Animal> prey = preySpecies.stream()
                .flatMap(species -> findNearestBySpecies(self, env, species).stream())
                .max(Comparator.comparingDouble(o -> detectability(o, self, env)));
        // =================================================================
        // ƯU TIÊN 2: Kích hoạt bản năng săn mồi sống
        // =================================================================
        prey.ifPresentOrElse(
                target -> {
                    float dist = self.getPosition().distanceTo(target.getPosition());
                    if (dist <= attackRange) {
                        // Mồi trong tầm -> Cắn
                        target.decreaseHp(attackDamage);
                    } else {
                        // Mồi ngoài tầm -> Đuổi theo
                        moveToward(self, target.getPosition(), env);
                    }
                },
                // =================================================================
                // ƯU TIÊN 3: Không có mồi, không có thịt -> Đi dạo
                // =================================================================
                () -> wander(self, env)
        );
    }
}
