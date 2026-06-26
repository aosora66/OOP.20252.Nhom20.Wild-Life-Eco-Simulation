package wildlife.model.brain;

import wildlife.model.environment.Environment;
import wildlife.model.environment.dto.FoodItem;
import wildlife.model.organism.animal.Animal;
import wildlife.model.organism.plant.Grass;
import wildlife.util.Vector2D;

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

    // Trạng thái wander có hướng: đi thẳng một đoạn rồi mới random hướng mới
    private Vector2D wanderTarget = null;
    private int wanderStepsLeft   = 0;
    private static final int WANDER_PERSIST_TICKS = 25;
    private static final float WANDER_DIST        = 50f;

    // Chỉ uống nước cơ hội khi thực sự có nhu cầu — tránh con vật đứng mãi ở nước
    private static final float OPPORTUNISTIC_DRINK_THRESHOLD = 15f;

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

        // Chưa tới ngưỡng đói/khát: đi theo hướng đã chọn một đoạn rồi mới random lại.
        directedWander(self, env);
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
        Optional<Grass> visible = findNearestGrass(self, env);
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
     * Ăn cơ hội: nếu có nước/thức ăn trong tầm với VÀ đang thực sự cần thì tiêu thụ.
     * Nước chỉ uống khi thirstLevel > OPPORTUNISTIC_DRINK_THRESHOLD để tránh đứng mãi ở nước
     * trong khi đang đói — không để uống nước thay thế việc đi tìm đồ ăn.
     */
    private boolean tryOpportunisticEat(Animal self, Environment env) {
        if (self.getStats().getThirstLevel() > OPPORTUNISTIC_DRINK_THRESHOLD) {
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
                var grass = findNearestGrass(self, env);
                if (grass.isPresent()
                        && self.getPosition().distanceTo(grass.get().getPosition()) <= attackRange) {
                    self.grazeOn(grass.get());
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Wander có hướng: chọn một điểm đích ngẫu nhiên cách WANDER_DIST đơn vị,
     * đi thẳng về đó trong WANDER_PERSIST_TICKS tick, rồi mới chọn hướng mới.
     * Tự nhiên hơn việc random góc mỗi tick và giúp con vật thoát khỏi vùng nước.
     */
    private void directedWander(Animal self, Environment env) {
        if (wanderStepsLeft <= 0 || wanderTarget == null) {
            float angle = RNG.nextFloat() * 2f * (float) Math.PI;
            float dist  = WANDER_DIST * (0.5f + RNG.nextFloat() * 0.5f);
            wanderTarget = new Vector2D(
                    self.getPosition().getX() + (float) Math.cos(angle) * dist,
                    self.getPosition().getY() + (float) Math.sin(angle) * dist
            );
            wanderStepsLeft = WANDER_PERSIST_TICKS;
        }
        if (self.getPosition().distanceTo(wanderTarget) <= stepSize) {
            wanderStepsLeft = 0;
        } else {
            moveToward(self, wanderTarget, env);
            wanderStepsLeft--;
        }
    }
}
