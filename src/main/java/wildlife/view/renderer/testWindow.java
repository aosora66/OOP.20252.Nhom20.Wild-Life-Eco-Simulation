package wildlife.view.renderer;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;
import wildlife.model.dto.RenderData;
import wildlife.model.organism.OrganismState;

import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * A simple test window using GLFW and OpenGL to verify that the view.renderer
 * package is ready for basic rendering.
 *
 * This class implements mock versions of {@link ITexture} and {@link TextureRegistry}
 * to demonstrate and validate the rendering pipeline with mock data.
 */
public class testWindow {

    // Concrete implementation of ITexture for testing/rendering colorful mock pixels
    public static class MockTexture implements ITexture {
        private final int id;
        private final int width;
        private final int height;

        public MockTexture(int width, int height, byte r, byte g, byte b, byte a) {
            this.width = width;
            this.height = height;

            id = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, id);

            // Set texture wrapping and filtering parameters
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

            // Fill a buffer with the solid color
            ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
            for (int i = 0; i < width * height; i++) {
                buffer.put(r);
                buffer.put(g);
                buffer.put(b);
                buffer.put(a);
            }
            buffer.flip();

            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
            glBindTexture(GL_TEXTURE_2D, 0);
        }

        @Override
        public int getTextureId() {
            return id;
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public void bind() {
            glBindTexture(GL_TEXTURE_2D, id);
        }

        @Override
        public void unbind() {
            glBindTexture(GL_TEXTURE_2D, 0);
        }

        public void dispose() {
            glDeleteTextures(id);
        }
    }

    // Concrete implementation of TextureRegistry for the mock textures
    public static class MockTextureRegistry implements TextureRegistry {
        private final java.util.Map<String, ITexture> registry = new java.util.HashMap<>();

        public void register(String name, ITexture texture) {
            registry.put(name, texture);
        }

        @Override
        public ITexture getTexture(String name) {
            return registry.get(name);
        }

        @Override
        public boolean hasTexture(String name) {
            return registry.containsKey(name);
        }
    }

    public static void main(String[] args) {
        // Setup an error callback to print errors in System.err
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Configure GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // The window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // The window will be resizable
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        int width = 800;
        int height = 600;

        // Create the window
        long window = glfwCreateWindow(width, height, "Wild-Life Eco Simulation - Renderer Test", MemoryUtil.NULL, MemoryUtil.NULL);
        if (window == MemoryUtil.NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(win, true); // We will detect this in our rendering loop
            }
        });

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        // Enable v-sync
        glfwSwapInterval(1);

        // Make the window visible
        glfwShowWindow(window);

        // This line is critical for LWJGL's interoperation with GLFW's OpenGL context,
        // or any context that is managed externally. LWJGL detects the context that is
        // current in the current thread, creates the GLCapabilities instance and makes
        // the OpenGL bindings available for use.
        GL.createCapabilities();

        // Create mock textures (16x16 pixels with solid colors)
        // Red for Wolf, Green for Grass, Blue for Rabbit
        MockTexture wolfTexture = new MockTexture(16, 16, (byte) 255, (byte) 50, (byte) 50, (byte) 255);
        MockTexture grassTexture = new MockTexture(16, 16, (byte) 50, (byte) 200, (byte) 50, (byte) 255);
        MockTexture rabbitTexture = new MockTexture(16, 16, (byte) 100, (byte) 150, (byte) 255, (byte) 255);

        // Create registry and register our textures
        MockTextureRegistry textureRegistry = new MockTextureRegistry();
        textureRegistry.register("Wolf", wolfTexture);
        textureRegistry.register("Grass", grassTexture);
        textureRegistry.register("Rabbit", rabbitTexture);

        // Create SpriteBatch and Renderer
        SpriteBatch spriteBatch = new SpriteBatch(width, height);
        Renderer renderer = new Renderer(spriteBatch, textureRegistry);

        // Setup resize callback to adjust projection matrix
        glfwSetFramebufferSizeCallback(window, (win, w, h) -> {
            glViewport(0, 0, w, h);
            spriteBatch.resize(w, h);
        });

        // Set the clear color
        glClearColor(0.15f, 0.15f, 0.15f, 1.0f);

        System.out.println("Renderer initialized and running successfully! Press ESC to exit.");

        // Game/Simulation Render Loop
        while (!glfwWindowShouldClose(window)) {
            // Clear the framebuffer
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // Submit Mock RenderData for demo purposes
            // 1. A stationary Grass cluster in the center
            renderer.submit(new RenderData("g1", "Grass", 400, 300, OrganismState.ALIVE));
            renderer.submit(new RenderData("g2", "Grass", 432, 300, OrganismState.ALIVE));
            renderer.submit(new RenderData("g3", "Grass", 400, 332, OrganismState.ALIVE));

            // 2. A moving Wolf circling the center
            double time = glfwGetTime();
            float wolfX = 400f + (float) Math.cos(time) * 150f;
            float wolfY = 300f + (float) Math.sin(time) * 150f;
            renderer.submit(new RenderData("w1", "Wolf", wolfX, wolfY, OrganismState.ALIVE));

            // 3. A bouncing Rabbit
            float rabbitX = 200f + (float) (time * 50 % 400);
            float rabbitY = 150f + (float) Math.abs(Math.sin(time * 3)) * 80f;
            renderer.submit(new RenderData("r1", "Rabbit", rabbitX, rabbitY, OrganismState.ALIVE));

            // Draw all submitted organisms
            renderer.renderAll();

            // Swap buffers and poll for window events
            glfwSwapBuffers(window);
            glfwPollEvents();
        }

        // Clean up resources
        renderer.stop();
        spriteBatch.dispose();
        wolfTexture.dispose();
        grassTexture.dispose();
        rabbitTexture.dispose();

        // Destroy window and terminate GLFW
        glfwDestroyWindow(window);
        glfwTerminate();

        // Free error callback
        java.util.Objects.requireNonNull(glfwSetErrorCallback(null)).free();
    }
}
