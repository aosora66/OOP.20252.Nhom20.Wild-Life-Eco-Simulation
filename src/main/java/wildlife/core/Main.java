package wildlife.core;
import wildlife.model.dto.RenderData;
import wildlife.model.environment.CompositeMap;
import wildlife.util.MapLoader;
import wildlife.view.ApplicationFrame;

import java.util.List;

import wildlife.model.organism.Organism;
import wildlife.view.renderer.Renderer;
import wildlife.view.ui.UIEventController;

import java.util.stream.Collectors;

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
            if (renderer != null) renderer.setTerrain(world.getTerrain());

            final int  TICK_RATE       = 30;
            final long NS_PER_TICK     = 1_000_000_000L / TICK_RATE;
            // Số tick tối đa được phép bù trong 1 frame — ngăn "spiral of death"
            // khi GC pause hoặc breakpoint làm đồng hồ nhảy vọt
            final int  MAX_CATCHUP     = 5;

            long lastTime    = System.nanoTime();
            long accumulator = 0L;
            int  currentTick = 0;

            while (true) {
                long frameStart = System.nanoTime();
                long elapsed    = frameStart - lastTime;
                lastTime = frameStart;

                // Clamp delta: nếu elapsed vượt MAX_CATCHUP tick, bỏ phần thừa
                accumulator += Math.min(elapsed, NS_PER_TICK * MAX_CATCHUP);

                // --- Xử lý tất cả tick đã tích lũy ---
                while (accumulator >= NS_PER_TICK) {
                    if (!UIEventController.isPaused()) {
                        currentTick++;
                        world.updateEnvironment(currentTick);
                    }
                    accumulator -= NS_PER_TICK;

                    // Cập nhật danh sách sinh vật cho UI click detection
                    if (currentTick % 30 == 0) {
                        List<Organism> all = world.getSubEnvironments().stream()
                                .flatMap(env -> env.getRegistry().getAll(Organism.class).stream())
                                .collect(Collectors.toList());
                        UIEventController.setActiveOrganisms(all);
                    }

                    // Cập nhật realtime UI panel của thực thể được chọn
                    UIEventController.tickUpdate();

                    if (currentTick % TICK_RATE == 0) {
                        var timeInfo = world.getTime();
                        System.out.printf("[Tick %d] Sinh vật: %d | Thời tiết: %s | Mùa: %s\n",
                                currentTick,
                                world.getTotalOrganismCount(),
                                timeInfo.getCurrentWeather(),
                                timeInfo.getCurrentSeason()
                        );
                    }
                }

                // --- Submit snapshot một lần duy nhất sau batch tick của frame ---
                if (renderer != null) {
                    for (RenderData o : world.getAllRenderSnapshots()) {
                        renderer.submit(o);
                    }
                    renderer.commitFrame();
                }

                // --- Ngủ đến khi tick kế tiếp, không spin CPU ---
                long frameWork = System.nanoTime() - frameStart;
                long sleepNs   = (NS_PER_TICK - accumulator) - frameWork;
                if (sleepNs > 1_000_000L) { // > 1ms: đáng ngủ
                    try {
                        Thread.sleep(sleepNs / 1_000_000L, (int)(sleepNs % 1_000_000L));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }, "Ecosystem-Core-loop");
        coreLoopThread.setDaemon(true);
        coreLoopThread.start();
        // Chạy JavaFX Application. Phương thức này sẽ chặn cho đến khi cửa sổ đóng.
        ApplicationFrame.launch(ApplicationFrame.class, args);
    }
}