package wildlife.model.environment.dto;

import wildlife.util.Vector2D;

/**
 * Value Object đại diện cho một đơn vị thức ăn/nước uống trong môi trường.
 *
 * Thiết kế bất biến (immutable record) vì:
 * - Tài nguyên không tự thay đổi vị trí sau khi được sinh ra.
 * - Chỉ bị xóa khỏi danh sách khi hết hạn hoặc bị tiêu thụ.
 */
public record FoodItem(
        /** Tọa độ của thức ăn trong môi trường */
        Vector2D position,

        /** Giá trị dinh dưỡng: dùng để giảm độ đói/khát và hồi HP */
        float nutritionalValue,

        /** true = nguồn nước, false = thức ăn rắn */
        boolean isWater,

        /** Số tick còn lại trước khi tài nguyên này biến mất (thối rữa/bốc hơi) */
        int ticksUntilExpiry
) {
    /**
     * Kiểm tra xem tài nguyên này đã hết hạn chưa.
     * @param currentTick tick hiện tại của hệ thống
     * @param spawnTick   tick lúc tài nguyên được tạo ra
     * @return true nếu đã hết hạn
     */
    public boolean isExpired(int currentTick, int spawnTick) {
        return (currentTick - spawnTick) >= ticksUntilExpiry;
    }
}