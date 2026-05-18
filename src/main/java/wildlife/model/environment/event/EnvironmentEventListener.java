package wildlife.model.environment.event;

/**
 * Interface lắng nghe sự kiện môi trường.
 *
 * Áp dụng Observer Pattern: bất kỳ thành phần nào muốn phản ứng với
 * sự kiện môi trường (ViewLogic phát âm thanh, hệ thống ghi log...)
 * đều implement interface này và đăng ký vào EnvironmentEventPublisher.
 *
 * Thiết kế mở rộng (OCP): để thêm loại sự kiện mới, chỉ cần định nghĩa
 * thêm hằng chuỗi trong EnvironmentEventPublisher, không cần sửa interface.
 */
public interface EnvironmentEventListener {

    /**
     * Được gọi khi có sự kiện xảy ra trong môi trường.
     *
     * @param eventType loại sự kiện (xem hằng số trong EnvironmentEventPublisher)
     */
    void onEnvironmentEvent(String eventType);
}