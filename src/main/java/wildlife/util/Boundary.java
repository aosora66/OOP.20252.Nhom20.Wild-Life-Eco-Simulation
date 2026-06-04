package wildlife.util;

import java.util.Random;

/**
 * Interface đại diện cho ranh giới của một khu vực (Chữ nhật, Tròn...)
 */
public interface Boundary {
    /** Kiểm tra xem tọa độ pos có nằm trong ranh giới này không */
    boolean contains(Vector2D pos);
    Vector2D getRandomPoint(Random random);
}