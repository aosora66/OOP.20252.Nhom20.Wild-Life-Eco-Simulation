package wildlife.model.organism.animal.hebivores;

import wildlife.model.brain.PassiveStrategy;
import wildlife.model.brain.ScaredStrategy;
import wildlife.model.environment.Environment;
import wildlife.model.environment.enums.FoodType;
import wildlife.model.environment.enums.TerrainType;
import wildlife.model.organism.animal.Animal;
import wildlife.model.organism.animal.canivores.Tiger;
import wildlife.model.organism.animal.canivores.Wolf;
import wildlife.model.organism.component.AdaptabilityComponent;
import wildlife.model.organism.component.GrowthComponent;
import wildlife.model.organism.component.SurvivalStatsComponent;
import wildlife.util.AppConfig;
import wildlife.util.ValueRange;
import wildlife.util.Vector2D;

import java.util.List;

public class Fish extends Animal {
    public Fish(String id,
                String speciesName,
                Vector2D startPos,
                Environment startEnv,
                GrowthComponent growth,
                SurvivalStatsComponent stats,
                AdaptabilityComponent adaptability) {
        super(id, speciesName, startPos, startEnv, growth, stats, adaptability, "HEBIVORE");
        this.combatPower = AppConfig.getFloat("animal.fish.combatPower");
        this.vision      = AppConfig.getFloat("animal.fish.vision");
        this.speed       = AppConfig.getFloat("animal.fish.speed");
        this.interactionRadius = AppConfig.getFloat("animal.fish.eatRadius");
        this.diet.add(FoodType.ALGAE);
        initStrategies();
    }

    private static final java.util.Random RNG = new java.util.Random();

    /** Sinh cá con (tuổi 0) — dùng trong reproduce(). */
    public static Fish create(Vector2D pos, Environment env) {
        return create(pos, env, 0f);
    }

    /**
     * Factory tạo Cá với sinh học mặc định từ config animal.fish.*.
     * Tuổi thọ dao động ±15% và cho phép đặt tuổi ban đầu (startAge) để rải đều tuổi đàn,
     * tránh cả đàn cùng già chết một lúc.
     */
    public static Fish create(Vector2D pos, Environment env, float startAge) {
        float maxHp       = AppConfig.getFloat("animal.fish.maxHp");
        float nutrition   = AppConfig.getFloat("animal.fish.nutrition");
        float hungerDecay = AppConfig.getFloat("animal.fish.hungerDecay");
        float thirstDecay = AppConfig.getFloat("animal.fish.thirstDecay");
        float maxAge      = AppConfig.getFloat("animal.fish.maxAge") * (0.75f + RNG.nextFloat() * 0.5f);
        float maxSize     = AppConfig.getFloat("animal.fish.maxSize");

        return new Fish(
                "FISH_" + System.nanoTime() + "_" + RNG.nextInt(1000),
                "Fish",
                pos,
                env,
                new GrowthComponent(maxAge, maxSize, 0.2f, 0.7f, startAge),
                new SurvivalStatsComponent(maxHp, nutrition, hungerDecay, thirstDecay),
                new AdaptabilityComponent(
                        List.of(TerrainType.DEEP_WATER),
                        new ValueRange(15f, 35f),   // tối ưu
                        new ValueRange(0f, 45f),    // chịu đựng
                        new ValueRange(-60f, -10f)  // vùng cực lạnh = chết
                )
        );
    }

    @Override
    protected void onTick(int currentTick) {
        executeStrategy(currentTick);
    }

    @Override
    protected void addSurvivalStrategies() {
        // Passive: chủ động đi tìm tảo sớm (ngưỡng đói 45 thay vì 80) vì cá đói rất nhanh —
        // chờ tới ngưỡng mất máu mới tìm thì không kịp ăn. Cá không khát (thirstDecay=0).
        this.addStrategy(new PassiveStrategy(
                speed,
                vision,
                interactionRadius,
                45f,
                defaultThirstSearchThreshold
        ));

        // Scared: Flee from predators (Tiger, Wolf)
        float fleeMultiplier = AppConfig.getFloat("animal.fish.flee.speedMultiplier");
        int sprintSteps = AppConfig.getInt("animal.fish.flee.sprintSteps");

        this.addStrategy(new ScaredStrategy(
                speed * fleeMultiplier,
                vision,
                sprintSteps,
                interactionRadius,
                0.25f,
                0.2f,
                Tiger.class, Wolf.class
        ));
    }

    @Override
    protected float getBaseHpDrainPerTick() {
        return AppConfig.getFloat("animal.fish.baseHpDrainPerTick");
    }

    /**
     * Sinh sản không giới hạn trần quần thể. Cá chỉ bị chặn bởi tuổi trưởng thành,
     * cooldown, mức đói và xác suất sinh sản; nếu đàn tụt thấp thì tăng nhẹ cơ hội
     * hồi phục để tránh tuyệt chủng do vài tick xui rủi.
     */
    @Override
    public void reproduce() {
        if (environment == null || !growth.isAdult()) return;

        int now = environment.getTime().getCurrentTick();
        int cooldown = AppConfig.getInt("animal.fish.reproduce.cooldownTicks");
        if (now - lastReproduceTick < cooldown) return;

        // Chỉ ngừng sinh khi đang đói lả (ngưỡng nới lỏng riêng cho cá)
        if (stats.getHungerLevel() >= AppConfig.getFloat("animal.fish.reproduce.hungerThreshold")) return;

        int pop = environment.getRegistry().getAllAlive(Fish.class).size();
        float chance = AppConfig.getFloat("animal.fish.reproduce.chance");
        if (pop <= 5) {
            chance = Math.max(chance, 0.35f);
        }
        if (Math.random() >= chance) return;

        // Tìm vị trí con nằm trong nước (DEEP_WATER); nếu không có thì sinh ngay cạnh bố mẹ
        float radius = AppConfig.getFloat("animal.reproduce.spawnRadius");
        Vector2D childPos = position;
        for (int attempt = 0; attempt < 5; attempt++) {
            float ox = (float) (Math.random() * 2 - 1) * radius;
            float oy = (float) (Math.random() * 2 - 1) * radius;
            Vector2D candidate = new Vector2D(position.getX() + ox, position.getY() + oy);
            if (environment.getTerrain().isPassable(candidate, this)) {
                childPos = candidate;
                break;
            }
        }

        environment.addOrganism(Fish.create(childPos, environment));
        lastReproduceTick = now;
    }
}
