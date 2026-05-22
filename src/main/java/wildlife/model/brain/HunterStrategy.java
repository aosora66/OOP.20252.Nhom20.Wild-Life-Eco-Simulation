package wildlife.model.brain;

import wildlife.model.environment.Environment;
import wildlife.model.organism.Organism;
import wildlife.util.SurvivalStrategy;

import java.util.Optional;

public class HunterStrategy extends AbstractSurvivalStrategy {

    private final String preySpecies;
    private final float  attackDamage;
    private final float  hungerSearchThreshold;

    public HunterStrategy(float stepSize, float sightRadius, float attackRange,
                          float attackDamage, float hungerSearchThreshold,
                          String preySpecies) {
        super(stepSize, sightRadius, attackRange);
        this.attackDamage          = attackDamage;
        this.hungerSearchThreshold = hungerSearchThreshold;
        this.preySpecies           = preySpecies;
    }

    @Override
    public void execute(Organism self, Environment env) {
        float seasonMult = env.getTime().getSeasonMultiplier();
        self.getStats().applyHungerThirstDecay(seasonMult, seasonMult);

        if (self.getStats().checkHpThreshold()) {
            self.decreaseHp(0);
            return;
        }

        if (self.getStats().getHungerLevel() >= hungerSearchThreshold) {
            Optional<Organism> prey = findNearestBySpecies(self, env, preySpecies);
            prey.ifPresentOrElse(
                target -> {
                    float dist = self.getPosition().distanceTo(target.getPosition());
                    if (dist <= attackRange) {
                        target.decreaseHp(attackDamage);
                    } else {
                        moveToward(self, target.getPosition(), env);
                    }
                },
                () -> wander(self, env)
            );
        } else {
            wander(self, env);
        }
    }
}