package wildlife.view.renderer.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

/**
 * A key→value map backed by a {@link HashMap}&lt;K, Integer&gt; index and a
 * contiguous {@link ArrayList}&lt;V&gt; for cache-friendly sequential iteration.
 * <p>
 * <b>Why this layout?</b>  The values live in a single {@code Object[]} inside
 * the {@code ArrayList}, so iterating over them walks a dense, contiguous
 * region of memory — much better for the CPU cache than chasing
 * {@code HashMap.Entry} pointers scattered across the heap.
 * <p>
 * Key lookup remains O(1) via the index map.
 *
 * <h3>Thread safety</h3>
 * Not thread-safe.  External synchronisation is required if instances are
 * shared between threads.
 *
 * @param <K> key type (must implement {@code hashCode}/{@code equals})
 * @param <V> value type
 */
public class IndexedMap<K, V> {

    private final HashMap<K, Integer> indexMap = new HashMap<>();
    private final ArrayList<V> values = new ArrayList<>();

    // ──────────────────────────────────────────────────────────────
    //  Lookup
    // ──────────────────────────────────────────────────────────────

    /**
     * @return the value mapped to {@code key}, or {@code null} if absent
     */
    public V get(K key) {
        Integer idx = indexMap.get(key);
        return idx != null ? values.get(idx) : null;
    }

    /**
     * @return {@code true} if the map contains a mapping for {@code key}
     */
    public boolean containsKey(K key) {
        return indexMap.containsKey(key);
    }

    // ──────────────────────────────────────────────────────────────
    //  Mutation
    // ──────────────────────────────────────────────────────────────

    /**
     * Insert a new key→value mapping.  If the key already exists its value
     * is overwritten in-place (the array position does not change).
     */
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
     * If {@code key} is absent, create a value with {@code factory}, store it,
     * and return it.  If present, return the existing value.
     * <p>
     * This is the primary insertion path for the renderer — it groups
     * positions by species, creating a new list only on first encounter.
     *
     * @param key     the lookup key
     * @param factory called at most once to produce the initial value
     * @return the existing or newly created value
     */
    public V computeIfAbsent(K key, Function<K, V> factory) {
        Integer idx = indexMap.get(key);
        if (idx != null) {
            return values.get(idx);
        }
        V value = factory.apply(key);
        indexMap.put(key, values.size());
        values.add(value);
        return value;
    }

    // ──────────────────────────────────────────────────────────────
    //  Iteration (cache-friendly — walks the contiguous ArrayList)
    // ──────────────────────────────────────────────────────────────

    /**
     * @return an <b>unmodifiable view</b> of the contiguous value list.
     *         Iterating this list walks a dense {@code Object[]} in memory.
     */
    public List<V> values() {
        return values;
    }

    // ──────────────────────────────────────────────────────────────
    //  Lifecycle
    // ──────────────────────────────────────────────────────────────

    /**
     * Remove all mappings.  The backing array capacity is retained so that
     * the next frame doesn't need to re-allocate.
     */
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
