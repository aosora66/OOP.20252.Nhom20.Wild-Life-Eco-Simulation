package wildlife.view.renderer.utils;

public class Camera {
    private final int[] center = new int[2];
    private int top_left_x, top_left_y, bot_right_x, bot_right_y;
    private int diagonal;

    public Camera(int x, int y, int diagonal){
        center[0] = x;
        center[1] = y;
        this.diagonal = diagonal;
        setBounds(this.diagonal, this.center);
    }

    private void setBounds(int diagonal, int[] center){
        this.top_left_x = center[0] - diagonal / 2;
        this.top_left_y = center[1] - diagonal / 2;
        this.bot_right_x = this.top_left_x + diagonal;
        this.bot_right_y = this.top_left_y + diagonal;
    }

    public synchronized void zoom(int n){
        // Giới hạn zoom: không cho diagonal quá nhỏ (phóng quá to) hoặc quá lớn
        int targetDiagonal = this.diagonal + n;
        if (targetDiagonal < 200 || targetDiagonal > 5000) {
            return;
        }
        this.diagonal = targetDiagonal;
        setBounds(this.diagonal, this.center);
    }

    public synchronized void pan(int deltaX, int deltaY){
        // Cho phép dịch chuyển tự do tâm camera
        center[0] += deltaX;
        center[1] += deltaY;
        setBounds(this.diagonal, this.center);
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
