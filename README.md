# 🌿 Hệ thống Mô phỏng Hệ sinh thái Hoang dã (Wild-Life Eco Simulation)

**Dự án Bài tập lớn Lập trình Hướng đối tượng (OOP) - Nhóm 20**

Dự án mô phỏng một hệ sinh thái động, nơi các loài động thực vật tương tác, sinh tồn và tiến hóa theo thời gian thực. Hệ thống tuân thủ chặt chẽ các nguyên lý thiết kế Hướng đối tượng (OOP), đặc biệt nhấn mạnh vào khả năng mở rộng (Extensibility) và phân tách rạch ròi giữa logic hệ thống (BioLogic) và giao diện (ViewLogic)[cite: 1, 2].

---

## 👥 Thành viên nhóm & Phân công

---

## 📂 Cấu trúc Thư mục Toàn Dự án

### 🏗️ Kiến trúc Phần mềm
Dự án được chia thành các package chuyên biệt, đảm bảo nguyên tắc Single Responsibility (Đơn trách nhiệm).

 1. `wildlife.core` (Trái tim hệ thống)
*   Chứa `Main.java` để khởi động chương trình.
*   Quản lý **Tick System** (vòng lặp thời gian). Mỗi 1 tick, hệ thống sẽ gọi các hàm cập nhật môi trường và hành động của toàn bộ sinh vật.

 2. `wildlife.model` (BioLogic - Logic Sinh tồn)
Đây là package cốt lõi, được chia thành 3 phần độc lập:
*   **`model.entity` (Thực thể):** Chứa hệ thống phân cấp sinh vật. Gồm các abstract class `SinhVat`, `DongVat`, `ThucVat` và các class cụ thể (Thỏ, Sói, Cỏ...).
*   **`model.environment` (Môi trường):** Định nghĩa không gian sống (Đồng Cỏ, Rừng Rậm, Hồ Nước).
*   **`model.brain` (Bộ não):** Chứa **Strategy Pattern**.
    *   `SurvivalStrategy` (Interface chung).
    *   `PassiveStrategy`: Chỉ đi lang thang và ăn cỏ.
    *   `HunterStrategy`: Luôn quét tìm mục tiêu trong bán kính X mét và tấn công.
    *   `ScaredStrategy`: Luôn di chuyển ngược hướng với kẻ thù gần nhất.
        Dễ dàng thay đổi "bộ não" này (ví dụ: Con thỏ khi đói quá có thể trở nên liều lĩnh - `AggressiveStrategy`).

 3. `wildlife.view` (ViewLogic - Giao diện hiển thị)
*   Đảm nhiệm việc render thế giới ra màn hình.
*   Hỗ trợ 2 chế độ: Basic (Vẽ hình khối cơ bản) và Graphics (Render ảnh động Gif/Sprite).

 4. `wildlife.util` (Tiện ích)
*   Chứa các công cụ dùng chung như tính khoảng cách toán học (áp dụng vào việc tìm đường, nhận diện khoảng cách mồi), hàm random tọa độ hợp lệ.

---

### Các thư mục tài nguyên và tài liệu nộp bài:

*   **`src/main/resources/`**: Nơi lưu trữ toàn bộ tài nguyên tĩnh của game.
    *   `config/`: Chứa file `setting.properties` để cấu hình thông số bản đồ, số lượng sinh vật ban đầu mà không cần can thiệp vào code.
    *   `images/`: Chứa ảnh động (Gif/Sprite) phục vụ cho ViewLogic hiển thị đồ họa.
    *   `sounds/`: Chứa các file âm thanh môi trường.
*   **`docs/`**: Nơi lưu trữ tài liệu mềm nộp kèm theo yêu cầu của môn học. Các thành viên phụ trách viết tài liệu sẽ làm việc chủ yếu ở đây:
    *   `UML/`: Chứa các file ảnh Biểu đồ lớp (Class Diagram) và Biểu đồ phụ thuộc gói (Package Diagram).
    *   `report/`: File báo cáo PDF, chi tiết phần trăm đóng góp và nhật ký làm việc nhóm.
    *   `slides/`: Slide dùng để thuyết trình bảo vệ đồ án.
*   **`demo/`**: Thư mục dùng để chứa Video demo quay lại quá trình chạy thử chương trình của nhóm.
*   **`.gitignore`**: File hệ thống cấu hình Git, giúp ngăn chặn việc vô tình đẩy các file rác của IDE (như `.idea`, `out`) lên kho lưu trữ chung.

---

## 🚀 Hướng dẫn cài đặt & Chạy chương trình

1.  **Clone dự án:** Kéo mã nguồn từ nhánh `main` về máy.
2.  **Cấu hình ban đầu:** Mở file `src/main/resources/config/setting.properties` để thay đổi số lượng sinh vật ban đầu, kích thước map mà không cần biên dịch lại code.
3.  **Khởi chạy:** Chạy hàm `main` trong class `core.Main`.

---

## 🛠️ Dành cho Team Phát triển
*   **Quy trình Git:** Không code trực tiếp lên `main`. Hãy tạo nhánh `feature/...` từ `develop`, code xong tạo Pull Request để review trước khi gộp.
*   **Thêm loài mới:** Để thêm một con vật (VD: Gấu), chỉ cần tạo class `Gau extends Carnivore` trong gói `organism`, gán cho nó một bộ não từ gói `brain`, và thả vào `environment`. Không cần sửa lại logic vòng lặp chính.
*   **Tránh Hardcode:** Tuyệt đối không fix cứng các con số (tốc độ, tầm nhìn) trong code. Hãy khai báo hằng số hoặc đưa vào file `setting.properties`.