package wildlife.model.organism.animal;

import wildlife.model.brain.SurvivalStrategy;
import wildlife.model.environment.Environment;
import wildlife.model.environment.dto.FoodItem;
import wildlife.model.environment.enums.FoodType;
import wildlife.model.environment.enums.TerrainType;
import wildlife.model.organism.Organism;
import wildlife.model.organism.component.AdaptabilityComponent;
import wildlife.model.organism.component.GrowthComponent;
import wildlife.model.organism.component.SurvivalStatsComponent;
import wildlife.util.AppConfig;
import wildlife.util.Vector2D;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Lớp abstract đại diện chức năng sinh học, thuộc tính vật lý chung của động vật
 * (tốc độ, tầm nhìn, sức chiến đấu) và cơ chế khởi tạo strategy.
 * Implement addSurvivalStrategies() để gọi addStrategy() cho từng strategy cần thiết.
 */
public abstract class Animal extends Organism {

    protected String gender;
    protected float vision;
    protected float combatPower;
    protected float speed;
    // Bán kính dùng chung cho ăn, uống, tấn công
    protected float interactionRadius;
    // Ngưỡng mặc định lấy từ config — subclass có thể override nếu muốn tinh chỉnh
    protected float defaultHungerSearchThreshold;
    protected float defaultThirstSearchThreshold;
    // Danh sách strategy gắn vào động vật — thứ tự thêm vào không quan trọng,
    protected final List<SurvivalStrategy> strategies = new ArrayList<>();
    // Chế độ ăn uống: danh sách các loại thức ăn có thể ăn
    protected final List<FoodType> diet = new ArrayList<>();

    protected Animal(String id,
                     String speciesName,
                     Vector2D startPos,
                     TerrainType startTer,
                     Environment startEnv,
                     GrowthComponent growth,
                     SurvivalStatsComponent stats,
                     AdaptabilityComponent adaptability) {
        super(id, speciesName, startPos, startTer, startEnv, growth, stats, adaptability);
        this.defaultHungerSearchThreshold = AppConfig.getFloat("organism.stats.hungerHpThreshold");
        this.defaultThirstSearchThreshold = AppConfig.getFloat("organism.stats.thirstHpThreshold");
    }

    // abstract
    /**
     * Subclass khai báo strategy bằng cách gọi addStrategy() một hoặc nhiều lần.
     */
    protected abstract void addSurvivalStrategies();

    // Phuong thức dùng chung
    /**
     * Gọi ở cuối constructor subclass để khởi tạo danh sách strategy.
     */
    protected final void initStrategies() {
        addSurvivalStrategies();
    }

    /** Thêm một strategy vào danh sách. Có thể gọi nhiều lần để gắn nhiều strategy. */
    public void addStrategy(SurvivalStrategy strategy) {
        this.strategies.add(strategy);
    }

    /** Kiểm tra xem động vật có thể ăn loại thức ăn này không. */
    public boolean canEat(FoodType type) {
        if (type == FoodType.WATER) return true;
        return diet.contains(type);
    }

    /**
     * Chọn và chạy strategy cos priority cao nhất mỗi tick, thỏa isApplicable()
     * Subclass gọi hàm này từ onTick().
     */
    protected void executeStrategy(int currentTick) {
        if (environment == null || strategies.isEmpty()) return;
        strategies.stream()
                .sorted(Comparator.comparingInt(SurvivalStrategy::getPriority).reversed())
                .filter(s -> s.isApplicable(this, environment))
                .findFirst()
                .ifPresent(s -> s.execute(this, environment));
    }

    /**
     * Tiêu thụ một FoodItem — giảm đói/khát, tăng HP, xóa khỏi resources.
     * Subclass có thể override để thêm hành vi (vd. trigger sinh sản khi no).
     */
    public void eating(FoodItem food) {
        if (food == null || environment == null) return;
        stats.consume(food.nutritionalValue(), food.isWater());
        environment.getResources().consume(food);
    }

}
