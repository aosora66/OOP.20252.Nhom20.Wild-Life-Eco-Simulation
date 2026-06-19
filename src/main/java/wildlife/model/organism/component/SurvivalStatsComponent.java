package wildlife.model.organism.component;

import wildlife.util.AppConfig;

/**
 * Các chỉ số sinh tồn, phương thức cập nhật liên quan
 */
public class SurvivalStatsComponent {
    private float hp;
    private final float maxHp;
    private float hungerLevel;
    private float thirstLevel;
    private final float nutritionalValue;

    // Tốc độ trao đổi chất (đói/khát) do từng loài tự quyết định
    private final float hungerDecayRate;
    private final float thirstDecayRate;

    // Các hằng số quy luật chung của hệ sinh thái (ngưỡng bắt đầu trừ máu theo thang 100)
    private static final float HUNGER_HP_THRESHOLD = AppConfig.getFloat("organism.stats.hungerHpThreshold");
    private static final float THIRST_HP_THRESHOLD = AppConfig.getFloat("organism.stats.thirstHpThreshold");
    private static final float HP_PENALTY_PER_TICK = AppConfig.getFloat("organism.stats.hpPenaltyPerTick");

    /**
     * @param hungerDecayRate tốc độ tăng độ đói mỗi tick
     * @param thirstDecayRate tốc độ tăng độ khát mỗi tick
     */
    public SurvivalStatsComponent(float maxHp, float nutritionalValue,
                                  float hungerDecayRate, float thirstDecayRate) {
        this.maxHp            = maxHp;
        this.hp               = maxHp;
        this.hungerLevel      = 0f;
        this.thirstLevel      = 0f;
        this.nutritionalValue = nutritionalValue;
        this.hungerDecayRate  = hungerDecayRate;
        this.thirstDecayRate  = thirstDecayRate;
    }

    public SurvivalStatsComponent(float maxHp, float nutritionalValue,
                                  float hungerDecayRate, float thirstDecayRate,
                                  float ignoredRegenRate) {
        this(maxHp, nutritionalValue, hungerDecayRate, thirstDecayRate);
    }

    // Cập nhật mức đói/khát
    public void applyHungerThirstDecay(float hungerMultiplier, float thirstMultiplier) {
        hungerLevel = Math.min(100f, hungerLevel + (hungerDecayRate * hungerMultiplier));
        thirstLevel = Math.min(100f, thirstLevel + (thirstDecayRate * thirstMultiplier));
    }

    /**
     * Cập nhật chỉ mức khát — dùng cho sinh vật không có khái niệm đói (vd. Plant).
     * hungerLevel giữ nguyên 0, không bao giờ tăng.
     */
    public void applyThirstOnlyDecay(float thirstMultiplier) {
        thirstLevel = Math.min(100f, thirstLevel + (thirstDecayRate * thirstMultiplier));
    }

    /**
     * Trả về lượng HP phạt mỗi tick nếu đói/khát vượt ngưỡng, ngược lại trả 0.
     * Caller (processSurvivalMetabolism) chịu trách nhiệm trừ HP — tránh trừ hai lần.
     */
    public float getStarvationPenalty() {
        if (hungerLevel >= HUNGER_HP_THRESHOLD || thirstLevel >= THIRST_HP_THRESHOLD) {
            return HP_PENALTY_PER_TICK;
        }
        return 0f;
    }

    /**
     * HP hồi thụ động mỗi tick khi sinh vật đủ nước (thirstLevel dưới ngưỡng).
     * Đây là cơ chế hax cũ: nước không hồi ngay, nhưng giữ cơ thể hồi chậm một thời gian.
     */
    public float getHydrationRegen() {
        float threshold = AppConfig.getFloat("organism.stats.hydrationRegenThreshold");
        if (thirstLevel <= threshold) {
            return AppConfig.getFloat("organism.stats.hydrationHpRegen");
        }
        return 0f;
    }

    // Chỉ kiểm tra sinh vật đã hết HP chưa — không tự trừ HP
    public boolean checkHpThreshold() {
        return hp <= 0f;
    }

    // Giảm hp với lượng bất kỳ
    public boolean reduceHp(float amount) {
        hp = Math.max(0f, hp - amount);
        return hp <= 0f;
    }

    // Tăng hp với lượng bất kỳ
    public void restoreHp(float amount) {
        hp = Math.min(maxHp, hp + amount);
    }

    // tiêu thụ -> khôi phục mức đói/khát/hp
    public void consume(float nutrition, boolean isWater) {
        boolean needsHealing = hungerLevel > 30f || thirstLevel > 30f;
        if (isWater) {
            thirstLevel = Math.max(0f, thirstLevel - nutrition);
        } else {
            hungerLevel = Math.max(0f, hungerLevel - nutrition);
        }
        if (needsHealing) {
            restoreHp(nutrition * AppConfig.getFloat("organism.stats.nutritionToHpRatio"));
        }
    }

    /** Hấp thụ độ ẩm thụ động từ môi trường (không hồi HP — khác với consume). */
    public void absorbMoisture(float amount) {
        thirstLevel = Math.max(0f, thirstLevel - amount);
    }

    // Getters
    public float getHp()               { return hp; }
    public float getMaxHp()            { return maxHp; }
    public float getHungerLevel()      { return hungerLevel; }
    public float getThirstLevel()      { return thirstLevel; }
    public float getNutritionalValue() { return nutritionalValue; }
}
