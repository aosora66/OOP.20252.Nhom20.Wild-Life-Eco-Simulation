package wildlife.model.organism;

import wildlife.model.environment.Environment;
import wildlife.model.environment.dto.FoodItem;
import wildlife.model.organism.component.AdaptabilityComponent;
import wildlife.model.organism.component.GrowthComponent;
import wildlife.model.organism.component.SurvivalStatsComponent;
import wildlife.util.AppConfig;
import wildlife.util.CircleBoundary;
import wildlife.util.Vector2D;

import java.util.List;


public class Grass extends Plant{

    public Grass(String id,
                 String speciesName,
                 Vector2D startPos,
                 Environment startEnv,
                 GrowthComponent growth,
                 SurvivalStatsComponent stats,
                 AdaptabilityComponent adaptability) {
        super(id, speciesName, startPos, startEnv, growth, stats, adaptability);
        photosynthesisRate         = AppConfig.getFloat("plant.grass.photosynthesisRate");
        lightLevelToPhotosynthesis = AppConfig.getFloat("plant.grass.minLightLevel");
        nutritionAsorbRadius       = AppConfig.getFloat("plant.grass.nutrientsAsorbRadius");
    }

    @Override
    protected void onTick(int currentTick) {
        photosynthesis();
        absorbNutrients();
    }

    @Override
    public Grass reproduce() {
        return null;
    }

    @Override
    public void photosynthesis() {
        if (currentEnvironment == null) return;

        // đủ điều kiện ánh sáng để quang hợp
        float lightLevel = currentEnvironment.getLightLevel();
        if (lightLevel < lightLevelToPhotosynthesis) {
            return;
        }

        // độ ẩm là hệ số quang hợp
        float humidity = currentEnvironment.getHumidity();
        float humidityFactor = humidity / AppConfig.getFloat("organism.stats.humidityMax");

        float energy = lightLevel * humidityFactor * photosynthesisRate;
        float hpGain = energy * AppConfig.getFloat("organism.stats.nutritionToHpRatio");
        stats.restoreHp(hpGain);
    }

    @Override
    public void absorbNutrients() {
        if (currentEnvironment == null) return;

        // Sinh vật gọi môi trường quét dưỡng chất (FoodItem) trong bán kính
        List<FoodItem> nearby = currentEnvironment.getResources().getFoodNear(position, nutritionAsorbRadius);

        // Chọn dưỡng chất gần nhất
        FoodItem nutrients = null;
        float minDistance = Float.MAX_VALUE;
        for (FoodItem item : nearby) {
            float dist = item.position().distanceTo(position);
            if (dist < minDistance) {
                minDistance = dist;
                nutrients = item;
            }
        }
        if (nutrients == null) return;

        stats.consume(nutrients.nutritionalValue(), nutrients.isWater());

        // Môi trường xóa dưỡng chất vừa được hấp thụ khỏi bản đồ
        currentEnvironment.getResources().consume(nutrients);
    }

}
