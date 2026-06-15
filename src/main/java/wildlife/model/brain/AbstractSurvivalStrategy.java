package wildlife.model.brain;

import wildlife.model.environment.Environment;
import wildlife.model.environment.dto.FoodItem;
import wildlife.model.organism.Organism;
import wildlife.model.organism.animal.Animal;
import wildlife.util.Vector2D;

import java.util.Comparator;
import java.util.Optional;
import java.util.Random;

/**
 * Lớp abstract đại diện "bộ não hành vi", cung cấp các hành động di chuyển và tìm kiếm dùng chung.
 * Subclass chỉ cần implement isApplicable(), getPriority() và execute().
 */
public abstract class AbstractSurvivalStrategy implements SurvivalStrategy {

    /** Sinh số random — protected để ScaredStrategy có thể dùng trực tiếp */
    protected static final Random RNG = new Random();

    /** Độ dài bước đi */
    protected final float stepSize;

    /** Bán kính quan sát — dùng để tìm mồi, kẻ thù, thức ăn */
    protected final float sightRadius;

    /** Khoảng cách tấn công hoặc ăn */
    protected final float attackRange;

    protected AbstractSurvivalStrategy(float stepSize, float sightRadius, float attackRange) {
        this.stepSize    = stepSize;
        this.sightRadius = sightRadius;
        this.attackRange = attackRange;
    }

    // Phương thức dùng chung

    /** Di chuyển ngẫu nhiên một bước, bỏ qua nếu ô đích không đi được. */
    protected void wander(Animal self, Environment env) {
        float angle = RNG.nextFloat() * 2f * (float) Math.PI;
        Vector2D next = new Vector2D(
                self.getPosition().getX() + (float) Math.cos(angle) * stepSize,
                self.getPosition().getY() + (float) Math.sin(angle) * stepSize
        );
        if (env.isPositionPassable(next, self)) {
            self.setPosition(next);
        }
    }

    /** Tiến về phía target tối đa stepSize, dừng khi đã chạm. */
    protected void moveToward(Animal self, Vector2D target, Environment env) {
        Vector2D pos = self.getPosition();
        float dist   = pos.distanceTo(target);
        if (dist < 0.001f) return;

        float step = Math.min(stepSize, dist);
        float dx = (target.getX() - pos.getX()) / dist;
        float dy = (target.getY() - pos.getY()) / dist;

        Vector2D next = new Vector2D(
                pos.getX() + dx * step,
                pos.getY() + dy * step
        );
        if (env.isPositionPassable(next, self)) {
            self.setPosition(next);
        }
    }

    /**
     * Chạy ra xa khỏi threat một bước stepSize.
     * Nếu hướng ngược lại bị chặn, sang wander để tránh bị kẹt góc.
     */
    protected void moveAwayFrom(Animal self, Vector2D threat, Environment env) {
        Vector2D pos = self.getPosition();
        float dist   = pos.distanceTo(threat);
        if (dist < 0.001f) { wander(self, env); return; }

        float dx = (pos.getX() - threat.getX()) / dist;
        float dy = (pos.getY() - threat.getY()) / dist;

        Vector2D next = new Vector2D(
                pos.getX() + dx * stepSize,
                pos.getY() + dy * stepSize
        );
        if (env.isPositionPassable(next, self)) {
            self.setPosition(next);
        } else {
            wander(self, env);
        }
    }

    /**
     * Độ nhận diện của một mục tiêu — kết hợp visibility và khoảng cách.
     *
     * detectability = visibility / (1 + distance)
     *
     * Ý nghĩa:
     *  - visibility = 0  → detectability = 0  (vô hình hoàn toàn)
     *  - distance = 0    → detectability = visibility (tối đa, cùng vị trí)
     *  - distance tăng   → detectability giảm dần
     *  - +1 tránh chia-cho-0 khi hai sinh vật đứng cùng ô
     *
     * Nếu sau này thêm stealthFactor per-animal:
     *   return (visibility * stealthFactor) / (1 + distance)
     * — công thức không thay đổi.
     *
     * Dùng .max(detectability) để chọn mục tiêu "dễ phát hiện nhất".
     */
    protected double detectability(Organism target, Animal self, Environment env) {
        float visibility = env.getVisibilityModifier(target.getPosition());
        double distance  = target.getPosition().distanceTo(self.getPosition());
        return visibility / (1.0 + distance);
    }

    protected <T extends Organism> Optional<T> findNearestBySpecies(Animal self, Environment env,
                                                                     Class<T> targetSpecies) {
        return env.getRegistry()
                .findNear(self.getPosition(), sightRadius, targetSpecies)
                .stream()
                .filter(o -> !o.getId().equals(self.getId()))
                .filter(o -> detectability(o, self, env) > 0)
                .max(Comparator.comparingDouble(o -> detectability(o, self, env)));
    }

    protected Optional<Organism> findNearestBySpecies(Animal self, Environment env,
                                                       String targetSpecies) {
        return env.getRegistry()
                .findNear(self.getPosition(), sightRadius, Organism.class)
                .stream()
                .filter(o -> o.isAlive()
                          && !o.getId().equals(self.getId())
                          && o.getSpeciesName().equals(targetSpecies))
                .filter(o -> detectability(o, self, env) > 0)
                .max(Comparator.comparingDouble(o -> detectability(o, self, env)));
    }

    protected Optional<Animal> findNearestApex(Animal self, Environment env) {
        return env.getRegistry()
                .findNear(self.getPosition(), sightRadius, Animal.class)
                .stream()
                .filter(a -> !a.getId().equals(self.getId()) && a.isApexPredator())
                .filter(a -> detectability(a, self, env) > 0)
                .max(Comparator.comparingDouble(a -> detectability(a, self, env)));
    }

    /** Tìm nguồn thức ăn (hoặc nước nếu wantWater=true) gần nhất trong sightRadius. */
    protected Optional<FoodItem> findNearestFood(Animal self, Environment env,
                                                  boolean wantWater) {
        return env.getResources()
                .getFoodNear(self.getPosition(), sightRadius)
                .stream()
                .filter(f -> f.isWater() == wantWater && self.canEat(f.type()))
                .min(Comparator.comparingDouble(
                        f -> f.position().distanceTo(self.getPosition())));
    }
}
