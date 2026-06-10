package wildlife.view.renderer;

/**
 * Registry that maps species/entity names to their corresponding textures.
 * <p>
 * External modules implement this interface and inject it into the
 * {@link Renderer}. This keeps the renderer decoupled from asset-loading
 * and file-system concerns.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 *   TextureRegistry registry = new MyTextureAtlas("assets/sprites/");
 *   ITexture wolfTex = registry.getTexture("Wolf");
 * }</pre>
 */
public interface TextureRegistry {

    /**
     * Retrieve the texture associated with the given name.
     *
     * @param name species or entity identifier (e.g. "Wolf", "Rabbit", "Grass")
     * @return the corresponding {@link ITexture}, or a fallback/default texture
     *         if the name is not registered
     */
    ITexture getTexture(String name);

    /**
     * Check whether a texture is registered for the given name.
     *
     * @param name species or entity identifier
     * @return {@code true} if a texture exists for this name
     */
    boolean hasTexture(String name);
}
