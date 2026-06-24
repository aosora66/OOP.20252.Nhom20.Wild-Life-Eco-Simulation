package wildlife.util;

import wildlife.model.environment.CompositeMap;
import wildlife.model.environment.enums.FoodType;
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
    private static final int LAKE_SHORE_BAND_TILES = 2;

    public static CompositeMap loadMapFromFile(String mapId, String mapName, String filePath) {
        List<String> layout = new ArrayList<>();

        // 1. Đọc file map.txt từ thư mục resources
        try (InputStream in = MapLoader.class.getClassLoader().getResourceAsStream(filePath)) {
            if (in == null) {
                throw new RuntimeException("Không tìm thấy file map: " + filePath);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            // Tìm đoạn đọc file trong MapLoader.java và sửa lại thành:
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Bỏ qua dòng trống và dòng bắt đầu bằng #
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                // Chỉ lấy các ký tự là số (0-4) và khoảng trắng
                String cleanLine = line.replaceAll("[^0-4\\s]", "");
                if (!cleanLine.trim().isEmpty()) {
                    layout.add(cleanLine.trim());
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
                        world.getTerrain().setTile(c, r, TerrainType.DEEP_WATER);
                        waterRegion.addTile(pos);
                    }
                    case 1 -> { // 1: Đồng cỏ
                        world.getTerrain().setTile(c, r, TerrainType.GRASSLAND);
                        grassRegion.addTile(pos);
                    }
                    case 2 -> { // 2: Rừng rậm
                        world.getTerrain().setTile(c, r, TerrainType.FOREST);
                        forestRegion.addTile(pos);
                    }
                    case 3 -> { // 3: Núi đá
                        world.getTerrain().setTile(c, r, TerrainType.MOUNTAIN);
                    }
                    case 4 -> { // 4: Bùn lầy (nhưng thuộc vùng Grassland)
                        world.getTerrain().setTile(c, r, TerrainType.MUD);
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

        spawnShorelineWaterSources(layout, forestEnv, grassEnv);

        world.addSubEnvironment(forestEnv);
        world.addSubEnvironment(grassEnv);
        world.addSubEnvironment(waterEnv);

        return world;
    }

    private static void spawnShorelineWaterSources(List<String> layout, Forest forestEnv, GrassLand grassEnv) {
        float waterNutrition = AppConfig.getFloat("food.water.nutritionalValue");

        for (int r = 0; r < layout.size(); r++) {
            String[] tokens = layout.get(r).split("\\s+");
            for (int c = 0; c < tokens.length; c++) {
                int tileCode = Integer.parseInt(tokens[c]);
                if (!isDrinkableShoreTile(tileCode) || !isNearLakeShore(layout, r, c, LAKE_SHORE_BAND_TILES)) {
                    continue;
                }

                Vector2D pos = new Vector2D((c + 0.5f) * TILE_SIZE, (r + 0.5f) * TILE_SIZE);
                if (tileCode == 2) {
                    forestEnv.getResources().spawnFood(pos, waterNutrition, FoodType.WATER, Integer.MAX_VALUE);
                } else {
                    grassEnv.getResources().spawnFood(pos, waterNutrition, FoodType.WATER, Integer.MAX_VALUE);
                }
            }
        }
    }

    private static boolean isDrinkableShoreTile(int tileCode) {
        return tileCode == 1 || tileCode == 2 || tileCode == 4;
    }

    private static boolean isNearLakeShore(List<String> layout, int row, int col, int shoreBandTiles) {
        for (int dr = -shoreBandTiles; dr <= shoreBandTiles; dr++) {
            for (int dc = -shoreBandTiles; dc <= shoreBandTiles; dc++) {
                int distanceInTiles = Math.abs(dr) + Math.abs(dc);
                if (distanceInTiles == 0 || distanceInTiles > shoreBandTiles) {
                    continue;
                }
                if (isWaterTile(layout, row + dr, col + dc)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isWaterTile(List<String> layout, int row, int col) {
        if (row < 0 || row >= layout.size() || col < 0) return false;
        String[] tokens = layout.get(row).split("\\s+");
        return col < tokens.length && Integer.parseInt(tokens[col]) == 0;
    }
}
