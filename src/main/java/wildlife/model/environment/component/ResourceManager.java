package wildlife.model.environment.component;

import wildlife.model.environment.dto.FoodItem;
import wildlife.model.environment.dto.ObstacleItem;
import wildlife.model.environment.enums.FoodType;
import wildlife.model.environment.enums.ObstacleType;
import wildlife.util.AppConfig;
import wildlife.util.Vector2D;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Component quản lý tài nguyên thiên nhiên trong môi trường:
 * thức ăn, nguồn nước và vật cản tĩnh.
 */
public class ResourceManager {

    /** Danh sách tất cả thức ăn/nước hiện có trong môi trường */
    private final List<FoodItem> foodItems;

    /** Danh sách vật cản tĩnh (đá, bụi rậm...) */
    private final List<ObstacleItem> obstacles;

    // ----------------------------------------------------------------
    //  Constructor
    // ----------------------------------------------------------------

    public ResourceManager() {
        this.foodItems = new ArrayList<>();
        this.obstacles  = new ArrayList<>();
    }

    // ----------------------------------------------------------------
    //  Sinh tài nguyên
    // ----------------------------------------------------------------

    /**
     * Sinh ra một đơn vị thức ăn hoặc nước tại vị trí chỉ định.
     * @param pos           tọa độ sinh ra tài nguyên
     * @param nutrition     giá trị dinh dưỡng
     * @param type
     * @param ticksExpiry   số tick tồn tại
     */
    public void spawnFood(Vector2D pos, float nutrition, FoodType type, int ticksExpiry) {
        foodItems.add(new FoodItem(pos, nutrition, type, ticksExpiry));
    }

    /**
     * Đặt một vật cản tĩnh vào môi trường.
     * @param pos tọa độ của vật cản
     * @param type
     */
    public void placeObstacle(Vector2D pos, ObstacleType type) {
        obstacles.add(new ObstacleItem(pos, type));
    }

    // ----------------------------------------------------------------
    //  Dọn dẹp tài nguyên hết hạn
    // ----------------------------------------------------------------

    /**
     * Xóa một FoodItem cụ thể khỏi danh sách (khi sinh vật ăn/uống).
     *
     * @param item tài nguyên đã bị tiêu thụ
     */
    public void consume(FoodItem item) {
        foodItems.remove(item);
    }

    /**
     * Xóa tất cả FoodItem đã hết thời hạn tồn tại.
     *
     * @param currentTick tick hiện tại
     */
    public void removeExpiredFood(int currentTick) {
        // Tạo lại danh sách food với ticksUntilExpiry giảm đi 1 mỗi tick,
        // loại bỏ những cái đã hết hạn (ticksUntilExpiry <= 0)
        List<FoodItem> updated = new ArrayList<>();
        for (FoodItem item : foodItems) {
            int remaining = item.ticksUntilExpiry() - 1;
            if (remaining > 0) {
                updated.add(new FoodItem(
                        item.position(),
                        item.nutritionalValue(),
                        item.type(),
                        remaining
                ));
            }
            // Nếu remaining <= 0: tài nguyên đã thối rữa/bốc hơi, không thêm lại
        }
        foodItems.clear();
        foodItems.addAll(updated);
    }

    /**
     * Chuyển đổi xác sinh vật chết thành thịt (FoodItem) tại vị trí của nó.
     *
     * @param pos       vị trí xác sinh vật
     * @param nutrition giá trị dinh dưỡng của xác (thường = nutritionalValue của loài)
     */
    public void convertDeadToMeat(Vector2D pos, float nutrition) {
        int expiry = AppConfig.getInt("environment.meat.expiryTicks");
        spawnFood(pos, nutrition, FoodType.MEAT, expiry);
    }

    // ----------------------------------------------------------------
    //  Tìm kiếm tài nguyên
    // ----------------------------------------------------------------

    /**
     * Lấy danh sách thức ăn/nước trong bán kính quanh một vị trí.
     * @param center tọa độ trung tâm
     * @param radius bán kính tìm kiếm
     * @return danh sách FoodItem trong vùng (chỉ đọc)
     */
    public List<FoodItem> getFoodNear(Vector2D center, float radius) {
        List<FoodItem> result = new ArrayList<>();
        for (FoodItem item : foodItems) {
            if (item.position().distanceTo(center) <= radius) {
                result.add(item);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Lấy danh sách vật cản trong bán kính quanh một vị trí.
     * Dùng để kiểm tra va chạm khi sinh vật di chuyển.
     *
     * @param center tọa độ trung tâm
     * @param radius bán kính tìm kiếm
     * @return danh sách ObstacleItem trong vùng (chỉ đọc)
     */
    public List<ObstacleItem> getObstaclesNear(Vector2D center, float radius) {
        List<ObstacleItem> result = new ArrayList<>();
        for (ObstacleItem item : obstacles) {
            if (item.position().distanceTo(center) <= radius) {
                result.add(item);
            }
        }
        return Collections.unmodifiableList(result);
    }

    // ----------------------------------------------------------------
    //  Tương tác của Người dùng (User Interaction)
    // ----------------------------------------------------------------
    /** Người dùng click để gieo mầm thức ăn (Tính năng thủ công) */
    public void spawnFoodManual(Vector2D pos, FoodType type, float nutrition) {
        int expiry = AppConfig.getInt("environment.meat.expiryTicks");
        spawnFood(pos, nutrition, type, expiry);
    }
    /**
     * Người dùng đặt thêm vật cản (đá, vách núi...) vào bản đồ.
     * @param position Tọa độ đặt vật cản
     */
    public void addObstacle(Vector2D position, ObstacleType type) {
        if (position != null) {
            obstacles.add(new ObstacleItem(position, type));
        }
    }

    /**
     * Người dùng xóa vật cản tại một khu vực (Click chuột).
     * @param targetPos Tọa độ click chuột
     * @param clickRadius Bán kính tác dụng của cú click (để dễ xóa trúng)
     */
    public void removeObstaclesNear(Vector2D targetPos, float clickRadius) {
        // Dùng removeIf để xóa nhanh các vật cản nằm trong vùng click chuột
        obstacles.removeIf(obs -> obs.position().distanceTo(targetPos) <= clickRadius);
    }
    // ----------------------------------------------------------------
    //  Getters
    // ----------------------------------------------------------------

    /** Trả về danh sách tất cả thức ăn hiện có (chỉ đọc) */
    public List<FoodItem> getAllFood() {
        return Collections.unmodifiableList(foodItems);
    }

    /** Trả về danh sách tất cả vật cản (chỉ đọc) */
    public List<ObstacleItem> getAllObstacles() {
        return Collections.unmodifiableList(obstacles);
    }
}