package wildlife.model.environment;

import wildlife.model.dto.RenderData;
import wildlife.model.environment.component.OrganismRegistry;
import wildlife.model.environment.component.ResourceManager;
import wildlife.model.environment.component.TerrainComponent;
import wildlife.model.environment.component.TimeComponent;
import wildlife.model.environment.enums.TerrainType;
import wildlife.util.RegionBoundary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bản đồ tổng hợp (Composite Pattern) — quản lý nhiều môi trường con cùng lúc.
 */
public class CompositeMap extends Environment {

    /** Danh sách các môi trường con được quản lý (Rừng, Đồng Cỏ, Hồ...) */
    private final List<Environment> subEnvironments;

    public CompositeMap(String id, String name) {
        // Khởi tạo một ranh giới tổng bằng RegionBoundary
        // Vì CompositeMap chỉ đóng vai trò quản lý (ủy quyền cho các môi trường con)
        super(
                id, name,
                0f, 0f, 1f,
                new TimeComponent(),
                new TerrainComponent(new RegionBoundary(50.0f), TerrainType.GRASSLAND), // SỬA Ở ĐÂY
                new OrganismRegistry(),
                new ResourceManager()
        );
        this.subEnvironments = new ArrayList<>();
    }

    /** Thêm môi trường con vào bản đồ tổng */
    public void addSubEnvironment(Environment env) {
        if (env == null) throw new IllegalArgumentException("Không thể thêm môi trường null");
        if (env == this) throw new IllegalArgumentException("Không thể thêm CompositeMap vào chính nó");
        subEnvironments.add(env);
    }

    /**
     * Ghi đè luồng cập nhật chính (Template Method).
     * Phân phối lệnh "Thời gian trôi qua" xuống tất cả các môi trường con.
     */
    @Override
    public final void updateEnvironment(int currentTick) {
        // Bản thân CompositeMap cũng cần cập nhật thời gian chung của toàn thế giới
        time.advance(currentTick);

        // Ủy quyền xuống từng môi trường con (Sub-environments) tự xử lý sinh vật, thời tiết của chúng
        for (Environment sub : subEnvironments) {
            sub.updateEnvironment(currentTick);
        }
    }

    /**
     * Thu thập danh sách RenderData từ tất cả các môi trường con.
     * ViewLogic (UI) sẽ gọi hàm này để lấy dữ liệu vẽ lên màn hình.
     */
    public List<RenderData> getAllRenderSnapshots() {
        List<RenderData> all = new ArrayList<>();
        // Giả sử Environment có hàm getRenderSnapshot() trả về List<RenderData>
        // của các sinh vật do nó quản lý
        for (Environment sub : subEnvironments) {
            all.addAll(sub.getRenderSnapshot());
        }
        return Collections.unmodifiableList(all);
    }

    /** Đếm tổng số sinh vật còn sống trên toàn bộ bản đồ */
    public int getTotalOrganismCount() {
        return subEnvironments.stream()
                .mapToInt(e -> e.getRegistry().count())
                .sum();
    }

    // ----------------------------------------------------------------
    // Các hàm sau bị vô hiệu hóa (rỗng) vì CompositeMap uỷ quyền hết cho con
    // ----------------------------------------------------------------
    @Override protected void initialize() {}
    @Override protected void applySeasonEffect() {}
    @Override protected void applyWeatherEffect() {}

    public List<Environment> getSubEnvironments() {
        return Collections.unmodifiableList(subEnvironments);
    }
}