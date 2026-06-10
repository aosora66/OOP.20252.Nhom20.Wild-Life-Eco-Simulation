package wildlife.core;

import java.util.ArrayList;
import java.util.List;
import wildlife.model.organism.Organism;
import wildlife.util.AppConfig;

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

        // BƯỚC 4: KHỞI TẠO GIAO DIỆN (VIEW)
        // TODO: Khởi tạo class View, truyền kích thước bản đồ vào để setup Window
        // SimulationView view = new SimulationView(mapWidth, mapHeight);
        // view.initWindow();

        System.out.println("--- KHỞI TẠO HOÀN TẤT! ---");
        System.out.println("Tổng số sinh vật: " + ecosystem.size());

        // TODO: Bắt đầu gọi vòng lặp Tick System (Game Loop) ở đây
        // runTickSystem(ecosystem, view);
    }
}