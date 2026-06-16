package wildlife.util;

import java.util.Random;

public class CircleBoundary implements Boundary {
    private final Vector2D center;
    private final float radius;

    public CircleBoundary(Vector2D center, float radius) {
        this.center = center;
        this.radius = radius;
    }

    @Override
    public boolean contains(Vector2D pos) {
        return pos.distanceTo(center) <= radius;
    }

    @Override
    public Vector2D getRandomPoint(Random random) {
        // sqrt(random) để phân bố đều theo diện tích, không dồn về tâm
        float angle = random.nextFloat() * 2f * (float) Math.PI;
        float r = radius * (float) Math.sqrt(random.nextFloat());
        float x = center.getX() + r * (float) Math.cos(angle);
        float y = center.getY() + r * (float) Math.sin(angle);
        return new Vector2D(x, y);
    }
}