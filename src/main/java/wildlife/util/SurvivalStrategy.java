package wildlife.util;

import wildlife.model.environment.Environment;
import wildlife.model.organism.Organism;

/**
 * Hợp đồng cho mọi hành vi sinh tồn của sinh vật.
 *
 * Mỗi strategy chỉ đảm nhận MỘT chức năng chuyên biệt (săn mồi, chạy trốn, ăn uống...).
 * Decay đói/khát và trừ HP ngưỡng được xử lý tập trung tại Organism.tick(),
 * không phải trong execute() — tránh decay bị gọi nhiều lần khi organism có nhiều strategy.
 *
 * Organism có thể gắn nhiều strategy. Mỗi tick, strategy có priority cao nhất
 * thỏa isApplicable() sẽ được chạy, các strategy còn lại bị bỏ qua.
 */
public interface SurvivalStrategy {

    /**
     * Kiểm tra strategy này có phù hợp với trạng thái hiện tại của sinh vật không.
     * Organism.executeStrategy() dùng kết quả này để chọn strategy nào chạy.
     */
    boolean isApplicable(Organism self, Environment env);

    /**
     * Thực thi hành vi chuyên biệt của strategy.
     * Không được gọi applyHungerThirstDecay hay checkHpThreshold ở đây.
     */
    void execute(Organism self, Environment env);

    /**
     * Độ ưu tiên — số càng cao càng được ưu tiên hơn.
     * Ví dụ: ScaredStrategy (30) > HunterStrategy (20) > PassiveStrategy (10).
     */
    int getPriority();
}