package wildlife.view.renderer;

import java.awt.Canvas;

public class renderer {
    private final Canvas canvas;
    private boolean running = true;

    public renderer(Canvas canvas) {
        this.canvas = canvas;
    }

    public void run() {
        // Khoi tao LWJGL
        // GL.createCapabilities();

        // Render loop
        while (running) {
            // link voi system tick sau
            // kiem tra canvas co hien thi & co du 2 chieu khong gian khong
            if (canvas.isDisplayable() && canvas.getWidth() > 0 && canvas.getHeight() > 0) {
                render();
            }
        }
        
        cleanup();
    }

    private void render() {
        // render procedure trong moi loop


    }
    
    private void cleanup() {

    }
    
    public void stop() {
        this.running = false;
    }
}