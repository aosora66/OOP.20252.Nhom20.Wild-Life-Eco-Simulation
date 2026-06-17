package wildlife.util;

import wildlife.model.environment.CompositeMap;
import wildlife.model.environment.enums.TerrainType;
import wildlife.model.environment.envType.Forest;
import wildlife.model.environment.envType.GrassLand;
import wildlife.model.environment.envType.Lake;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MapLoader {

    private static final float TILE_SIZE = AppConfig.getFloat("environment.terrain.tileSize");

    public static CompositeMap loadMapFromFile(String mapId, String mapName, String filePath) {
        List<String> layout = new ArrayList<>();

        // 1. Đọc file map.txt từ thư mục resources
        try (InputStream in = MapLoader.class.getClassLoader().getResourceAsStream(filePath)) {
            if (in == null) {
                throw new RuntimeException("Không tìm thấy file map: " + filePath);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) { // Bỏ qua dòng trống và dòng comment
                    layout.add(line);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Lỗi đọc file map: " + e.getMessage(), e);
        }

        // 2. Dựng map từ dữ liệu vừa đọc
        return buildCompositeMap(mapId, mapName, layout);
    }

    private static CompositeMap buildCompositeMap(String id, String name, List<String> layout) {
        CompositeMap world = new CompositeMap(id, name);

        RegionBoundary forestRegion = new RegionBoundary(TILE_SIZE);
        RegionBoundary grassRegion = new RegionBoundary(TILE_SIZE);
        RegionBoundary waterRegion = new RegionBoundary(TILE_SIZE);

        for (int r = 0; r < layout.size(); r++) {
            // Cắt dòng hiện tại thành các chuỗi con dựa trên khoảng trắng
            String[] tokens = layout.get(r).split("\\s+");

            for (int c = 0; c < tokens.length; c++) {
                int tileCode = Integer.parseInt(tokens[c]); // Chuyển chuỗi thành số
                Vector2D pos = new Vector2D(c * TILE_SIZE, r * TILE_SIZE);

                // Dùng số nguyên để map với TerrainType
                switch (tileCode) {
                    case 0 -> { // 0: Nước sâu
                        world.getTerrain().addCustomTerrain(pos, TerrainType.DEEP_WATER);
                        waterRegion.addTile(pos);
                    }
                    case 1 -> { // 1: Đồng cỏ
                        world.getTerrain().addCustomTerrain(pos, TerrainType.GRASSLAND);
                        grassRegion.addTile(pos);
                    }
                    case 2 -> { // 2: Rừng rậm
                        world.getTerrain().addCustomTerrain(pos, TerrainType.FOREST);
                        forestRegion.addTile(pos);
                    }
                    case 3 -> { // 3: Núi đá
                        world.getTerrain().addCustomTerrain(pos, TerrainType.MOUNTAIN);
                    }
                    case 4 -> { // 4: Bùn lầy (nhưng thuộc vùng Grassland)
                        world.getTerrain().addCustomTerrain(pos, TerrainType.MUD);
                        grassRegion.addTile(pos);
                    }
                    default -> throw new IllegalArgumentException("Mã địa hình không hợp lệ: " + tileCode);
                }
            }
        }

        Forest forestEnv = new Forest("forest", "Rừng rậm", forestRegion);
        GrassLand grassEnv = new GrassLand("grass", "Đồng cỏ", grassRegion);
        Lake waterEnv = new Lake("lake", "Hồ nước", waterRegion, 100f);

        // Xử lý đè MUD lên GrassLand
        for (int r = 0; r < layout.size(); r++) {
            String[] tokens = layout.get(r).split("\\s+");
            for (int c = 0; c < tokens.length; c++) {
                if (Integer.parseInt(tokens[c]) == 4) { // Nếu là 4 (Bùn lầy)
                    grassEnv.getTerrain().addCustomTerrain(new Vector2D(c * TILE_SIZE, r * TILE_SIZE), TerrainType.MUD);
                }
            }
        }

        world.addSubEnvironment(forestEnv);
        world.addSubEnvironment(grassEnv);
        world.addSubEnvironment(waterEnv);

        return world;
    }
}