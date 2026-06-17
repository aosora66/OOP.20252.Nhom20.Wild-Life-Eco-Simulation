package wildlife.view.renderer;

import wildlife.model.dto.RenderData;
import wildlife.view.renderer.utils.IndexedMap;

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

    /** 0 = Basic (solid color), 1 = Sprite (textured). Written by the JavaFX thread, read by the render thread. */
    private final AtomicInteger renderMode = new AtomicInteger(0);

    private record SpeciesGroup(String speciesName, List<float[]> positions) {}


    /**
     * Gom tọa độ của các thực thể cùng loài vào 1 nhóm {@link SpeciesGroup} để vẽ cùng nhau, giảm số lượt texture-bind
     * Các nhóm này được lưu liên tục kế tiếp trên 1 mảng liên tục, kết hợp với HashMap để tăng tốc độ insert / iterate
     * (Tham khảo kỹ hơn trong {@link IndexedMap})
     */
    private final IndexedMap<String, SpeciesGroup> renderQueue = new IndexedMap<>();

    private boolean running = true;
    private final Semaphore framePending = new Semaphore(0);

    public Renderer(SpriteBatch spriteBatch, TextureRegistry textureRegistry) {
        this.spriteBatch     = Objects.requireNonNull(spriteBatch, "spriteBatch");
        this.textureRegistry = Objects.requireNonNull(textureRegistry, "textureRegistry");
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
        synchronized (renderQueue) {
            renderQueue.computeIfAbsent(data.speciesName, k -> new SpeciesGroup(k, new ArrayList<>())).positions().add(new float[]{data.x, data.y});
        }
    }

    public void renderAll() {
        List<SpeciesGroup> groupsToRender;
        synchronized (renderQueue) {
            if (renderQueue.isEmpty()) {
                return;
            }
            groupsToRender = new ArrayList<>(renderQueue.values());
            renderQueue.clear();
        }

        // Snapshot once so every flush() in this frame uses the same mode.
        spriteBatch.setRenderMode(renderMode.get());
        spriteBatch.begin();

        for (SpeciesGroup group : groupsToRender) {
            ITexture texture = textureRegistry.getTexture(group.speciesName());
            if (texture == null) {
                continue;
            }

            for (float[] pos : group.positions()) {
                spriteBatch.draw(
                        texture,
                        pos[0],
                        pos[1],
                        DEFAULT_SPRITE_WIDTH,
                        DEFAULT_SPRITE_HEIGHT
                );
            }
            spriteBatch.flush();
        }

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
