package wildlife.core;
import wildlife.model.dto.RenderData;
import wildlife.model.organism.animal.carnivores.Wolf;
import wildlife.view.ApplicationFrame;

import java.util.ArrayList;
import java.util.List;
import wildlife.model.organism.Organism;
import wildlife.util.AppConfig;
import wildlife.view.renderer.Renderer;

public class Main {

    public static void main(String[] args) {
        System.out.println("--- BẮT ĐẦU KHỞI TẠO HỆ SINH THÁI ---");

        // BƯỚC 1: ĐỌC CẤU HÌNH HỆ THỐNG
        int mapWidth = AppConfig.getInt("map.width");
        int mapHeight = AppConfig.getInt("map.height");
        int initRabbits = AppConfig.getInt("init.rabbit.count");
        int initWolves = AppConfig.getInt("init.wolf.count");
        int initGrass = AppConfig.getInt("init.grass.count");

        // BƯỚC 2: DỰNG MÔI TRƯỜNG (ENVIRONMENT)
        // TODO: Khởi tạo đối tượng quản lý bản đồ tổng hợp
        // EnvironmentManager mapManager = new EnvironmentManager(mapWidth, mapHeight);

        // TODO: Thêm các phân vùng (Đồng cỏ, Rừng rậm, Hồ nước) vào mapManager
        // mapManager.addZone(new Grassland(...));
        // mapManager.addZone(new Forest(...));
        // mapManager.addZone(new Lake(...));

        // BƯỚC 3: KHOI TAO SINH VAT
        List<Organism> ecosystem = new ArrayList<>();

        // 3.1 Khởi tạo Thực vật (Cỏ)
        for (int i = 0; i < initGrass; i++) {
            // TODO: Lấy tọa độ random hợp lệ trên bản đồ
            // Vector2D pos = mapManager.getRandomPositionInZone(EnvironmentType.GRASSLAND);

            // TODO: Tạo instance Cỏ và add vào ecosystem
            // ecosystem.add(new Co("Grass_" + i, pos, ...));
        }

        // 3.2 Khởi tạo Động vật ăn cỏ (Thỏ)
        for (int i = 0; i < initRabbits; i++) {
            // TODO: Lấy tọa độ random hợp lệ
            // Vector2D pos = mapManager.getRandomValidPosition();

            // TODO: Tạo instance Thỏ và add vào ecosystem
            // ecosystem.add(new Tho("Tho_" + i, pos, ...));
        }

        // 3.3 Khởi tạo Động vật ăn thịt (Sói)
        for (int i = 0; i < initWolves; i++) {
            // TODO: Lấy tọa độ random hợp lệ
            // Vector2D pos = mapManager.getRandomValidPosition();

            // TODO: Tạo instance Sói và add vào ecosystem
            // ecosystem.add(new Soi("Soi_" + i, pos, ...));
        }
        System.out.println("--- KHỞI TẠO HOÀN TẤT! ---");
        System.out.println("Tổng số sinh vật: " + ecosystem.size());


        // BƯỚC 4: KHỞI TẠO VÀ CHẠY GIAO DIỆN (VIEW)
            // Khởi tạo 2 luồng song song: JavaFX và một deamon thread để chạy coreloop
        // deamon thread chạy coreloop
        Thread coreLoopThread = new Thread(() -> {
            System.out.println("[Core Loop] Đang chờ giao diện hiển thị");
            Renderer renderer = ApplicationFrame.getRendererInstance();

            final int TICK_RATE = 64;
            final long MS_PER_SECOND = 1000 / TICK_RATE;

            while(true) {
                long startTime = System.currentTimeMillis();

                runSimulationTick(renderer);

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
            // todo: core loop system
        }, "Ecosystem-Core-loop");
        coreLoopThread.setDaemon(true);
        coreLoopThread.start();
        // Chạy JavaFX Application. Phương thức này sẽ chặn cho đến khi cửa sổ đóng.
        ApplicationFrame.launch(ApplicationFrame.class, args);
    }
    private static void runSimulationTick(Renderer renderer){
        // todo: cập nhật môi trường

        // todo: Cập nhật các sinh vật trong organism registry, mỗi sinh vật cập nhật xong thì goi renderer.submit()

        renderer.renderAll();
    }
}