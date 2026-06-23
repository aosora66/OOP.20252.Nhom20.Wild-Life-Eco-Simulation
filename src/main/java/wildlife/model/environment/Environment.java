package wildlife.model.environment;

import wildlife.model.dto.RenderData;
import wildlife.model.environment.component.OrganismRegistry;
import wildlife.model.environment.component.ResourceManager;
import wildlife.model.environment.component.TerrainComponent;
import wildlife.model.environment.component.TimeComponent;
import wildlife.model.environment.dto.ObstacleItem;
import wildlife.model.environment.enums.ObstacleType;
import wildlife.model.environment.enums.TerrainType;
import wildlife.model.organism.Organism;
import wildlife.model.organism.OrganismState;
import wildlife.model.organism.animal.Animal;
import wildlife.model.organism.animal.canivores.Tiger;
import wildlife.model.organism.animal.canivores.Wolf;
import wildlife.model.organism.animal.hebivores.Deer;
import wildlife.model.organism.animal.hebivores.Elephant;
import wildlife.model.organism.animal.hebivores.Fish;
import wildlife.model.organism.animal.hebivores.Rabbit;
import wildlife.model.organism.component.AdaptabilityComponent;
import wildlife.model.organism.component.GrowthComponent;
import wildlife.model.organism.component.SurvivalStatsComponent;
import wildlife.model.organism.plant.AppleTree;
import wildlife.model.organism.plant.Plant;
import wildlife.model.organism.plant.TreeForest;
import wildlife.util.AppConfig;
import wildlife.util.ValueRange;
import wildlife.util.Vector2D;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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

    /** Bán kính chặn quanh gốc cây lớn (TreeForest/AppleTree) khi động vật di chuyển. */
    private static final float PLANT_BLOCK_RADIUS = 6.0f;
    /** Bán kính thân Voi dùng như vật cản sinh học. */
    private static final float ELEPHANT_BLOCK_RADIUS = AppConfig.getFloat("animal.elephant.blockRadius");

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

    /** Thống kê tử vong tích lũy: species → [killed, oldAge, starvation, dehydration, other] */
    private final Map<String, int[]> deathTally = new HashMap<>();
    /** Thống kê sinh nở tích lũy: species → số con sinh ra */
    private final Map<String, Integer> birthTally = new HashMap<>();

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
        this.currentHumidity = humidity;
        this.currentTemp     = temperature;
        this.currentLight    = lightLevel;
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
     * @param currentTick tick hiện tại của hệ thống
     */
    public void updateEnvironment(int currentTick) {
        // 1. Cập nhật thời gian
        time.advance(currentTick);

        // 2. Hiệu ứng mùa đặc trưng của từng môi trường
        applySeasonEffect();

        // 3. Hiệu ứng thời tiết đặc trưng của từng môi trường
        applyWeatherEffect();

        // 4. Cập nhật theo ngày/đêm
        if (!time.isDaytime()) {
            this.currentTemp = this.currentTemp - 5.0f;
            this.currentHumidity = clamp(this.currentHumidity + 15.0f, 0.0f, 100.0f);
            this.currentLight = clamp(this.lightLevel - 0.6f, 0.0f, 1.0f);
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

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }


    // ----------------------------------------------------------------
    //  Phương thức tiện ích dùng chung (Concrete)
    // ----------------------------------------------------------------

    /**
     * Hàm Generic khởi tạo động vật
     * @param animalClass Lớp của động vật (VD: Rabbit.class, Wolf.class)
     * @param count Số lượng cần sinh
     * @param <T> Kiểu dữ liệu phải kế thừa từ Animal
     */
    protected <T extends Animal> void spawnAnimals(Class<T> animalClass, int count) {
        Random random = new Random();
        String species = animalClass.getSimpleName().toLowerCase();

        for (int i = 0; i < count; i++) {
            Vector2D randomPos = terrain.getRandomValidPosition();
            try {
                // 1. LẤY THÔNG SỐ CƠ BẢN TỪ CONFIG
                float baseHp = AppConfig.getFloat("animal." + species + ".maxHp");
                float baseNutrition = AppConfig.getFloat("animal." + species + ".nutrition");
                float hungerDecay = AppConfig.getFloat("animal." + species + ".hungerDecay");
                float thirstDecay = AppConfig.getFloat("animal." + species + ".thirstDecay");

                float baseAge = AppConfig.getFloat("animal." + species + ".maxAge");
                float baseSize = AppConfig.getFloat("animal." + species + ".maxSize");

                // 2. RANDOM DAO ĐỘNG ±15% (Nhân với khoảng 0.85 -> 1.15)
                float randomFactorHp = 0.85f + (random.nextFloat() * 0.3f);
                float randomFactorAge = 0.85f + (random.nextFloat() * 0.3f);
                float randomFactorSize = 0.85f + (random.nextFloat() * 0.3f);

                float finalHp = baseHp * randomFactorHp;
                float finalMaxAge = baseAge * randomFactorAge;
                float finalMaxSize = baseSize * randomFactorSize;

                // 3. KHỞI TẠO CÁC COMPONENT
                // Rải tuổi ban đầu (0..60% đời) để quần thể có cả con non và con trưởng thành.
                float startAge = random.nextFloat() * finalMaxAge * 0.6f;
                GrowthComponent growth = new GrowthComponent(finalMaxAge, finalMaxSize, 0.2f, 0.7f, startAge);
                SurvivalStatsComponent stats = new SurvivalStatsComponent(finalHp, baseNutrition, hungerDecay, thirstDecay);

                // Setup Adaptability: Phân loại môi trường sống cho Cá và Thú trên cạn
                List<TerrainType> survivableTerrains = species.equals("fish")
                        ? List.of(TerrainType.DEEP_WATER)
                        : List.of(TerrainType.GRASSLAND, TerrainType.FOREST, TerrainType.MUD);

                // LƯU Ý SEMANTIC: lethalLimit là VÙNG nhiệt độ GÂY CHẾT (isLethal = lethalLimit.contains(temp)),
                // KHÔNG phải "biên sống sót". Ngoài vùng tolerance đã tự chết qua !canTolerate rồi.
                // Để lethalLimit là vùng cực lạnh tách rời tolerance, tránh lỗi giết sạch ở nhiệt độ thường.
                AdaptabilityComponent adapt = new AdaptabilityComponent(
                        survivableTerrains,
                        new ValueRange(15f, 35f),   // Tối ưu (bao trùm Hồ 22°, Rừng 24°, Đồng cỏ 33°)
                        new ValueRange(0f, 45f),    // Chịu đựng được
                        new ValueRange(-60f, -10f)  // Vùng cực lạnh = chết (tách rời tolerance)
                );

                // 4. KHỞI TẠO ĐỘNG VẬT BẰNG REFLECTION
                String id = animalClass.getSimpleName().toUpperCase() + "_" + System.nanoTime();

                T animal = animalClass.getDeclaredConstructor(
                        String.class, String.class, Vector2D.class, Environment.class,
                        GrowthComponent.class, SurvivalStatsComponent.class, AdaptabilityComponent.class
                ).newInstance(id, animalClass.getSimpleName(), randomPos, this, growth, stats, adapt);

                // 5. THÊM VÀO MÔI TRƯỜNG
                registry.add(animal);

            } catch (Exception e) {
                System.err.println("Lỗi khởi tạo! Có thể bạn quên thêm key cấu hình cho loài: " + species);
                e.printStackTrace();
            }
        }
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

        // 3. Vật cản sinh học: thân cây lớn và Voi chặn đường mọi loài.
        //    Cỏ (Grass) nhỏ nên KHÔNG chặn. Bán kính nhỏ — chỉ chặn ngay tại thân.
        for (Organism o : registry.findNear(pos, PLANT_BLOCK_RADIUS, Organism.class)) {
            if (o instanceof TreeForest || o instanceof AppleTree) {
                return false;
            }
        }
        for (Elephant elephant : registry.findNear(pos, ELEPHANT_BLOCK_RADIUS, Elephant.class)) {
            if (!elephant.getId().equals(self.getId())) {
                return false;
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
                    float nutrition = o.getStats().getNutritionalValue();
                    resources.convertDeadToMeat(o.getPosition(), nutrition);
                }
                toRemove.add(o.getId());
            }
        }

        for (String id : toRemove) {
            registry.remove(id);
        }
    }

    public void recordDeath(String species, Organism.DeathCause cause) {
        int[] tally = deathTally.computeIfAbsent(species, k -> new int[5]);
        switch (cause) {
            case KILLED      -> tally[0]++;
            case OLD_AGE     -> tally[1]++;
            case STARVATION  -> tally[2]++;
            case DEHYDRATION -> tally[3]++;
            default          -> tally[4]++;
        }
    }

    public void recordBirth(String species) {
        birthTally.merge(species, 1, Integer::sum);
    }

    public Map<String, int[]> getDeathTally() { return deathTally; }
    public Map<String, Integer> getBirthTally() { return birthTally; }

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

    @Override
    public String toString() {
        return String.format("[Environment | id=%s | name=%s | season=%s | weather=%s | organisms=%d]",
                id, name,
                time.getCurrentSeason(),
                time.getCurrentWeather(),
                registry.count());
    }

    public Collection<? extends RenderData> getRenderSnapshot() {
        List<RenderData> list = new ArrayList<>();
        for(Organism o: registry.getAll(Organism.class)) {
            int layer;
            if(o instanceof Plant){
                layer = 2;
            }else {
                if (o instanceof Fish) {
                    layer = 1;
                } else {
                    layer = 3;
                }
            }
            list.add(new RenderData(o, layer));
        }
        return list;
    }
}
