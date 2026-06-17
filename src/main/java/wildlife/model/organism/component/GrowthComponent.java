package wildlife.model.organism.component;

/**
 * Thành phần liên quan tới Tăng Trưởng
 */
public class GrowthComponent {
    private float currentAge;
    private final float maxAge;
    private float currentSize;
    private final float maxSize;

    // Tỉ lệ vòng đời do từng loài tự quyết định
    private final float maturityRatio; // Ví dụ: 0.2 (Trưởng thành khi đạt 20% maxAge)
    private final float decayAgeRatio; // Ví dụ: 0.6 (Bắt đầu lão hóa khi đạt 60% maxAge)

    public GrowthComponent(float maxAge, float maxSize,
                           float maturityRatio, float decayAgeRatio) {
        this(maxAge, maxSize, maturityRatio, decayAgeRatio, 0f);
    }

    /**
     * Cho phép khởi tạo với tuổi ban đầu > 0 — dùng để rải đều tuổi đàn ban đầu,
     * tránh hiện tượng cả đàn cùng già và chết đồng loạt (boom–bust).
     */
    public GrowthComponent(float maxAge, float maxSize,
                           float maturityRatio, float decayAgeRatio, float startAge) {
        this.maxAge            = maxAge;
        this.maxSize           = maxSize;
        this.maturityRatio     = maturityRatio;
        this.decayAgeRatio     = decayAgeRatio;
        this.currentAge        = Math.max(0f, startAge);
        float maturityAge      = maxAge * maturityRatio;
        this.currentSize       = (this.currentAge >= maturityAge)
                ? maxSize
                : Math.max(1f, maxSize * (this.currentAge / maturityAge));
    }

    // Tăng kích thước khi chưa tới maxsize
    public void computeGrowth() {
        currentAge++;
        float maturityAge = maxAge * maturityRatio;
        if (currentAge <= maturityAge) {
            currentSize = maxSize * (currentAge / maturityAge);
        }
    }

    // Kiểm tra đã trưởng thành chưa
    public boolean isAdult() {
        return currentAge >= (maxAge * maturityRatio);
    }

    // Kiểm tra có đang ở tuổi lão hóa không
    public boolean isDecaying() {
        return currentAge >= (maxAge * decayAgeRatio);
    }

    public float getCurrentAge()  { return currentAge; }
    public float getMaxAge()      { return maxAge; }
    public float getCurrentSize() { return currentSize; }
    public float getMaxSize()     { return maxSize; }
}
