package wildlife.view.renderer;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

/**
 * Gom tọa độ của các thực thể cùng loài vào 1 nhóm để vẽ cùng nhau, giảm số lượt texture-bind.
 * Dùng double-buffer: positionBuffer (submit thread ghi) và renderBuffer (render thread đọc)
 * được hoán đổi nguyên tử bên trong lock — loại bỏ hoàn toàn float[] per-entity.
 */
public final class SpeciesGroup {
    private static final int INITIAL_CAPACITY = 3072; // 1024 vị trí (x,y)

    public final String speciesName;
    public FloatBuffer positionBuffer; // ghi bởi submit()
    public FloatBuffer renderBuffer;   // đọc bởi renderMap()

    public SpeciesGroup(String speciesName) {
        this.speciesName = speciesName;
        this.positionBuffer = BufferUtils.createFloatBuffer(INITIAL_CAPACITY);
        this.renderBuffer = BufferUtils.createFloatBuffer(INITIAL_CAPACITY);
    }

    public void addPosition(float x, float y, boolean goWest) {
        if (positionBuffer.remaining() <= 0) {
            return;
        }
        positionBuffer.put(x).put(y).put(goWest?1.0f:0.0f);
    }

    /**
     * Phải gọi bên trong lock renderQueue.
     * Flip positionBuffer thành ready-to-read, hoán đổi sang renderBuffer,
     * rồi clear buffer cũ (nay là positionBuffer) để frame sau có thể ghi ngay.
     */
    public void swapForRead() {
        positionBuffer.flip();
        FloatBuffer tmp = renderBuffer;
        renderBuffer = positionBuffer;
        positionBuffer = tmp;
        positionBuffer.clear();
    }
}
