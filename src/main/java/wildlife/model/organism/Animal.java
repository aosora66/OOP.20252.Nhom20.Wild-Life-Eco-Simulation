package wildlife.model.organism;

import wildlife.model.environment.enums.TerrainType;
import wildlife.model.organism.component.AdaptabilityComponent;
import wildlife.model.organism.component.GrowthComponent;
import wildlife.model.organism.component.SurvivalStatsComponent;
import wildlife.util.AppConfig;
import wildlife.util.Vector2D;

/**
 * Lớp nền cho mọi động vật — cung cấp các thuộc tính vật lý chung
 * (tốc độ, tầm nhìn, sức chiến đấu) và cơ chế khởi tạo strategy.
 *
 * Subclass cần:
 * 1. Gán speed, vision, combatPower, interactionRadius trong constructor.
 * 2. Gọi initStrategies() ở cuối constructor sau khi gán xong các field trên.
 * 3. Implement addSurvivalStrategies() để gọi addStrategy() cho từng strategy cần thiết.
 */
public abstract class Animal extends Organism {

    protected String gender;
    protected float vision;
    protected float combatPower;
    protected float speed;
    // Bán kính dùng chung cho ăn, uống, tấn công — thay cho eatRadius + drinkRadius riêng lẻ
    protected float interactionRadius;
    // Ngưỡng mặc định lấy từ config — subclass có thể override nếu muốn tinh chỉnh
    protected float defaultHungerSearchThreshold;
    protected float defaultThirstSearchThreshold;

    protected Animal(String id,
                     String speciesName,
                     Vector2D startPos,
                     TerrainType startEnv,
                     GrowthComponent growth,
                     SurvivalStatsComponent stats,
                     AdaptabilityComponent adaptability) {
        super(id, speciesName, startPos, startEnv, growth, stats, adaptability);
        this.defaultHungerSearchThreshold = AppConfig.getFloat("organism.stats.hungerHpThreshold");
        this.defaultThirstSearchThreshold = AppConfig.getFloat("organism.stats.thirstHpThreshold");
    }

    /**
     * Subclass khai báo strategy bằng cách gọi addStrategy() một hoặc nhiều lần.
     * Thứ tự thêm không quan trọng — executeStrategy() tự sắp xếp theo priority.
     *
     * Ví dụ cho Rabbit:
     *   addStrategy(new ScaredStrategy(speed, vision, "Wolf", 3));
     *   addStrategy(new PassiveStrategy(speed, vision, interactionRadius, ...));
     */
    protected abstract void addSurvivalStrategies();

    /**
     * Gọi ở cuối constructor subclass để khởi tạo danh sách strategy.
     * final để đảm bảo luồng khởi tạo không bị ghi đè.
     */
    protected final void initStrategies() {
        addSurvivalStrategies();
    }

    // eating(FoodItem) được kế thừa từ Organism — override ở đây nếu cần thêm hành vi animal-specific
}
