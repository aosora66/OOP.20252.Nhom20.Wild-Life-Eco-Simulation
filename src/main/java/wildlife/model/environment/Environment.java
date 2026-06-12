package wildlife.model.environment;

import wildlife.model.dto.RenderData;
import wildlife.model.environment.component.OrganismRegistry;
import wildlife.model.environment.component.ResourceManager;
import wildlife.model.environment.component.TerrainComponent;
import wildlife.model.environment.component.TimeComponent;
import wildlife.model.environment.dto.ObstacleItem;
import wildlife.model.environment.enums.ObstacleType;
import wildlife.model.environment.enums.TerrainType;
import wildlife.model.environment.event.EnvironmentEventListener;
import wildlife.model.environment.event.EnvironmentEventPublisher;
import wildlife.model.organism.Organism;
import wildlife.model.organism.OrganismState;
import wildlife.model.organism.animal.Animal;
import wildlife.util.AppConfig;
import wildlife.util.Vector2D;
import wildlife.model.environment.enums.FoodType;
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
    protected float humidity;

    /**
     * Nhiệt độ hiện tại (độ C).
     */
    protected float temperature;

    /**
     * Cường độ ánh sáng [0.0 – 1.0].
     * 0.0 = tối hoàn toàn (đêm sâu), 1.0 = ban ngày nắng gắt.
     */
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

    /** Component phát sự kiện (Observer Pattern) */
    protected final EnvironmentEventPublisher events;

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
     * @param events      Component sự kiện (không được null)
     */
    protected Environment(String id,
                          String name,
                          float humidity,
                          float temperature,
                          float lightLevel,
                          TimeComponent time,
                          TerrainComponent terrain,
                          OrganismRegistry registry,
                          ResourceManager resources,
                          EnvironmentEventPublisher events) {
        // Kiểm tra null để phát hiện lỗi cấu hình sớm
        if (time == null)      throw new IllegalArgumentException("TimeComponent không được null");
        if (terrain == null)   throw new IllegalArgumentException("TerrainComponent không được null");
        if (registry == null)  throw new IllegalArgumentException("OrganismRegistry không được null");
        if (resources == null) throw new IllegalArgumentException("ResourceManager không được null");
        if (events == null)    throw new IllegalArgumentException("EnvironmentEventPublisher không được null");

        this.id          = id;
        this.name        = name;
        this.humidity    = humidity;
        this.temperature = temperature;
        this.lightLevel  = lightLevel;
        this.time        = time;
        this.terrain     = terrain;
        this.registry    = registry;
        this.resources   = resources;
        this.events      = events;
    }

    // ----------------------------------------------------------------
    //  Template Method — Luồng cập nhật chính (KHÔNG được ghi đè)
    // ----------------------------------------------------------------

    /**
     * Cập nhật toàn bộ trạng thái môi trường mỗi tick.
     *
     * Thứ tự thực thi được ĐÓng gói cố định (final) để đảm bảo tính nhất quán:
     *
     *   1. Cập nhật thời gian (mùa, thời tiết)
     *   2. Cập nhật chỉ số theo ánh sáng ngày/đêm
     *   3. Áp dụng hiệu ứng mùa (lớp con implement)
     *   4. Áp dụng hiệu ứng thời tiết (lớp con implement)
     *   5. Tick toàn bộ sinh vật còn sống
     *   6. Xử lý sinh vật DEAD (chuyển thành thịt + xóa)
     *   7. Sinh tài nguyên thiên nhiên (lớp con implement)
     *   8. Dọn dẹp tài nguyên hết hạn
     *
     * @param currentTick tick hiện tại của hệ thống
     */
    public void updateEnvironment(int currentTick) {
        // 1. Cập nhật thời gian
        time.advance(currentTick);

        // 2. Cập nhật cường độ ánh sáng theo ngày/đêm
        float dayLight = AppConfig.getFloat("environment.light.day");
        float nightLight = AppConfig.getFloat("environment.light.night");

        // Lưu chỉ số cũ trước khi tính toán mục tiêu mới
        float prevTemp = this.temperature;
        float prevHumid = this.humidity;
        float prevLight = this.lightLevel;

        // Đặt mục tiêu ánh sáng cơ bản
        this.lightLevel = time.isDaytime() ? dayLight : nightLight;

        // 3. Hiệu ứng mùa đặc trưng của từng môi trường
        applySeasonEffect();

        // 4. Hiệu ứng thời tiết đặc trưng của từng môi trường
        applyWeatherEffect();

        // Thực hiện nội suy tuyến tính (lerp) để làm mượt các chỉ số môi trường
        float targetTemp = this.temperature;
        float targetHumid = this.humidity;
        float targetLight = this.lightLevel;

        float lerpTemp = AppConfig.getFloat("environment.transition.temperature");
        float lerpHumid = AppConfig.getFloat("environment.transition.humidity");
        float lerpLight = AppConfig.getFloat("environment.transition.lightLevel");

        this.temperature = prevTemp + (targetTemp - prevTemp) * lerpTemp;
        this.humidity = prevHumid + (targetHumid - prevHumid) * lerpHumid;
        this.lightLevel = prevLight + (targetLight - prevLight) * lerpLight;

        // Gọi hook xử lý sau khi làm mượt
        postClimateUpdate();

        // 5. Tick toàn bộ sinh vật
        for (Organism o : registry.getAllAlive()) {
            o.updateOrganism(currentTick);
        }

        // 6. Xử lý xác sinh vật DEAD
        processDeadOrganisms(currentTick);

        // 7. Sinh tài nguyên tự nhiên theo đặc trưng môi trường
        generateNaturalResources();

        // 8. Dọn dẹp tài nguyên hết hạn
        resources.removeExpiredFood(currentTick);
    }

    // ----------------------------------------------------------------
    //  Abstract Methods — Design Contract cho lớp con
    // ----------------------------------------------------------------

    /**
     * Hook method cho phép lớp con thực hiện logic sau khi các chỉ số môi trường
     * đã được cập nhật và làm mượt (ví dụ: đóng băng hồ nước).
     */
    protected void postClimateUpdate() {
        // Mặc định không làm gì
    }

    /**
     * Áp dụng tác động của mùa hiện tại lên chỉ số môi trường.
     */
    protected abstract void applySeasonEffect();

    /**
     * Áp dụng tác động của thời tiết hiện tại lên chỉ số môi trường.
     */
    protected abstract void applyWeatherEffect();

    /**
     *
     * Sinh tài nguyên thiên nhiên đặc trưng của môi trường này.

     */
    protected abstract void generateNaturalResources();

    // ----------------------------------------------------------------
    //  Phương thức tiện ích dùng chung (Concrete)
    // ----------------------------------------------------------------


    /**
     * Kiểm tra một vị trí có hợp lệ để sinh vật di chuyển đến không.
     * Kết hợp kiểm tra địa hình và vật cản.
     *
     * @param pos     tọa độ đích
     * @param species tên loài (để TerrainComponent kiểm tra đặc thù loài)
     * @return true nếu có thể di chuyển đến
     */
    /**
     * Kiểm tra một vị trí có hợp lệ để sinh vật di chuyển đến không.
     * Kết hợp kiểm tra địa hình và vật cản tĩnh (Bụi rậm, đá).
     */
/**
     * Kiểm tra một vị trí có hợp lệ để sinh vật di chuyển đến không.
     * Kết hợp kiểm tra địa hình và vật cản tĩnh (Bụi rậm, đá).
     */
    public boolean isPositionPassable(Vector2D pos, String species) {
        // 1. Kiểm tra giới hạn địa hình gốc (Nước sâu, vách núi, Cá lên bờ...)
        // Phương thức này đã gọi xuống TerrainComponent mà chúng ta vừa hoàn thiện
        if (!terrain.isPassable(pos, species)) return false;

        // 2. Kiểm tra vật cản trên bề mặt
        List<ObstacleItem> obstacles = resources.getObstaclesNear(pos, 0.5f);
        if (!obstacles.isEmpty()) {
            
            if (species == null) return false;

            // Kiểm tra từng vật cản tại vị trí này
            for (ObstacleItem obstacle : obstacles) {
                ObstacleType type = obstacle.getType(); 

                // --- XỬ LÝ ĐÁ (ROCK) ---
                if (type == ObstacleType.ROCK) {
                    // Đá là vật thể rắn. Công tâm mà nói, loài nào cũng sẽ bị chặn lại ở đây.
                    return false; 
                }
                // --- XỬ LÝ CÂY CỔ THỤ (TREE) ---
                if (type == ObstacleType.TREE) {
                    // Cây cổ thụ có thân rất to và cứng. 
                    // Kể cả Voi cũng không dẫm qua được, mọi loài trên cạn đều phải đi vòng.
                    return false; 
                }

                // --- XỬ LÝ BỤI RẬM (BUSH) ---
                if (type == ObstacleType.BUSH) {
                    // Voi (Động vật đầu bảng): Dẫm nát và càn lướt qua bụi rậm dễ dàng
                    if (species.equalsIgnoreCase("Elephant")) {
                        continue; // Bỏ qua vật cản này, xét tiếp
                    }

                    // Động vật ăn cỏ (Thỏ, Hươu): Dáng nhỏ gọn, lách vào bụi rậm để trốn
                    if (species.equalsIgnoreCase("Rabbit") || species.equalsIgnoreCase("Hươu") || species.equalsIgnoreCase("Deer")) {
                        continue; 
                    }

                    // Động vật ăn thịt và Người: To xác, vướng víu không chui qua được
                    if (species.equalsIgnoreCase("Wolf") || species.equalsIgnoreCase("Tiger") || species.equalsIgnoreCase("Hunter")) {
                        return false; // Bị chặn lại
                    }
                }
            }
        }

        return true; // Nếu qua được hết mọi bài test thì vị trí này hợp lệ để bước vào
    }
    /**
     * Xử lý các sinh vật đang ở trạng thái DEAD:
     * - Nếu là Động vật (Animal): Chuyển vị trí thành FoodItem (thịt).
     * - Dù là Động vật hay Thực vật: Xóa chúng khỏi registry.* - Xóa chúng khỏi registry.
     * Private — không lộ ra ngoài, không ghi đè được.
     */
    private void processDeadOrganisms(int currentTick) {
        List<String> toRemove = new ArrayList<>();

        for (Organism o : registry.getAll()) {
            if (o.getState() == OrganismState.DEAD) {
                if (o instanceof Animal) {
                    // Chuyển xác thành thịt
                    float nutrition = o.getStats().getNutritionalValue();
                    resources.convertDeadToMeat(o.getPosition(), nutrition);
                    events.publish(EnvironmentEventPublisher.EVENT_ORGANISM_DIED);
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
     * Phát sự kiện ORGANISM_BORN để ViewLogic / SoundManager phản ứng.
     *
     * @param organism sinh vật cần thêm
     */
    public void addOrganism(Organism organism) {
        registry.add(organism);
        events.publish(EnvironmentEventPublisher.EVENT_ORGANISM_BORN);
    }

    /**
     * Đăng ký listener lắng nghe sự kiện của môi trường này.
     * ViewLogic hoặc SoundManager gọi phương thức này để đăng ký.
     *
     * @param listener listener cần đăng ký
     */
    public void addEventListener(EnvironmentEventListener listener) {
        events.addListener(listener);
    }

    /**
     * Lấy snapshot dữ liệu render của toàn bộ sinh vật còn sống trong môi trường.
     * Đây là ĐIỂM ViewLogic giao tiếp với Environment.
     *
     * @return danh sách RenderData để ViewLogic vẽ lên màn hình
     */
    public List<RenderData> getRenderSnapshot() {
        List<RenderData> snapshot = new ArrayList<>();
        for (Organism o : registry.getAll()) {
            snapshot.add(o.getRenderData());
        }
        return snapshot;
    }

    // ----------------------------------------------------------------
    //  Getters
    // ----------------------------------------------------------------

    public String getId()             { return id; }
    public String getName()           { return name; }
    public float getHumidity()        { return humidity; }
    public float getTemperature()     { return temperature; }
    public float getLightLevel()      { return lightLevel; }

    public TimeComponent getTime()                    { return time; }
    public TerrainComponent getTerrain()              { return terrain; }
    public OrganismRegistry getRegistry()             { return registry; }
    public ResourceManager getResources()             { return resources; }
    public EnvironmentEventPublisher getEvents()      { return events; }

    @Override
    public String toString() {
        return String.format("[Environment | id=%s | name=%s | season=%s | weather=%s | organisms=%d]",
                id, name,
                time.getCurrentSeason(),
                time.getCurrentWeather(),
                registry.count());
    }
    // ====================================================================
    //  NHÓM HÀM HỖ TRỢ SINH VẬT (GIÁC QUAN & CẢM NHẬN)
    // ====================================================================

    /**
     * Cấp danh sách sinh vật trong tầm nhìn.
     * * @param center Tọa độ của sinh vật đang nhìn (tâm)
     * @param radius Bán kính tầm nhìn
     * @return Danh sách các sinh vật đang sống nằm trong bán kính đó
     */
    public List<Organism> getOrganismsInVision(Vector2D center, float radius) {
        // Gọi thẳng hàm findNear đã được tối ưu trong OrganismRegistry
        return registry.findNear(center, radius);
    }

    /**
     * Cung cấp hướng/tọa độ đi tới nguồn nước gần nhất.
     * * @param currentPos Tọa độ hiện tại của động vật đang khát
     * @return Tọa độ của vùng nước gần nhất, hoặc null nếu không tìm thấy
     */


    /**
     * Giảm độ nhận diện khi sinh vật ở nơi trú ẩn (bụi rậm, rừng sâu...).
     * * @param pos Tọa độ sinh vật đang đứng
     * @return Tỉ lệ % độ lộ diện (1.0 = bình thường, < 1.0 = đang trốn)
     */
    public float getStealthModifier(Vector2D pos) {
        // 1. Kiểm tra lợi thế tàng hình từ bản thân loại địa hình (VD: Rừng rậm)
        float terrainModifier = terrain.getVisibilityModifier(pos);

        // 2. Kiểm tra xem ngay vị trí đó có vật cản tĩnh nào để nấp không (VD: Bụi rậm)
        // Dùng bán kính rất nhỏ (0.5f) để xác định sinh vật đang thực sự nấp sau vật cản
        if (!resources.getObstaclesNear(pos, 0.5f).isEmpty()) {
            // Nếu có bụi rậm, độ lộ diện giảm mạnh xuống 30% (0.3f)
            // Lấy giá trị nhỏ nhất giữa địa hình và vật cản để sinh vật có lợi thế nấp tốt nhất
            return Math.min(terrainModifier, 0.3f);
        }

        return terrainModifier;
    }
    // ====================================================================
    //  NHÓM HÀM THAO TÁC, ÂM THANH (TƯƠNG TÁC TỪ NGOÀI)
    // ====================================================================

    /**
     * Người chơi (hoặc hệ thống) đặt vật cản tĩnh vào bản đồ.
     * * @param pos Tọa độ đặt vật cản
     * @param obstacleType Loại vật cản (đá, bụi rậm...). Hiện tại hệ thống 
     * gom chung thành ObstacleItem, nhưng giữ tham số 
     * để dễ mở rộng hình ảnh/tính năng sau này.
     */
    public void placeObstacle(Vector2D pos, ObstacleType type) {
        // Ủy quyền cho ResourceManager thêm vật cản vào danh sách với đầy đủ 2 tham số
        resources.addObstacle(pos, type);
    }
    /**
     * Sinh ra thức ăn (Táo rụng, thả miếng thịt...) và phát âm thanh.
     * * @param pos Vị trí xuất hiện thức ăn
     * @param foodType Loại thức ăn (Táo, Thịt...)
     * @param nutrition Giá trị dinh dưỡng của thức ăn đó
     */
    public void spawnFood(Vector2D pos, String foodType, float nutrition) {
        FoodType type = FoodType.APPLE;
        if (foodType != null) {
            try {
                type = FoodType.valueOf(foodType.toUpperCase());
            } catch (IllegalArgumentException e) {
                // fallback to APPLE
            }
        }
        // Gọi hàm sinh thức ăn thủ công đã được định nghĩa trong ResourceManager
        resources.spawnFoodManual(pos, type, nutrition);

        // QUAN TRỌNG: Báo cáo sự kiện ra loa phường!
        // ViewLogic đang vểnh tai nghe, thấy sự kiện này sẽ tự động phát tiếng "bụp"
        events.publish(EnvironmentEventPublisher.EVENT_FOOD_SPAWNED);
    }
    /**
     * Trả về lượng tốc độ BỊ GIẢM (lượng x) của sinh vật tại một vị trí cụ thể.
     * Ủy quyền hoàn toàn cho TerrainComponent để xử lý logic địa hình.
     *
     * @param pos Tọa độ sinh vật đang đứng
     * @param animal Đối tượng sinh vật đang di chuyển
     * @return Lượng tốc độ bị trừ đi (0.0 = di chuyển bình thường)
     */
    public float getSpeedPenalty(Vector2D pos, Organism animal) {
        // Môi trường không tự tính mà chuyển "câu hỏi" này xuống cho bộ phận Địa hình
        return terrain.getSpeedPenalty(pos, animal);
    
}
}