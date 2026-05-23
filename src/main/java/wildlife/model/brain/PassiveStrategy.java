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
        // đi tìm nước xung quanh nếu đang khát
        if (self.getStats().getThirstLevel() >= thirstSearchThreshold) {
            findNearestFood(self, env, true).ifPresentOrElse(       // quét nước xung quanh
                water -> {
                    moveToward(self, water.position(), env);                   // di chuyển về phía nước gần nhất
                    if (self.getPosition().distanceTo(water.position()) <= attackRange) {
                        self.getStats().consume(water.nutritionalValue(), true);
                        env.getResources().consume(water);
                    }
                },
                () -> wander(self, env)
            );
            return;
        }

        //đi tìm thức ăn nếu đang đói
        if (self.getStats().getHungerLevel() >= hungerSearchThreshold) {
            findNearestFood(self, env, false).ifPresentOrElse(      // quét thức ăn xung quanh
                food -> {
                    moveToward(self, food.position(), env);     // di chuyển về phía thức ăn phù hợp nhất
                    if (self.getPosition().distanceTo(food.position()) <= attackRange) {    // trong bán kính ăn được thì ăn
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