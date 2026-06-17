package wildlife.core;
import wildlife.model.dto.RenderData;
import wildlife.model.environment.CompositeMap;
import wildlife.model.organism.animal.canivores.Wolf;
import wildlife.util.MapLoader;
import wildlife.view.ApplicationFrame;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

import wildlife.model.organism.Organism;
import wildlife.util.AppConfig;
import wildlife.view.renderer.Renderer;

public class Main {
    public static void main(String[] args) {


        // ==========================================
        // PHẦN 1: INIT MODEL (KHỞI TẠO DỮ LIỆU LÕI)
        // ==========================================
        System.out.println("Đang nạp dữ liệu thế giới...");

        // Khởi tạo và cắt lớp bản đồ từ file map.txt
        CompositeMap world = MapLoader.loadMapFromFile(
                "world_01",
                "Hệ Sinh Thái Nhiệt Đới",
                "config/map.txt"
        );


        //Kiểm tra và in log xác nhận
        System.out.println("✅ Khởi tạo Model hoàn tất!");
        System.out.println("   - Số phân khu môi trường: " + world.getSubEnvironments().size());
        System.out.println("   - Tổng lượng thực/động vật ban đầu: " + world.getTotalOrganismCount());
        System.out.println("--------------------------------------------------");


        // 1.gameLoop
        Thread coreLoopThread = new Thread(() -> {
            System.out.println("Khởi động vòng lặp sinh thái...");

            Renderer renderer = ApplicationFrame.getRendererInstance();

            final int TICK_RATE = 60;
            final long MS_PER_SECOND = 1000 / TICK_RATE;
            int currentTick = 0;

            while(true) {
                currentTick++;
                long startTime = System.currentTimeMillis();

                world.updateEnvironment(currentTick);
                if (renderer != null) {
                    for(RenderData o: world.getAllRenderSnapshots()){
                        renderer.submit(o);
                    }
                    renderer.commitFrame();
                }
                var timeInfo = world.getTime();
                if(currentTick % (TICK_RATE) == 0) {
                    System.out.printf("[Tick %d] Sinh vật: %d | Thời tiết: %s | Mùa: %s\n",
                            currentTick,
                            world.getTotalOrganismCount(),
                            timeInfo.getCurrentWeather(),
                            timeInfo.getCurrentSeason()
                    );
                }

                long elapsedTime = System.currentTimeMillis() - startTime;
                long sleepTime = MS_PER_SECOND - elapsedTime;
                if(sleepTime > 0) {
                    try{
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }, "Ecosystem-Core-loop");
        // wildlife.view.ui.UIEventController.setActiveOrganisms(ecosystem);
        coreLoopThread.setDaemon(true);
        coreLoopThread.start();
        // Chạy JavaFX Application. Phương thức này sẽ chặn cho đến khi cửa sổ đóng.
        ApplicationFrame.launch(ApplicationFrame.class, args);
    }
}