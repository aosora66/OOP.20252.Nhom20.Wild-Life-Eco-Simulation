package wildlife.model.brain;

import wildlife.model.environment.Environment;
import wildlife.model.organism.Organism;

import java.util.Optional;

/**
 * Strategy chạy trốn — kích hoạt ngay khi phát hiện kẻ thù trong tầm nhìn, ưu tiên 30.
 * Ưu tiên cao nhất để đảm bảo sinh tồn luôn được đặt trước săn mồi hay ăn uống.
 */
public class ScaredStrategy extends AbstractSurvivalStrategy {

    private final String predatorSpecies;
    // Số bước chạy mỗi tick — cho phép con mồi tăng tốc khi hoảng loạn
    private final int    sprintSteps;

    public ScaredStrategy(float stepSize, float fearRadius,
                          String predatorSpecies, int sprintSteps) {
        // attackRange = 0 vì strategy này không tấn công
        super(stepSize, fearRadius, 0f);
        this.predatorSpecies = predatorSpecies;
        this.sprintSteps     = Math.max(1, sprintSteps);
    }

    /** Kích hoạt khi có kẻ thù trong fearRadius — gọi findNearestBySpecies để kiểm tra. */
    @Override
    public boolean isApplicable(Organism self, Environment env) {
        return findNearestBySpecies(self, env, predatorSpecies).isPresent();
    }

    @Override
    public int getPriority() { return 30; }

    /** Chạy sprintSteps bước liên tiếp ra xa kẻ thù gần nhất. */
    @Override
    public void execute(Organism self, Environment env) {
        Optional<Organism> threat = findNearestBySpecies(self, env, predatorSpecies);
        threat.ifPresent(predator -> {
            for (int i = 0; i < sprintSteps; i++) {
                moveAwayFrom(self, predator.getPosition(), env);
            }
        });
    }
}