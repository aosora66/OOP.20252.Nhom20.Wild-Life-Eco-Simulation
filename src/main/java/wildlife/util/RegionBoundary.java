package wildlife.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class RegionBoundary implements Boundary {
    private final List<Vector2D> tileCenters = new ArrayList<>();
    private final Set<String> tileSet = new HashSet<>();
    private final float tileSize;

    public RegionBoundary(float tileSize) {
        this.tileSize = tileSize;
    }

    // Thêm một ô (tile) vào vùng sinh thái này
    public void addTile(Vector2D pos) {
        tileCenters.add(pos);
        int tx = (int) (pos.getX() / tileSize);
        int ty = (int) (pos.getY() / tileSize);
        tileSet.add(tx + "," + ty); // Dùng HashSet để tra cứu tốc độ O(1)
    }

    @Override
    public boolean contains(Vector2D pos) {
        int tx = (int) (pos.getX() / tileSize);
        int ty = (int) (pos.getY() / tileSize);
        return tileSet.contains(tx + "," + ty);
    }

    @Override
    public Vector2D getRandomPoint(Random random) {
        if (tileCenters.isEmpty()) return new Vector2D(0, 0);
        // Chọn ngẫu nhiên 1 ô thuộc vùng này
        Vector2D tile = tileCenters.get(random.nextInt(tileCenters.size()));

        // Random tọa độ chi tiết nằm TRONG ô đó
        float x = tile.getX() + random.nextFloat() * tileSize;
        float y = tile.getY() + random.nextFloat() * tileSize;
        return new Vector2D(x, y);
    }
}