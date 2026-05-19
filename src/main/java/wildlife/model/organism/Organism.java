package wildlife.model.organism;

import wildlife.model.dto.RenderData;
import wildlife.model.environment.Environment;
import wildlife.model.environment.enums.TerrainType;
import wildlife.model.organism.component.AdaptabilityComponent;
import wildlife.model.organism.component.GrowthComponent;
import wildlife.model.organism.component.SurvivalStatsComponent;
import wildlife.util.AppConfig;
import wildlife.util.Vector2D;

/**
 * Abstract class trung tâm đại diện cho mọi sinh vật
 *
 * Nguyên tắc thiết kế:
 * - Logic sinh tồn (BioLogic) hoàn toàn tách khỏi ViewLogic.
 * - Các chỉ số cụ thể được ủy quyền cho 3 Component, tránh class phình to.
 * - Áp dụng Template Method Pattern ở hàm tick() để quản lý luồng sống chết cơ bản.
 * - Subclass CHỈ cần implement onTick() và reproduce().
 */
public abstract class Organism {
    // Lấy chỉ số trừ HP khi lão hóa từ file config
    private static final float DECAY_HP_PENALTY = AppConfig.getFloat("organism.stats.hpPenaltyPerTick");

    // ----------------------------------------------------------
    //  5 thuộc tính nhận diện cơ bản
    // ----------------------------------------------------------
    protected final String id;
    protected final String speciesName;
    protected Vector2D position;
    protected Environment currentEnvironment;
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
        this.currentEnvironment = startEnv;
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
     * @param currentTick  số thứ tự tick hiện tại
     */
    public final void tick(int currentTick) {
        if (!isAlive()) return;

        // 1. Logic hệ thống bắt buộc (sinh vật tự động lớn lên và lão hóa)
        this.growUp();

        // 2. Logic hành vi riêng của từng loài (nếu sinh vật vẫn còn sống sau khi growUp)
        if (isAlive()) {
            this.onTick(currentTick);
        // 3. Đói/khát, trao đổi cơ bản và ngưỡng HP (sau khi ăn/uống/quang hợp trong onTick)
            this.processSurvivalMetabolism();
        }
        // cập nhật trạng thái
        if (stats.checkHpThreshold()) {
            die();
        }
    }

    // ----------------------------------------------------------
    //  Abstract methods
    // ----------------------------------------------------------

    /**
     * Hành vi cụ thể của từng loài trong mỗi tick.
     * Được gọi tự động bởi hàm tick() chung.
     * @param currentTick số thứ tự tick hiện tại
     */
    protected abstract void onTick(int currentTick);

    /**
     * Sinh sản: tạo ra một sinh vật con cùng loài.
     * Subclass trả về đúng kiểu của mình (covariant return).
     *
     * @return sinh vật con mới, hoặc null nếu chưa đủ điều kiện
     */
    public abstract Organism reproduce();

    // ----------------------------------------------------------
    //  Concrete methods — hành vi mặc định dùng chung
    // ----------------------------------------------------------

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
     * Cập nhật đói/khát và trừ HP mỗi tick — dùng chung cho mọi loài.
     * Gọi sau {onTick(int)} để hành vi (ăn, quang hợp...) được tính trước.
     */
    protected void processSurvivalMetabolism() {
        if (currentEnvironment == null) return;

        float seasonMultiplier = currentEnvironment.getTime().getSeasonMultiplier();
        float humidityFactor = currentEnvironment.getHumidity()
                / AppConfig.getFloat("organism.stats.humidityMax");
        float thirstMultiplier = seasonMultiplier
                * (1f + (1f - humidityFactor) * AppConfig.getFloat("organism.stats.thirstHumidityFactor"));

        // cập nhật chỉ số đói và chỉ số khát mỗi tick
        stats.applyHungerThirstDecay(seasonMultiplier, thirstMultiplier);

        // giảm Hp mỗi tick
        float hpDrain = AppConfig.getFloat("organism.stats.baseHpDrainPerTick");
        float stressPenalty = getEnvironmentalStressHpPenalty();
        if (stressPenalty > 0f && seasonMultiplier > 1f) {
            stressPenalty *= seasonMultiplier;
        }
        hpDrain += stressPenalty;

        if (stats.reduceHp(hpDrain)) {
            die();
            return;
        }
    }

    /**
     * HP phạt thêm khi nhiệt độ ngoài vùng thích nghi (dùng {@link AdaptabilityComponent}).
     */
    protected float getEnvironmentalStressHpPenalty() {
        if (currentEnvironment == null) return 0f;

        float temperature = currentEnvironment.getTemperature();
        if (adaptability.isLethal(temperature) || !adaptability.canTolerate(temperature)) {
            return AppConfig.getFloat("organism.stats.lethalStressHpPenalty");
        }
        if (!adaptability.isOptimal(temperature)) {
            return AppConfig.getFloat("organism.stats.suboptimalStressHpPenalty");
        }
        return 0f;
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

    /**
     * Xử lý khi sinh vật chết: chuyển trạng thái → TRANSFORMING,
     * sau đó Environment sẽ xóa khỏi danh sách sau một khoảng thời gian.
     *
     * Protected — chỉ được gọi từ decreaseHp() hoặc subclass.
     */
    protected void die() {
        if (state == OrganismState.ALIVE) {
            state = OrganismState.TRANSFORMING;
            // Environment sẽ lắng nghe trạng thái này và xóa sau N tick
        }
    }

    // ----------------------------------------------------------
    //  Getters / Setters
    // ----------------------------------------------------------

    public String getId()                        { return id; }
    public String getSpeciesName()               { return speciesName; }
    public OrganismState getState()              { return state; }
    public Vector2D getPosition()                { return position; }
    public Environment getCurrentEnvironment() { return currentEnvironment; }
    public GrowthComponent getGrowth()           { return growth; }
    public SurvivalStatsComponent getStats()     { return stats; }
    public AdaptabilityComponent getAdaptability() { return adaptability; }

    public void setPosition(Vector2D pos)                    { this.position = pos; }
    public void setCurrentEnvironment(Environment    env)   { this.currentEnvironment = env; }

    public boolean isAlive() { return state == OrganismState.ALIVE; }

    @Override
    public String toString() {
        return String.format("[%s | id=%s | hp=%.1f | age=%.0f | pos=%s | state=%s]",
                speciesName, id,
                stats.getHp(), growth.getCurrentAge(),
                position, state);
    }
}
