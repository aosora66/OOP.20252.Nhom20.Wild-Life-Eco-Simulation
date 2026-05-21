package wildlife.model.brain;

import wildlife.model.environment.Environment;
import wildlife.model.organism.Organism;
import wildlife.util.SurvivalStrategy;

import java.util.Optional;

public class ScaredStrategy extends AbstractSurvivalStrategy {

    private final String predatorSpecies;
    private final int    sprintSteps;

    public ScaredStrategy(float stepSize, float fearRadius,
                          String predatorSpecies, int sprintSteps) {
        super(stepSize, fearRadius, 0f);
        this.predatorSpecies = predatorSpecies;
        this.sprintSteps     = Math.max(1, sprintSteps);
    }

    @Override
    public void execute(Organism self, Environment env) {
        float seasonMult = env.getTime().getSeasonMultiplier();
        self.getStats().applyHungerThirstDecay(seasonMult, seasonMult);

        if (self.getStats().checkHpThreshold()) {
            self.decreaseHp(0);
            return;
        }

        Optional<Organism> threat = findNearestBySpecies(self, env, predatorSpecies);
        threat.ifPresentOrElse(
            predator -> {
                for (int i = 0; i < sprintSteps; i++) {
                    if (self.isAlive()) {
                        moveAwayFrom(self, predator.getPosition(), env);
                    }
                }
            },
            () -> wander(self, env)
        );
    }
}