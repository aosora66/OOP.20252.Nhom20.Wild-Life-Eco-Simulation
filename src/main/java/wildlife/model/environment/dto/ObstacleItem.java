package wildlife.model.environment.dto;

import wildlife.model.environment.enums.ObstacleType;
import wildlife.util.Vector2D;

/**
 * Value Object đại diện cho một vật cản tĩnh trong môi trường (đá, bụi rậm...).
 *
 * Bất biến (immutable record): vật cản không tự dịch chuyển sau khi được đặt.
 * Environment sẽ dùng danh sách các ObstacleItem này để kiểm tra va chạm.
 */
public record ObstacleItem(
        /** Tọa độ của vật cản trong môi trường */
        Vector2D position,

        /** Phân loại vật cản (Đá, Bụi rậm...) */
        ObstacleType type
) { 
    // ----------------------------------------------------------------
    //  Custom Getters (Tương thích ngược)
    // ----------------------------------------------------------------
    
    /**
     * Phương thức hỗ trợ để khớp với logic gọi hàm getType() trong Environment.
     * Mặc định record tạo ra hàm type(), nhưng ta định nghĩa thêm cho đồng bộ hệ thống.
     */
    public ObstacleType getType() {
        return this.type;
    }

    /**
     * Phương thức hỗ trợ để khớp với logic gọi hàm getPosition() ở các class khác.
     */
    public Vector2D getPosition() {
        return this.position;
    }
}