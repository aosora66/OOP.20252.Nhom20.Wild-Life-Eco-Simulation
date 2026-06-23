package wildlife.view.renderer;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;


/* Stolen src code cuz i don't really understand a god damn thing about this
*  It uses GLSL anyway, don't expect that i can understand it*/
/**
 * Self-contained shader program for 2D sprite rendering.
 * Compiles and links an embedded vertex/fragment shader pair.
 * No external file I/O — sources are inlined as constants.
 */

public class ShaderProgram {
    //  Embedded GLSL sources (OpenGL 3.3 core profile)
    /** Vertex shader: transforms position and forwards UV + color to the fragment stage. */
    private static final String VERTEX_SOURCE = """
            #version 330 core
            layout (location = 0) in vec2 aPos;
            layout (location = 1) in vec2 aTexCoord;
            layout (location = 2) in vec4 aColor;

            uniform mat4 uProjection;

            out vec2 fTexCoord;
            out vec4 fColor;

            void main() {
                fTexCoord   = aTexCoord;
                fColor      = aColor;
                gl_Position = uProjection * vec4(aPos, 0.0, 1.0);
            }
            """;

    /**
     * Fragment shader with two rendering modes selected by {@code uMode}:
     *   0 – Basic: samples the bound solid-color texture (SimpleTexture stores the species color).
     *   1 – Sprite: samples the bound atlas texture and tints it by the vertex color.
     */
    private static final String FRAGMENT_SOURCE = """
            #version 330 core
            in vec2 fTexCoord;
            in vec4 fColor;

            uniform sampler2D uTexture;
            uniform int       uMode;

            out vec4 FragColor;

            void main() {
                if (uMode == 0) {
                    FragColor = texture(uTexture, fTexCoord);
                } else {
                    FragColor = texture(uTexture, fTexCoord) * fColor;
                }
            }
            """;

    private final int programId;
    private final int vertexShaderId;
    private final int fragmentShaderId;

    /** Reusable buffer for uploading 4×4 matrices. */
    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

    /** Cache for uniform locations to avoid repeated GPU queries. */
    private final Map<String, Integer> uniformCache = new HashMap<>();

    // ──────────────────────────────────────────────────────────────
    //  Lifecycle
    // ──────────────────────────────────────────────────────────────

    public ShaderProgram() {
        vertexShaderId   = compileShader(GL_VERTEX_SHADER,   VERTEX_SOURCE);
        fragmentShaderId = compileShader(GL_FRAGMENT_SHADER, FRAGMENT_SOURCE);
        programId        = linkProgram(vertexShaderId, fragmentShaderId);
    }

    /** Activate this program for subsequent draw calls. */
    public void bind() {
        glUseProgram(programId);
    }

    /** Deactivate any shader program. */
    public void unbind() {
        glUseProgram(0);
    }

    /** Release all GPU resources held by this program. */
    public void dispose() {
        glDetachShader(programId, vertexShaderId);
        glDetachShader(programId, fragmentShaderId);
        glDeleteShader(vertexShaderId);
        glDeleteShader(fragmentShaderId);
        glDeleteProgram(programId);
    }

    // ──────────────────────────────────────────────────────────────
    //  Uniform setters
    // ──────────────────────────────────────────────────────────────

    /**
     * Upload a 4×4 projection matrix.
     *
     * @param name   uniform name in the shader source (e.g. "uProjection")
     * @param matrix the projection matrix to upload
     */
    public void setUniformMat4(String name, Matrix4f matrix) {
        int location = getUniformLocation(name);
        matrix.get(matrixBuffer);
        glUniformMatrix4fv(location, false, matrixBuffer);
    }

    /**
     * Upload a single int uniform (used for texture sampler units).
     *
     * @param name  uniform name
     * @param value integer value
     */
    public void setUniform1i(String name, int value) {
        int location = getUniformLocation(name);
        glUniform1i(location, value);
    }

    /**
     * Select the active rendering mode via the {@code uMode} uniform.
     *
     * @param mode 0 = Basic (solid color), 1 = Sprite (textured + tint)
     */
    public void setMode(int mode) {
        setUniform1i("uMode", mode);
    }

    // ──────────────────────────────────────────────────────────────
    //  Uniform location cache
    // ──────────────────────────────────────────────────────────────

    /**
     * Look up the uniform location for {@code name}, caching the result
     * so that subsequent calls avoid the GPU round-trip.
     */
    private int getUniformLocation(String name) {
        return uniformCache.computeIfAbsent(name,
                n -> glGetUniformLocation(programId, n));
    }

    // ──────────────────────────────────────────────────────────────
    //  Internal helpers
    // ──────────────────────────────────────────────────────────────

    /** Compile a single shader stage and return its OpenGL name. */
    private static int compileShader(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);

        if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0) {
            String log = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            throw new RuntimeException("Shader compilation failed:\n" + log);
        }
        return shader;
    }

    /** Link vertex + fragment shaders into a program and return its OpenGL name. */
    private static int linkProgram(int vertexShader, int fragmentShader) {
        int program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        glLinkProgram(program);

        if (glGetProgrami(program, GL_LINK_STATUS) == 0) {
            String log = glGetProgramInfoLog(program);
            glDeleteProgram(program);
            throw new RuntimeException("Shader link failed:\n" + log);
        }
        return program;
    }
}
