# Organism & Animal — lỗi đã sửa (16/06/2026)

File này ghi lại các lỗi (C/W) đã sửa **trong phạm vi `organism` (Plant/Animal)**. Phần `environment` (Forest, GrassLand, Lake, CompositeMap, Boundary) không liệt kê ở đây — đó là khu vực khác.

## Đã sửa

### C5 — `Grass.java` / `GrassLand.java`
**Lỗi:** `GrassLand` gọi `new Grass(grassPos)` — chỉ truyền 1 tham số, nhưng constructor `Grass` cần 7 tham số (`id, speciesName, startPos, startEnv, growth, stats, adaptability`). Lỗi compile, thiếu cả import `Grass` trong `GrassLand.java`.

**Sửa:** Thêm factory method `Grass.create(Vector2D pos, Environment env)` trong `Grass.java`, tự sinh `id` (UUID), `GrowthComponent`, `SurvivalStatsComponent`, `AdaptabilityComponent` mặc định (sống được ở `GRASSLAND`, optimal 25–38°C). `GrassLand.initialize()` giờ gọi `Grass.create(grassPos, this)`.

Tiện tay implement luôn `Grass.addOffspring()` (trước đó để trống) — gọi lại `Grass.create()` để sinh cây con.

> Theo đúng pattern đã dùng cho `TreeForest.create()` — nếu sau này thêm loài cây mới, cứ theo pattern factory này.

### C6 — `AppleTree.java:82`
**Lỗi:** `AppConfig.getInt("food.apple.expiryTicks=100")` — key bị dính nhầm `=100` vào trong chuỗi key, gây `RuntimeException` mỗi lần cây rớt quả (mỗi `appleDropInterval` tick).

**Sửa:** Đổi thành `AppConfig.getInt("food.apple.expiryTicks")`.

### W1/W2 — `Fish.java`
**Lỗi:** `Fish` dùng nhầm toàn bộ config của `Rabbit` (`animal.rabbit.combatPower/vision/speed/eatRadius/flee.*`) và có `diet.add(FoodType.APPLE)` — cá ăn táo là vô lý.

**Sửa:**
- Thêm key riêng `animal.fish.*` trong `setting.properties` (combatPower=0.5, vision=80, speed=2.5, eatRadius=4.0, flee.speedMultiplier=1.6, flee.sprintSteps=2)
- Thêm `FoodType.ALGAE` (rêu/tảo), đổi diet của Fish từ `APPLE` → `ALGAE`
- **Lưu ý:** môi trường (Lake) hiện đã sinh `ALGAE` ngẫu nhiên theo chu kỳ (`plant.algae.spawnInterval/spawnCount`), nên Fish có nguồn ăn thật, không bị đói chết.

## Còn tồn đọng (chưa sửa, thuộc phạm vi organism)

### C11 — `Plant.java:47`
Dùng key `organism.stats.humidityMax` trong `photosynthesis()` nhưng key này **không tồn tại** trong `setting.properties` → `RuntimeException` mỗi lần cây quang hợp. Cần thêm key này vào config (hoặc đổi sang key tương đương nếu đã có sẵn — kiểm tra lại trước khi thêm tránh trùng).

## Thay đổi khác (không thuộc C/W nhưng ảnh hưởng trực tiếp Plant/Animal)

Các thay đổi này không nằm trong danh sách lỗi đã review, nhưng đổi hành vi/API của `Organism`/`Animal`/`Plant` — cần biết để tránh xung đột khi code tiếp:

1. **`Animal.isApexPredator()`** (mặc định `false`) — `Elephant` override `true`. Mọi `ScaredStrategy` tự động né apex predator, không cần khai báo tên loài thủ công.
2. **3 lớp mới:** `Elephant` (Voi, apex), `Hunter` (Thợ săn, săn mọi loài trừ apex), `TreeForest` (cây ra quả, dùng factory `TreeForest.create()`).
3. **`Hunter`** không săn được apex predator (`HunterStrategy` tự filter `!isApexPredator()`) và có thêm `ScaredStrategy` để né Voi.
4. **Plant bỏ khái niệm "đói"** — chỉ còn "khát":
   - `Organism` thêm hook `protected void applyMetabolismDecay(...)` (override được)
   - `Plant` override hook này, chỉ decay khát; `Plant.reproduce()` giờ check `getThirstLevel()` thay vì `getHungerLevel()`
   - Config key đổi: `plant.reproduce.hungerThreshold` → `plant.reproduce.thirstThreshold`
   - **`Animal` không đổi gì** — vẫn đói + khát như cũ
5. **Công thức chọn mục tiêu (detectability)** trong `AbstractSurvivalStrategy`, dùng bởi `HunterStrategy`/`ScaredStrategy`:
   ```
   detectability = visibility / (1 + distance)
   ```
   `visibility = 0` (núp kỹ trong rừng/bụi rậm/ban đêm) → không thể bị chọn làm mục tiêu. Nếu đang code thêm strategy mới cần tìm mục tiêu, dùng `findNearestBySpecies()`/`findNearestApex()` có sẵn, không tự viết lại logic chọn khoảng cách.
