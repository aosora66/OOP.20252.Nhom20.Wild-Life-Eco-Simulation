package wildlife.model.organism.component;

import wildlife.model.environment.enums.TerrainType;
import wildlife.util.ValueRange;
import java.util.Collections;
import java.util.List;

/**
 * Component mô tả khả năng thích nghi môi trường của một sinh vật.
 */
public class AdaptabilityComponent {
    private final List<TerrainType> survivableEnvironments; // danh sách môi trường có thể tồn tại
    private final ValueRange optimalRange;
    private final ValueRange toleranceRange;
    private final ValueRange lethalLimit;

    /**
     * Khởi tạo Component Thích nghi
     * * @param survivableEnvironments danh sách môi trường sinh tồn được
     * @param optimalRange           khoảng điều kiện thuận lợi nhất
     * @param toleranceRange         khoảng điều kiện chịu đựng được
     * @param lethalLimit            khoảng điều kiện gây chết
     */
    public AdaptabilityComponent(List<TerrainType> survivableEnvironments,
                                 ValueRange optimalRange,
                                 ValueRange toleranceRange,
                                 ValueRange lethalLimit) {
        // danh sách không thể bị can thiệp
        this.survivableEnvironments = Collections.unmodifiableList(survivableEnvironments);
        this.optimalRange           = optimalRange;
        this.toleranceRange         = toleranceRange;
        this.lethalLimit            = lethalLimit;
    }

    // ----------------------------------------------------------
    //  Kiểm tra có thuộc các khoảng thuộc tính
    // ----------------------------------------------------------

    public boolean canSurviveIn(TerrainType env) {
        return survivableEnvironments.contains(env);
    }

    public boolean isOptimal(float envValue) {
        return optimalRange.contains(envValue);
    }

    public boolean canTolerate(float envValue) {
        return toleranceRange.contains(envValue);
    }

    public boolean isLethal(float envValue) {
        return lethalLimit.contains(envValue);
    }

    // ----------------------------------------------------------
    //  Getters
    // ----------------------------------------------------------

    public List<TerrainType> getSurvivableEnvironments() {
        return survivableEnvironments;
    }

    public ValueRange getOptimalRange() {
        return optimalRange;
    }

    public ValueRange getToleranceRange() {
        return toleranceRange;
    }

    public ValueRange getLethalLimit() {
        return lethalLimit;
    }
}
