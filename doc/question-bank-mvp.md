# Question Bank & Rubric API

Mọi endpoint cần `Authorization: Bearer <JWT>`. `ADMIN` có toàn quyền; `LECTURER` chỉ thao tác các môn đã được Admin phân công. Mọi response JSON (trừ file Excel tải về) dùng envelope `ApiResponse`.

## Rubric theo môn học

- `POST /api/courses/{courseId}/rubrics`
- `GET /api/courses/{courseId}/rubrics`
- `GET|PUT|DELETE /api/courses/{courseId}/rubrics/{id}`

Body tạo/cập nhật giữ nguyên tên rubric, mô tả và danh sách criteria. Rubric chỉ gắn được vào câu hỏi của chính course đó và không thể xoá khi còn câu hỏi sử dụng.

## Câu hỏi

- `POST /api/questions` tạo nháp; body gồm `courseId`, `rubricId` (tùy chọn), `content`, `bloomLevel`, `aiGenerated`.
- `GET /api/questions?courseId={id}&status=&bloomLevel=&keyword=&page=0&size=20`
- `GET|PUT|DELETE /api/questions/{id}`
- `POST /api/questions/{id}/approve`

Người tạo được lấy từ JWT, không được gửi `createdById`. Cập nhật một câu hỏi đã duyệt luôn đưa trạng thái về `DRAFT`. Câu hỏi đã xuất hiện trong ca thi không thể xoá.

## Import Excel

- `GET /api/courses/{courseId}/questions/import-template`
- `POST /api/courses/{courseId}/questions/import` dạng `multipart/form-data`: `file` (`.xlsx`) và `defaultRubricId` tùy chọn.

Sheet đầu tiên phải có cột `content`, `bloomLevel`, `rubricId`; cột rubricId có thể trống nếu gửi `defaultRubricId`. File được kiểm tra toàn bộ trước khi ghi; khi có lỗi, response trả số dòng/cột lỗi và không có dòng nào được lưu.

## Tài liệu nguồn và AI

- `GET|POST /api/courses/{courseId}/documents`; upload gửi `file` (PDF/DOCX/PPTX) và `title` tùy chọn.
- `DELETE /api/courses/{courseId}/documents/{documentId}`.
- `POST /api/courses/{courseId}/questions/generate` với `{ count, topic?, bloomLevels?, rubricId?, documentIds? }`.

Tài liệu được lưu tại `APP_UPLOAD_DIR`, trích text và index vào Gemini File Search. Provider mặc định là `gemini`; cần cấu hình `GEMINI_API_KEY` và `GEMINI_MODEL`. AI chỉ tạo câu hỏi `DRAFT`; câu hỏi giữ liên kết tới các tài liệu nguồn được chọn nên tài liệu đó không thể bị xoá trước khi xoá các câu hỏi tham chiếu.
