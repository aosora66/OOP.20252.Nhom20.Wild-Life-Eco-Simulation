package wildlife.core;

import wildlife.model.environment.CompositeMap;
import wildlife.model.environment.enums.TerrainType;
import wildlife.util.MapLoader;
import wildlife.util.Vector2D;

// Các thư viện UI (Giả sử dùng Java Swing)
import javax.swing.Timer;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Main {
    private static int currentTick = 0;

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

        // Kiểm tra và in log xác nhận
        System.out.println("✅ Khởi tạo Model hoàn tất!");
        System.out.println("   - Số phân khu môi trường: " + world.getSubEnvironments().size());
        System.out.println("   - Tổng lượng thực/động vật ban đầu: " + world.getTotalOrganismCount());
        System.out.println("--------------------------------------------------");

        // ==========================================
        // PHẦN 2: INIT VIEW (GIAO DIỆN)
        // ==========================================
        // GameFrame là một class kế thừa JFrame (Swing) do nhóm bạn tự thiết kế
        GameFrame gameWindow = new GameFrame(1000, 1000);
        gameWindow.setVisible(true);


        // ==========================================
        // PHẦN 3: TƯƠNG TÁC NGƯỜI DÙNG (CONTROLLER)
        // ==========================================
        // Lắng nghe cú click chuột trên màn hình
        gameWindow.getRenderPanel().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Vector2D clickPos = new Vector2D(e.getX(), e.getY());

                // Chuột trái: Gieo thức ăn thủ công
                if (e.getButton() == MouseEvent.BUTTON1) {
                    world.getResourceManager().spawnFoodManual(clickPos);
                    System.out.println("Đã thả thức ăn tại: " + clickPos);
                }
                // Chuột phải: Đặt vách đá/vật cản
                else if (e.getButton() == MouseEvent.BUTTON3) {
                    world.getTerrain().addCustomTerrain(clickPos, TerrainType.MOUNTAIN);
                    System.out.println("Đã đặt vật cản tại: " + clickPos);
                }
            }
        });


        // ==========================================
        // PHẦN 4: VÒNG LẶP THỜI GIAN THỰC (GAME LOOP)
        // ==========================================
        System.out.println("Khởi động vòng lặp sinh thái...");

        // Thời gian trễ giữa mỗi tick (mili-giây).
        // 50ms tương đương với 20 FPS (Frame Per Second).
        // Nếu muốn game chạy nhanh hơn, giảm số này xuống (VD: 20ms).
        int delay = 50;
        final int[] currentTick = {0};

        Timer gameLoop = new Timer(delay, actionEvent -> {

            // Bước 4.1: THỜI GIAN TRÔI QUA (UPDATE MODEL)
            currentTick[0]++;
            world.updateEnvironment(currentTick[0]);

            // Bước 4.2: TRÍCH XUẤT DỮ LIỆU ĐỂ VẼ (FETCH DATA)
            // Lấy danh sách tọa độ mới nhất của toàn bộ sinh vật (thỏ, sói, cây...)
            var snapshots = world.getAllRenderSnapshots();

            // Lấy thông tin thời gian hiện tại (Mùa, Thời tiết, Giờ)
            var timeInfo = world.getTime();

            // Bước 4.3: YÊU CẦU GIAO DIỆN VẼ LẠI (RENDER VIEW)
            // (Lưu ý: Chỉ mở comment dòng này khi bạn ĐÃ code xong class GameFrame UI)
            // gameWindow.updateScreen(snapshots, timeInfo);

            // [Tùy chọn] In log ra console mỗi 50 tick (Khoảng 2.5 giây thực tế) để tiện theo dõi
            if (currentTick[0] % 50 == 0) {
                System.out.printf("[Tick %d] Sinh vật: %d | Thời tiết: %s | Mùa: %s\n",
                        currentTick[0],
                        world.getTotalOrganismCount(),
                        timeInfo.getCurrentWeather(),
                        timeInfo.getCurrentSeason()
                );

                // Nếu sinh vật chết hết, có thể dừng mô phỏng
                if (world.getTotalOrganismCount() <= 0) {
                    System.out.println("Tất cả sinh vật đã tuyệt chủng. Kết thúc mô phỏng!");
                    ((Timer)actionEvent.getSource()).stop(); // Dừng Timer
                }
            }
        });

        // Bắt đầu chạy vòng lặp!
        gameLoop.start();
    }
}