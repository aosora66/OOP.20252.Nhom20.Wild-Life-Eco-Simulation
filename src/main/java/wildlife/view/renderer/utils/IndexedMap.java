package wildlife.view.renderer.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
public class IndexedMap<K, V> {

    private final HashMap<K, Integer> indexMap = new HashMap<>();
    private final ArrayList<V> values = new ArrayList<>();
    public V get(K key) {
        Integer idx = indexMap.get(key);
        return idx != null ? values.get(idx) : null;
    }
    public boolean containsKey(K key) {
        return indexMap.containsKey(key);
    }
    public void put(K key, V value) {
        Integer idx = indexMap.get(key);
        if (idx != null) {
            values.set(idx, value);
        } else {
            indexMap.put(key, values.size());
            values.add(value);
        }
    }

    /**
     * @param key   key của phần tử cần tìm
     * @param factory   hàm generate ra phần tử mới trong trường hợp trong map chưa có phần tử nào mang key {@link key}
     * @return ô nhớ đang chứa phần tử có key là {@link key}, nếu chưa tồn tại, khởi tạo ô nhớ đầu tiên và trả về ô nhớ đó
     */
    public V computeIfAbsent(K key, Function<K, V> factory) {
        Integer idx = indexMap.get(key);
        if (idx != null) {
            return values.get(idx);
        }
        // khởi tạo phần tử V đầu tiên trong bảng values mang tham số khởi tạo là key
        V value = factory.apply(key);
        indexMap.put(key, values.size());
        values.add(value);
        return value;
    }

    public List<V> values() {
        return values;
    }
    public void clear() {
        indexMap.clear();
        values.clear();
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public int size() {
        return values.size();
    }
}
