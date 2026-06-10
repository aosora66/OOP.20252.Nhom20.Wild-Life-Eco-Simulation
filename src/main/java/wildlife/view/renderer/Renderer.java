package wildlife.view.renderer;

import wildlife.model.dto.RenderData;
import wildlife.view.renderer.utils.IndexedMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Nhận các sinh vật vào dưới dạng {@link RenderData}, gom nhóm theo loài để tăng hiệu quả batch theo texture, giảm lượt gọi {@link SpriteBatch}
 */
public class Renderer {

    private static final float DEFAULT_SPRITE_WIDTH  = 32f;
    private static final float DEFAULT_SPRITE_HEIGHT = 32f;

    private static final int BATCH_FLUSH_LIMIT = 512;

    private final SpriteBatch spriteBatch;
    private final TextureRegistry textureRegistry;

    private record SpeciesGroup(String speciesName, List<float[]> positions) {}


    /**
     * Gom tọa độ của các thực thể cùng loài vào 1 nhóm {@link SpeciesGroup} để vẽ cùng nhau, giảm số lượt texture-bind
     * Các nhóm này được lưu liên tục kế tiếp trên 1 mảng liên tục, kết hợp với HashMap để tăng tốc độ insert / iterate
     * (Tham khảo kỹ hơn trong {@link IndexedMap})
     */
    private final IndexedMap<String, SpeciesGroup> renderQueue = new IndexedMap<>();

    private boolean running = true;

    public Renderer(SpriteBatch spriteBatch, TextureRegistry textureRegistry) {
        this.spriteBatch     = Objects.requireNonNull(spriteBatch, "spriteBatch");
        this.textureRegistry = Objects.requireNonNull(textureRegistry, "textureRegistry");
    }

    /**
     * Usage:
     * Chương trình goi method submit để đẩy sinh vật cần render vào hàng chờ render
     * <b>Sinh vật đẩy vào dưới dạng thông tin cô đọng sử dụng: {@link RenderData}</b>
     */
    public void submit(RenderData data) {
        if (!running) return;
        renderQueue.computeIfAbsent(data.speciesName, k -> new SpeciesGroup(k, new ArrayList<>())).positions().add(new float[]{data.x, data.y});
    }

    public void renderAll() {
        if (renderQueue.isEmpty()) {
            return;
        }

        spriteBatch.begin();

        int processed = 0;

        for (SpeciesGroup group : renderQueue.values()) {
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
                processed++;
            }

            if (processed >= BATCH_FLUSH_LIMIT) {
                spriteBatch.end();
                spriteBatch.begin();
                processed = 0;
            }
        }

        spriteBatch.end();
        renderQueue.clear();
    }

    /**
     * Ngừng nhận thêm vào hàng đợi
     */
    public void stop() {
        this.running = false;
    }
    public boolean isRunning() {
        return running;
    }
    public int getQueueSize() {
        return renderQueue.size();
    }
}
