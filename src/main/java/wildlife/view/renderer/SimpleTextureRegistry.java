package wildlife.view.renderer;

import java.util.HashMap;
import java.util.Map;

/**
 * Concrete implementation of TextureRegistry using an internal HashMap.
 * Allows dynamic registering of standard and custom textures.
 */
public class SimpleTextureRegistry implements TextureRegistry {
    private final Map<String, ITexture> registry = new HashMap<>();

    public void register(String name, ITexture texture) {
        registry.put(name, texture);
    }

    @Override
    public ITexture getTexture(String name) {
        return registry.get(name);
    }

    @Override
    public boolean hasTexture(String name) {
        return registry.containsKey(name);
    }

    /**
     * Clear all registered textures and dispose of them if they support disposal.
     */
    public void clear() {
        for (ITexture texture : registry.values()) {
            if (texture instanceof SimpleTexture) {
                ((SimpleTexture) texture).dispose();
            }
        }
        registry.clear();
    }
}