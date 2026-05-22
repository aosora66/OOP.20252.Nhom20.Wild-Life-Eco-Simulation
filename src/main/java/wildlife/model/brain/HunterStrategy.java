package wildlife.model.brain;

import wildlife.model.environment.Environment;
import wildlife.model.environment.dto.FoodItem;
import wildlife.model.organism.Organism;
import wildlife.util.AppConfig;
import wildlife.util.SurvivalStrategy;

import java.util.List;
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
        // kiểm tra nếu đói thì tìm con mồi xung quanh
        if (self.getStats().getHungerLevel() >= hungerSearchThreshold) {
            Optional<Organism> prey = findNearestBySpecies(self, env, preySpecies);
            prey.ifPresentOrElse(
                target -> {
                    float dist = self.getPosition().distanceTo(target.getPosition());
                    if (dist <= attackRange) {
                        target.decreaseHp(attackDamage);
                        // If prey died immediately, convert to meat and eat right away
                        if (!target.isAlive()) {
                            float nutrition = target.getStats().getNutritionalValue();
                            // convert corpse to meat immediately (uses configured expiry)
                            env.getResources().convertDeadToMeat(target.getPosition(), nutrition);
                            // remove prey from registry to avoid double-processing later
                            env.getRegistry().remove(target.getId());

                            // find the spawned meat near hunter and eat it
                            List<FoodItem> nearby = env.getResources().getFoodNear(self.getPosition(), attackRange);
                            FoodItem meat = null;
                            float min = Float.MAX_VALUE;
                            for (FoodItem f : nearby) {
                                if (f.isWater()) continue;
                                float d = f.position().distanceTo(self.getPosition());
                                if (d < min) { min = d; meat = f; }
                            }
                            if (meat != null) {
                                if (self instanceof wildlife.model.organism.Animal) {
                                    ((wildlife.model.organism.Animal) self).eating(meat);
                                } else {
                                    self.getStats().consume(meat.nutritionalValue(), false);
                                    env.getResources().consume(meat);
                                }
                            }
                        }
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