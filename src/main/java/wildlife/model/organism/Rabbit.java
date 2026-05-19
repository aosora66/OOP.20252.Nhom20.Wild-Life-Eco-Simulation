package wildlife.model.organism;

import wildlife.model.environment.Environment;
import wildlife.model.environment.dto.FoodItem;
import wildlife.model.organism.component.AdaptabilityComponent;
import wildlife.model.organism.component.GrowthComponent;
import wildlife.model.organism.component.SurvivalStatsComponent;
import wildlife.util.AppConfig;
import wildlife.util.Vector2D;

import java.util.List;
import java.util.Random;

public class Rabbit extends Animal {

    private final Random random = new Random();
    private final float eatRadius;
    private final float drinkRadius;

    public Rabbit(String id,
                  String speciesName,
                  Vector2D startPos,
                  Environment startEnv,
                  GrowthComponent growth,
                  SurvivalStatsComponent stats,
                  AdaptabilityComponent adaptability) {
        super(id, speciesName, startPos, startEnv, growth, stats, adaptability);
        this.vision      = AppConfig.getFloat("animal.rabbit.vision");
        this.speed       = AppConfig.getFloat("animal.rabbit.speed");
        this.eatRadius   = AppConfig.getFloat("animal.rabbit.eatRadius");
        this.drinkRadius = AppConfig.getFloat("animal.rabbit.drinkRadius");
    }

    @Override
    protected void onTick(int currentTick) {
        hunting();
        drinking();
        wandering();
    }

    @Override
    public Organism reproduce() {
        return null;
    }

    // tìm thức ăn phù hợp và liên tục moveTo() đến vị trí thức ăn đó (liên tục đuổi con mồi)
    @Override
    public void hunting() {
        if (currentEnvironment == null) return;

        FoodItem prey = findNearestFood(false);
        if (prey == null) return;

        Vector2D foodPos = prey.position();
        float dist = position.distanceTo(foodPos);
        moveTo(foodPos);

        if (dist <= speed && position.distanceTo(foodPos) <= eatRadius) {
            eating(prey);
        }
    }

    // di chuyển đến vị trí ngẫu nhiên trong bán kính di chuyển 1 tick
    @Override
    public void wandering() {
        if (currentEnvironment == null) return;

        float angle = random.nextFloat() * (float) (2 * Math.PI);
        float radius = random.nextFloat() * speed;
        Vector2D destination = new Vector2D(
                position.getX() + (float) Math.cos(angle) * radius,
                position.getY() + (float) Math.sin(angle) * radius
        );

        if (isPassable(destination)) {
            setPosition(destination);
        } else {
            moveTo(destination);
        }
    }


    @Override
    public void eating(FoodItem food) {
        if (food == null || currentEnvironment == null) return;

        stats.consume(food.nutritionalValue(), food.isWater());
        currentEnvironment.getResources().consume(food);
    }

    // liên tục quét tìm nước gần nhất và moveTo() đến vị trí nước
    @Override
    public void drinking() {
        if (currentEnvironment == null) return;

        FoodItem water = findNearestFood(true);
        if (water == null) return;

        Vector2D waterPos = water.position();
        float dist = position.distanceTo(waterPos);
        moveTo(waterPos);

        if (dist <= speed && position.distanceTo(waterPos) <= drinkRadius) {
            eating(water);
        }
    }

    /**
     * Tìm thức ăn hoặc nước gần nhất trong tầm nhìn.
     *
     * @param waterOnly true = chỉ nước, false = chỉ thức ăn rắn
     */
    private FoodItem findNearestFood(boolean waterOnly) {
        List<FoodItem> nearby = currentEnvironment.getResources().getFoodNear(position, vision);

        FoodItem nearest = null;
        float minDistance = Float.MAX_VALUE;
        for (FoodItem item : nearby) {
            if (waterOnly != item.isWater()) continue;
            float dist = item.position().distanceTo(position);
            if (dist < minDistance) {
                minDistance = dist;
                nearest = item;
            }
        }
        return nearest;
    }

    @Override
    public void attacking(Organism org) {

    }


}
