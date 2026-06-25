package wildlife.view.renderer;

import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

/**
 * Quản lý nhiều atlas texture:
 * - Sprite atlas (creatures/plants): hardcoded UVs, backward-compatible API
 * - Env atlas    (terrain tiles)   : UVs từ env_atlas_coordinates.json
 * - Resources atlas (food items)   : UVs từ resources_atlas_coordinates.json
 *
 * Mỗi sub-atlas là một OpenGL texture riêng biệt.
 * Dùng {@link #loadEnvAtlas} / {@link #loadResourcesAtlas} để nạp các atlas phụ.
 */
public class AtlasTexture implements ITexture {

    // -------------------------------------------------------------------------
    //  Sprite atlas — UVs hardcoded (creatures / plants), atlas.png 64×384
    // -------------------------------------------------------------------------
    private static final float ATLAS_W = 64f;
    private static final float ATLAS_H = 384f;

    private static final Map<String, float[]> IDLE_UVS = Map.ofEntries(
        Map.entry("Hunter",      spriteUV(0,   0)),
        Map.entry("Tiger",       spriteUV(0,  32)),
        Map.entry("Wolf",        spriteUV(0,  64)),
        Map.entry("Deer",        spriteUV(0,  96)),
        Map.entry("Elephant",    spriteUV(0, 128)),
        Map.entry("Fish",        spriteUV(0, 160)),
        Map.entry("Rabbit",      spriteUV(0, 192)),
        Map.entry("AppleTree",   spriteUV(0, 224)),
        Map.entry("Grass",       spriteUV(0, 256)),
        Map.entry("TreeForest",  spriteUV(0, 288)),
        Map.entry("ElephantEating", spriteUV(0, 320)),
        Map.entry("RabbitEating",   spriteUV(0, 352))
    );

    private static float[] spriteUV(int x, int y) {
        return new float[]{ x / ATLAS_W, y / ATLAS_H, (x + 32f) / ATLAS_W, (y + 32f) / ATLAS_H };
    }

    // -------------------------------------------------------------------------
    //  SubAtlas — một texture OpenGL + bảng UV chuẩn hóa
    // -------------------------------------------------------------------------
    public static final class SubAtlas implements ITexture {
        private final int id;
        private final int width, height;
        private final Map<String, float[]> uvMap;   // key → [u0, v0, u1, v1]

        SubAtlas(int id, int width, int height, Map<String, float[]> uvMap) {
            this.id = id; this.width = width; this.height = height; this.uvMap = uvMap;
        }

        /** [u0, v0, u1, v1] chuẩn hóa cho key, hoặc null nếu không tồn tại. */
        public float[] getUVs(String key) { return uvMap.get(key); }
        public boolean has(String key)    { return uvMap.containsKey(key); }
        public void dispose()             { glDeleteTextures(id); }

        @Override public int  getTextureId() { return id; }
        @Override public int  getWidth()     { return width; }
        @Override public int  getHeight()    { return height; }
        @Override public void bind()         { glBindTexture(GL_TEXTURE_2D, id); }
        @Override public void unbind()       { glBindTexture(GL_TEXTURE_2D, 0); }
    }

    // -------------------------------------------------------------------------
    //  Fields
    // -------------------------------------------------------------------------
    private final SubAtlas spriteAtlas;      // creature/plant atlas (luôn có)
    private SubAtlas envAtlas;               // terrain atlas — null cho đến khi loadEnvAtlas()
    private SubAtlas resourcesAtlas;         // resource atlas — null cho đến khi loadResourcesAtlas()

    // -------------------------------------------------------------------------
    //  Constructor
    // -------------------------------------------------------------------------
    public AtlasTexture(InputStream png) throws IOException {
        spriteAtlas = buildSubAtlas(png, IDLE_UVS);
    }

    // -------------------------------------------------------------------------
    //  Loaders — gọi trên LWJGL thread
    // -------------------------------------------------------------------------

    /**
     * Tải atlas địa hình từ PNG + JSON tọa độ.
     * Nếu {@code png} là null (chưa có file), bỏ qua.
     */
    public void loadEnvAtlas(InputStream png, InputStream coordJson) throws IOException {
        if (png == null || coordJson == null) return;
        envAtlas = buildSubAtlas(png, parseObjectJson(coordJson));
    }

    /**
     * Tải atlas tài nguyên từ PNG + JSON tọa độ.
     * Nếu {@code png} là null (chưa có file), bỏ qua.
     */
    public void loadResourcesAtlas(InputStream png, InputStream coordJson) throws IOException {
        if (png == null || coordJson == null) return;
        resourcesAtlas = buildSubAtlas(png, parseObjectJson(coordJson));
    }

    // -------------------------------------------------------------------------
    //  Public API — sprite atlas (backward-compatible)
    // -------------------------------------------------------------------------
    /** [u0, v0, u1, v1] cho idle sprite của loài, hoặc null. */
    public float[] getIdleUVs(String speciesName) { return IDLE_UVS.get(speciesName); }
    public boolean hasSprite(String speciesName)   { return IDLE_UVS.containsKey(speciesName); }

    // -------------------------------------------------------------------------
    //  Public API — sub-atlases
    // -------------------------------------------------------------------------
    /** Sub-atlas địa hình — null nếu chưa load (env_atlas.png chưa có). */
    public SubAtlas getEnvAtlas()       { return envAtlas; }
    /** Sub-atlas tài nguyên — null nếu chưa load. */
    public SubAtlas getResourcesAtlas() { return resourcesAtlas; }

    // -------------------------------------------------------------------------
    //  Dispose
    // -------------------------------------------------------------------------
    public void dispose() {
        spriteAtlas.dispose();
        if (envAtlas       != null) envAtlas.dispose();
        if (resourcesAtlas != null) resourcesAtlas.dispose();
    }

    // -------------------------------------------------------------------------
    //  ITexture — delegate đến spriteAtlas (backward-compatible)
    // -------------------------------------------------------------------------
    @Override public int  getTextureId() { return spriteAtlas.getTextureId(); }
    @Override public int  getWidth()     { return spriteAtlas.getWidth(); }
    @Override public int  getHeight()    { return spriteAtlas.getHeight(); }
    @Override public void bind()         { spriteAtlas.bind(); }
    @Override public void unbind()       { spriteAtlas.unbind(); }

    // -------------------------------------------------------------------------
    //  Private helpers
    // -------------------------------------------------------------------------

    private static SubAtlas buildSubAtlas(InputStream png, Map<String, float[]> uvMap) throws IOException {
        BufferedImage img = ImageIO.read(png);
        int w = img.getWidth(), h = img.getHeight();

        int[] pixels = new int[w * h];
        img.getRGB(0, 0, w, h, pixels, 0, w);

        ByteBuffer buf = BufferUtils.createByteBuffer(w * h * 4);
        for (int px : pixels) {
            buf.put((byte) ((px >> 16) & 0xFF));
            buf.put((byte) ((px >>  8) & 0xFF));
            buf.put((byte) ( px        & 0xFF));
            buf.put((byte) ((px >> 24) & 0xFF));
        }
        buf.flip();

        int id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);
        glBindTexture(GL_TEXTURE_2D, 0);

        return new SubAtlas(id, w, h, uvMap);
    }

    /**
     * Parse JSON dạng "object" atlas:
     * <pre>
     * {
     *   "meta": { "size": { "w": W, "h": H } },
     *   "object": {
     *     "KEY": { "x": X, "y": Y, "w": W2, "h": H2 },
     *     ...
     *   }
     * }
     * </pre>
     * Trả về: KEY → [u0, v0, u1, v1] đã chuẩn hóa theo kích thước atlas.
     */
    private static Map<String, float[]> parseObjectJson(InputStream json) throws IOException {
        String text = new String(json.readAllBytes(), StandardCharsets.UTF_8);
        Map<String, float[]> result = new HashMap<>();

        // 1. Lấy kích thước atlas từ meta.size
        Pattern sizePattern = Pattern.compile(
            "\"size\"\\s*:\\s*\\{[^}]*\"w\"\\s*:\\s*(\\d+)[^}]*\"h\"\\s*:\\s*(\\d+)");
        Matcher sizeMatcher = sizePattern.matcher(text);
        if (!sizeMatcher.find()) throw new IOException("Atlas JSON thiếu meta.size");
        float atlasW = Float.parseFloat(sizeMatcher.group(1));
        float atlasH = Float.parseFloat(sizeMatcher.group(2));

        // 2. Tìm nội dung bên trong khối "object": { ... }
        int objIdx   = text.indexOf("\"object\"");
        if (objIdx < 0) return result;
        int openBrace = text.indexOf('{', objIdx);
        if (openBrace < 0) return result;
        // innerSection bắt đầu ngay sau '{' mở của "object"
        String innerSection = text.substring(openBrace + 1);

        // 3. Match từng entry: "KEY": { "x": X, "y": Y, "w": W, "h": H }
        Pattern entryPattern = Pattern.compile(
            "\"(\\w+)\"\\s*:\\s*\\{[^}]*\"x\"\\s*:\\s*(\\d+)[^}]*\"y\"\\s*:\\s*(\\d+)" +
            "[^}]*\"w\"\\s*:\\s*(\\d+)[^}]*\"h\"\\s*:\\s*(\\d+)[^}]*\\}",
            Pattern.DOTALL
        );
        Matcher m = entryPattern.matcher(innerSection);
        while (m.find()) {
            String key = m.group(1);
            float x = Float.parseFloat(m.group(2));
            float y = Float.parseFloat(m.group(3));
            float w = Float.parseFloat(m.group(4));
            float h = Float.parseFloat(m.group(5));
            result.put(key, new float[]{ x / atlasW, y / atlasH, (x + w) / atlasW, (y + h) / atlasH });
        }
        return result;
    }
}
