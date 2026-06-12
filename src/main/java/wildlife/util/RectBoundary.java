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
    @Override
    public Vector2D getRandomPoint(java.util.Random random) {
        // Lưu ý: Tùy thuộc vào class ValueRange của cậu đặt tên hàm get là gì
        // (có thể là getMin/getMax hoặc getStart/getEnd). Cậu hãy sửa lại cho khớp nhé!
        float randomX = boundsX.getMin() + random.nextFloat() * (boundsX.getMax() - boundsX.getMin());
        float randomY = boundsY.getMin() + random.nextFloat() * (boundsY.getMax() - boundsY.getMin());
        return new Vector2D(randomX, randomY);
    }

    @Override
    public Vector2D getCenter() {
        float centerX = (boundsX.getMin() + boundsX.getMax()) / 2.0f;
        float centerY = (boundsY.getMin() + boundsY.getMax()) / 2.0f;
        return new Vector2D(centerX, centerY);
    }
}