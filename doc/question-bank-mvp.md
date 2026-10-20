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

Nếu không gửi `topic` (hoặc để trống), backend chọn ngẫu nhiên các đoạn văn bản đã trích từ những tài liệu `READY` được chọn (`documentIds`; bỏ trống thì dùng tài liệu của course), rồi yêu cầu AI tạo câu hỏi từ các đoạn đó. Nhánh này không dùng truy vấn File Search chung chung. Nếu file không trích được chữ (ví dụ PDF chỉ chứa ảnh), API trả 422; cần tài liệu có text hoặc OCR trước. Có `topic` thì AI tiếp tục truy hồi File Search theo chủ đề.

Tài liệu được lưu tại `APP_UPLOAD_DIR` và lập chỉ mục theo provider trong `AI_PROVIDER` (`gemini` hoặc `openai`). Cấu hình OpenAI: `AI_PROVIDER=openai`, `OPENAI_MODEL=gpt-5.4-mini`, `OPENAI_API_KEY=<key>`, `AI_TIMEOUT_SECONDS=45` (thời gian chờ đọc mỗi HTTP request) và `AI_MAX_OUTPUT_TOKENS=700` (giới hạn cả token suy luận lẫn nội dung trả về). Để trống API key thì ứng dụng vẫn khởi động nhưng upload/index và Generate không dùng được OpenAI. `AI_EXPLAIN_LIMIT_PER_USER` hiện chỉ là cấu hình dự phòng, chưa có API giải thích theo người dùng để áp dụng giới hạn này.

Một course đã có Gemini File Search store không tự chuyển thành OpenAI vector store khi đổi provider; cần tạo course mới và upload/index lại tài liệu bằng OpenAI, hoặc xây luồng migration riêng. AI chỉ tạo câu hỏi `DRAFT`; câu hỏi giữ liên kết tới các tài liệu nguồn được chọn nên tài liệu đó không thể bị xoá trước khi xoá các câu hỏi tham chiếu.
