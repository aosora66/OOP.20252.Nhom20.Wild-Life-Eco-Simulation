package wildlife.view.renderer;

import org.lwjgl.BufferUtils;
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
    private final Camera camera;
    /** 0 = Basic (solid color), 1 = Sprite (textured). Written by the JavaFX thread, read by the render thread. */
    private final AtomicInteger renderMode = new AtomicInteger(0);

    /**
     * Gom tọa độ của các thực thể cùng loài vào 1 nhóm để vẽ cùng nhau, giảm số lượt texture-bind.
     * Dùng double-buffer: positionBuffer (submit thread ghi) và renderBuffer (render thread đọc)
     * được hoán đổi nguyên tử bên trong lock — loại bỏ hoàn toàn float[] per-entity.
     */
    private static final class SpeciesGroup {
        private static final int INITIAL_CAPACITY = 2048; // đủ cho 1024 vị trí (x,y) không cần resize

        final String speciesName;
        FloatBuffer positionBuffer; // ghi bởi submit()
        FloatBuffer renderBuffer;   // đọc bởi renderAll()

        SpeciesGroup(String speciesName) {
            this.speciesName    = speciesName;
            this.positionBuffer = BufferUtils.createFloatBuffer(INITIAL_CAPACITY);
            this.renderBuffer   = BufferUtils.createFloatBuffer(INITIAL_CAPACITY);
        }

        void addPosition(float x, float y) {
            if (positionBuffer.remaining() < 2) {
                FloatBuffer bigger = BufferUtils.createFloatBuffer(positionBuffer.capacity() * 2);
                positionBuffer.flip();
                bigger.put(positionBuffer);
                positionBuffer = bigger;
            }
            positionBuffer.put(x).put(y);
        }

        /**
         * Phải gọi bên trong lock renderQueue.
         * Flip positionBuffer thành ready-to-read, hoán đổi sang renderBuffer,
         * rồi clear buffer cũ (nay là positionBuffer) để frame sau có thể ghi ngay.
         */
        void swapForRead() {
            positionBuffer.flip();
            FloatBuffer tmp = renderBuffer;
            renderBuffer    = positionBuffer;
            positionBuffer  = tmp;
            positionBuffer.clear();
        }
    }

    /**
     * Các nhóm được lưu liên tục trên 1 mảng liên tục, kết hợp với HashMap để tăng tốc độ insert / iterate.
     * (Tham khảo kỹ hơn trong {@link IndexedMap})
     */
    private final IndexedMap<String, SpeciesGroup> renderQueue = new IndexedMap<>();

    private boolean running = true;
    private final Semaphore framePending = new Semaphore(0);

    public Renderer(SpriteBatch spriteBatch, TextureRegistry textureRegistry, Camera camera) {
        this.spriteBatch     = Objects.requireNonNull(spriteBatch, "spriteBatch");
        this.textureRegistry = Objects.requireNonNull(textureRegistry, "textureRegistry");
        this.camera = camera;
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
            //culling: nếu nằm ngoài khung nhìn -> không cho vào renderQueue
            if  (
                    data.x - DEFAULT_SPRITE_WIDTH/2 > camera.getBotRightX() ||
                    data.x + DEFAULT_SPRITE_WIDTH/2 < camera.getTopLeftX() ||
                    data.y + DEFAULT_SPRITE_HEIGHT/2 < camera.getTopLeftY() ||
                    data.y - DEFAULT_SPRITE_HEIGHT/2 > camera.getBotRightY()
                )
            {
                return;
            }
            renderQueue.computeIfAbsent(data.speciesName, SpeciesGroup::new)
                       .addPosition(data.x, data.y);
        }
    }

    public void renderAll() {
        List<SpeciesGroup> groupsToRender;
        synchronized (renderQueue) {
            if (renderQueue.isEmpty()) return;

            groupsToRender = new ArrayList<>();
            for (SpeciesGroup group : renderQueue.values()) {
                if (group.positionBuffer.position() > 0) {
                    group.swapForRead(); // flip write→read, clear write — all under the lock
                    groupsToRender.add(group);
                }
            }
        }
        if (groupsToRender.isEmpty()) return;

        // Snapshot once so every flush() in this frame uses the same mode.
        spriteBatch.setRenderMode(renderMode.get());
        spriteBatch.begin();

        for (SpeciesGroup group : groupsToRender) {
            ITexture texture = textureRegistry.getTexture(group.speciesName);
            if (texture == null) {
                group.renderBuffer.clear();
                continue;
            }

            FloatBuffer buf = group.renderBuffer; // already flipped, ready to read
            while (buf.hasRemaining()) {
                spriteBatch.draw(texture, buf.get(), buf.get(), DEFAULT_SPRITE_WIDTH, DEFAULT_SPRITE_HEIGHT);
            }
            spriteBatch.flush();
            buf.clear(); // reset renderBuffer so it's ready for the next swapForRead() cycle
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
