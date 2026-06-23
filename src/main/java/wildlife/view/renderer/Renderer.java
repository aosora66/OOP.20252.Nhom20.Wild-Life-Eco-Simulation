package wildlife.view.renderer;

import wildlife.model.dto.RenderData;
import wildlife.view.renderer.utils.Camera;
import wildlife.view.renderer.utils.IndexedMap;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

    private final SpriteBatch spriteBatch;
    private final TextureRegistry textureRegistry;
    private final AtlasTexture spriteAtlas;
    private final Camera camera;

    /** 0 = Basic , 1 = Sprite*/
    private final AtomicInteger renderMode = new AtomicInteger(0);

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
                // Basic mode (or species not in atlas): draw with solid-color texture
                ITexture texture = textureRegistry.getTexture(group.speciesName);
                if (texture == null) {
                    buf.clear();
                    continue;
                }
                while (buf.hasRemaining()) {
                    spriteBatch.draw(texture, buf.get(), buf.get(), buf.get(), DEFAULT_SPRITE_WIDTH, DEFAULT_SPRITE_HEIGHT);
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
