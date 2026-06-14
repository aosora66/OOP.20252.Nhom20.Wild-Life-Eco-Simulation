package wildlife.model.organism.animal.hebivores;

import wildlife.model.brain.SurvivalStrategy;
import wildlife.model.environment.Environment;
import wildlife.model.organism.Organism;
import wildlife.model.organism.animal.Animal;
import wildlife.model.organism.component.AdaptabilityComponent;
import wildlife.model.organism.component.GrowthComponent;
import wildlife.model.organism.component.SurvivalStatsComponent;
import wildlife.util.AppConfig;
import wildlife.util.Vector2D;
import wildlife.model.brain.PassiveStrategy;
import wildlife.model.brain.ScaredStrategy;

public class Rabbit extends Animal {

    public Rabbit(String id,
                  String speciesName,
                  Vector2D startPos,
                  Environment startEnv,
                  GrowthComponent growth,
                  SurvivalStatsComponent stats,
                  AdaptabilityComponent adaptability,
                  String gender) {
        super(id, speciesName, startPos, startEnv, growth, stats, adaptability, "HEBIVORE");
        this.combatPower = AppConfig.getFloat("animal.rabbit.combatPower");
        this.vision      = AppConfig.getFloat("animal.rabbit.vision");
        this.speed       = AppConfig.getFloat("animal.rabbit.speed");
        this.interactionRadius = AppConfig.getFloat("animal.rabbit.eatRadius");
        initStrategies();
    }

    @Override
    protected SurvivalStrategy createSurvivalStrategy() {
        return new PassiveStrategy(speed, vision, interactionRadius,
                defaultHungerSearchThreshold, defaultThirstSearchThreshold);
    }

    @Override
    protected void onTick(int currentTick) {
        // 1) Run current strategy (movement/search/consume)
        executeStrategy(currentTick);

        if (this.getEnvironment() == null) return;

        // 2) strategy switching: if a stronger nearby animal exists -> switch to ScaredStrategy,
        //    otherwise ensure Rabbit uses PassiveStrategy.
        boolean predatorFound = false;
        String predatorSpecies = null;
        for (Organism o : this.getEnvironment().getRegistry().findNear(position, vision, Organism.class)) {
            if (o.getId().equals(this.getId())) continue;
            if (!(o instanceof Animal)) continue;
            Animal other = (Animal) o;
            // consider as predator when other has greater combat power than this rabbit
            if (other.combatPower > this.combatPower) {
                predatorFound = true;
                predatorSpecies = o.getSpeciesName();
                break;
            }
        }

        if (predatorFound) {
            // change to scared strategy if not already scared
            if (!(strategy instanceof ScaredStrategy)) {
                int sprint = 3;
                try { sprint = AppConfig.getInt("animal.rabbit.sprintSteps"); } catch (RuntimeException ignored) {}
                setStrategy(new ScaredStrategy(speed, vision, predatorSpecies, sprint));
            }
        } else {
            if (!(strategy instanceof PassiveStrategy)) {
                setStrategy(new PassiveStrategy(speed, vision, interactionRadius,
                        defaultHungerSearchThreshold, defaultThirstSearchThreshold));
            }
        }
    }

    @Override
    public Organism reproduce() {
        return null;
    }
}
