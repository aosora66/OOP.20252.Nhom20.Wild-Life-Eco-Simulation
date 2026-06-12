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
 * Method abstract là implement isApplicable(), getPriority() và execute().
 */
public abstract class AbstractSurvivalStrategy implements SurvivalStrategy {

    /** sinh số random */
    private static final Random RNG = new Random();

    /** độ dài bước đi */
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
        // RNG.nextFloat() rd 0->1.0
        float angle = RNG.nextFloat() * 2f * (float) Math.PI;
        Vector2D next = new Vector2D(
                self.getPosition().getX() + (float) Math.cos(angle) * stepSize,
                self.getPosition().getY() + (float) Math.sin(angle) * stepSize
        );
        if (env.isPositionPassable(next, self.getSpeciesName())) {
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
        if (env.isPositionPassable(next, self.getSpeciesName())) {
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
        if (env.isPositionPassable(next, self.getSpeciesName())) {
            self.setPosition(next);
        } else {
            wander(self, env);
        }
    }

    /**
     * Tìm sinh vật còn sống gần nhất cùng loài targetSpecies trong sightRadius,
     * loại trừ bản thân và xác đã chết (DEAD).
     * Lọc isAlive() để hunter không tấn công xác và scared không chạy khỏi predator đã chết.
     */
    protected Optional<Organism> findNearestBySpecies(Animal self, Environment env,
                                                      String targetSpecies) {
        return env.getRegistry()
                .findNear(self.getPosition(), sightRadius)
                .stream()
                .filter(o -> o.isAlive()
                        && !o.getId().equals(self.getId())
                        && o.getSpeciesName().equals(targetSpecies))
                .min(Comparator.comparingDouble(
                        o -> o.getPosition().distanceTo(self.getPosition())));
    }

    /** Tìm nguồn thức ăn (hoặc nước nếu wantWater=true) gần nhất trong sightRadius. */
    protected Optional<FoodItem> findNearestFood(Animal self, Environment env,
                                                 boolean wantWater) {
        return env.getResources()
                .getFoodNear(self.getPosition(), sightRadius)
                .stream()
                .filter(f -> f.isWater() == wantWater)
                .min(Comparator.comparingDouble(
                        f -> f.position().distanceTo(self.getPosition())));
    }
}
