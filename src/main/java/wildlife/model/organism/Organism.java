package wildlife.model.organism;

import wildlife.model.dto.RenderData;
import wildlife.model.environment.Environment;
import wildlife.model.environment.dto.FoodItem;
import wildlife.model.environment.enums.TerrainType;
import wildlife.model.organism.component.AdaptabilityComponent;
import wildlife.model.organism.component.GrowthComponent;
import wildlife.model.organism.component.SurvivalStatsComponent;
import wildlife.util.AppConfig;
import wildlife.util.Vector2D;

/**
 * Abstract class trung tâm đại diện cho mọi sinh vật
 */
public abstract class Organism {
    // HP bị trừ mỗi tick khi sinh vật vào giai đoạn lão hóa (key riêng, không dùng chung với starvation penalty)
    private static final float DECAY_HP_PENALTY = AppConfig.getFloat("organism.growth.decayHpPenalty");

    // ----------------------------------------------------------
    //  5 thuộc tính nhận diện cơ bản
    // ----------------------------------------------------------
    public enum DeathCause { OLD_AGE, KILLED, STARVATION, DEHYDRATION, OTHER }

    protected final String id;
    protected final String speciesName;
    protected Vector2D position;
    protected TerrainType currentTerrain;
    protected Environment environment;
    protected OrganismState state;
    protected boolean goWest;
    private DeathCause deathCause = DeathCause.OTHER;

    // ----------------------------------------------------------
    //  3 Components (gom 11 thuộc tính còn lại)
    // ----------------------------------------------------------
    protected final GrowthComponent growth;
    protected final SurvivalStatsComponent stats;
    protected final AdaptabilityComponent adaptability;

    // ----------------------------------------------------------
    //  Constructor
    // ----------------------------------------------------------

    /**
     * @param id             ID duy nhất (nên dùng UUID)
     * @param speciesName    tên loài ("Tho", "Soi", "Co"...)
     * @param startPos       tọa độ xuất phát
     * @param startEnv       môi trường ban đầu
     * @param growth         component sinh trưởng
     * @param stats          component chỉ số sinh tồn
     * @param adaptability   component thích nghi môi trường
     */
    protected Organism(String id,
                       String speciesName,
                       Vector2D startPos,
                       Environment startEnv,
                       GrowthComponent growth,
                       SurvivalStatsComponent stats,
                       AdaptabilityComponent adaptability) {
        this.id                 = id;
        this.speciesName        = speciesName;
        this.position           = startPos;
        this.environment        = startEnv;
        this.state              = OrganismState.ALIVE;
        this.growth             = growth;
        this.stats              = stats;
        this.adaptability       = adaptability;
    }

    // ----------------------------------------------------------
    //  Template Method - Logic vòng đời cốt lõi (Không được ghi đè)
    // ----------------------------------------------------------

    /**
     * Cập nhật toàn bộ trạng thái sinh vật mỗi tick.
     * Hàm này bị khóa (final) để ép luồng logic cốt lõi của hệ sinh thái.
     *
     * Thứ tự cố định: growUp → onTick (hành động) → reproduce → processSurvivalMetabolism (decay).
     * Hành động (ăn/uống) chạy trước decay để kết quả ăn trong tick này
     * được tính vào trước khi trừ HP.
     *
     * @param currentTick  số thứ tự tick hiện tại
     */
    public final void updateOrganism(int currentTick) {
        if (!isAlive()) return;
        refreshCurrentTerrain();

        // 1. Logic hệ thống bắt buộc (sinh vật tự động lớn lên và lão hóa)
        this.growUp();
        if (!isAlive()) return;

        this.onTick(currentTick);
        if (!isAlive()) return;
        refreshCurrentTerrain();

        this.reproduce();
        if (!isAlive()) return;

        // processSurvivalMetabolism() tự gọi die() khi HP về 0, không cần check lại sau đó
        this.processSurvivalMetabolism();
    }

    private void refreshCurrentTerrain() {
        if (environment != null) {
            this.currentTerrain = environment.getTerrain().getTerrainAt(position);
        }
    }

    // ----------------------------------------------------------
    //  Abstract methods
    // ----------------------------------------------------------

    /**
     * Hành vi cụ thể của từng loài trong mỗi tick.
     * @param currentTick số thứ tự tick hiện tại
     */
    protected abstract void onTick(int currentTick);

    /**
     * Sinh sản: xử lý logic tạo ra thế hệ tiếp theo.
     * Có thể tạo ra 0, 1 hoặc nhiều sinh vật con và trực tiếp thêm vào environment thông qua addOrganism().
     */
    public abstract void reproduce();

    // ----------------------------------------------------------
    //  Concrete methods — hành vi mặc định dùng chung
    // ----------------------------------------------------------

    /**
     * Tính và áp dụng decay đói/khát + toàn bộ HP drain mỗi tick.
     * Gộp 3 nguồn drain: base drain cơ bản + stress nhiệt độ + starvation penalty.
     * Thirst multiplier tính thêm yếu tố độ ẩm — môi trường khô làm khát nhanh hơn.
     */
    protected void processSurvivalMetabolism() {
        if (environment == null) return;

        float seasonMultiplier = environment.getTime().getSeasonMultiplier();
        float humidityFactor   = environment.getHumidity() / 100f;
        float thirstMultiplier = seasonMultiplier
                * (1f + (1f - humidityFactor)
                * AppConfig.getFloat("organism.stats.thirstHumidityFactor"));

        applyMetabolismDecay(seasonMultiplier, thirstMultiplier);

        float hpDrain      = getBaseHpDrainPerTick();
        float stressPenalty = getEnvironmentalStressHpPenalty();
        // Mùa khắc nghiệt làm stress tệ hơn
        if (stressPenalty > 0f && seasonMultiplier > 1f) {
            stressPenalty *= seasonMultiplier;
        }
        hpDrain += stressPenalty;
        hpDrain += stats.getStarvationPenalty();

        if (stats.reduceHp(hpDrain)) {
            if (stats.getThirstLevel() >= AppConfig.getFloat("organism.stats.thirstHpThreshold")) {
                deathCause = DeathCause.DEHYDRATION;
            } else if (stats.getHungerLevel() >= AppConfig.getFloat("organism.stats.hungerHpThreshold")) {
                deathCause = DeathCause.STARVATION;
            }
            die();
            return;
        }
        // Hồi máu thụ động khi đủ nước — chỉ chạy nếu còn sống sau drain
        stats.restoreHp(stats.getHydrationRegen());
    }

    /**
     * Áp dụng decay đói/khát mỗi tick. Mặc định cả đói và khát.
     * Subclass override nếu không có khái niệm đói (vd. Plant — chỉ khát, dùng năng lượng
     * từ quang hợp thay cho ăn).
     */
    protected void applyMetabolismDecay(float seasonMultiplier, float thirstMultiplier) {
        stats.applyHungerThirstDecay(seasonMultiplier, thirstMultiplier);
    }

    protected float getBaseHpDrainPerTick() {
        return AppConfig.getFloat("organism.stats.baseHpDrainPerTick");
    }

    /**
     * HP phạt thêm mỗi tick khi môi trường không phù hợp với khả năng thích nghi.
     * Hai mức kiểm tra:
     *   1. Terrain không thuộc danh sách sinh tồn được (vd. cá trên cạn) → lethal
     *   2. Nhiệt độ nằm ngoài vùng tolerance → lethal; ngoài optimal → suboptimal
     */
    protected float getEnvironmentalStressHpPenalty() {
        if (environment == null) return 0f;

        // Terrain check trước — sai môi trường thì coi như chết ngay, bất kể nhiệt độ
        if (currentTerrain != null && !adaptability.canSurviveIn(currentTerrain)) {
            return AppConfig.getFloat("organism.stats.lethalStressHpPenalty");
        }

        float temperature = environment.getTemperature();
        if (adaptability.isLethal(temperature) || !adaptability.canTolerate(temperature)) {
            return AppConfig.getFloat("organism.stats.lethalStressHpPenalty");
        }
        if (!adaptability.isOptimal(temperature)) {
            return AppConfig.getFloat("organism.stats.suboptimalStressHpPenalty");
        }
        return 0f;
    }

    /**
     * Tăng tuổi và cập nhật kích thước mỗi tick.
     * Nếu đang lão hóa, trừ thêm HP theo chỉ số cấu hình.
     */
    protected void growUp() {
        growth.computeGrowth();
        if (growth.isDecaying()) {
            boolean died = stats.reduceHp(DECAY_HP_PENALTY);
            if (died) { deathCause = DeathCause.OLD_AGE; die(); }
        }
    }

    /**
     * Trừ HP từ nguồn bên ngoài (bị tấn công bởi sinh vật khác).
     * Tự động gọi die() nếu HP về 0.
     */
    public void decreaseHp(float amount) {
        boolean died = stats.reduceHp(amount);
        if (died) { deathCause = DeathCause.KILLED; die(); }
    }

    public DeathCause getDeathCause() { return deathCause; }

    /**
     * Xử lý khi sinh vật chết: chuyển trạng thái → DEAD,
     * sau đó Environment sẽ xóa khỏi danh sách sau một khoảng thời gian.
     *
     * Protected — chỉ được gọi từ decreaseHp() hoặc subclass.
     */
    protected void die() {
        if (state == OrganismState.ALIVE) {
            state = OrganismState.DEAD;
            if (environment != null) {
                environment.recordDeath(speciesName, deathCause);
            }
        }
    }

    /**
     * Đóng gói dữ liệu tối thiểu để ViewLogic render.
     * Không lộ bất kỳ thuộc tính nội bộ nào và không chứa logic xử lý chuỗi của View.
     *
     * @return RenderData gửi cho view
     */
    /*
    public RenderData getRenderData() {
        return new RenderData(
                id,
                speciesName,
                position.getX(),
                position.getY(),
                state
        );
    }
    */
    // ----------------------------------------------------------
    //  Getters / Setters
    // ----------------------------------------------------------

    public String getId()                        { return id; }
    public String getSpeciesName()               { return speciesName; }
    public OrganismState getState()              { return state; }
    public Vector2D getPosition()                { return position; }
    public TerrainType getCurrentTerrain() { return currentTerrain; }
    public Environment getEnvironment()   { return environment; }
    public GrowthComponent getGrowth()           { return growth; }
    public SurvivalStatsComponent getStats()     { return stats; }
    public boolean isGoingWest(){return goWest;}
    public AdaptabilityComponent getAdaptability() { return adaptability; }

    public void setPosition(Vector2D pos)                    { this.position = pos; }
    public void setCurrentTerrain(TerrainType ter)   { this.currentTerrain = ter; }
    public void setEnvironment(Environment evn)   { this.environment = evn; }

    public boolean isAlive() { return state == OrganismState.ALIVE; }

    /**
     * Phải gọi sau khi thêm sinh vật vào Environment để các method phụ thuộc môi trường hoạt động.
     * Cập nhật environment reference mà không cần tạo lại sinh vật.
     */
    public void bindEnvironment(Environment env) {
        this.environment = env;
    }

    @Override
    public String toString() {
        return String.format("[%s | id=%s | hp=%.1f | age=%.0f | pos=%s | state=%s]",
                speciesName, id,
                stats.getHp(), growth.getCurrentAge(),
                position, state);
    }

}
