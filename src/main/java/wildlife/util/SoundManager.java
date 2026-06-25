package wildlife.util;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý âm thanh cho toàn bộ hệ thống mô phỏng.
 * Hỗ trợ phát nhạc nền lặp lại (Ambiance) và âm thanh hiệu ứng (Sound Effects).
 */
public class SoundManager {

    // Cache các kênh nhạc nền (Ví dụ: "TIME_AMBIANCE" -> Clip, "SEASON_AMBIANCE" -> Clip)
    private static final Map<String, Clip> ambianceChannels = new HashMap<>();

    // Rate-limiting cho các hiệu ứng phát liên tục (như tiếng bước chân)
    private static final Map<String, Long> lastPlayedTime = new ConcurrentHashMap<>();

    // Tên file cache để check (nếu đang phát thì không khởi động lại clip)
    private static final Map<String, String> currentAmbianceFiles = new HashMap<>();

    // Thuộc tính theo dõi đối tượng focus để phát tiếng bước chân
    private static String focusedAnimalId = null;
    private static Vector2D focusPosition = new Vector2D(0, 0);

    public static void setFocusedAnimalId(String id) {
        focusedAnimalId = id;
    }

    public static String getFocusedAnimalId() {
        return focusedAnimalId;
    }

    public static void setFocusPosition(Vector2D pos) {
        focusPosition = pos;
    }

    public static Vector2D getFocusPosition() {
        return focusPosition;
    }

    /**
     * Kiểm tra xem một con vật có phải là con vật được focus (hoặc gần vị trí focus nhất) hay không.
     */
    public static boolean isFocused(wildlife.model.organism.animal.Animal animal, wildlife.model.environment.Environment env) {
        if (animal == null || env == null) return false;

        // Nếu có id cụ thể được set
        if (focusedAnimalId != null) {
            return animal.getId().equals(focusedAnimalId);
        }

        // Nếu không có id cụ thể, tìm con vật gần vị trí focus nhất
        java.util.List<wildlife.model.organism.animal.Animal> animals = env.getRegistry().getAllAlive(wildlife.model.organism.animal.Animal.class);
        if (animals.isEmpty()) return false;

        wildlife.model.organism.animal.Animal nearest = null;
        double minDist = Double.MAX_VALUE;
        Vector2D fPos = (focusPosition != null) ? focusPosition : new Vector2D(0, 0);

        for (wildlife.model.organism.animal.Animal a : animals) {
            double d = a.getPosition().distanceTo(fPos);
            if (d < minDist) {
                minDist = d;
                nearest = a;
            }
        }

        return nearest != null && animal.getId().equals(nearest.getId());
    }

    /**
     * Helper method to get AudioInputStream and convert it to a supported 16-bit PCM_SIGNED format if needed.
     */
    private static AudioInputStream getAudioStream(String fileName) throws Exception {
        InputStream is = SoundManager.class.getResourceAsStream("/sounds/" + fileName);
        if (is == null) {
            throw new java.io.FileNotFoundException("Sound file not found in resources: " + fileName);
        }
        InputStream bufferedIn = new BufferedInputStream(is);
        AudioInputStream sourceStream = AudioSystem.getAudioInputStream(bufferedIn);
        AudioFormat sourceFormat = sourceStream.getFormat();

        // Convert format if sample size is greater than 16 bits or not PCM_SIGNED
        if (sourceFormat.getSampleSizeInBits() > 16 || sourceFormat.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
            AudioFormat targetFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    sourceFormat.getSampleRate(),
                    16,
                    sourceFormat.getChannels(),
                    sourceFormat.getChannels() * 2,
                    sourceFormat.getSampleRate(),
                    false // little-endian
            );
            if (AudioSystem.isConversionSupported(targetFormat, sourceFormat)) {
                return AudioSystem.getAudioInputStream(targetFormat, sourceStream);
            }
        }
        return sourceStream;
    }

    /**
     * Phát một file âm thanh nền và lặp liên tục trên một kênh cụ thể.
     * @param channel Kênh phát (VD: "TIME", "SEASON")
     * @param fileName Tên file trong resources/sounds/
     */
    public static void playAmbiance(String channel, String fileName) {
        // Tránh phát lại nếu file đang chạy trên kênh này
        if (fileName.equals(currentAmbianceFiles.get(channel))) {
            return;
        }

        stopAmbiance(channel);

        try {
            AudioInputStream audioStream = getAudioStream(fileName);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            clip.start();

            ambianceChannels.put(channel, clip);
            currentAmbianceFiles.put(channel, fileName);

        } catch (Exception e) {
            System.err.println("Error playing ambiance " + fileName + ": " + e.getMessage());
        }
    }

    /**
     * Dừng âm nền trên một kênh cụ thể.
     */
    public static void stopAmbiance(String channel) {
        Clip clip = ambianceChannels.get(channel);
        if (clip != null && clip.isRunning()) {
            clip.stop();
            clip.close();
        }
        ambianceChannels.remove(channel);
        currentAmbianceFiles.remove(channel);
    }

    /**
     * Phát một âm thanh hiệu ứng 1 lần.
     * @param fileName Tên file (VD: "GunFire.wav")
     */
    public static void playSoundEffect(String fileName) {
        playSoundEffect(fileName, 1.0f);
    }

    /**
     * Phát một âm thanh hiệu ứng với âm lượng tùy chỉnh.
     */
    public static void playSoundEffect(String fileName, float volume) {
        try {
            AudioInputStream audioStream = getAudioStream(fileName);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);

            // Chỉnh âm lượng nếu có hỗ trợ
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                // Giới hạn giá trị nằm trong range
                float min = gainControl.getMinimum();
                float max = gainControl.getMaximum();
                float range = max - min;
                float gain = (range * volume) + min;
                gainControl.setValue(gain);
            }

            clip.start();

            // Lắng nghe sự kiện kết thúc để tự động đóng clip, giải phóng bộ nhớ
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    clip.close();
                }
            });

        } catch (Exception e) {
            System.err.println("Error playing sound effect " + fileName + ": " + e.getMessage());
        }
    }

    /**
     * Phát âm thanh nhưng có giới hạn thời gian (cooldown) để tránh spam.
     * Dùng cho tiếng bước chân của hàng trăm con vật.
     */
    public static void playSoundEffectWithCooldown(String fileName, long cooldownMs, float volume) {
        long now = System.currentTimeMillis();
        long lastTime = lastPlayedTime.getOrDefault(fileName, 0L);
        if (now - lastTime >= cooldownMs) {
            playSoundEffect(fileName, volume);
            lastPlayedTime.put(fileName, now);
        }
    }

    /**
     * Phát 2 hiệu ứng âm thanh liên tiếp nhau (VD: Lên đạn -> Bắn súng).
     * Sẽ block một Thread tạm thời để chờ (do đây là hiệu ứng tick, nên dùng Thread tách biệt).
     */
    public static void playSequentialSoundEffects(String firstFile, String secondFile) {
        new Thread(() -> {
            try {
                AudioInputStream audioStream1 = getAudioStream(firstFile);
                Clip clip1 = AudioSystem.getClip();
                clip1.open(audioStream1);
                long durationMs = clip1.getMicrosecondLength() / 1000;

                clip1.start();

                // Đợi file 1 phát xong
                Thread.sleep(durationMs);
                clip1.close();

                // Phát ngay file 2
                playSoundEffect(secondFile);

            } catch (Exception e) {
                System.err.println("Error playing sequential sounds: " + e.getMessage());
            }
        }).start();
    }
}