package wildlife.model.brain;

import wildlife.model.environment.Environment;
import wildlife.model.organism.Animal;

/**
 * Hợp đồng cho mọi hành vi sinh tồn của động vật.
 *
 * Mỗi strategy chỉ đảm nhận MỘT chức năng chuyên biệt (săn mồi, chạy trốn, ăn uống...).
 * Decay đói/khát và trừ HP ngưỡng được xử lý tập trung tại Organism.tick(),
 * không phải trong execute() — tránh decay bị gọi nhiều lần khi animal có nhiều strategy.
 *
 * Animal có thể gắn nhiều strategy. Mỗi tick, strategy có priority cao nhất
 * thỏa isApplicable() sẽ được chạy, các strategy còn lại bị bỏ qua.
 */
public interface SurvivalStrategy {

    /**
     * Kiểm tra strategy này có phù hợp với trạng thái hiện tại của động vật không.
     * Animal.executeStrategy() dùng kết quả này để chọn strategy nào chạy.
     */
    boolean isApplicable(Animal self, Environment env);

    /**
     * Thực thi hành vi chuyên biệt của strategy.
     * Không được gọi applyHungerThirstDecay hay checkHpThreshold ở đây.
     */
    void execute(Animal self, Environment env);

    /**
     * Độ ưu tiên — số càng cao càng được ưu tiên hơn.
     * Ví dụ: ScaredStrategy (30) > HunterStrategy (20) > PassiveStrategy (10).
     */
    int getPriority();
}