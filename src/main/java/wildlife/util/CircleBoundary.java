package wildlife.util;

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
}