package wildlife.util;

/**
 * đóng gói các thuộc tính x, y là final.
 * Bất cứ khi nào sinh vật di chuyển, ta tạo ra một Vector2D tọa độ mới.
 */
public class Vector2D {
    private final float x;
    private final float y;

    // constructor
    public Vector2D(float x, float y) {
        this.x = x;
        this.y = y;
    }

    // Tính khoảng cách tới tọa độ khác
    public float distanceTo(Vector2D other) {
        float dx = this.x - other.x;
        float dy = this.y - other.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    public float getX() { return x; }
    public float getY() { return y; }

    @Override
    public String toString() { return "(" + x + ", " + y + ")"; }
}
