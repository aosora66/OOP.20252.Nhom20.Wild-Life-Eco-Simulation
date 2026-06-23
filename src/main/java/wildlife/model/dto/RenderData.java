package wildlife.model.dto;

import wildlife.model.organism.Organism;
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
    public final int layer;
    public final boolean goWest;

    private float r = 1.0f;
    private float g = 1.0f;
    private float b = 1.0f;
    private float a = 1.0f;

    public RenderData(String entityId, String speciesName, float x, float y, OrganismState state, int layer,  boolean goWest) {
        this.entityId = entityId;
        this.speciesName = speciesName;
        this.x = x;
        this.y = y;
        this.state = state;
        this.layer = layer;
        this.goWest = goWest;
    }

    public RenderData(Organism organism, int layer) {
        this.entityId = organism.getId();
        this.speciesName = organism.getSpeciesName();
        this.x = organism.getPosition().getX();
        this.y = organism.getPosition().getY();
        this.state = organism.getState();
        this.layer = layer;
        this.goWest = organism.isGoingWest();
    }

    public float getR() { return r; }
    public float getG() { return g; }
    public float getB() { return b; }
    public float getA() { return a; }

    public void setR(float r) { this.r = r; }
    public void setG(float g) { this.g = g; }
    public void setB(float b) { this.b = b; }
    public void setA(float a) { this.a = a; }

    /** Convenience setter for all four channels at once. */
    public void setColor(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }
}
