package wildlife.view.renderer;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class SpriteBatch {
    private static final int MAX_SPRITES = 1000;
    private static final int FLOATS_PER_VERTEX = 8;
    private static final int VERTICES_PER_SPRITE = 4;
    private static final int INDICES_PER_SPRITE = 6;
    private static final int BUFFER_CAPACITY = MAX_SPRITES * VERTICES_PER_SPRITE * FLOATS_PER_VERTEX;
    private final int vaoId;
    private final int vboId;
    private final int iboId;

    // VBO
    private final float[] vertices = new float[BUFFER_CAPACITY];
    private int vertexIndex = 0;
    private int spriteCount = 0;
    private ITexture currentTexture = null;

    /** Được gán thành true ở giữa 2 lần gọi lệnh {@link #begin()} và {@link #end()}. */
    private boolean drawing = false;

    private final FloatBuffer uploadBuffer = BufferUtils.createFloatBuffer(BUFFER_CAPACITY);

    private final ShaderProgram shader;
    private final Matrix4f projection = new Matrix4f();

    private float colorR = 1f;
    private float colorG = 1f;
    private float colorB = 1f;
    private float colorA = 1f;

    private int currentMode = 0;

    public SpriteBatch(int screenWidth, int screenHeight) {
        shader = new ShaderProgram();
        // Orthographic projection: đặt điểm gốc ở top-left, trục y hướng xuống dưới
        projection.ortho2D(0f, screenWidth, screenHeight, 0f);

        // VAO
        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        // VBO
        vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, (long) BUFFER_CAPACITY * Float.BYTES, GL_DYNAMIC_DRAW);

        // IBO
        iboId = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, iboId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, generateIndices(), GL_STATIC_DRAW);

        /*layout tổng của 1 vertex, cứ quét liên tục {@link stride} bytes là đủ dữ liệu cho 1 vertex hoàn chỉnh */
        int stride = FLOATS_PER_VERTEX * Float.BYTES;

        // metadata của 1 vertex
        //  vertex[0..1]: attribute position
        glVertexAttribPointer(0, 2, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);

        // vertex[2..3]: tọa độ texture attribute
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 2L * Float.BYTES);
        glEnableVertexAttribArray(1);

        // vertex[4..7]: attribute color
        glVertexAttribPointer(2, 4, GL_FLOAT, false, stride, 4L * Float.BYTES);
        glEnableVertexAttribArray(2);

        // Unbind to prevent accidental mutation.
        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    public void begin() {
        if (drawing) {
            throw new IllegalStateException("SpriteBatch.end() must be called before begin().");
        }
        drawing = true;

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        shader.bind();
        shader.setUniformMat4("uProjection", projection);
        shader.setUniform1i("uTexture", 0);    // texture unit 0
    }

    public void draw(ITexture texture, float x, float y, float width, float height) {
        draw(texture, x, y, width, height, 0f, 0f, 1f, 1f);
    }

    public void draw(ITexture texture, float x, float y, float width, float height,
                     float u0, float v0, float u1, float v1) {
        if (!drawing) {
            throw new IllegalStateException("SpriteBatch.begin() must be called before draw().");
        }

        if (texture != currentTexture) {
            flush();
            currentTexture = texture;
            glActiveTexture(GL_TEXTURE0);
            currentTexture.bind();
        }
        if (spriteCount >= MAX_SPRITES) {
            flush();
        }

        // ── Emit 4 vertices (top-left, top-right, bottom-right, bottom-left) ──
        // đặt x, y làm tâm của quad
        float top = y - height/2;
        float bottom = y + height/2;
        float right = x + width/2;
        float left = x - width/2;

        putVertex(left,  top,  u0, v0);
        putVertex(right, top,  u1, v0);
        putVertex(right, bottom, u1, v1);
        putVertex(left, bottom, u0, v1);

        spriteCount++;
    }

    public void end() {
        if (!drawing) {
            throw new IllegalStateException("SpriteBatch.begin() must be called before end().");
        }
        flush();
        drawing = false;

        if (currentTexture != null) {
            currentTexture.unbind();
            currentTexture = null;
        }

        shader.unbind();
        glDisable(GL_BLEND);
    }

    public void setRenderMode(int mode) {
        this.currentMode = mode;
    }

    public void setColor(float r, float g, float b, float a) {
        this.colorR = r;
        this.colorG = g;
        this.colorB = b;
        this.colorA = a;
    }

    /** Reset tint về màu trắng */
    public void resetColor() {
        setColor(1f, 1f, 1f, 1f);
    }

    public void resize(int screenWidth, int screenHeight) {
        projection.identity().ortho2D(0f, screenWidth, screenHeight, 0f);
    }

    public void updateProjection(wildlife.view.renderer.utils.Camera camera) {
        projection.identity().ortho2D(
            camera.getTopLeftX(),
            camera.getBotRightX(),
            camera.getBotRightY(),
            camera.getTopLeftY()
        );
    }

    public void dispose() {
        shader.dispose();
        glDeleteBuffers(vboId);
        glDeleteBuffers(iboId);
        glDeleteVertexArrays(vaoId);
    }

    /** gắn vertex mới vào VBO */
    private void putVertex(float x, float y, float u, float v) {
        vertices[vertexIndex++] = x;
        vertices[vertexIndex++] = y;
        vertices[vertexIndex++] = u;
        vertices[vertexIndex++] = v;
        vertices[vertexIndex++] = colorR;
        vertices[vertexIndex++] = colorG;
        vertices[vertexIndex++] = colorB;
        vertices[vertexIndex++] = colorA;
    }

    public void flush() {
        if (spriteCount == 0) {
            return;
        }

        // đưa phần đã ghi của VBO hiện tại vào uploadBuffer
        uploadBuffer.clear();
        uploadBuffer.put(vertices, 0, vertexIndex).flip();

        glBindVertexArray(vaoId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferSubData(GL_ARRAY_BUFFER, 0, uploadBuffer);

        shader.setMode(currentMode);
        // Vẽ tất cả các TỨ GIÁC đã đưa vào hàng đợi theo dạng ghép tam giác (1 tứ giác = 2 tam giác)
        glDrawElements(GL_TRIANGLES, spriteCount * INDICES_PER_SPRITE, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);

        // Reset counters.
        vertexIndex = 0;
        spriteCount = 0;
    }


    /** 1 sprites thứ n được bao trong khung vertex[n..n+{@value VERTICES_PER_SPRITE}],
     * Ở đây chúng ta cần phải vẽ quad theo tam giác, nên trong IBO, cứ 6 index liên tiếp là đủ để ghép 2 tam giác thành 1 quad */
    private static IntBuffer generateIndices() {
        IntBuffer indices = BufferUtils.createIntBuffer(MAX_SPRITES * INDICES_PER_SPRITE);
        for (int i = 0; i < MAX_SPRITES; i++) {
            int offset = i * VERTICES_PER_SPRITE;
            indices.put(offset);
            indices.put(offset + 1);
            indices.put(offset + 2);
            indices.put(offset + 2);
            indices.put(offset + 3);
            indices.put(offset);
        }
        indices.flip();
        return indices;
    }
}
