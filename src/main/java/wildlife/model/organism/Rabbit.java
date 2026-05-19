package wildlife.model.organism;

import wildlife.model.environment.dto.FoodItem;
import wildlife.model.environment.enums.TerrainType;
import wildlife.model.organism.component.AdaptabilityComponent;
import wildlife.model.organism.component.GrowthComponent;
import wildlife.model.organism.component.SurvivalStatsComponent;
import wildlife.util.AppConfig;
import wildlife.util.Vector2D;

public class Rabbit extends Animal{
    public Rabbit(String id,
                  String speciesName,
                  Vector2D startPos,
                  TerrainType startEnv,
                  GrowthComponent growth,
                  SurvivalStatsComponent stats,
                  AdaptabilityComponent adaptability) {
        super(id, speciesName, startPos, startEnv, growth, stats, adaptability);
    }

    @Override
    public Organism reproduce() {
        return null;
    }

    @Override
    public void hunting() {
        //sử dụng hàm môi trường để quét các thức ăn trên bản đồ
        // khi tìm thấy thức ăn thì gọi hàm eating()
    }

    @Override
    public void wandering() {
        // cập nhật Vector2D một cách ngẫu nhiên
    }

    @Override
    public void eating(FoodItem food) {
        this.stats.consume(food.nutritionalValue(),true);
        // môi trường xóa thức ăn vừa được ăn khỏi bản đồ
    }


}
