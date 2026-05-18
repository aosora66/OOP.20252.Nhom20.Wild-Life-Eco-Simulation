package wildlife.model.environment;

import wildlife.model.dto.RenderData;
import wildlife.model.environment.component.OrganismRegistry;
import wildlife.model.environment.component.ResourceManager;
import wildlife.model.environment.component.TerrainComponent;
import wildlife.model.environment.component.TimeComponent;
import wildlife.model.environment.enums.TerrainType;
import wildlife.model.environment.event.EnvironmentEventPublisher;
import wildlife.util.AppConfig;
import wildlife.util.ValueRange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Bản đồ tổng hợp (Composite Pattern) — quản lý nhiều môi trường con cùng lúc.
 */
public class CompositeMap extends Environment {

    // ----------------------------------------------------------------
    //  Danh sách môi trường con
    // ----------------------------------------------------------------

    /** Danh sách các môi trường con được quản lý */
    private final List<Environment> subEnvironments;

    // ----------------------------------------------------------------
    //  Constructor
    // ----------------------------------------------------------------

    /**
     * @param id   ID duy nhất của bản đồ tổng
     * @param name Tên hiển thị
     */
    public CompositeMap(String id, String name) {
        // CompositeMap tự tạo các component tối giản (placeholder),
        // vì thực chất nó không dùng chúng — chỉ ủy quyền xuống con.
        super(
                id, name,
                0f, 0f, 1f,
                new TimeComponent(
                        AppConfig.getInt("environment.time.ticksPerDayCycle"),
                        AppConfig.getInt("environment.time.ticksPerSeason")
                ),
                // --- DÒNG ĐƯỢC SỬA ---
                // Bản đồ tổng hợp bao trọn toàn bộ thế giới (0 đến 1000)
                new TerrainComponent(new wildlife.util.RectBoundary(0, 1000, 0, 1000), TerrainType.GRASSLAND),
                // ---------------------

                new OrganismRegistry(),
                new ResourceManager(),
                new EnvironmentEventPublisher("sounds/world_ambient.wav")
        );
        this.subEnvironments = new ArrayList<>();
    }

    // ----------------------------------------------------------------
    //  Quản lý môi trường con
    // ----------------------------------------------------------------

    /**
     * Thêm một môi trường con vào bản đồ tổng.
     *
     * @param env môi trường cần thêm
     * @throws IllegalArgumentException nếu env là null hoặc là CompositeMap lồng nhau bản thân
     */
    public void addSubEnvironment(Environment env) {
        if (env == null) throw new IllegalArgumentException("Không thể thêm môi trường null");
        if (env == this) throw new IllegalArgumentException("Không thể thêm CompositeMap vào chính nó");
        subEnvironments.add(env);
    }

    /**
     * Xóa một môi trường con khỏi bản đồ tổng.
     *
     * @param id ID của môi trường cần xóa
     * @return true nếu xóa thành công
     */
    public boolean removeSubEnvironment(String id) {
        return subEnvironments.removeIf(env -> env.getId().equals(id));
    }

    // ----------------------------------------------------------------
    //  Ghi đè updateEnvironment — phân phối lệnh xuống con
    // ----------------------------------------------------------------

    /**
     * Cập nhật toàn bộ bản đồ tổng bằng cách ra lệnh cho từng môi trường con.
     *
     * Ghi đè hoàn toàn logic của lớp cha vì CompositeMap không tự tick —
     * nó chỉ là bộ điều phối (orchestrator).
     *
     * @param currentTick tick hiện tại từ hệ thống
     */
    @Override
    public final void updateEnvironment(int currentTick) {
        for (Environment sub : subEnvironments) {
            sub.updateEnvironment(currentTick);
        }
    }

    // ----------------------------------------------------------------
    //  Lấy dữ liệu render từ toàn bộ môi trường con
    // ----------------------------------------------------------------

    /**
     * Gom toàn bộ RenderData từ tất cả môi trường con.
     * ViewLogic gọi phương thức này để lấy snapshot toàn thế giới.
     *
     * @return danh sách RenderData của toàn bộ sinh vật trên bản đồ
     */
    public List<RenderData> getAllRenderSnapshots() {
        List<RenderData> all = new ArrayList<>();
        for (Environment sub : subEnvironments) {
            all.addAll(sub.getRenderSnapshot());
        }
        return Collections.unmodifiableList(all);
    }

    /**
     * Tìm môi trường con theo ID.
     *
     * @param id ID môi trường cần tìm
     * @return môi trường tìm được, hoặc null nếu không tồn tại
     */
    public Environment findSubEnvironment(String id) {
        return subEnvironments.stream()
                .filter(e -> e.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * Đếm tổng số sinh vật còn sống trên toàn bộ bản đồ.
     *
     * @return tổng số sinh vật còn sống
     */
    public int getTotalOrganismCount() {
        return subEnvironments.stream()
                .mapToInt(e -> e.getRegistry().getAllAlive().size())
                .sum();
    }

    // ----------------------------------------------------------------
    //  Implement abstract methods (rỗng — ủy quyền xuống con)
    // ----------------------------------------------------------------

    /**
     * CompositeMap không tự xử lý hiệu ứng mùa.
     * Mỗi môi trường con tự xử lý trong updateEnvironment() của nó.
     */
    @Override
    protected void applySeasonEffect() {
        // Ủy quyền xuống từng subEnvironment — không cần xử lý ở đây
    }

    /**
     * CompositeMap không tự xử lý hiệu ứng thời tiết.
     * Mỗi môi trường con tự xử lý trong updateEnvironment() của nó.
     */
    @Override
    protected void applyWeatherEffect() {
        // Ủy quyền xuống từng subEnvironment — không cần xử lý ở đây
    }

    /**
     * CompositeMap không tự sinh tài nguyên.
     * Mỗi môi trường con tự sinh tài nguyên trong updateEnvironment() của nó.
     */
    @Override
    protected void generateNaturalResources() {
        // Ủy quyền xuống từng subEnvironment — không cần xử lý ở đây
    }

    // ----------------------------------------------------------------
    //  Getters
    // ----------------------------------------------------------------

    /** Trả về danh sách môi trường con (chỉ đọc) */
    public List<Environment> getSubEnvironments() {
        return Collections.unmodifiableList(subEnvironments);
    }

    @Override
    public String toString() {
        return String.format("[CompositeMap | id=%s | subEnvironments=%d | totalOrganisms=%d]",
                getId(), subEnvironments.size(), getTotalOrganismCount());
    }
}