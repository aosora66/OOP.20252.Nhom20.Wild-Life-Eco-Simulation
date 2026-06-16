package wildlife.util;

import java.util.Random;

public class RectBoundary implements Boundary {
    private final ValueRange boundsX;
    private final ValueRange boundsY;

    public RectBoundary(float startX, float endX, float startY, float endY) {
        this.boundsX = new ValueRange(startX, endX);
        this.boundsY = new ValueRange(startY, endY);
    }

    @Override
    public boolean contains(Vector2D pos) {
        return boundsX.contains(pos.getX()) && boundsY.contains(pos.getY());
    }

    @Override
    public Vector2D getRandomPoint(Random random) {
        float x = boundsX.getMin() + random.nextFloat() * (boundsX.getMax() - boundsX.getMin());
        float y = boundsY.getMin() + random.nextFloat() * (boundsY.getMax() - boundsY.getMin());
        return new Vector2D(x, y);
    }
}