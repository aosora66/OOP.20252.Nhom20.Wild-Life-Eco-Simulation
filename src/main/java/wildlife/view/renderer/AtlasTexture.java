package wildlife.view.renderer;

import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

/**
 * OpenGL texture wrapping the creature sprite atlas (atlas.png).
 * UV coordinates are hardcoded to match atlas_coordinates.json (64×224 px, 32×32 per sprite).
 * Left column (x=0): idle sprites. Right column (x=32): dead sprites.
 */
public class AtlasTexture implements ITexture {

    private static final float ATLAS_W = 64f;
    private static final float ATLAS_H = 320f;

    // [u0, v0, u1, v1] normalized idle UV per species, matching atlas_coordinates.json
    private static final Map<String, float[]> IDLE_UVS = Map.of(
        "Hunter",       uv(0,   0),
        "Tiger",        uv(0,  32),
        "Wolf",         uv(0,  64),
        "Deer",         uv(0,  96),
        "Elephant",     uv(0, 128),
        "Fish",         uv(0, 160),
        "Rabbit",       uv(0, 192),
        "AppleTree",    uv(0, 224),
        "Grass",        uv(0, 256),
        "TreeForest",  uv(0, 288)
    );

    private static float[] uv(int x, int y) {
        return new float[]{ x / ATLAS_W, y / ATLAS_H, (x + 32f) / ATLAS_W, (y + 32f) / ATLAS_H };
    }

    private final int id;
    private final int width;
    private final int height;

    public AtlasTexture(InputStream png) throws IOException {
        BufferedImage img = ImageIO.read(png);
        width  = img.getWidth();
        height = img.getHeight();

        int[] pixels = new int[width * height];
        img.getRGB(0, 0, width, height, pixels, 0, width);

        ByteBuffer buf = BufferUtils.createByteBuffer(width * height * 4);
        for (int px : pixels) {
            buf.put((byte) ((px >> 16) & 0xFF)); // R
            buf.put((byte) ((px >>  8) & 0xFF)); // G
            buf.put((byte) ( px        & 0xFF)); // B
            buf.put((byte) ((px >> 24) & 0xFF)); // A
        }
        buf.flip();

        id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    /** Returns normalized [u0, v0, u1, v1] for the idle sprite of the given species, or null if not in atlas. */
    public float[] getIdleUVs(String speciesName) {
        return IDLE_UVS.get(speciesName);
    }

    public boolean hasSprite(String speciesName) {
        return IDLE_UVS.containsKey(speciesName);
    }

    public void dispose() {
        glDeleteTextures(id);
    }

    @Override public int getTextureId() { return id; }
    @Override public int getWidth()     { return width; }
    @Override public int getHeight()    { return height; }
    @Override public void bind()        { glBindTexture(GL_TEXTURE_2D, id); }
    @Override public void unbind()      { glBindTexture(GL_TEXTURE_2D, 0); }
}