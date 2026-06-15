package wildlife.model.brain;

import wildlife.model.environment.Environment;
import wildlife.model.organism.animal.Animal;

/**
 * Strategy sinh tồn cơ bản — tìm nước/thức ăn khi cần, wander khi đủ no.
 * Luôn isApplicable() nên đóng vai trò fallback khi không có strategy nào khác chạy.
 * Ưu tiên 10 — thấp nhất, chỉ chạy khi ScaredStrategy và HunterStrategy không kích hoạt.
 */
public class PassiveStrategy extends AbstractSurvivalStrategy {

    // Ngưỡng đói/khát để bắt đầu tìm kiếm (0–100)
    private final float hungerSearchThreshold;
    private final float thirstSearchThreshold;

    public PassiveStrategy(float stepSize, float sightRadius, float eatRange,
                           float hungerSearchThreshold, float thirstSearchThreshold) {
        super(stepSize, sightRadius, eatRange);
        this.hungerSearchThreshold = hungerSearchThreshold;
        this.thirstSearchThreshold = thirstSearchThreshold;
    }

    /** Luôn true — đây là hành vi mặc định khi không có strategy ưu tiên cao hơn nào áp dụng. */
    @Override
    public boolean isApplicable(Animal self, Environment env) { return true; }

    @Override
    public int getPriority() { return 10; }

    /**
     * Thứ tự ưu tiên bên trong: khát → đói → wander.
     * Khát được ưu tiên hơn đói vì thiếu nước gây chết nhanh hơn trong hệ sinh thái này.
     * Khi đã vào nhánh khát/đói, dùng return sớm để không đồng thời tìm cả hai.
     */
    @Override
    public void execute(Animal self, Environment env) {
        if (self.getStats().getThirstLevel() >= thirstSearchThreshold) {
            findNearestFood(self, env, true).ifPresentOrElse(
                    water -> {
                        moveToward(self, water.position(), env);
                        if (self.getPosition().distanceTo(water.position()) <= attackRange) {
                            self.eating(water);
                        }
                    },
                    () -> wander(self, env)
            );
            return;
        }

        if (self.getStats().getHungerLevel() >= hungerSearchThreshold) {
            findNearestFood(self, env, false).ifPresentOrElse(
                    food -> {
                        moveToward(self, food.position(), env);
                        if (self.getPosition().distanceTo(food.position()) <= attackRange) {
                            self.eating(food);
                        }
                    },
                    () -> wander(self, env)
            );
            return;
        }

        wander(self, env);
    }
}
