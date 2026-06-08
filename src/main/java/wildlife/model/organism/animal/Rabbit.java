package wildlife.model.organism.animal;

import wildlife.model.environment.Environment;
import wildlife.model.environment.enums.TerrainType;
import wildlife.model.organism.Organism;
import wildlife.model.organism.component.AdaptabilityComponent;
import wildlife.model.organism.component.GrowthComponent;
import wildlife.model.organism.component.SurvivalStatsComponent;
import wildlife.util.AppConfig;
import wildlife.util.Vector2D;

public class Rabbit extends Animal{
    public Rabbit(String id,
                  String speciesName,
                  Vector2D startPos,
                  TerrainType startTer,
                  Environment startEnv,
                  GrowthComponent growth,
                  SurvivalStatsComponent stats,
                  AdaptabilityComponent adaptability,
                  String gender) {
        super(id, speciesName, startPos,startTer, startEnv, growth, stats, adaptability);
        this.gender      = gender;
        this.combatPower = AppConfig.getFloat("animal.rabbit.combatPower");
        this.vision      = AppConfig.getFloat("animal.rabbit.vision");
        this.speed       = AppConfig.getFloat("animal.rabbit.speed");
        this.interactionRadius = AppConfig.getFloat("animal.rabbit.eatRadius");
        initStrategies();
    }

    @Override
    protected void onTick(int currentTick) {

        executeStrategy(currentTick);

        // call reproduce() base on age;
    }

    @Override
    protected void addSurvivalStrategies() {

    }

    @Override
    public Rabbit reproduce() {

        return null;
    }
}
