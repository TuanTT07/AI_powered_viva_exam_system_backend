# 📌 Tài liệu Mô tả Kỹ thuật: Nhóm chức năng 2 - Quản lý Kỳ thi & Lịch thi (AIVES)

Tài liệu này chi tiết hóa toàn bộ các tính năng, kiến trúc, API và thuật toán đã được xây dựng cho **Nhóm chức năng 2 (Exam & Schedule Management)** thuộc hệ thống Backend **AIVES (AI-powered Viva Exam System)**.

---

## 🏗️ 1. Tổng quan Module 2

Package: [`cocxanhcoder.viva.exam.system.exammgt`](file:///d:/CloneRepo/AI_powered_viva_exam_system_backend/src/main/java/cocxanhcoder/viva/exam/system/exammgt)

Module 2 đóng vai trò điều phối toàn bộ phiên thi vấn đáp, chịu trách nhiệm:
- Quản lý thông tin kỳ thi và cấu hình chi tiết (`exam_config`).
- Quản lý danh sách sinh viên tham gia kỳ thi và tự động chia ca thi (Slot allocation).
- Cấp phát bộ câu hỏi thích ứng theo thang Bloom & áp dụng thuật toán **Anti-Overlap** chống lộ đề thi giữa các ca thi kế tiếp.
- Cung cấp Dashboard giám sát phiên thi thời gian thực cho Giảng viên/Giám thị và hỗ trợ xử lý sự cố.
- Cung cấp API cho Sinh viên tra cứu lịch thi cá nhân.

---

## ⚙️ 2. Chi tiết các Tính năng đã Triển khai

### 2.1. Quản lý Kỳ thi & Cấu hình (`ExamController` & `ExamService`)
- **Tạo kỳ thi (`POST /api/v1/exams`)**: Gắn kỳ thi với Môn học (`course_id`), tên kỳ thi, thời gian bắt đầu và kết thúc.
- **Cấu hình kỳ thi dạng JSONB (`exam_config`)**:
  - `maxMainQuestions`: Số câu hỏi chính tối đa cho mỗi sinh viên (mặc định: 3 câu).
  - `maxFollowUpQuestions`: Số câu hỏi đào sâu/hỏi xoáy tối đa mỗi câu chính (mặc định: 2 câu).
  - `timeLimitPerTurnSeconds`: Thời gian trả lời tối đa mỗi lượt (mặc định: 60 giây).
  - `bloomRatios`: Tỷ lệ phân bổ độ khó theo thang Bloom (Remember, Understand, Apply, Analyze).
  - `antiOverlapEnabled`: Bật/tắt tính năng chống trùng lặp câu hỏi giữa các ca thi liền kề.
- **Quản lý trạng thái kỳ thi**: `DRAFT` ➔ `PUBLISHED` ➔ `IN_PROGRESS` ➔ `COMPLETED` / `CANCELLED`.
- **Cập nhật thông tin kỳ thi (`PUT /api/v1/exams/{id}`)** & **Xóa kỳ thi (`DELETE /api/v1/exams/{id}`)**.

### 2.2. Quản lý Thí sinh & Thuật toán Xếp Slot Ca thi (`ExamScheduleController` & `ExamScheduleService`)
- **Gán thí sinh (`POST /api/v1/exams/{id}/candidates`)**: Thêm danh sách ID sinh viên vào kỳ thi.
- **Thuật toán Tự động Xếp Ca thi (`POST /api/v1/exams/{id}/schedule/auto`)**:
  - Tự động chia tổng khung giờ kỳ thi (`startTime` -> `endTime`) thành các khoảng Slot thi cá nhân dựa vào `slotDurationMinutes` (vd: 15 phút) và `breakDurationMinutes` (vd: 2 phút).
  - Gán thời gian ca thi cá nhân (`scheduled_start_time`, `scheduled_end_time`) và số thứ tự ca (`slot_number`).
- **Đổi ca thi thủ công (`PUT /api/v1/exams/{id}/schedule/reschedule`)**: Cho phép Giảng viên điều chỉnh slot ca thi cho sinh viên xin hoãn/đổi ca.
- **Xem danh sách lịch thi (`GET /api/v1/exams/{id}/schedule`)**.

### 2.3. Thuật toán Cấp phát Câu hỏi Thích ứng & Anti-Overlap (`QuestionSelectorService`)
- Lọc danh sách câu hỏi đã được Giảng viên **DUYỆT (`APPROVED`)** từ Module 1 QuestionBank.
- **Thuật toán Anti-Overlap**: Kiểm tra các câu hỏi đã được chọn cho các thí sinh ở 2 ca thi ngay trước đó (Slot $K-1$, Slot $K-2$) và loại trừ khỏi danh sách bốc thăm ca hiện tại.
- Xáo trộn ngẫu nhiên có trọng số theo thang Bloom Level để chọn ra đủ số câu hỏi chính theo cấu hình.
- Tự động sinh danh sách `QuestionAttempt` cho từng thí sinh.

### 2.4. Dashboard Giám sát Ca thi Realtime & Xử lý Sự cố (`ExamMonitoringController` & `ExamMonitoringService`)
- **Dashboard Giám sát (`GET /api/v1/exams/{id}/monitor`)**: Thống kê số lượng ca thi thời gian thực: `SCHEDULED`, `READY`, `IN_PROGRESS`, `COMPLETED`, `ABSENT`, `CANCELLED`.
- **Reset Ca thi (`POST /api/v1/exams/{id}/attempts/{attemptId}/reset`)**: Cho phép Giảng viên/Giám thị reset ca thi của sinh viên bị rớt mạng/lỗi thiết bị về trạng thái `READY` để thực hiện thi lại.
- **Đánh dấu Vắng thi (`POST /api/v1/exams/{id}/attempts/{attemptId}/absent`)**.

### 2.5. API dành cho Sinh viên (`StudentExamController`)
- **Tra cứu ca thi cá nhân (`GET /api/v1/student/exams/{examId}/my-slot`)**: Trả về slot thi, thời gian ca thi, trạng thái và danh sách câu hỏi thi của sinh viên.

---

## 📡 3. Danh sách REST Endpoints Specifications

| Component | Endpoint | HTTP Method | Chức năng |
| :--- | :--- | :---: | :--- |
| `ExamController` | `/api/v1/exams` | `POST` | Tạo kỳ thi mới & cấu hình `exam_config` |
| `ExamController` | `/api/v1/exams` | `GET` | Danh sách kỳ thi (phân trang, lọc theo môn học/trạng thái) |
| `ExamController` | `/api/v1/exams/{id}` | `GET` | Chi tiết kỳ thi & danh sách ca thi |
| `ExamController` | `/api/v1/exams/{id}` | `PUT` | Cập nhật tên, thời gian & cấu hình kỳ thi |
| `ExamController` | `/api/v1/exams/{id}/status` | `PATCH` | Chuyển trạng thái kỳ thi |
| `ExamController` | `/api/v1/exams/{id}` | `DELETE` | Xóa kỳ thi (khi chưa diễn ra) |
| `ExamScheduleController` | `/api/v1/exams/{id}/candidates` | `POST` | Gán danh sách sinh viên vào kỳ thi |
| `ExamScheduleController` | `/api/v1/exams/{id}/schedule/auto` | `POST` | Tự động chia slot ca thi & cấp phát câu hỏi |
| `ExamScheduleController` | `/api/v1/exams/{id}/schedule/reschedule` | `PUT` | Điều chỉnh ca thi thủ công |
| `ExamScheduleController` | `/api/v1/exams/{id}/schedule` | `GET` | Danh sách ca thi cá nhân toàn bộ sinh viên |
| `ExamMonitoringController` | `/api/v1/exams/{id}/monitor` | `GET` | Dashboard thống kê giám sát ca thi realtime |
| `ExamMonitoringController` | `/api/v1/exams/{id}/attempts/{attemptId}/reset` | `POST` | Reset ca thi cho phép sinh viên thi lại khi gặp sự cố |
| `ExamMonitoringController` | `/api/v1/exams/{id}/attempts/{attemptId}/absent` | `POST` | Ghi nhận sinh viên vắng thi |
| `StudentExamController` | `/api/v1/student/exams/{examId}/my-slot` | `GET` | Sinh viên tra cứu ca thi cá nhân |

---

## 🗄️ 4. Cơ sở Dữ liệu & Flyway Script (`V4__enhance_exam_management.sql`)

Cấu trúc các bảng liên quan:
- **`exams`**: `id`, `course_id`, `title`, `start_time`, `end_time`, `status`, `exam_config` (JSONB), `created_by`, `created_at`.
- **`exam_attempts`**: `id`, `exam_id`, `student_id`, `scheduled_start_time`, `scheduled_end_time`, `actual_start_time`, `actual_end_time`, `slot_number`, `status`, `access_code`, `created_at`.
- **`question_attempts`**: `id`, `exam_attempt_id`, `question_id`, `question_order`, `status`, `created_at`.

Indexes đã được tạo:
- `idx_exams_status`, `idx_exams_created_by`
- `idx_exam_attempts_scheduled_time`, `idx_exam_attempts_status`, `idx_exam_attempts_student_exam`

---

## 🧪 5. Kết quả Kiểm thử (Unit Tests)

Bộ Unit tests đã được viết và chạy thành công 100%:
- `ExamServiceImplTest`: Kiểm thử tạo kỳ thi, kiểm tra thời gian hợp lệ, xử lý ngoại lệ `BusinessException` và `ResourceNotFoundException`. (PASSED ✅)
- `ExamScheduleServiceImplTest`: Kiểm thử thuật toán xếp slot ca thi tự động, xử lý trường hợp không đủ thời gian thi. (PASSED ✅)
- `QuestionSelectorServiceImplTest`: Kiểm thử thuật toán bốc thăm câu hỏi và cơ chế Anti-Overlap lọc câu hỏi ca trước. (PASSED ✅)

---

## 💻 6. Khởi chạy & Kiểm thử API Swagger

1. Khởi chạy Postgres:
   ```bash
   docker compose up -d
   ```
2. Chạy Unit Tests:
   ```bash
   .\mvnw.cmd test
   ```
3. Chạy Server Spring Boot:
   ```bash
   .\mvnw.cmd spring-boot:run
   ```
4. Truy cập Swagger UI:
   `http://localhost:8080/swagger-ui.html`
