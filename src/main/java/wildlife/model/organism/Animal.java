package wildlife.model.organism;

import wildlife.model.environment.Environment;
import wildlife.model.environment.dto.FoodItem;
import wildlife.model.environment.enums.TerrainType;
import wildlife.model.organism.component.AdaptabilityComponent;
import wildlife.model.organism.component.GrowthComponent;
import wildlife.model.organism.component.SurvivalStatsComponent;
import wildlife.util.AppConfig;
import wildlife.util.Vector2D;

import java.util.List;
import java.util.Random;

public abstract class Animal extends Organism {
    private final Random random = new Random();

    protected String gender;
    protected float vision;
    protected float combatPower;
    protected float speed;
    protected float eatRadius;
    protected float drinkRadius;
    protected List<FoodItem> EdibleFood;

    protected Animal(String id,
                     String speciesName,
                     Vector2D startPos,
                     Environment startEnv,
                     GrowthComponent growth,
                     SurvivalStatsComponent stats,
                     AdaptabilityComponent adaptability) {
        super(id, speciesName, startPos, startEnv, growth, stats, adaptability);
    }

    // di chuyển đến vị trí ngẫu nhiên trong bán kính di chuyển 1 tick
    public void wandering()  {
        if (currentEnvironment == null) return;

        float angle = random.nextFloat() * (float) (2 * Math.PI);
        float radius = random.nextFloat() * speed;
        Vector2D destination = new Vector2D(
                position.getX() + (float) Math.cos(angle) * radius,
                position.getY() + (float) Math.sin(angle) * radius
        );

        if (isPassable(destination)) {
            setPosition(destination);
        } else {
            moveTo(destination);
        }
    }

    public abstract void hunting();

    // ngược lại với moveTo() chọn vị trí xa vị trí cần chạy khỏi nhất (chạy khỏi kẻ đi săn)
    public void escaping(Vector2D pos) {

    }

    /**
     * Di chuyển tối đa {@link #speed} trong một tick về phía {@code target}.
     * Chọn bước đi hợp lệ (không vật cản, địa hình cho phép) và gần đích nhất.
     */
    public void moveTo(Vector2D target) {
        if (currentEnvironment == null || target == null) return;

        float distToTarget = position.distanceTo(target);
        if (distToTarget == 0f) return;

        if (distToTarget <= speed && isPassable(target)) {
            setPosition(target);
            return;
        }

        float stepLen = Math.min(speed, distToTarget);
        float dirX = (target.getX() - position.getX()) / distToTarget;
        float dirY = (target.getY() - position.getY()) / distToTarget;

        Vector2D bestStep = null;
        float bestRemainingDist = Float.MAX_VALUE;

        int samples = AppConfig.getInt("animal.move.directionSamples");
        float baseAngle = (float) Math.atan2(dirY, dirX);
        float spread = (float) Math.PI;

        for (int i = 0; i < samples; i++) {
            float t = samples == 1 ? 0f : (float) i / (samples - 1);
            float angle = baseAngle - spread / 2f + spread * t;

            Vector2D candidate = new Vector2D(
                    position.getX() + (float) Math.cos(angle) * stepLen,
                    position.getY() + (float) Math.sin(angle) * stepLen
            );

            if (!isPassable(candidate)) continue;

            float remaining = candidate.distanceTo(target);
            if (remaining < bestRemainingDist) {
                bestRemainingDist = remaining;
                bestStep = candidate;
            }
        }

        if (bestStep != null) {
            setPosition(bestStep);
        }
    }

    protected boolean isPassable(Vector2D pos) {
        return currentEnvironment != null && currentEnvironment.isPositionPassable(pos, speciesName);
    }

    public abstract void attacking(Organism org);

    // hàm được gọi khi đã tìm được ăn (có thể được gọi trong wandering() hoặc moving()
    public abstract void eating(FoodItem food);

    // tìm nước và uống nước
    public abstract void drinking();

}
