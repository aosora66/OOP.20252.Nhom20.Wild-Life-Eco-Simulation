package wildlife.model.environment.component;

import wildlife.model.organism.Organism;
import wildlife.util.Vector2D;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Component quản lý toàn bộ danh sách sinh vật trong một môi trường.
 */
public class OrganismRegistry {

    /** Danh sách tất cả sinh vật đang được quản lý trong môi trường này */
    private final List<Organism> organisms;

    public OrganismRegistry() {
        this.organisms = new ArrayList<>();
    }

    // ----------------------------------------------------------------
    //  Thêm / Xóa
    // ----------------------------------------------------------------

    /**
     * Đăng ký một sinh vật mới vào môi trường.
     *
     * @param organism sinh vật cần thêm (không được null)
     * @throws IllegalArgumentException nếu organism là null
     */
    public void add(Organism organism) {
        if (organism == null) throw new IllegalArgumentException("Không thể thêm sinh vật null vào registry");
        organisms.add(organism);
    }

    /**
     * Xóa sinh vật khỏi môi trường theo ID.
     * @param id ID của sinh vật cần xóa
     * @return true nếu xóa thành công
     */
    public boolean remove(String id) {
        return organisms.removeIf(o -> o.getId().equals(id));
    }

    // ----------------------------------------------------------------
    //  Truy vấn
    // ----------------------------------------------------------------

    /**
     * Tìm sinh vật theo ID duy nhất.
     *
     * @param id ID cần tìm
     * @return Optional chứa sinh vật, hoặc empty nếu không tìm thấy
     */
    public Optional<Organism> findById(String id) {
        return organisms.stream()
                .filter(o -> o.getId().equals(id))
                .findFirst();
    }

    /**
     * Đếm tổng số sinh vật hiện đang được quản lý (kể cả chết).
     *
     * @return số lượng sinh vật
     */
    public int count() {
        return organisms.size();
    }

    /**
     * Tìm tất cả sinh vật trong bán kính cho trước tính từ một vị trí.
     * Chỉ trả về sinh vật còn sống (ALIVE).
     *
     * @param center tọa độ trung tâm
     * @param radius bán kính tìm kiếm
     * @return danh sách sinh vật trong vùng (chỉ đọc)
     */
    public List<Organism> findNear(Vector2D center, float radius) {
        List<Organism> result = new ArrayList<>();
        for (Organism o : organisms) {
            if (o.isAlive() && o.getPosition().distanceTo(center) <= radius) {
                result.add(o);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Lấy toàn bộ danh sách sinh vật còn sống (ALIVE).
     * @return danh sách chỉ đọc các sinh vật còn sống
     */
    public List<Organism> getAllAlive() {
        List<Organism> alive = new ArrayList<>();
        for (Organism o : organisms) {
            if (o.isAlive()) alive.add(o);
        }
        return Collections.unmodifiableList(alive);
    }

    /**
     * Lấy toàn bộ danh sách sinh vật (kể cả DEAD).
     * Dùng để Environment dọn dẹp xác sinh vật sau N tick.
     *
     * @return danh sách chỉ đọc tất cả sinh vật
     */
    public List<Organism> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(organisms));
    }
}