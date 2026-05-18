package wildlife.model.dto;

import wildlife.model.organism.OrganismState;

/**
 * Gói thông tin phục vụ cho View
 */
public class RenderData {
    public final String entityId;
    public final String speciesName;
    public final float x;
    public final float y;
    public final OrganismState state;

    public RenderData(String entityId, String speciesName, float x, float y, OrganismState state) {
        this.entityId = entityId;
        this.speciesName = speciesName;
        this.x = x;
        this.y = y;
        this.state = state;
    }
}
