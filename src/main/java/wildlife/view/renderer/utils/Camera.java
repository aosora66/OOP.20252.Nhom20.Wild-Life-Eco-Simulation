package wildlife.view.renderer.utils;

import wildlife.model.environment.Environment;
import wildlife.util.AppConfig;

public class Camera {
    private final int[] center = new int[2];
    private int top_left_x, top_left_y, bot_right_x, bot_right_y;
    private int width, height;

    public Camera(int x, int y, int width) {
        center[0] = x;
        center[1] = y;
        this.width = width;
        this.height = (int)(width/16*9);
        setBounds();
    }
    public Camera(int width){
        this.width = (int) (width/16) * 16;
        //lam tron thanh boi so cua 16
        this.height = (int)(width/16*9);
        center[0] = width/2;
        center[1] = height/2;
        setBounds();
    }

    private void setBounds(){
        this.top_left_x = center[0] - width / 2;
        this.top_left_y = center[1] - height / 2;
        this.bot_right_x = this.top_left_x + width;
        this.bot_right_y = this.top_left_y +  height;
    }

    public synchronized void zoom(int n){
        int targetWidth = (int)((this.width + n)/16) * 16;
        if(targetWidth < 200) {
            return;
        }
        if(targetWidth > 16*100) {
            targetWidth = 16*100;
        }

        this.width = targetWidth;
        this.height = targetWidth /16 * 9;
        setBounds();
    }

    public synchronized void pan(int deltaX, int deltaY){
        center[0] += deltaX;
        if (width >= 1600) {
            center[0] = 800;
        } else if(center[0] - width/2 < 0){
            center[0] = width/2;
        }else if(center[0] + width/2 > 1600){
            center[0] = 1600-width/2;
        }
        center[1] += deltaY;
        if (height >= 1600) {
            center[1] = 800;
        } else if(center[1] - height/2 < 0){
            center[1] = height/2;
        }else if(center[1] + height/2 > 1600){
            center[1] = 1600 - height / 2;
        }
        setBounds();
    }

    public synchronized int getTopLeftX() {
        return top_left_x;
    }

    public synchronized int getTopLeftY() {
        return top_left_y;
    }

    public synchronized int getBotRightX() {
        return bot_right_x;
    }

    public synchronized int getBotRightY() {
        return bot_right_y;
    }
}
