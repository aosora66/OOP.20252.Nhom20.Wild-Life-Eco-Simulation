package wildlife.util;

import wildlife.model.environment.Environment;
import wildlife.model.organism.Organism;

public interface SurvivalStrategy {
    void execute(Organism self, Environment env);
}