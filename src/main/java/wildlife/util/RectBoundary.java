package wildlife.util;

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
}