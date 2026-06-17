package wildlife.model.environment.component;

import wildlife.model.organism.Organism;
import wildlife.util.Vector2D;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
     * Lấy danh sách TẤT CẢ sinh vật thuộc một loại cụ thể (kể cả đã chết).
            * * @param type Lớp của sinh vật muốn lấy (VD: Animal.class / Plant.class / Organism.class)
     * @return Danh sách đã được cast sẵn về đúng kiểu T
     */
    public <T extends Organism> List<T> getAll(Class<T> type) {
        return organisms.stream()
                .filter(type::isInstance)       // Chỉ lấy đối tượng thuộc class T hoặc subclass của T
                .map(type::cast)                // Ép kiểu an toàn về T
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách các sinh vật CÒN SỐNG thuộc một loại cụ thể.
     */
    public <T extends Organism> List<T> getAllAlive(Class<T> type) {
        return organisms.stream()
                .filter(o -> o.isAlive() && type.isInstance(o))
                .map(type::cast)
                .collect(Collectors.toList());
    }

    /**
     * Tìm tất cả sinh vật CÒN SỐNG thuộc một loại cụ thể trong bán kính.
     * Rất hữu ích cho ViewLogic hoặc khi Động vật săn mồi tìm kiếm con mồi.
     */
    public <T extends Organism> List<T> findNear(Vector2D center, float radius, Class<T> type) {
        return organisms.stream()
                .filter(o -> o.isAlive()
                        && type.isInstance(o)
                        && o.getPosition().distanceTo(center) <= radius)
                .map(type::cast)
                .collect(Collectors.toList());
    }
}