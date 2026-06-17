package wildlife.model.brain;

import wildlife.model.environment.Environment;
import wildlife.model.environment.dto.FoodItem;
import wildlife.model.organism.animal.Animal;
import wildlife.model.organism.plant.Grass;

import java.util.Comparator;
import java.util.Optional;

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
        if (tryOpportunisticEat(self, env)) return;

        if (self.getStats().getThirstLevel() >= thirstSearchThreshold) {
            findNearestKnownFood(self, env, true).ifPresentOrElse(
                    water -> {
                        moveToward(self, water.position(), env);
                        if (self.getPosition().distanceTo(water.position()) <= attackRange) {
                            self.eating(water);
                        }
                    },
                    () -> {
                        if (self.getStats().getHungerLevel() >= hungerSearchThreshold) {
                            seekNearestFoodOrGrass(self, env);
                        } else {
                            wander(self, env);
                        }
                    }
            );
            return;
        }

        if (self.getStats().getHungerLevel() >= hungerSearchThreshold) {
            seekNearestFoodOrGrass(self, env);
            return;
        }

        // Chưa tới ngưỡng đói/khát: đi lang thang. Nhưng nếu vô tình có thức ăn/nước
        // ngay dưới chân và đang còn nhu cầu (đói/khát > 0) thì nhặt ăn luôn — không bỏ phí.
        wander(self, env);
    }

    /**
     * Khi đói: đi tới và tiêu thụ nguồn dinh dưỡng GẦN NHẤT — thức ăn rơi (quả/tảo) hoặc
     * bụi Cỏ (nếu là loài gặm cỏ). So sánh khoảng cách để chọn mục tiêu gần hơn; không có gì
     * thì wander chờ tick sau.
     */
    private void seekNearestFoodOrGrass(Animal self, Environment env) {
        var food  = findNearestKnownFood(self, env, false);
        Optional<Grass> grass = self.canGraze() ? findNearestKnownGrass(self, env) : Optional.empty();

        double dFood  = food.map(f -> (double) f.position().distanceTo(self.getPosition()))
                .orElse(Double.MAX_VALUE);
        double dGrass = grass.map(g -> (double) g.getPosition().distanceTo(self.getPosition()))
                .orElse(Double.MAX_VALUE);

        if (dFood == Double.MAX_VALUE && dGrass == Double.MAX_VALUE) {
            wander(self, env);
            return;
        }

        if (dGrass <= dFood) {                       // gặm cỏ gần hơn (hoặc không có quả)
            moveToward(self, grass.get().getPosition(), env);
            if (self.getPosition().distanceTo(grass.get().getPosition()) <= attackRange) {
                self.grazeOn(grass.get());
            }
        } else {                                     // tới chỗ thức ăn rơi
            moveToward(self, food.get().position(), env);
            if (self.getPosition().distanceTo(food.get().position()) <= attackRange) {
                self.eating(food.get());
            }
        }
    }

    private Optional<FoodItem> findNearestKnownFood(Animal self, Environment env, boolean wantWater) {
        Optional<FoodItem> visible = findNearestFood(self, env, wantWater);
        if (visible.isPresent()) {
            return visible;
        }

        return env.getResources()
                .getAllFood()
                .stream()
                .filter(f -> f.isWater() == wantWater && self.canEat(f.type()))
                .min(Comparator.comparingDouble(
                        f -> f.position().distanceTo(self.getPosition())));
    }

    private Optional<Grass> findNearestKnownGrass(Animal self, Environment env) {
        Optional<Grass> visible = findNearestBySpecies(self, env, Grass.class);
        if (visible.isPresent()) {
            return visible;
        }

        return env.getRegistry()
                .getAllAlive(Grass.class)
                .stream()
                .min(Comparator.comparingDouble(
                        g -> g.getPosition().distanceTo(self.getPosition())));
    }

    /**
     * Ăn cơ hội: nếu có nước/thức ăn (hoặc cỏ với loài gặm cỏ) trong tầm với (attackRange)
     * và còn nhu cầu thì tiêu thụ. Ưu tiên nước trước. Trả về true nếu đã ăn/uống.
     */
    private boolean tryOpportunisticEat(Animal self, Environment env) {
        if (self.getStats().getThirstLevel() > 0f) {
            var water = findNearestFood(self, env, true);
            if (water.isPresent()
                    && self.getPosition().distanceTo(water.get().position()) <= attackRange) {
                self.eating(water.get());
                return true;
            }
        }
        if (self.getStats().getHungerLevel() > 0f) {
            var food = findNearestFood(self, env, false);
            if (food.isPresent()
                    && self.getPosition().distanceTo(food.get().position()) <= attackRange) {
                self.eating(food.get());
                return true;
            }
            if (self.canGraze()) {
                var grass = findNearestBySpecies(self, env, Grass.class);
                if (grass.isPresent()
                        && self.getPosition().distanceTo(grass.get().getPosition()) <= attackRange) {
                    self.grazeOn(grass.get());
                    return true;
                }
            }
        }
        return false;
    }
}
