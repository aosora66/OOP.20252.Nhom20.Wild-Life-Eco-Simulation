package wildlife.core;

import wildlife.model.environment.envType.GrassLand;
import wildlife.model.environment.enums.Season;
import wildlife.model.environment.enums.WeatherType;
import wildlife.model.organism.animal.canivores.Tiger;
import wildlife.model.organism.animal.canivores.Wolf;
import wildlife.model.organism.animal.canivores.Hunter;
import wildlife.model.organism.animal.hebivores.Elephant;
import wildlife.model.organism.animal.hebivores.Rabbit;
import wildlife.util.RectBoundary;
import wildlife.util.Vector2D;
import wildlife.util.SoundManager;
import wildlife.model.organism.animal.Animal;
import wildlife.model.organism.Organism;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("=========================================================================");
        System.out.println("\uD83C\uDF3F HỆ THỐNG MÔ PHỎNG HỆ SINH THÁI HOANG DÃ - BÀI TẬP LỚN OOP NHÓM 20 \uD83C\uDF3F");
        System.out.println("\uD83D\uDD0A DEMO HỆ THỐNG ÂM THANH MÔ PHỎNG SINH ĐỘNG");
        System.out.println("=========================================================================");
        System.out.println("Hướng dẫn nghe thử:");
        System.out.println("1. Tiếng bước chân chỉ phát cho con vật được focus.");
        System.out.println("2. Nhạc nền tự động thay đổi theo ngày/đêm và mùa/thời tiết:");
        System.out.println("   - Ambiance.wav (Ngày thường)");
        System.out.println("   - NightAmbiance.wav (Buổi đêm)");
        System.out.println("   - DryAmbiance.wav (Mùa hạn hán)");
        System.out.println("   - RainingAmbiance.wav (Thời tiết mưa)");
        System.out.println("3. Tiếng động vật & thợ săn hành động:");
        System.out.println("   - TigerRoar.wav khi Hổ tấn công");
        System.out.println("   - ElephantGrowl.wav khi Voi tấn công");
        System.out.println("   - WolfHowl.wav khi bầy Sói tụ tập gần nhau");
        System.out.println("   - Snarl.wav khi động vật ăn thịt chuẩn bị ăn thịt");
        System.out.println("   - GunLoad -> GunFire.wav nối tiếp nhau khi Thợ săn bắn súng");
        System.out.println("=========================================================================\n");

        // Khởi tạo môi trường GrassLand
        // Bản đồ kích thước 500x500
        System.out.println("[System] Đang khởi tạo môi trường Grassland...");
        GrassLand grassland = new GrassLand("grassland_01", "Đồng Cỏ Xanh", new RectBoundary(0f, 500f, 0f, 500f));
        System.out.println("[System] Môi trường Grassland khởi tạo thành công!");

        // Khởi tạo và thả các sinh vật vào môi trường
        System.out.println("[System] Đang tạo các loài động vật...");
        
        // Thả Hổ ở (100, 100)
        Tiger tiger = Tiger.create(new Vector2D(100, 100), grassland);
        grassland.addOrganism(tiger);
        System.out.println(" + Tạo Tiger thành công (ID: " + tiger.getId() + ")");

        // Thả Thợ săn ở (120, 120)
        Hunter hunter = Hunter.create(new Vector2D(120, 120), grassland);
        grassland.addOrganism(hunter);
        System.out.println(" + Tạo Hunter thành công (ID: " + hunter.getId() + ")");

        // Thả Voi ở (200, 200)
        Elephant elephant = Elephant.create(new Vector2D(200, 200), grassland);
        grassland.addOrganism(elephant);
        System.out.println(" + Tạo Elephant thành công (ID: " + elephant.getId() + ")");

        // Thả 2 con Sói ở gần nhau (150, 150) và (155, 155) để kích hoạt tiếng hú bầy
        Wolf wolf1 = Wolf.create(new Vector2D(150, 150), grassland);
        Wolf wolf2 = Wolf.create(new Vector2D(155, 155), grassland);
        grassland.addOrganism(wolf1);
        grassland.addOrganism(wolf2);
        System.out.println(" + Tạo 2 Wolf thành công để kích hoạt tiếng hú WolfHowl");

        // Thả vài con Thỏ làm mồi cho Hổ và Thợ săn
        Rabbit rabbit1 = Rabbit.create(new Vector2D(105, 105), grassland);
        Rabbit rabbit2 = Rabbit.create(new Vector2D(125, 125), grassland);
        grassland.addOrganism(rabbit1);
        grassland.addOrganism(rabbit2);
        System.out.println(" + Tạo 2 Rabbit thành công để làm mục tiêu săn mồi");

        System.out.println("\n[System] Bắt đầu vòng lặp mô phỏng. Sẽ chạy 100 tick...");
        System.out.println("[System] Phân bố chu kỳ focus động vật:");
        System.out.println(" - Ticks 1-20: Focus Tiger (Bạn sẽ nghe thấy GrassFootstep/BushRustling của Tiger)");
        System.out.println(" - Ticks 21-40: Focus Hunter (Bạn sẽ nghe thấy GrassFootstep của Hunter)");
        System.out.println(" - Ticks 41-60: Focus Elephant (Bạn sẽ nghe thấy GrassFootstep của Elephant)");
        System.out.println(" - Ticks 61-80: Focus Wolf (Bạn sẽ nghe thấy GrassFootstep của Wolf)");
        System.out.println(" - Ticks 81-100: Focus con vật gần tọa độ (100, 100) nhất");
        System.out.println("=========================================================================\n");

        for (int tick = 1; tick <= 100; tick++) {
            // Thiết lập đối tượng được focus
            if (tick >= 1 && tick <= 20) {
                SoundManager.setFocusedAnimalId(tiger.getId());
                System.out.print("[FOCUS: Tiger] ");
            } else if (tick >= 21 && tick <= 40) {
                SoundManager.setFocusedAnimalId(hunter.getId());
                System.out.print("[FOCUS: Hunter] ");
            } else if (tick >= 41 && tick <= 60) {
                SoundManager.setFocusedAnimalId(elephant.getId());
                System.out.print("[FOCUS: Elephant] ");
            } else if (tick >= 61 && tick <= 80) {
                SoundManager.setFocusedAnimalId(wolf1.getId());
                System.out.print("[FOCUS: Wolf] ");
            } else {
                SoundManager.setFocusedAnimalId(null);
                SoundManager.setFocusPosition(new Vector2D(100, 100));
                System.out.print("[FOCUS: Auto (gần (100, 100) nhất)] ");
            }

            // In thông tin chu kỳ thời gian và thời tiết
            boolean isDay = grassland.getTime().isDaytime();
            Season season = grassland.getTime().getCurrentSeason();
            WeatherType weather = grassland.getTime().getCurrentWeather();
            
            System.out.printf("Tick %03d | %s | Mùa: %s | Thời tiết: %s | Số lượng sinh vật: %d%n",
                    tick,
                    isDay ? "🌞 BAN NGÀY" : "🌙 BAN ĐÊM",
                    season,
                    weather,
                    grassland.getRegistry().count()
            );

            // Cập nhật môi trường và hành vi động vật
            grassland.updateEnvironment(tick);

            // In vị trí hiện tại của các con vật chính để theo dõi
            List<Animal> aliveAnimals = grassland.getRegistry().getAllAlive(Animal.class);
            for (Animal animal : aliveAnimals) {
                if (animal.getId().equals(tiger.getId())) {
                    System.out.printf("   * Tiger: %.1f HP tại %s%n", animal.getStats().getHp(), animal.getPosition());
                } else if (animal.getId().equals(hunter.getId())) {
                    System.out.printf("   * Hunter: %.1f HP tại %s%n", animal.getStats().getHp(), animal.getPosition());
                } else if (animal.getId().equals(elephant.getId())) {
                    System.out.printf("   * Elephant: %.1f HP tại %s%n", animal.getStats().getHp(), animal.getPosition());
                }
            }

            // Chờ 800ms để âm thanh có thời gian phát và không bị dồn dập
            try {
                Thread.sleep(800);
            } catch (InterruptedException e) {
                System.err.println("Mô phỏng bị gián đoạn: " + e.getMessage());
                break;
            }
        }

        System.out.println("\n=========================================================================");
        System.out.println("🎉 KẾT THÚC MÔ PHỎNG TEST");
        System.out.println("=========================================================================");
        // Dừng tất cả nhạc nền trước khi kết thúc
        SoundManager.stopAmbiance("TIME");
        SoundManager.stopAmbiance("SEASON");
        System.exit(0);
    }
}
