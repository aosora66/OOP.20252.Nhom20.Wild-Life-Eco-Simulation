package wildlife.model.brain;

import wildlife.model.environment.Environment;
import wildlife.model.organism.Organism;
import wildlife.util.SurvivalStrategy;

public class PassiveStrategy extends AbstractSurvivalStrategy {

    private final float hungerSearchThreshold;
    private final float thirstSearchThreshold;

    public PassiveStrategy(float stepSize, float sightRadius, float eatRange,
                           float hungerSearchThreshold, float thirstSearchThreshold) {
        super(stepSize, sightRadius, eatRange);
        this.hungerSearchThreshold = hungerSearchThreshold;
        this.thirstSearchThreshold = thirstSearchThreshold;
    }

    @Override
    public void execute(Organism self, Environment env) {
        float seasonMult = env.getTime().getSeasonMultiplier();
        self.getStats().applyHungerThirstDecay(seasonMult, seasonMult);

        if (self.getStats().checkHpThreshold()) {
            self.decreaseHp(0);
            return;
        }

        if (self.getStats().getThirstLevel() >= thirstSearchThreshold) {
            findNearestFood(self, env, true).ifPresentOrElse(
                water -> {
                    moveToward(self, water.position(), env);
                    if (self.getPosition().distanceTo(water.position()) <= attackRange) {
                        self.getStats().consume(water.nutritionalValue(), true);
                        env.getResources().consume(water);
                    }
                },
                () -> wander(self, env)
            );
            return;
        }

        if (self.getStats().getHungerLevel() >= hungerSearchThreshold) {
            findNearestFood(self, env, false).ifPresentOrElse(
                food -> {
                    moveToward(self, food.position(), env);
                    if (self.getPosition().distanceTo(food.position()) <= attackRange) {
                        self.getStats().consume(food.nutritionalValue(), false);
                        env.getResources().consume(food);
                    }
                },
                () -> wander(self, env)
            );
            return;
        }

        wander(self, env);
    }
}