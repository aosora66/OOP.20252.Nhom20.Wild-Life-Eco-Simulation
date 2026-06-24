package wildlife.util;

import java.util.Random;

public class MapGenerator {
    public static void main(String[] args) {
        int width = 100;
        int height = 100;
        int[][] map = new int[height][width];
        Random rng = new Random(42);

        // Bước 1: Nền mặc định là ĐỒNG CỎ
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                map[y][x] = 1; // GRASSLAND
            }
        }

        // Bước 2: Vẽ vùng RỪNG RẬM — các khoảng loang lổ rải đều (không phải khối nửa dưới),
        // ngưỡng 0.6 giúp diện tích rừng chỉ còn ~30% map (đồng cỏ chiếm phần lớn hơn).
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

        // Bước 3: Vẽ HỒ NƯỚC — cụm lớn loang lổ giữa-trái + vài hồ nhỏ rải rác
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

        // Các hồ nhỏ riêng lẻ (tọa độ tỉ lệ 0..1, bán kính riêng từng hồ)
        double[][] smallLakes = {
                // {centerX, centerY, radiusX, radiusY}
                {0.18, 0.13, 0.10, 0.07},  // hồ trên-trái
                {0.62, 0.22, 0.07, 0.06},  // hồ trên-giữa (cạnh núi)
                {0.93, 0.10, 0.05, 0.05},  // hồ nhỏ góc trên-phải
                {0.70, 0.33, 0.05, 0.045}, // hồ nhỏ giữa-phải trên
                {0.74, 0.55, 0.10, 0.08},  // hồ giữa-phải
                {0.83, 0.72, 0.07, 0.06}   // hồ dưới-phải
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

        // Bước 4: Vẽ VÁCH NÚI — dải chéo lớn từ trên xuống giữa-phải + mảng nhỏ trên-giữa & giữa-trái
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double nx = x / (double) width;
                double ny = y / (double) height;

                // Dải núi chính: đường chéo răng cưa bên phải, từ (0.62, 0.0) tới (1.0, 0.65)
                double mainRidge = nx - (0.62 + ny * 0.55);
                double ridgeJagged = Math.sin(ny * 30.0) * 0.04 + Math.sin(ny * 11.0) * 0.025;
                boolean onMainRidge = (mainRidge + ridgeJagged) > 0 && ny < 0.78;

                // Mảng núi nhỏ trên-giữa
                double dxTop = nx - 0.45;
                double dyTop = ny - 0.06;
                boolean onTopPatch = (dxTop * dxTop * 3.0 + dyTop * dyTop * 8.0) < 0.012
                        && ny < 0.20;

                // Mảng núi nhỏ giữa-trái
                double dxLeft = nx - 0.04;
                double dyLeft = ny - 0.40;
                boolean onLeftPatch = (dxLeft * dxLeft * 6.0 + dyLeft * dyLeft * 10.0) < 0.010;

                if (onMainRidge || onTopPatch || onLeftPatch) {
                    map[y][x] = 3; // MOUNTAIN
                }
            }
        }

        // Bước 5: Rải BÙN (MUD) — các đốm nhỏ lẻ tẻ trong vùng đồng cỏ, gần khu hồ
        int mudSpots = 8;
        for (int i = 0; i < mudSpots; i++) {
            int cx = 8 + rng.nextInt(18);   // tập trung khu giữa-trái (gần hồ)
            int cy = 18 + rng.nextInt(20);
            int spotSize = 1 + rng.nextInt(2);
            for (int oy = -spotSize; oy <= spotSize; oy++) {
                for (int ox = -spotSize; ox <= spotSize; ox++) {
                    int tx = cx + ox, ty = cy + oy;
                    if (tx < 0 || tx >= width || ty < 0 || ty >= height) continue;
                    if (ox * ox + oy * oy <= spotSize * spotSize && map[ty][tx] == 1) {
                        map[ty][tx] = 4; // MUD — chỉ đè lên GRASSLAND
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