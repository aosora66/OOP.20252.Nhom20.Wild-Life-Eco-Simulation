package wildlife.model.environment;

import wildlife.model.dto.RenderData;
import wildlife.model.environment.component.OrganismRegistry;
import wildlife.model.environment.component.ResourceManager;
import wildlife.model.environment.component.TerrainComponent;
import wildlife.model.environment.component.TimeComponent;
import wildlife.model.environment.dto.ObstacleItem;
import wildlife.model.environment.enums.ObstacleType;
import wildlife.model.environment.enums.WeatherType;
import wildlife.model.environment.event.EnvironmentEventListener;
import wildlife.model.environment.event.EnvironmentEventPublisher;
import wildlife.model.organism.Organism;
import wildlife.model.organism.OrganismState;
import wildlife.model.organism.animal.Animal;
import wildlife.model.organism.animal.canivores.Tiger;
import wildlife.model.organism.animal.canivores.Wolf;
import wildlife.model.organism.animal.hebivores.Deer;
import wildlife.model.organism.animal.hebivores.Rabbit;
import wildlife.util.AppConfig;
import wildlife.util.Boundary;
import wildlife.util.Vector2D;
import wildlife.util.SoundManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class trung tâm của tầng môi trường.
 *
 * luồng cập nhật chính (tick → sinh vật → tài nguyên → dọn dẹp xác)
 *
 */
public abstract class Environment {

    // ----------------------------------------------------------------
    //  Định danh & Chỉ số môi trường
    // ----------------------------------------------------------------

    /** ID duy nhất của môi trường (dùng UUID hoặc tên cố định) */
    private final String id;

    /** Tên hiển thị của môi trường */
    private final String name;

    /**
     * Độ ẩm hiện tại [0.0 – 100.0].
     */
    protected float currentHumidity;
    protected float humidity; // base
    /**
     * Nhiệt độ hiện tại (độ C).
     */
    protected float currentTemp;
    protected float temperature; // base
    /**
     * Cường độ ánh sáng [0.0 – 1.0].
     * 0.0 = tối hoàn toàn (đêm), 1.0 = ban ngày nắng gắt.
     */
    protected float currentLight;
    protected float lightLevel;

    // ----------------------------------------------------------------
    //  5 Components
    // ----------------------------------------------------------------

    /** Component quản lý thời gian, mùa, thời tiết */
    protected final TimeComponent time;

    /** Component quản lý bản đồ địa hình và tính thông qua */
    protected final TerrainComponent terrain;

    /** Component quản lý danh sách sinh vật */
    protected final OrganismRegistry registry;

    /** Component quản lý tài nguyên (thức ăn, nước, vật cản) */
    protected final ResourceManager resources;

    // ----------------------------------------------------------------
    //  Constructor
    // ----------------------------------------------------------------

    /**
     * Constructor duy nhất — ép lớp con phải cung cấp đủ 5 component.
     *
     * @param id          ID duy nhất của môi trường
     * @param name        Tên hiển thị
     * @param humidity    Độ ẩm ban đầu [0, 100]
     * @param temperature Nhiệt độ ban đầu (°C)
     * @param lightLevel  Cường độ ánh sáng ban đầu [0, 1]
     * @param time        Component thời gian (không được null)
     * @param terrain     Component địa hình (không được null)
     * @param registry    Component sinh vật (không được null)
     * @param resources   Component tài nguyên (không được null)
     */
    protected Environment(String id,
                          String name,
                          float humidity,
                          float temperature,
                          float lightLevel,
                          TimeComponent time,
                          TerrainComponent terrain,
                          OrganismRegistry registry,
                          ResourceManager resources) {
        // Kiểm tra null để phát hiện lỗi cấu hình sớm
        if (time == null)      throw new IllegalArgumentException("TimeComponent không được null");
        if (terrain == null)   throw new IllegalArgumentException("TerrainComponent không được null");
        if (registry == null)  throw new IllegalArgumentException("OrganismRegistry không được null");
        if (resources == null) throw new IllegalArgumentException("ResourceManager không được null");

        this.id          = id;
        this.name        = name;
        this.humidity    = humidity;
        this.temperature = temperature;
        this.lightLevel  = lightLevel;
        this.time        = time;
        this.terrain     = terrain;
        this.registry    = registry;
        this.resources   = resources;
    }

    // ----------------------------------------------------------------
    //  Template Method — Luồng cập nhật chính (KHÔNG được ghi đè)
    // ----------------------------------------------------------------

    /**
     * Cập nhật toàn bộ trạng thái môi trường mỗi tick.
     *
     * Thứ tự thực thi được đóng gói cố định (final) để đảm bảo tính nhất quán:
     *
     *   1. Cập nhật thời gian (mùa, thời tiết)
     *   2. Cập nhật chỉ số theo ánh sáng ngày/đêm
     *   3. Áp dụng hiệu ứng mùa (lớp con implement)
     *   4. Áp dụng hiệu ứng thời tiết (lớp con implement)
     *   5. Tick toàn bộ sinh vật còn sống
     *   6. Xử lý sinh vật DEAD (chuyển thành thịt + xóa)
     *   7. Dọn dẹp tài nguyên hết hạn
     *
     * @param currentTick tick hiện tại của hệ thống
     */
    public void updateEnvironment(int currentTick) {
        // 1. Cập nhật thời gian
        time.advance(currentTick);

        // 2. Cập nhật cường độ ánh sáng theo ngày/đêm
        float dayLight = AppConfig.getFloat("environment.light.day");
        float nightLight = AppConfig.getFloat("environment.light.night");
        lightLevel = time.isDaytime() ? dayLight : nightLight;

        // Cập nhật âm thanh ngày/đêm
        if (time.isDaytime()) {
            SoundManager.playAmbiance("TIME", "Ambiance.wav");
        } else {
            SoundManager.playAmbiance("TIME", "NightAmbiance.wav");
        }

        // 3. Hiệu ứng mùa đặc trưng của từng môi trường
        applySeasonEffect();

        // 4. Hiệu ứng thời tiết đặc trưng của từng môi trường
        applyWeatherEffect();

        // Cập nhật âm thanh mùa/thời tiết
        if (time.getCurrentSeason() == wildlife.model.environment.enums.Season.DROUGHT) {
            SoundManager.playAmbiance("SEASON", "DryAmbiance.wav");
        } else if (time.getCurrentWeather() == wildlife.model.environment.enums.WeatherType.RAIN) {
            SoundManager.playAmbiance("SEASON", "RainingAmbiance.wav");
        } else {
            SoundManager.stopAmbiance("SEASON");
        }

        // 5. Tick toàn bộ sinh vật
        for (Organism o : registry.getAllAlive(Organism.class)) {
            o.updateOrganism(currentTick);
        }

        // 6. Xử lý xác sinh vật DEAD
        processDeadOrganisms(currentTick);

        // 7. Dọn dẹp tài nguyên hết hạn
        resources.removeExpiredFood(currentTick);
    }

    // ----------------------------------------------------------------
    //  Abstract Methods — Design Contract cho lớp con
    // ----------------------------------------------------------------
    /**
     *
     * Khoi tao môi trường
     */
    protected abstract void initialize();
    /**
     * Áp dụng tác động của mùa hiện tại lên chỉ số môi trường.
     */
    protected abstract void applySeasonEffect();

    /**
     * Áp dụng tác động của thời tiết hiện tại lên chỉ số môi trường.
     */
    protected abstract void applyWeatherEffect();


    // ----------------------------------------------------------------
    //  Phương thức tiện ích dùng chung (Concrete)
    // ----------------------------------------------------------------

    /**
     * Cập nhật các chỉ số vật lý dựa trên sự giao thoa của Thời gian, Mùa và Thời tiết.
     */
    private void updateClimateMetrics() {
        // --- 1. Ánh sáng ---
        // Ban ngày sáng (max), ban đêm mờ (max-0.6)
        this.lightLevel = time.isDaytime() ? this.lightLevel : (this.lightLevel - 0.6f);
    }

    /**
     * Kiểm tra một vị trí có hợp lệ để động vật di chuyển đến không.
     * Kết hợp kiểm tra địa hình và vật cản.
     *
     * @param pos     tọa độ đích
     * @param self     loài
     * @return true nếu có thể di chuyển đến
     */
    public boolean isPositionPassable(Vector2D pos, Animal self) {
        if (self == null) return false;
        // 1. Kiểm tra địa hình
        if (!terrain.isPassable(pos, self)) return false;

        // 2. Kiểm tra vật cản trên bề mặt
        List<ObstacleItem> obstacles = resources.getObstaclesNear(pos, 0.5f);
        if (!obstacles.isEmpty()) {
            // Kiểm tra từng vật cản tại vị trí này
            for (ObstacleItem obstacle : obstacles) {
                // --- XỬ LÝ ĐÁ (ROCK) ---
                if (obstacle.type() == ObstacleType.ROCK) {
                    return false;
                }
                // --- XỬ LÝ BỤI RẬM (BUSH) ---
                if (obstacle.type() == ObstacleType.BUSH) {
                    // Động vật ăn cỏ (Thỏ, Hươu): Dáng nhỏ gọn, lách vào bụi rậm để trốn
                    if (self instanceof Rabbit || self instanceof Deer) {
                        continue;
                    }

                    // Động vật ăn thịt và Người: To xác, vướng víu không chui qua được
                    if (self instanceof Wolf || self instanceof Tiger || self.getSpeciesName().equalsIgnoreCase("Hunter")) {
                        return false; // Bị chặn lại
                    }
                }
            }
        }

        return true; // Nếu qua được hết mọi bài test thì vị trí này hợp lệ để bước vào
    }

    /**
     * Trả về hệ số độ nhận diện của một vị trí trong môi trường.
     * Hiệu ứng được cộng dồn từ địa hình và vật cản (bụi rậm).
     */
    public float getVisibilityModifier(Vector2D pos) {
        // Hệ số cơ bản do địa hình quyết định.
        float modifier = terrain.getVisibility(pos);

        // Nếu có bụi rậm tại vị trí này thì giảm thêm 0.4.
        for (ObstacleItem obstacle : resources.getObstaclesNear(pos, 0.5f)) {
            if (obstacle.type() == ObstacleType.BUSH) {
                modifier -= 0.4f;
                break;
            }
        }

        if (!time.isDaytime()) modifier -= 0.3f;

        // Đảm bảo hệ số không âm.
        return Math.max(modifier, 0.0f);
    }

    /**
     * Xử lý các sinh vật đang ở trạng thái DEAD:
     * - Nếu là Động vật (Animal): Chuyển vị trí thành FoodItem (thịt).
     * - Dù là Động vật hay Thực vật: Xóa chúng khỏi registry.
     * Private — không lộ ra ngoài, không ghi đè được.
     */
    private void processDeadOrganisms(int currentTick) {
        List<String> toRemove = new ArrayList<>();

        for (Organism o : registry.getAll(Organism.class)) {
            if (o.getState() == OrganismState.DEAD) {
                if (o instanceof Animal) {
                    // Chuyển xác thành thịt
                    float nutrition = o.getStats().getNutritionalValue();
                    resources.convertDeadToMeat(o.getPosition(), nutrition);
                }
                toRemove.add(o.getId());
            }
        }

        // Xóa sau khi duyệt xong để tránh ConcurrentModificationException
        for (String id : toRemove) {
            registry.remove(id);
        }
    }

    /**
     * Đăng ký một sinh vật vào môi trường này.
     * Tự động gắn environment reference vào sinh vật (bindEnvironment).
     *
     * @param organism sinh vật cần thêm
     */
    public void addOrganism(Organism organism) {
        registry.add(organism);
        organism.bindEnvironment(this);
    }

    // ----------------------------------------------------------------
    //  Getters
    // ----------------------------------------------------------------

    public String getId()             { return id; }
    public String getName()           { return name; }
    public float getHumidity()        { return currentHumidity; }
    public float getTemperature()     { return currentTemp; }
    public float getLightLevel()      { return currentLight; }

    public TimeComponent getTime()                    { return time; }
    public TerrainComponent getTerrain()              { return terrain; }
    public OrganismRegistry getRegistry()             { return registry; }
    public ResourceManager getResources()             { return resources; }

    /**
     * Lấy danh sách RenderData của tất cả sinh vật trong môi trường này.
     * @return danh sách RenderData
     */
    public List<RenderData> getRenderSnapshot() {
        List<RenderData> list = new ArrayList<>();
        for (Organism o : registry.getAll(Organism.class)) {
            list.add(o.getRenderData());
        }
        return list;
    }

    @Override
    public String toString() {
        return String.format("[Environment | id=%s | name=%s | season=%s | weather=%s | organisms=%d]",
                id, name,
                time.getCurrentSeason(),
                time.getCurrentWeather(),
                registry.count());
    }
}
