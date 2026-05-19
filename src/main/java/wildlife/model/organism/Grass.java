package wildlife.model.organism;

import wildlife.model.environment.dto.FoodItem;
import wildlife.model.environment.enums.TerrainType;
import wildlife.model.organism.component.AdaptabilityComponent;
import wildlife.model.organism.component.GrowthComponent;
import wildlife.model.organism.component.SurvivalStatsComponent;
import wildlife.util.AppConfig;
import wildlife.util.CircleBoundary;
import wildlife.util.Vector2D;


public class Grass extends Plant{

    private CircleBoundary waterAsorb;

    public Grass(String id,
                 String speciesName,
                 Vector2D startPos,
                 TerrainType startEnv,
                 GrowthComponent growth,
                 SurvivalStatsComponent stats,
                 AdaptabilityComponent adaptability) {
        super(id, speciesName, startPos, startEnv, growth, stats, adaptability);
    }

    @Override
    protected void onTick(int currentTick) {

    }

    @Override
    public Organism reproduce() {
        return null;
    }

    @Override
    public void photosynthesis() {

    }

    @Override
    public void absorbNutrients() {

        // sinh vật sẽ gọi môi trường để quét dưỡng chất gần nhất và trả về
        //...


        FoodItem nutrients = new FoodItem(new Vector2D(0,0),0,false,AppConfig.getInt("environment.meat.expiryTicks")); // ví dụ thức ăn tạm thời
        this.stats.consume(nutrients.nutritionalValue(), nutrients.isWater());

        // môi trường xóa dưỡng chất vừa được ăn trên bản đồ
        //...

    }

}
