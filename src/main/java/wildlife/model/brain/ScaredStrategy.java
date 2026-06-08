package wildlife.model.brain;

import wildlife.model.environment.Environment;
import wildlife.model.organism.animal.Animal;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Strategy chạy trốn — kích hoạt ngay khi phát hiện kẻ thù trong tầm nhìn, ưu tiên 30.
 * Ưu tiên cao nhất để đảm bảo sinh tồn luôn được đặt trước săn mồi hay ăn uống.
 * Hỗ trợ nhiều loài kẻ thù — chạy khỏi con gần nhất trong tất cả các loài nguy hiểm.
 */
public class ScaredStrategy extends AbstractSurvivalStrategy {

    private final List<String> predatorSpecies;
    // Số bước chạy mỗi tick — cho phép con mồi tăng tốc khi hoảng loạn
    private final int          sprintSteps;

    public ScaredStrategy(float stepSize, float fearRadius,
                          int sprintSteps, String... predatorSpecies) {
        // attackRange = 0 vì strategy này không tấn công
        super(stepSize, fearRadius, 0f);
        this.predatorSpecies = List.of(predatorSpecies);
        this.sprintSteps     = Math.max(1, sprintSteps);
    }

    /** Kích hoạt khi có BẤT KỲ loài kẻ thù nào xuất hiện trong fearRadius. */
    @Override
    public boolean isApplicable(Animal self, Environment env) {
        return predatorSpecies.stream()
                .anyMatch(s -> findNearestBySpecies(self, env, s).isPresent());
    }

    @Override
    public int getPriority() { return 30; }

    /** Chạy sprintSteps bước liên tiếp ra xa kẻ thù GẦN NHẤT trong tất cả các loài nguy hiểm. */
    @Override
    public void execute(Animal self, Environment env) {
        predatorSpecies.stream()
                .map(s -> findNearestBySpecies(self, env, s))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .min(Comparator.comparingDouble(o -> o.getPosition().distanceTo(self.getPosition())))
                .ifPresent(threat -> {
                    for (int i = 0; i < sprintSteps; i++) {
                        moveAwayFrom(self, threat.getPosition(), env);
                    }
                });
    }
}