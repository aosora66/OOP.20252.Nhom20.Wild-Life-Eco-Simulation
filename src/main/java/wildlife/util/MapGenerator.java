package wildlife.util;

import java.util.Random;

public class MapGenerator {
    public static void main(String[] args) {
        int width = (int)AppConfig.getFloat("environment.terrain.map_cols");
        int height = (int)AppConfig.getFloat("environment.terrain.map_rows");
        int[][] map = new int[height][width];
        Random rng = new Random(42);

        // Bước 1: Nền mặc định là ĐỒNG CỎ
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                map[y][x] = 1; // GRASSLAND
            }
        }

        // Bước 2: Vẽ vùng RỪNG RẬM
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double nx = x / (double) width;
                double ny = y / (double) height;

                double forestBlob = Math.sin(nx * 4.0 - 1.0) * Math.cos(ny * 3.0 - 1.2)
                        + Math.sin((nx + ny) * 2.5);
                boolean inForestZone = forestBlob > 0.6;

                if (inForestZone) {
                    map[y][x] = 2; // FOREST
                }
            }
        }

        // Bước 3: Vẽ HỒ NƯỚC
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double nx = x / (double) width;
                double ny = y / (double) height;

                double dx = nx - 0.35;
                double dy = ny - 0.45;
                double distCore = Math.sqrt(dx * dx * 2.2 + dy * dy * 1.4);

                double lakeNoise = Math.sin(nx * 14.0) * Math.cos(ny * 11.0)
                        + Math.sin((nx + ny) * 9.0) * 0.6;

                if (distCore < 0.30 && lakeNoise > 0.15) {
                    map[y][x] = 0; // DEEP_WATER
                }
            }
        }

        // Các hồ nhỏ riêng lẻ
        double[][] smallLakes = {
                {0.18, 0.13, 0.10, 0.07},
                {0.62, 0.22, 0.07, 0.06},
                {0.93, 0.10, 0.05, 0.05},
                {0.70, 0.33, 0.05, 0.045},
                {0.74, 0.55, 0.10, 0.08},
                {0.83, 0.72, 0.07, 0.06}
        };
        for (double[] lake : smallLakes) {
            double cx = lake[0], cy = lake[1], rx = lake[2], ry = lake[3];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    double nx = x / (double) width;
                    double ny = y / (double) height;
                    double dx = (nx - cx) / rx;
                    double dy = (ny - cy) / ry;
                    double wobble = Math.sin(nx * 20.0 + cx * 10) * Math.cos(ny * 20.0 + cy * 10) * 0.15;
                    if (dx * dx + dy * dy + wobble < 1.0) {
                        map[y][x] = 0; // DEEP_WATER
                    }
                }
            }
        }

        // Bước 4: Vẽ VÁCH NÚI
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double nx = x / (double) width;
                double ny = y / (double) height;

                double mainRidge = nx - (0.62 + ny * 0.55);
                double ridgeJagged = Math.sin(ny * 30.0) * 0.04 + Math.sin(ny * 11.0) * 0.025;
                boolean onMainRidge = (mainRidge + ridgeJagged) > 0 && ny < 0.78;

                double dxTop = nx - 0.45;
                double dyTop = ny - 0.06;
                boolean onTopPatch = (dxTop * dxTop * 3.0 + dyTop * dyTop * 8.0) < 0.012 && ny < 0.20;

                double dxLeft = nx - 0.04;
                double dyLeft = ny - 0.40;
                boolean onLeftPatch = (dxLeft * dxLeft * 6.0 + dyLeft * dyLeft * 10.0) < 0.010;

                if (onMainRidge || onTopPatch || onLeftPatch) {
                    map[y][x] = 3; // MOUNTAIN
                }
            }
        }

        // Bước 5: Rải BÙN (MUD) — rải toàn phần đồng cỏ (5-6 vũng)
        int mudSpots = 5 + rng.nextInt(2); // Ra ngẫu nhiên 5 hoặc 6 vũng bùn
        for (int i = 0; i < mudSpots; i++) {
            int cx, cy;
            // Đảm bảo tâm của vũng bùn phải nằm trên khu vực đồng cỏ
            do {
                cx = rng.nextInt(width);
                cy = rng.nextInt(height);
            } while (map[cy][cx] != 1);

            int spotSize = 1 + rng.nextInt(2); // Bán kính vũng bùn
            for (int oy = -spotSize; oy <= spotSize; oy++) {
                for (int ox = -spotSize; ox <= spotSize; ox++) {
                    int tx = cx + ox, ty = cy + oy;
                    if (tx < 0 || tx >= width || ty < 0 || ty >= height) continue;
                    // Bùn sẽ chỉ ghi đè lên các ô có sẵn là GRASSLAND (1)
                    if (ox * ox + oy * oy <= spotSize * spotSize && map[ty][tx] == 1) {
                        map[ty][tx] = 4; // MUD
                    }
                }
            }
        }

        // In ra console để copy vào map.txt
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                System.out.print(map[y][x] + " ");
            }
            System.out.println();
        }
    }
}