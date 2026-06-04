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
    @Override
    public Vector2D getRandomPoint(java.util.Random random) {
        // Lấy ngẫu nhiên một góc (từ 0 đến 360 độ / 2PI)
        float angle = random.nextFloat() * (float) (2 * Math.PI);
        
        // Lấy ngẫu nhiên khoảng cách từ tâm (dùng căn bậc 2 để phân bố đều điểm)
        float r = radius * (float) Math.sqrt(random.nextFloat());
        
        // Tính toán tọa độ X, Y mới
        float x = center.getX() + r * (float) Math.cos(angle);
        float y = center.getY() + r * (float) Math.sin(angle);
        
        return new Vector2D(x, y);
    }
}