package wildlife.model.brain;

import wildlife.model.environment.Environment;
import wildlife.model.environment.dto.FoodItem;
import wildlife.model.organism.Organism;
import wildlife.util.SurvivalStrategy;
import wildlife.util.Vector2D;

import java.util.Comparator;
import java.util.Optional;
import java.util.Random;

public abstract class AbstractSurvivalStrategy implements SurvivalStrategy {

    private static final Random RNG = new Random();

    protected final float stepSize;
    protected final float sightRadius;
    protected final float attackRange;

    protected AbstractSurvivalStrategy(float stepSize, float sightRadius, float attackRange) {
        this.stepSize    = stepSize;
        this.sightRadius = sightRadius;
        this.attackRange = attackRange;
    }

    protected void wander(Organism self, Environment env) {
        float angle = RNG.nextFloat() * 2f * (float) Math.PI;
        Vector2D next = new Vector2D(
            self.getPosition().getX() + (float) Math.cos(angle) * stepSize,
            self.getPosition().getY() + (float) Math.sin(angle) * stepSize
        );
        if (env.isPositionPassable(next, self.getSpeciesName())) {
            self.setPosition(next);
        }
    }

    protected void moveToward(Organism self, Vector2D target, Environment env) {
        Vector2D pos = self.getPosition();
        float dist   = pos.distanceTo(target);
        if (dist < 0.001f) return;

        float step = Math.min(stepSize, dist);
        float dx = (target.getX() - pos.getX()) / dist;
        float dy = (target.getY() - pos.getY()) / dist;

        Vector2D next = new Vector2D(
            pos.getX() + dx * step,
            pos.getY() + dy * step
        );
        if (env.isPositionPassable(next, self.getSpeciesName())) {
            self.setPosition(next);
        }
    }

    protected void moveAwayFrom(Organism self, Vector2D threat, Environment env) {
        Vector2D pos = self.getPosition();
        float dist   = pos.distanceTo(threat);
        if (dist < 0.001f) { wander(self, env); return; }

        float dx = (pos.getX() - threat.getX()) / dist;
        float dy = (pos.getY() - threat.getY()) / dist;

        Vector2D next = new Vector2D(
            pos.getX() + dx * stepSize,
            pos.getY() + dy * stepSize
        );
        if (env.isPositionPassable(next, self.getSpeciesName())) {
            self.setPosition(next);
        } else {
            wander(self, env);
        }
    }

    protected Optional<Organism> findNearestBySpecies(Organism self, Environment env,
                                                       String targetSpecies) {
        return env.getRegistry()
                  .findNear(self.getPosition(), sightRadius)
                  .stream()
                  .filter(o -> !o.getId().equals(self.getId())
                            && o.getSpeciesName().equals(targetSpecies))
                  .min(Comparator.comparingDouble(
                      o -> o.getPosition().distanceTo(self.getPosition())));
    }

    protected Optional<FoodItem> findNearestFood(Organism self, Environment env,
                                                  boolean wantWater) {
        return env.getResources()
                  .getFoodNear(self.getPosition(), sightRadius)
                  .stream()
                  .filter(f -> f.isWater() == wantWater)
                  .min(Comparator.comparingDouble(
                      f -> f.position().distanceTo(self.getPosition())));
    }
}