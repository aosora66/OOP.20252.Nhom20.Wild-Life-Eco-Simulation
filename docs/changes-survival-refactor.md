# Refactor: Multi-Strategy Support

## SurvivalStrategy.java (`wildlife/util/SurvivalStrategy.java`)

Thêm 2 method vào interface:

```java
boolean isApplicable(Organism self, Environment env);
int getPriority();
```

Mỗi strategy tự khai báo điều kiện áp dụng và độ ưu tiên thay vì để Organism quản lý.

---

## Organism.java (`wildlife/model/organism/Organism.java`)

### 1. Field `strategy` → `strategies`

```java
// Trước
protected SurvivalStrategy strategy;

// Sau
protected final List<SurvivalStrategy> strategies = new ArrayList<>();
```

### 2. `tick()` — thứ tự act trước, decay sau

Trước đây mỗi strategy tự gọi `applyHungerThirstDecay` và `checkHpThreshold`, dẫn đến decay chạy nhiều lần nếu organism có nhiều strategy. Sau đó tiếp tục được cải thiện thêm một lần nữa (xem mục bên dưới): decay chuyển sang `processSurvivalMetabolism()` và thứ tự được đảo lại để hành động (ăn/uống) chạy trước decay.

```
growUp → onTick() [hành động] → processSurvivalMetabolism() [decay] → checkHpThreshold()
```

Lý do: nếu organism ăn trong tick này, hunger giảm trước khi starvation penalty được tính — phản ánh đúng thực tế hơn.

### 3. `executeStrategy()` — chạy theo priority

```java
// Trước
strategy.execute(this, environment);

// Sau: chọn strategy ưu tiên cao nhất thỏa isApplicable()
strategies.stream()
    .sorted(Comparator.comparingInt(SurvivalStrategy::getPriority).reversed())
    .filter(s -> s.isApplicable(this, environment))
    .findFirst()
    .ifPresent(s -> s.execute(this, environment));
```

### 4. `setStrategy` → `addStrategy`

```java
// Trước — ghi đè strategy cũ
public void setStrategy(SurvivalStrategy strategy) { ... }

// Sau — thêm vào danh sách
public void addStrategy(SurvivalStrategy strategy) {
    this.strategies.add(strategy);
}
```

---

## Các Strategy hiện có

Mỗi strategy chỉ làm đúng một việc. Decay đói/khát và kiểm tra HP không nằm trong strategy mà được xử lý tập trung ở `Organism.tick()`.

### ScaredStrategy — Chạy trốn (priority 30)

Kích hoạt khi phát hiện kẻ thù (`predatorSpecies`) trong tầm nhìn (`fearRadius`). Mỗi tick chạy `sprintSteps` bước liên tiếp ra xa kẻ thù. Có priority cao nhất để đảm bảo sinh tồn luôn được ưu tiên trước mọi hành vi khác.

Dùng cho: con mồi (thỏ, hươu...).

### HunterStrategy — Săn mồi (priority 20)

Kích hoạt khi mức đói đạt `hungerSearchThreshold`. Tìm con mồi (`preySpecies`) gần nhất trong tầm nhìn, tiến lại gần rồi tấn công khi đủ gần (`attackRange`). Nếu không tìm thấy con mồi thì wander chờ tick sau.

Dùng cho: động vật ăn thịt (sói, hổ...).

### PassiveStrategy — Ăn uống cơ bản (priority 10)

Luôn `isApplicable = true` nên đóng vai trò fallback khi không có strategy nào khác chạy. Ưu tiên xử lý khát trước đói (thiếu nước gây chết nhanh hơn), sau đó wander nếu no đủ.

Dùng cho: mọi sinh vật cần ăn/uống — thường ghép cùng ScaredStrategy hoặc HunterStrategy.

### Cách ghép strategy theo loài

| Loài | Strategy |
|------|----------|
| Thỏ, hươu... | `ScaredStrategy` + `PassiveStrategy` |
| Sói, hổ... | `HunterStrategy` + `PassiveStrategy` |
| Động vật ăn cỏ không có kẻ thù | `PassiveStrategy` |

---

## Tích hợp từ nhánh `feature/bio-organism`

### SurvivalStatsComponent.java

`checkHpThreshold()` không còn tự trừ HP nữa — chỉ kiểm tra `hp <= 0f`. Việc trừ HP được chuyển vào `processSurvivalMetabolism()` để gộp cùng các nguồn drain khác, tránh trừ hai lần.

Thêm `getStarvationPenalty()` trả về lượng HP phạt nếu đói/khát vượt ngưỡng, ngược lại trả 0:

```java
public float getStarvationPenalty() {
    if (hungerLevel >= HUNGER_HP_THRESHOLD || thirstLevel >= THIRST_HP_THRESHOLD) {
        return HP_PENALTY_PER_TICK;
    }
    return 0f;
}
```

### Organism.java — `processSurvivalMetabolism()` và `getEnvironmentalStressHpPenalty()`

Thay đoạn decay đơn giản trong `tick()` bằng phương thức chi tiết hơn, tính 3 nguồn HP drain gộp lại:

```
hpDrain = baseHpDrainPerTick + stressPenalty (nhiệt độ) + starvationPenalty (đói/khát)
```

`thirstMultiplier` tính thêm yếu tố độ ẩm: môi trường khô làm khát nhanh hơn.

```java
float thirstMultiplier = seasonMultiplier
    * (1f + (1f - humidityFactor) * thirstHumidityFactor);
```

`getEnvironmentalStressHpPenalty()` trả về penalty dựa trên nhiệt độ so với `AdaptabilityComponent` của loài: lethal > tolerance → penalty nặng; không optimal → penalty nhẹ.

Config keys mới thêm vào `setting.properties`:

| Key | Giá trị | Ý nghĩa |
|-----|---------|---------|
| `organism.stats.baseHpDrainPerTick` | 0.1 | HP drain cơ bản mỗi tick |
| `organism.stats.humidityMax` | 100.0 | Độ ẩm tối đa để chuẩn hóa |
| `organism.stats.thirstHumidityFactor` | 0.5 | Hệ số ảnh hưởng của độ ẩm lên khát |
| `organism.stats.lethalStressHpPenalty` | 3.0 | Penalty khi nhiệt độ gây chết/không chịu được |
| `organism.stats.suboptimalStressHpPenalty` | 0.5 | Penalty khi nhiệt độ không tối ưu |

### HunterStrategy.java — ăn sau khi giết

Khi `target.decreaseHp()` làm target chết, hunter gọi `eatCorpse()`:
1. `env.getResources().convertDeadToMeat(pos, nutrition)` — spawn FoodItem tại vị trí xác
2. `env.getRegistry().remove(target.getId())` — xóa xác khỏi registry
3. Tìm miếng thịt gần nhất trong `attackRange` và `consume()` ngay

Không dùng `instanceof Animal` — gọi trực tiếp `self.getStats().consume()` trên `Organism`.

### Animal.java — file mới

Lớp nền cho mọi động vật, thay thế các abstract method cũ (`hunting()`, `drinking()`...) bằng cơ chế strategy.

Các thay đổi chính so với phiên bản cũ:
- `eatRadius` + `drinkRadius` → gộp thành `interactionRadius`
- `eating(FoodItem)` là concrete method dùng chung
- `addSurvivalStrategies()` (abstract) + `initStrategies()` (final) thay cho `createSurvivalStrategy()` — hỗ trợ nhiều strategy thay vì một

```java
// Subclass implement:
protected void addSurvivalStrategies() {
    addStrategy(new ScaredStrategy(speed, vision, "Wolf", 3));
    addStrategy(new PassiveStrategy(speed, vision, interactionRadius, ...));
}

// Gọi ở cuối constructor subclass:
initStrategies();
```

---

## Vòng fix logic (sau khi review code)

### 1. `findNearestBySpecies` lọc `isAlive()` — `AbstractSurvivalStrategy`

Trước fix: hunter có thể chọn xác đang ở trạng thái `TRANSFORMING` để tấn công lần nữa → gọi `convertDeadToMeat` lần thứ hai, spawn thừa thịt. Scared cũng có thể tiếp tục chạy khỏi predator đã chết.

Sau fix: thêm `o.isAlive()` vào filter — chỉ trả về sinh vật còn sống.

### 2. `getEnvironmentalStressHpPenalty` check terrain — `Organism`

Trước fix: chỉ check temperature, không check terrain. Cá trên cạn không bị phạt HP.

Sau fix: thêm check `adaptability.canSurviveIn(currentEnvironment)` đầu method — sai terrain → lethal penalty ngay, không cần check thêm nhiệt độ.

### 3. `DECAY_HP_PENALTY` dùng đúng config key — `Organism`

Trước fix: `AppConfig.getFloat("organism.stats.hpPenaltyPerTick")` (2.0, dùng cho starvation) — lão hóa bị trừ 2 HP/tick thay vì 0.5.

Sau fix: `AppConfig.getFloat("organism.growth.decayHpPenalty")` (0.5, đúng key cho lão hóa).

### 4. Bỏ `checkHpThreshold()` thừa cuối `tick()` — `Organism`

`processSurvivalMetabolism()` đã tự gọi `die()` khi HP về 0. Check lại bên ngoài là dead code (do `die()` idempotent nên không sai, chỉ thừa).

### 5. Bỏ `isAlive()` thừa trong sprint loop — `ScaredStrategy`

`moveAwayFrom` chỉ set position, không thể giết organism giữa loop. Check thừa.

### 6. Đưa `eating()` lên `Organism` — bỏ `instanceof` thật sự

Trước fix: `eating()` chỉ có ở `Animal`. Strategy muốn gọi phải `instanceof Animal` rồi cast (theo cách của nhánh kia) hoặc gọi thẳng `stats.consume() + resources.consume()` (mất polymorphism).

Sau fix: `eating(FoodItem)` định nghĩa ở `Organism` với default impl. `Animal` kế thừa, có thể override nếu cần. `PassiveStrategy` và `HunterStrategy.eatCorpse` gọi `self.eating(food)` polymorphic — không cần `instanceof`, subclass override sẽ tự được gọi.

---

## Refactor tiếp theo (session 2)

### 1. Strategy code xuống `Animal` — bỏ khỏi `Organism`

Thực vật không có strategy nên không cần `strategies`, `addStrategy()`, `executeStrategy()` ở `Organism`.

Sau refactor:
- `Organism`: chỉ còn `eating()`, `processSurvivalMetabolism()` và các method sinh tồn chung
- `Animal`: chứa `strategies` list, `addStrategy()`, `executeStrategy()`, `getCombatPower()`

### 2. `SurvivalStrategy` chuyển package

`wildlife/util/SurvivalStrategy.java` → `wildlife/model/brain/SurvivalStrategy.java`

`util` dành cho công cụ kỹ thuật (`AppConfig`, `Vector2D`). `SurvivalStrategy` là domain contract — đặt cùng package với các implementation của nó hợp lý hơn.

### 3. `Organism self` → `Animal self` trong toàn bộ strategy

Interface và tất cả implementation (`AbstractSurvivalStrategy`, `HunterStrategy`, `PassiveStrategy`, `ScaredStrategy`) đổi tham số `self` từ `Organism` sang `Animal`.

Lý do: chỉ `Animal` mới có strategy. Dùng `Animal` cho phép strategy truy cập trực tiếp `combatPower`, `speed`, `vision` mà không cần cast hay truyền thêm tham số.

`AbstractSurvivalStrategy.RNG` đổi từ `private` → `protected` để subclass dùng được.

### 4. Multi-species — `String` → `List<String>` varargs

`ScaredStrategy.predatorSpecies` và `HunterStrategy.preySpecies` đổi từ `String` (1 loài) sang `List<String>` (nhiều loài).

Constructor dùng varargs:

```java
new ScaredStrategy(speed, vision, 3, range, 0.3f, 0.5f, "Wolf", "Tiger");
new HunterStrategy(speed, vision, range, damage, threshold, "Rabbit", "Deer");
```

- `ScaredStrategy`: chạy trốn con **gần nhất** trong tất cả loài kẻ thù
- `HunterStrategy`: săn con **gần nhất** trong tất cả loài con mồi

### 5. Cơ chế phản kháng — `ScaredStrategy`

Khi HP thấp và kẻ thù đã áp sát, con mồi có tỉ lệ đánh lại thay vì chỉ chạy.

Điều kiện kích hoạt (3 điều kiện AND):
1. Kẻ thù trong `attackRange` (= `counterAttackRange` truyền vào constructor)
2. `hp / maxHp <= counterHpThreshold`
3. `RNG.nextFloat() < counterAttackChance`

Sát thương = `self.getCombatPower()` — không cần truyền thêm tham số.

**Cooldown** chống spam: sau mỗi lần phản kháng, phải chờ `organism.scared.counterAttackCooldown` tick mới được phản kháng tiếp. Giá trị đọc từ config thay vì hardcode trong constructor.

```java
// setting.properties
organism.scared.counterAttackCooldown=20
```

Constructor mới:

```java
new ScaredStrategy(speed, vision, sprintSteps,
                   counterAttackRange, counterHpThreshold, counterAttackChance,
                   "Wolf", "Tiger");
```
