package wildlife.model.environment.component;

import wildlife.model.environment.enums.Season;
import wildlife.model.environment.enums.WeatherType;
import wildlife.util.AppConfig;

import java.util.Random;

/**
 * Component quản lý thời gian, mùa và thời tiết của một môi trường.
 *
 * Trách nhiệm (SRP):
 * - Đếm tick và quy đổi sang chu kỳ ngày/đêm.
 * - Xác định mùa hiện tại dựa trên tổng số tick đã qua.
 * - Cập nhật thời tiết ngẫu nhiên theo xác suất của từng mùa.
 * - Cung cấp hệ số nhân (multiplier) để các component khác điều chỉnh hành vi.
 */
public class TimeComponent {

    // ----------------------------------------------------------------
    //  Hằng số xác suất thời tiết theo mùa
    // ----------------------------------------------------------------

    /** Xác suất xuất hiện mưa trong mùa sinh sản */
    private static final float BREEDING_RAIN_CHANCE   = AppConfig.getFloat("environment.time.breeding.rainChance");
    /** Xác suất xuất hiện hạn trong mùa hạn hán */
    private static final float DROUGHT_DROUGHT_CHANCE = AppConfig.getFloat("environment.time.drought.droughtChance");
    /** Xác suất xuất hiện mưa trong mùa bình thường */
    private static final float NORMAL_RAIN_CHANCE     = AppConfig.getFloat("environment.time.normal.rainChance");
    /** Xác suất xuất hiện hạn trong mùa bình thường */
    private static final float NORMAL_DROUGHT_CHANCE  = AppConfig.getFloat("environment.time.normal.droughtChance");

    // ----------------------------------------------------------------
    //  Trạng thái nội tại
    // ----------------------------------------------------------------

    /** Tổng số tick đã trôi qua kể từ khi môi trường khởi tạo */
    private int currentTick;

    /** Số tick tương đương một chu kỳ ngày-đêm đầy đủ */
    private final int ticksPerDayCycle;

    /** Số tick để chuyển sang một mùa mới */
    private final int ticksPerSeason;

    /** Mùa hiện tại */
    private Season currentSeason;

    /** Thời tiết hiện tại */
    private WeatherType currentWeather;
    // Các chỉ số vật lý của môi trường
    private float temperature; // Nhiệt độ (Độ C)
    private float humidity;    // Độ ẩm (0.0 đến 100.0)
    private float lightLevel;  // Ánh sáng (0.0 đến 1.0)
    private final Random random;

    // ----------------------------------------------------------------
    //  Constructor
    // ----------------------------------------------------------------

    /**
     * @param ticksPerDayCycle  số tick cho 1 chu kỳ ngày-đêm (VD: 24)
     * @param ticksPerSeason    số tick cho 1 mùa (VD: 240)
     */
    public TimeComponent(int ticksPerDayCycle, int ticksPerSeason) {
        this.ticksPerDayCycle = ticksPerDayCycle;
        this.ticksPerSeason   = ticksPerSeason;
        this.currentTick      = 0;
        this.currentSeason    = Season.NORMAL;
        this.currentWeather   = WeatherType.NORMAL;
        this.random           = new Random();
    }

    // ----------------------------------------------------------------
    //  Phương thức cập nhật
    // ----------------------------------------------------------------

    /**
     * Tiến tick: cập nhật mùa và thời tiết theo chu kỳ.
     * Được gọi bởi Environment mỗi vòng lặp chính.
     *
     * @param tick tick hiện tại từ hệ thống (dùng để đồng bộ nếu cần)
     */
    public void advance(int tick) {
        this.currentTick = tick;
        updateSeason();
        updateWeatherBySeason();
        updateClimateMetrics();
    }

    /**
     * Cập nhật mùa dựa theo số tick đã qua.
     * Thứ tự mùa: NORMAL → BREEDING → DROUGHT →NORMAL  (lặp lại)
     */
    private void updateSeason() {
        // Lấy vị trí trong chu kỳ 3 mùa
        int cycleLength = ticksPerSeason * 3;
        int posInCycle  = currentTick % cycleLength;

        if (posInCycle < ticksPerSeason) {
            currentSeason = Season.NORMAL;
        } else if (posInCycle < ticksPerSeason * 2) {
            currentSeason = Season.BREEDING;
        } else {
            currentSeason = Season.DROUGHT;
        }
    }

    /**
     * Cập nhật thời tiết ngẫu nhiên dựa trên xác suất của mùa hiện tại.
     * Chỉ thay đổi thời tiết mỗi nửa chu kỳ ngày để tránh dao động liên tục.
     */
    public void updateWeatherBySeason() {
        // Chỉ xét thay đổi thời tiết định kỳ (mỗi nửa ngày)
        if (currentTick % (ticksPerDayCycle / 2) != 0) return;

        float roll = random.nextFloat(); // [0.0, 1.0)

        switch (currentSeason) {
            case BREEDING -> {
                if (roll < BREEDING_RAIN_CHANCE)          currentWeather = WeatherType.RAIN;
                else                                       currentWeather = WeatherType.NORMAL;
            }
            case DROUGHT -> {
                if (roll < DROUGHT_DROUGHT_CHANCE)         currentWeather = WeatherType.DROUGHT;
                else                                       currentWeather = WeatherType.NORMAL;
            }
            default -> { // NORMAL
                if (roll < NORMAL_RAIN_CHANCE)             currentWeather = WeatherType.RAIN;
                else if (roll < NORMAL_RAIN_CHANCE + NORMAL_DROUGHT_CHANCE)
                    currentWeather = WeatherType.DROUGHT;
                else                                       currentWeather = WeatherType.NORMAL;
            }
        }
    }

    // ----------------------------------------------------------------
    //  Truy vấn trạng thái
    // ----------------------------------------------------------------

    /**
     * Kiểm tra có đang là ban ngày không.
     * Ban ngày: nửa đầu của chu kỳ ngày-đêm.
     *
     * @return true nếu đang là ban ngày
     */
    public boolean isDaytime() {
        return (currentTick % ticksPerDayCycle) < (ticksPerDayCycle / 2);
    }

    /**
     * Trả về hệ số nhân tổng hợp của mùa + thời tiết.
     * Hệ số này được dùng bởi SurvivalStatsComponent để tính tốc độ đói/khát.
     *
     * Quy ước: > 1.0 = khắc nghiệt hơn, < 1.0 = dễ chịu hơn.
     *
     * @return hệ số nhân môi trường
     */
    public float getSeasonMultiplier() {
        float multiplier = 1.0f;

        // Điều chỉnh theo mùa
        multiplier *= switch (currentSeason) {
            case BREEDING -> AppConfig.getFloat("environment.multiplier.season.breeding");
            case DROUGHT  -> AppConfig.getFloat("environment.multiplier.season.drought");
            default       -> 1.0f;
        };

        // Điều chỉnh thêm theo thời tiết
        multiplier *= switch (currentWeather) {
            case RAIN    -> AppConfig.getFloat("environment.multiplier.weather.rain");
            case DROUGHT -> AppConfig.getFloat("environment.multiplier.weather.drought");
            default      -> 1.0f;
        };

        return multiplier;
    }

    // ----------------------------------------------------------------
    //  Getters
    // ----------------------------------------------------------------

    public int getCurrentTick()          { return currentTick; }
    public Season getCurrentSeason()     { return currentSeason; }
    public WeatherType getCurrentWeather() { return currentWeather; }
    public int getTicksPerDayCycle()     { return ticksPerDayCycle; }
    public int getTicksPerSeason()       { return ticksPerSeason; }

    public float getTemperature() { return temperature; }
    public float getHumidity() { return humidity; }
    public float getLightLevel() { return lightLevel; }
    /**
     * Cập nhật các chỉ số vật lý dựa trên sự giao thoa của Thời gian, Mùa và Thời tiết.
     * Nhiệt độ thay đổi liên tục theo tiến trình ngày-đêm (đường cong sin),
     * không cố định một giá trị duy nhất.
     */
    private void updateClimateMetrics() {
        // --- 1. Ánh sáng ---
        float dayLight   = AppConfig.getFloat("environment.light.day");
        float nightLight  = AppConfig.getFloat("environment.light.night");
        this.lightLevel = isDaytime() ? dayLight : nightLight;

        // --- 2. Nhiệt độ: biến thiên liên tục theo chu kỳ ngày-đêm (sin) ---
        // Lấy nhiệt độ cơ bản và biên độ dao động từ config theo từng mùa
        float baseTemp = AppConfig.getFloat("environment.climate." + currentSeason.name().toLowerCase() + ".baseTemp");
        float tempAmplitude = AppConfig.getFloat("environment.climate." + currentSeason.name().toLowerCase() + ".tempAmplitude");

        // Tính vị trí trong chu kỳ ngày (0.0 ~ 1.0)
        float dayProgress = (float) (currentTick % ticksPerDayCycle) / ticksPerDayCycle;

        // Dùng hàm sin để nhiệt độ đạt cực đại vào giữa ban ngày (0.25 chu kỳ)
        // và cực tiểu vào giữa ban đêm (0.75 chu kỳ)
        // sin(2π * (progress - 0.25)) → đỉnh tại progress=0.5 (giữa ngày), đáy tại progress=0.0 (nửa đêm)
        float tempOffset = (float) Math.sin(2 * Math.PI * (dayProgress - 0.25));
        float currentTemp = baseTemp + tempAmplitude * tempOffset;

        // Yếu tố tinh chỉnh thêm từ thời tiết
        float rainTempDelta    = AppConfig.getFloat("environment.climate.weather.rain.tempDelta");
        float droughtTempDelta = AppConfig.getFloat("environment.climate.weather.drought.tempDelta");
        if (currentWeather == WeatherType.RAIN)    currentTemp += rainTempDelta;
        if (currentWeather == WeatherType.DROUGHT)  currentTemp += droughtTempDelta;

        this.temperature = currentTemp;

        // --- 3. Độ ẩm: phụ thuộc mùa, bị thời tiết ghi đè ---
        float baseHumidity = AppConfig.getFloat("environment.climate." + currentSeason.name().toLowerCase() + ".humidity");

        if (currentWeather == WeatherType.RAIN) {
            baseHumidity = AppConfig.getFloat("environment.climate.weather.rain.humidity");
        } else if (currentWeather == WeatherType.DROUGHT) {
            baseHumidity = AppConfig.getFloat("environment.climate.weather.drought.humidity");
        }

        this.humidity = baseHumidity;
    }
}