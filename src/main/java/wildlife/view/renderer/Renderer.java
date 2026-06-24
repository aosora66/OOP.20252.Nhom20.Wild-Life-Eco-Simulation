package wildlife.view.renderer;

import wildlife.model.dto.RenderData;
import wildlife.model.environment.component.TerrainComponent;
import wildlife.model.environment.enums.TerrainType;
import wildlife.util.AppConfig;
import wildlife.view.renderer.utils.Camera;
import wildlife.view.renderer.utils.IndexedMap;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Nhận các sinh vật vào dưới dạng {@link RenderData}, gom nhóm theo loài để tăng hiệu quả batch theo texture, giảm lượt gọi {@link SpriteBatch}
 */
public class Renderer {

    private static volatile Renderer instance;

    private static final float DEFAULT_SPRITE_WIDTH  = 32f;
    private static final float DEFAULT_SPRITE_HEIGHT = 32f;
    private static final float TILE_SIZE = AppConfig.getFloat("environment.terrain.tileSize");
    private static final int   MAP_COLS  = 20;
    private static final int   MAP_ROWS  = 20;

    private static final Set<String> PREDATORS  = Set.of("Hunter", "Tiger", "Wolf");
    private static final Set<String> HERBIVORES = Set.of("Deer", "Elephant", "Fish", "Rabbit");

    private final SpriteBatch spriteBatch;
    private final TextureRegistry textureRegistry;
    private final AtlasTexture spriteAtlas;
    private final Camera camera;

    /** 0 = Basic , 1 = Sprite*/
    private final AtomicInteger renderMode = new AtomicInteger(0);

    private volatile TerrainComponent terrain;
    private volatile Set<Integer> regionFilter = null;

    /**
     * Các nhóm được lưu liên tục trên 1 mảng liên tục, kết hợp với HashMap để tăng tốc độ insert / iterate.
     * (Tham khảo kỹ hơn trong {@link IndexedMap})
     */
    private final IndexedMap<String, SpeciesGroup> background = new IndexedMap<>();
    private final IndexedMap<String, SpeciesGroup> midground = new IndexedMap<>();
    private final IndexedMap<String, SpeciesGroup> foreground = new IndexedMap<>();

    private boolean running = true;
    private final Semaphore framePending = new Semaphore(0);

    public Renderer(SpriteBatch spriteBatch, TextureRegistry textureRegistry, AtlasTexture spriteAtlas, Camera camera) {
        this.spriteBatch     = Objects.requireNonNull(spriteBatch, "spriteBatch");
        this.textureRegistry = Objects.requireNonNull(textureRegistry, "textureRegistry");
        this.spriteAtlas     = spriteAtlas;
        this.camera          = camera;
        instance = this;
    }
    /** Returns the most recently constructed Renderer, or {@code null} if none has been created yet. */
    public static Renderer getInstance() {
        return instance;
    }
    /** Called from the JavaFX thread to switch rendering mode. Thread-safe via AtomicInteger. */
    public void setRenderMode(int mode) {
        renderMode.set(mode);
    }

    /** Gắn TerrainComponent để renderer truy vấn địa hình khi vẽ tilemap. */
    public void setTerrain(TerrainComponent terrain) {
        this.terrain = terrain;
    }

    public void setRegionFilter(Set<Integer> filter) { this.regionFilter = filter; }
    public void clearRegionFilter() { this.regionFilter = null; }

    private void renderTerrain() {
        if (terrain == null || spriteAtlas == null) return;
        AtlasTexture.SubAtlas envAtlas = spriteAtlas.getEnvAtlas();
        if (envAtlas == null) return;

        int camLeft   = camera.getTopLeftX();
        int camTop    = camera.getTopLeftY();
        int camRight  = camera.getBotRightX();
        int camBottom = camera.getBotRightY();

        for (int row = 0; row < MAP_ROWS; row++) {
            for (int col = 0; col < MAP_COLS; col++) {
                float tileLeft   = col * TILE_SIZE;
                float tileTop    = row * TILE_SIZE;
                float tileRight  = tileLeft + TILE_SIZE;
                float tileBottom = tileTop  + TILE_SIZE;

                // Culling: bỏ qua ô nằm hoàn toàn ngoài tầm nhìn camera
                if (tileRight < camLeft || tileLeft > camRight ||
                    tileBottom < camTop || tileTop  > camBottom) continue;

                Set<Integer> filter = this.regionFilter;
                if (filter != null && !filter.contains(row * MAP_COLS + col)) continue;

                TerrainType type = terrain.getTerrainAtTile(col, row);
                if (!envAtlas.has(type.name())) continue;

                float[] uvs = envAtlas.getUVs(type.name());
                float cx = tileLeft + TILE_SIZE / 2f;
                float cy = tileTop  + TILE_SIZE / 2f;
                spriteBatch.draw(envAtlas, cx, cy, 0f, TILE_SIZE, TILE_SIZE,
                                 uvs[0], uvs[1], uvs[2], uvs[3]);
            }
        }
        spriteBatch.flush();
    }

    /**
     * Usage:
     * Chương trình goi method submit để đẩy sinh vật cần render vào hàng chờ render
     * <b>Sinh vật đẩy vào dưới dạng thông tin cô đọng sử dụng: {@link RenderData}</b>
     */
    public void submit(RenderData data) {
        if (!running) return;
        //culling: nếu nằm ngoài khung nhìn -> không cho vào renderQueue
        if  (   data.x - DEFAULT_SPRITE_WIDTH/2 > camera.getBotRightX() ||
                data.x + DEFAULT_SPRITE_WIDTH/2 < camera.getTopLeftX() ||
                data.y + DEFAULT_SPRITE_HEIGHT/2 < camera.getTopLeftY() ||
                data.y - DEFAULT_SPRITE_HEIGHT/2 > camera.getBotRightY()    ) {return;}

        switch(data.layer){
            case 1:
                synchronized (background) {
                    background.computeIfAbsent(data.speciesName, SpeciesGroup::new)
                            .addPosition(data.x, data.y, data.goWest);
                }
                break;
            case 2:
                synchronized (midground) {
                    midground.computeIfAbsent(data.speciesName, SpeciesGroup::new)
                            .addPosition(data.x, data.y, data.goWest);
                }
                break;
            case 3:
                synchronized (foreground) {
                    foreground.computeIfAbsent(data.speciesName, SpeciesGroup::new)
                            .addPosition(data.x, data.y, data.goWest);
                }
                break;
            default:
                break;
        }
    }

    public void renderMap(IndexedMap<String, SpeciesGroup> CurrentMap, int mode) {
        if(CurrentMap == null) return;
        List<SpeciesGroup> groupsToRender;
        synchronized (CurrentMap) {
            if (CurrentMap.isEmpty()) return;

            groupsToRender = new ArrayList<>();
            for (SpeciesGroup group : CurrentMap.values()) {
                if (group.positionBuffer.position() > 0) {
                    group.swapForRead(); // Đẩy dữ liệu từ position sang renderer, clear position để coreloop nạp vào tiếp
                    groupsToRender.add(group);
                }
            }
        }
        if (groupsToRender.isEmpty()) return;

        // Snapshot once so every flush() in this frame uses the same mode.

        for (SpeciesGroup group : groupsToRender) {
            FloatBuffer buf = group.renderBuffer; // đã flip

            if (mode == 1 && spriteAtlas != null && spriteAtlas.hasSprite(group.speciesName)) {
                // Sprite mode
                float[] uvs = spriteAtlas.getIdleUVs(group.speciesName);
                while (buf.hasRemaining()) {
                    spriteBatch.draw(spriteAtlas, buf.get(), buf.get(), buf.get(),
                                     DEFAULT_SPRITE_WIDTH, DEFAULT_SPRITE_HEIGHT,
                                     uvs[0], uvs[1], uvs[2], uvs[3]);
                }
            } else {
                // Basic mode (or species not in atlas): draw with solid-color texture + shape
                ITexture texture = textureRegistry.getTexture(group.speciesName);
                if (texture == null) {
                    buf.clear();
                    continue;
                }
                boolean isPredator  = PREDATORS.contains(group.speciesName);
                boolean isHerbivore = HERBIVORES.contains(group.speciesName);
                while (buf.hasRemaining()) {
                    float x = buf.get(), y = buf.get();
                    buf.get(); // goWest — không dùng trong basic mode
                    if (isPredator) {
                        spriteBatch.drawTriangleDown(texture, x, y, DEFAULT_SPRITE_WIDTH, DEFAULT_SPRITE_HEIGHT);
                    } else if (isHerbivore) {
                        spriteBatch.drawTriangleUp(texture, x, y, DEFAULT_SPRITE_WIDTH, DEFAULT_SPRITE_HEIGHT);
                    } else {
                        spriteBatch.draw(texture, x, y, 0f, DEFAULT_SPRITE_WIDTH, DEFAULT_SPRITE_HEIGHT);
                    }
                }
            }

            spriteBatch.flush();
            buf.clear(); // reset renderBuffer so it's ready for the next swapForRead() cycle
        }

    }

    public void renderAll(){
        int mode = renderMode.get();
        spriteBatch.setRenderMode(mode);
        spriteBatch.begin();
        renderTerrain();
        renderMap(background, mode);
        renderMap(midground, mode);
        renderMap(foreground, mode);
        spriteBatch.end();
    }

    /**
     * Báo hiệu cho LWJGL thread rằng 1 tick đã hoàn tất, sẵn sàng để render.
     * Được gọi bởi coreLoopThread sau khi submit() xong toàn bộ sinh vật trong tick.
     */
    public void commitFrame() {
        if (running) framePending.release();
    }

    /**
     * LWJGL thread gọi method này để chờ frame mới từ coreLoopThread.
     * Trả về false nếu timeout mà không có frame nào được commit (dùng để kiểm tra vòng lặp vẫn chạy).
     */
    public boolean awaitFrame(long timeoutMs) throws InterruptedException {
        if (!framePending.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS)) return false;
        framePending.drainPermits(); // bỏ qua các frame tích lũy nếu render chậm hơn tick
        return true;
    }

    public void stop() {
        this.running = false;
        framePending.release(); // mở khóa awaitFrame() nếu LWJGL thread đang chờ
    }
}
