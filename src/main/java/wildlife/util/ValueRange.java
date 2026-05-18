package wildlife.util;

/**
 * Khoảng giá trị: chịu đựng, thuận lợi, gây chết
 */
public class ValueRange {
    private final float min;
    private final float max;

    // constructor
    public ValueRange(float min, float max) {
        if (min > max) throw new IllegalArgumentException("min phải <= max");
        this.min = min;
        this.max = max;
    }

    // Kiểm tra có trong khoảng giá trị
    public boolean contains(float value) { return value >= min && value <= max; }
    public float getMin() { return min; }
    public float getMax() { return max; }
}
