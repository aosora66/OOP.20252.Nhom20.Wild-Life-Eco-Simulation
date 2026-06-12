package wildlife.model.brain;

import wildlife.model.environment.Environment;
import wildlife.model.organism.animal.Animal;

/**
 * Hợp đồng cho mọi hành vi sinh tồn của sinh vật.
 */
public interface SurvivalStrategy {
    /**
     * Độ ưu tiên — số càng cao càng được ưu tiên hơn.
     */
    int getPriority();

    /**
     * Kiểm tra strategy này có phù hợp với trạng thái hiện tại của sinh vật không.
     */
    boolean isApplicable(Animal self, Environment env);

    /**
     * Thực thi hành vi chuyên biệt của strategy.
     */
    void execute(Animal self, Environment env);
}