package wildlife.model.environment.dto;

import wildlife.util.Vector2D;

/**
 * Value Object đại diện cho một vật cản tĩnh trong môi trường (đá, bụi rậm...).
 *
 * Bất biến (immutable record): vật cản không tự dịch chuyển sau khi được đặt.
 * TerrainComponent sẽ dùng danh sách các ObstacleItem này để kiểm tra va chạm.
 */
public record ObstacleItem(
        /** Tọa độ của vật cản trong môi trường */
        Vector2D position
) { }