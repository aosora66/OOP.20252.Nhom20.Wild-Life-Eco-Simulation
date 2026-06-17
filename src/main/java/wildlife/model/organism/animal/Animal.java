package wildlife.model.organism.animal;

import wildlife.model.brain.SurvivalStrategy;
import wildlife.model.environment.Environment;
import wildlife.model.environment.dto.FoodItem;
import wildlife.model.environment.enums.FoodType;
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

    protected String animalType;
    protected float vision;
    protected float combatPower;
    protected float speed;
    protected int lastReproduceTick = 0;
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
                     Environment startEnv,
                     GrowthComponent growth,
                     SurvivalStatsComponent stats,
                     AdaptabilityComponent adaptability,
                     String animalType) {
        super(id, speciesName, startPos, startEnv, growth, stats, adaptability);
        this.animalType = animalType;
        this.defaultHungerSearchThreshold = AppConfig.getFloat("organism.stats.hungerHpThreshold");
        this.defaultThirstSearchThreshold = AppConfig.getFloat("organism.stats.thirstHpThreshold");
    }

    @Override
    public abstract void reproduce();


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
        if (!food.isWater()) {
            environment.getResources().consume(food);
        }
    }

    public boolean canEat(FoodType type) {
        if (type == FoodType.WATER) return true;
        return diet.contains(type);
    }


    /** Getter dùng cho ScaredStrategy counter-attack. */
    public float getCombatPower() { return combatPower; }
    public float getVision()      { return vision; }
    public float getSpeed()       { return speed; }

    /**
     * Trả về true nếu loài này là "apex" — khiến MỌI động vật có ScaredStrategy phải chạy trốn.
     * Chỉ cần override trong các lớp đặc biệt (ví dụ: Elephant). Mặc định là false.
     */
    public boolean isApexPredator() { return false; }

    /**
     * Trả về true nếu loài này biết GẶM CỎ (ăn trực tiếp cây Grass, không chỉ ăn quả rụng).
     * Mặc định false; các loài ăn cỏ (Thỏ, Hươu, Voi) override thành true.
     */
    public boolean canGraze() { return false; }

    /**
     * Gặm một cây (vd. Cỏ): trừ "sinh khối" (HP) của cây và nạp dinh dưỡng cho bản thân.
     * Cây chết khi HP cạn (Organism.decreaseHp tự gọi die()); cỏ mọc lại nhờ sinh sản,
     * nên đây là nguồn thức ăn bền vững cho thú ăn cỏ.
     */
    public void grazeOn(wildlife.model.organism.plant.Plant plant) {
        if (plant == null || !plant.isAlive()) return;
        plant.decreaseHp(AppConfig.getFloat("animal.graze.biomassPerBite"));
        stats.consume(AppConfig.getFloat("animal.graze.nutritionPerBite"), false);
    }

    /** Kiểm tra xem động vật có thể sinh sản không (trưởng thành, no, khát, cooldown, may rủi). */
    protected boolean canReproduce(int currentTick) {
        float hungerThreshold = AppConfig.getFloat("animal.reproduce.hungerThreshold");
        float thirstThreshold = AppConfig.getFloat("animal.reproduce.thirstThreshold");
        int cooldown = AppConfig.getInt("animal.reproduce.cooldownTicks");
        float chance = AppConfig.getFloat("animal.reproduce.chance");

        return growth.isAdult() &&
                stats.getHungerLevel() < hungerThreshold &&
                stats.getThirstLevel() < thirstThreshold &&
                (currentTick - lastReproduceTick) >= cooldown &&
                Math.random() < chance;
    }
}
