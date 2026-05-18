package wildlife.model.environment.event;

import java.util.ArrayList;
import java.util.List;

/**
 * Component phát sự kiện môi trường theo Observer Pattern.
 *
 * Trách nhiệm (SRP):
 * - Lưu danh sách listener đã đăng ký.
 * - Phát (publish) sự kiện tới tất cả listener.
 * - Quản lý ID âm thanh môi trường (ambient sound).
 *
 * Tách biệt hoàn toàn khỏi ViewLogic: Publisher không biết âm thanh
 * được phát như thế nào, chỉ thông báo "có sự kiện X".
 * ViewLogic (hoặc SoundManager) đăng ký lắng nghe và tự xử lý.
 *
 * === Hằng số sự kiện ===
 * Các lớp con hoặc ViewLogic tham chiếu các hằng này để tránh hardcode chuỗi.
 */
public class EnvironmentEventPublisher {

    // ----------------------------------------------------------------
    //  Hằng số loại sự kiện
    // ----------------------------------------------------------------

    /** Mưa bắt đầu */
    public static final String EVENT_RAIN_START      = "RAIN_START";
    /** Mưa kết thúc */
    public static final String EVENT_RAIN_END        = "RAIN_END";
    /** Hạn hán bắt đầu */
    public static final String EVENT_DROUGHT_START   = "DROUGHT_START";
    /** Mùa sinh sản bắt đầu */
    public static final String EVENT_BREEDING_SEASON = "BREEDING_SEASON";
    /** Sinh vật chết → xác xuất hiện */
    public static final String EVENT_ORGANISM_DIED   = "ORGANISM_DIED";
    /** Sinh vật mới được sinh ra */
    public static final String EVENT_ORGANISM_BORN   = "ORGANISM_BORN";
    /** Thức ăn mới xuất hiện trong môi trường */
    public static final String EVENT_FOOD_SPAWNED    = "FOOD_SPAWNED";
    /** Ngày mới bắt đầu (hết 1 chu kỳ ngày-đêm) */
    public static final String EVENT_NEW_DAY         = "NEW_DAY";

    // ----------------------------------------------------------------
    //  Trạng thái nội tại
    // ----------------------------------------------------------------

    /**
     * ID tham chiếu âm thanh nền của môi trường này.
     * ViewLogic dùng ID này để tải và phát file âm thanh phù hợp.
     * VD: "sounds/grassland_ambient.wav"
     */
    private final String ambientSoundId;

    /** Danh sách các listener đã đăng ký */
    private final List<EnvironmentEventListener> listeners;

    // ----------------------------------------------------------------
    //  Constructor
    // ----------------------------------------------------------------

    /**
     * @param ambientSoundId đường dẫn hoặc ID âm thanh nền của môi trường
     */
    public EnvironmentEventPublisher(String ambientSoundId) {
        this.ambientSoundId = ambientSoundId;
        this.listeners      = new ArrayList<>();
    }

    // ----------------------------------------------------------------
    //  Quản lý listener
    // ----------------------------------------------------------------

    /**
     * Đăng ký một listener mới.
     * Listener sẽ nhận thông báo mỗi khi có sự kiện được phát.
     *
     * @param listener listener cần đăng ký (không được null)
     */
    public void addListener(EnvironmentEventListener listener) {
        if (listener == null) return;
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Hủy đăng ký một listener.
     *
     * @param listener listener cần xóa
     */
    public void removeListener(EnvironmentEventListener listener) {
        listeners.remove(listener);
    }

    // ----------------------------------------------------------------
    //  Phát sự kiện
    // ----------------------------------------------------------------

    /**
     * Phát một sự kiện tới tất cả listener đã đăng ký.
     * Sử dụng bản sao danh sách để tránh ConcurrentModificationException
     * nếu listener tự hủy đăng ký trong quá trình xử lý.
     *
     * @param eventType loại sự kiện (nên dùng hằng số EVENT_* của class này)
     */
    public void publish(String eventType) {
        // Duyệt trên bản sao để an toàn
        List<EnvironmentEventListener> snapshot = new ArrayList<>(listeners);
        for (EnvironmentEventListener listener : snapshot) {
            listener.onEnvironmentEvent(eventType);
        }
    }

    // ----------------------------------------------------------------
    //  Getters
    // ----------------------------------------------------------------

    public String getAmbientSoundId() { return ambientSoundId; }

    /** Trả về số lượng listener đang đăng ký */
    public int getListenerCount() { return listeners.size(); }
}