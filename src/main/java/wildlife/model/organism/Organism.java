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
    // Lấy chỉ số trừ HP khi lão hóa từ file config
    private static final float DECAY_HP_PENALTY = AppConfig.getFloat("organism.growth.decayHpPenalty");

    // ----------------------------------------------------------
    //  5 thuộc tính nhận diện cơ bản
    // ----------------------------------------------------------
    protected final String id;
    protected final String speciesName;
    protected Vector2D position;
    protected TerrainType currentTerrain;
    protected Environment environment;
    protected OrganismState state;

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
     * @param startTer       dia hinh ban đầu
     * @param startEnv       môi trường ban đầu
     * @param growth         component sinh trưởng
     * @param stats          component chỉ số sinh tồn
     * @param adaptability   component thích nghi môi trường
     */
    protected Organism(String id,
                       String speciesName,
                       Vector2D startPos,
                       TerrainType startTer,
                       Environment startEnv,
                       GrowthComponent growth,
                       SurvivalStatsComponent stats,
                       AdaptabilityComponent adaptability) {
        this.id                 = id;
        this.speciesName        = speciesName;
        this.position           = startPos;
        this.currentTerrain = startTer;
        this.environment = startEnv;
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
     * @param currentTick  số thứ tự tick hiện tại
     */
    public final void updateOrganism(int currentTick) {
        if (!isAlive()) return;

        // 1. Logic hệ thống bắt buộc (sinh vật tự động lớn lên và lão hóa)
        this.growUp();
        if (!isAlive()) return;

        this.onTick(currentTick);
        if (!isAlive()) return;

        // processSurvivalMetabolism() tự gọi die() khi HP về 0, không cần check lại sau đó
        this.processSurvivalMetabolism();
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
     * Sinh sản: tạo ra một sinh vật con cùng loài.
     * @return sinh vật con mới, hoặc null nếu chưa đủ điều kiện
     */
    public abstract Organism reproduce();

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

        stats.applyHungerThirstDecay(seasonMultiplier, thirstMultiplier);

        float hpDrain      = AppConfig.getFloat("organism.stats.baseHpDrainPerTick");
        float stressPenalty = getEnvironmentalStressHpPenalty();
        // Mùa khắc nghiệt làm stress tệ hơn
        if (stressPenalty > 0f && seasonMultiplier > 1f) {
            stressPenalty *= seasonMultiplier;
        }
        hpDrain += stressPenalty;
        hpDrain += stats.getStarvationPenalty();

        if (stats.reduceHp(hpDrain)) {
            die();
        }
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
            // Lão hóa: trừ HP từ từ (đã loại bỏ magic number 0.5f)
            boolean died = stats.reduceHp(DECAY_HP_PENALTY);
            if (died) die();
        }
    }

    /**
     * Trừ HP từ nguồn bên ngoài (bị tấn công, môi trường khắc nghiệt...).
     * Tự động gọi die() nếu HP về 0.
     *
     * @param amount lượng HP bị trừ
     */
    public void decreaseHp(float amount) {
        boolean died = stats.reduceHp(amount);
        if (died) die();
    }

    /**
     * Xử lý khi sinh vật chết: chuyển trạng thái → TRANSFORMING,
     * sau đó Environment sẽ xóa khỏi danh sách sau một khoảng thời gian.
     *
     * Protected — chỉ được gọi từ decreaseHp() hoặc subclass.
     */
    protected void die() {
        if (state == OrganismState.ALIVE) {
            state = OrganismState.DEAD;
            // Environment sẽ lắng nghe trạng thái này và xóa sau N tick
        }
    }

    /**
     * Đóng gói dữ liệu tối thiểu để ViewLogic render.
     * Không lộ bất kỳ thuộc tính nội bộ nào và không chứa logic xử lý chuỗi của View.
     *
     * @return RenderData gửi cho view
     */
    public RenderData getRenderData() {
        return new RenderData(
                id,
                speciesName,
                position.getX(),
                position.getY(),
                state
        );
    }

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
    public AdaptabilityComponent getAdaptability() { return adaptability; }

    public void setPosition(Vector2D pos)                    { this.position = pos; }
    public void setCurrentTerrain(TerrainType ter)   { this.currentTerrain = ter; }
    public void setEnvironment(Environment evn)   { this.environment = evn; }

    public boolean isAlive() { return state == OrganismState.ALIVE; }

    @Override
    public String toString() {
        return String.format("[%s | id=%s | hp=%.1f | age=%.0f | pos=%s | state=%s]",
                speciesName, id,
                stats.getHp(), growth.getCurrentAge(),
                position, state);
    }
    /**
     * Cập nhật trạng thái của sinh vật.
     * @param newState Trạng thái mới (ví dụ: ALIVE, DEAD, TRANSFORMING...)
     */
    public void setState(OrganismState newState) {
        this.state = newState; // Giả định biến lưu trạng thái của bạn tên là 'state'
    }
    
}
