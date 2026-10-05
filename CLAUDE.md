# AIVES – AI-powered Viva Exam System (Backend)

## Tech Stack
- Java 21
- Spring Boot 4.1.1
- Maven
- PostgreSQL (chạy local bằng Docker Compose: `docker compose up -d`)
- JPA / Hibernate
- Flyway (quản lý schema DB)
- springdoc-openapi (Swagger UI: http://localhost:8080/swagger-ui.html)

## Architecture
- Layered Architecture (kiến trúc phân lớp)
- Controller → Service → Repository
- Controller  →  Service  →  Repository  →  DB  (nhận request)  (xử lý nghiệp vụ)  (truy cập dữ liệu)
- DTO dùng cho request/response (dùng Java `record`)
- Entity chỉ dùng cho persistence, không trả Entity ra API
- Mapping Entity ↔ DTO viết tay trong class `XxxMapper`

## Coding Rules
- Use constructor injection
- Không dùng field injection
- REST API trả về DTO
- Validation dùng Jakarta Validation (`@Valid` + `@NotBlank`, `@Size`...)
- Lỗi: ném `ResourceNotFoundException` / `BusinessException`, `GlobalExceptionHandler` trả về `ApiResponse` với `success=false`
- Schema DB: KHÔNG dùng `ddl-auto: update`. Mọi thay đổi bảng viết thành file Flyway mới
  `src/main/resources/db/migration/V{n}__mo_ta.sql`, không sửa file migration đã chạy.
- Gọi AI qua interface (`AiClient`), không gọi thẳng SDK trong service.

## API conventions
- MỌI API (thành công lẫn lỗi) trả cùng format `ApiResponse<T>` (common/dto):
  `{ "success": true, "status": 200, "message": "Lấy dữ liệu thành công", "data": {...} }`
- Controller trả `ResponseEntity<ApiResponse<T>>`, dùng helper:
  `ApiResponse.ok(message, data)` (200), `ApiResponse.ok(message)` (200, data = null, dùng cho xoá),
  `ApiResponse.created(location, message, data)` (201). Không dùng 204 vì 204 không có body.
- Lỗi validation: `status = 400`, `data` = map `{ field: message }`.
- API có phân trang trả `PageResponse<T>` (common/dto), tạo Pageable bằng `PageableUtils.of(page, size, sort)`.
- Tìm kiếm theo từ khoá: service gọi `SearchUtils.toLikePattern(keyword)` rồi truyền vào `LIKE :keyword`.
- Service: `@Transactional(readOnly = true)` ở class, hàm ghi đánh `@Transactional`; map Entity -> DTO bên trong service.
- API cho Admin đặt dưới `/api/admin/**`.

## Package Structure (chia theo feature)
src/main/java/cocxanhcoder/viva/exam/system
├── Application.java
├── common/            # dùng chung cho mọi module
│   ├── config/        # SecurityConfig (+ PasswordEncoder BCrypt), ...
│   ├── dto/           # ApiResponse, PageResponse
│   ├── util/          # PageableUtils, SearchUtils
│   └── exception/     # custom exception + GlobalExceptionHandler
├── academic/          # Quản trị hệ thống: tài khoản, role, môn học, phân công GV - môn
│   ├── controller/    # AdminUserController, AdminRoleController, AdminCourseController
│   ├── service/       # UserService, CourseService
│   ├── dto/
│   ├── mapper/
│   ├── entity/        # Role, User, Course, Document
│   └── repository/
├── questionbank/      # Module 1: ngân hàng câu hỏi + rubric
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── dto/
│   ├── mapper/
│   └── entity/
├── interview/         # Module 3: AI interview core (cùng cấu trúc)
└── grading/           # Module 4: AI grading support (cùng cấu trúc)

## Thứ tự phát triển
Module 1 (questionbank) → Module 3 (interview) → Module 4 (grading)

## Database
- Schema gốc: `V1__init_schema.sql` (12 bảng, theo thiết kế DB của nhóm). Role mặc định: `V2__seed_roles.sql`.
  Phân công giảng viên - môn học (N-N): `V3__course_lecturers.sql` -> `Course.lecturers` (`@ManyToMany`).
- Entity: id `UUID` (`GenerationType.UUID`), thời gian `OffsetDateTime` (TIMESTAMPTZ),
  cột TEXT dùng `@Column(columnDefinition = "TEXT")`, enum dùng `@Enumerated(EnumType.STRING)`.
- Quan hệ `@ManyToOne` luôn `fetch = LAZY`. Không dùng `@Data` của Lombok cho entity (chỉ `@Getter @Setter`).

## Cấu hình môi trường
- Biến môi trường để trong `.env` ở thư mục gốc (không commit). Mẫu: `.env.example` (có commit).
- Spring đọc `.env` qua `spring.config.import: optional:file:.env[.properties]`; Docker Compose tự đọc `.env`.
- Thêm biến mới: thêm vào cả `.env.example` và dùng `${TEN_BIEN:mac_dinh}` trong `application.yml`.

## Ghi chú
- `SecurityConfig` hiện đang `permitAll` để dev; sẽ thay bằng JWT + role (Admin/Lecturer/Student).
