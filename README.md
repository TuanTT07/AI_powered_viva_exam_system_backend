# AIVES – AI-powered Viva Exam System (Backend)

Hệ thống Thi Vấn đáp Thông minh hỗ trợ Giảng viên tổ chức, điều phối ca thi, tự động chọn câu hỏi thích ứng theo thang Bloom và chống trùng lặp, bóc băng giọng nói thời gian thực và chấm điểm gợi ý theo Rubric.

---

## 🛠️ Tech Stack
- **Java 21**
- **Spring Boot 4.1.1**
- **Maven**
- **PostgreSQL** (chạy local qua Docker Compose)
- **Spring Data JPA / Hibernate**
- **Flyway** (Quản lý Database Migration)
- **springdoc-openapi** (Swagger UI: `http://localhost:8080/swagger-ui.html`)

---

## 📌 Các Phân hệ Tính năng (Modules)

1. **Module 1: Ngân hàng câu hỏi & Rubric (`questionbank`)**
2. **Module 2: Quản lý kỳ thi & lịch thi (`exammgt`)**: Xem chi tiết tài liệu tính năng tại [MODULE2_EXAM_MANAGEMENT.md](file:///d:/CloneRepo/AI_powered_viva_exam_system_backend/MODULE2_EXAM_MANAGEMENT.md)
3. **Module 3: Lõi phỏng vấn AI (`interview`)**
4. **Module 4: Hỗ trợ chấm điểm bằng AI (`grading`)**

---

## 💻 Hướng dẫn Khởi chạy & Kiểm thử

### 1. Khởi chạy Database local
```bash
docker compose up -d
```

### 2. Biên dịch & Chạy Unit Tests
```bash
# Windows
.\mvnw.cmd test

# Linux/macOS
./mvnw test
```

### 3. Chạy Server
```bash
.\mvnw.cmd spring-boot:run
```

TRUY CẬP SWAGGER UI DOCUMENTATION: `http://localhost:8080/swagger-ui.html`
