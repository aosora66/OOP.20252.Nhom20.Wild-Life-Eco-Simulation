package wildlife.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Singleton đọc toàn bộ setting.properties.
 * Mọi class lấy hằng số qua AppConfig.get("key") hoặc AppConfig.getFloat("key").
 */
public class AppConfig {

    private static final Properties props = new Properties();

    static {
        try (InputStream in = AppConfig.class.getClassLoader()
                .getResourceAsStream("config/setting.properties")) {
            if (in == null) {
                throw new RuntimeException("Không tìm thấy config/setting.properties");
            }
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Lỗi đọc setting.properties: " + e.getMessage(), e);
        }
    }

    private AppConfig() {}

    public static String get(String key) {
        return props.getProperty(key);
    }

    public static float getFloat(String key) {
        String val = props.getProperty(key);
        if (val == null) throw new RuntimeException("Thiếu config key: " + key);
        return Float.parseFloat(val.trim());
    }

    public static int getInt(String key) {
        String val = props.getProperty(key);
        if (val == null) throw new RuntimeException("Thiếu config key: " + key);
        return Integer.parseInt(val.trim());
    }
}
