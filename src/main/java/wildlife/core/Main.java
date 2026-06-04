package wildlife.core;

import wildlife.model.environment.Environment;
import wildlife.model.environment.CompositeMap;
import wildlife.model.environment.terrain.Forest;
import wildlife.model.environment.terrain.Grassland;
import wildlife.model.environment.terrain.Lake;
import wildlife.util.RectBoundary;

public class Main {

    public static void main(String[] args) {
        System.out.println("Bắt đầu khởi tạo Hệ sinh thái Hoang dã...");

        // 1. Phân chia ranh giới địa lý bằng RectBoundary
        // Cấu trúc tham số: (startX, endX, startY, endY)
        
        // Rừng rậm chiếm góc trên bên trái (X: 0->100, Y: 0->100)
        RectBoundary forestBoundary = new RectBoundary(0, 100, 0, 100);
        
        // Đồng cỏ chiếm góc trên bên phải (X: 100->200, Y: 0->100)
        RectBoundary grassBoundary = new RectBoundary(100, 200, 0, 100);
        
        // Hồ nước trải dài toàn bộ nửa dưới bản đồ (X: 0->200, Y: 100->200)
        RectBoundary lakeBoundary = new RectBoundary(0, 200, 100, 200);

        // 2. Khởi tạo các môi trường vật lý
        Environment forest = new Forest("ENV_FOREST", "Rừng Già Amazon", forestBoundary);
        Environment grassland = new Grassland("ENV_GRASS", "Đồng Cỏ Savan", grassBoundary);
        Environment lake = new Lake("ENV_LAKE", "Hồ Bán Nguyệt", lakeBoundary, 50.0f);

        // 3. Khởi tạo CompositeMap 
        // Lưu ý: Tớ vẫn giữ constructor này theo giả định. 
        // Nếu class CompositeMap thật của cậu yêu cầu tham số khác (VD: truyền Boundary), cậu hãy sửa lại cho khớp nhé!
        CompositeMap worldMap = new CompositeMap("WORLD_MAP", "Bản Đồ Tổng Hợp");

        // 4. Lắp ráp các mảnh ghép vào bản đồ
        // Nếu hàm của cậu không tên là addEnvironment, hãy đổi thành add() hoặc addChild() tương ứng trong class CompositeMap.
        worldMap.addSubEnvironment(forest);
        worldMap.addSubEnvironment(grassland);
        worldMap.addSubEnvironment(lake);

        System.out.println("✅ Lắp ráp bản đồ thành công!");

        // 5. Khởi động Game Loop
        runSimulation(worldMap);
    }

    private static void runSimulation(CompositeMap worldMap) {
        int maxTicks = 1000; 
        int tickRateMs = 50; 

        System.out.println("⏳ Đang chạy mô phỏng...");

        for (int currentTick = 0; currentTick < maxTicks; currentTick++) {
            // worldMap.update(currentTick); // Bỏ comment khi CompositeMap đã có hàm update()

            if (currentTick % 100 == 0) {
                System.out.println("------------------------------------------------");
                System.out.println("[TICK " + currentTick + "]");
            }

            try {
                Thread.sleep(tickRateMs);
            } catch (InterruptedException e) {
                System.err.println("Mô phỏng bị gián đoạn!");
                break;
            }
        }

        System.out.println("Kết thúc mô phỏng.");
    }
}