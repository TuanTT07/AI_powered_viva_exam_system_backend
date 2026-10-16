# Hướng dẫn FE — Nhóm chức năng 1: Ngân hàng câu hỏi & Rubric

Tài liệu này mô tả **API đang có trong backend** để FE xây đủ màn hình và đúng luồng. Base URL ví dụ: `http://localhost:8080`. UUID, ngày giờ và nội dung trong các JSON bên dưới chỉ là mẫu. Không gọi Gemini trực tiếp từ trình duyệt và không đưa `GEMINI_API_KEY` vào FE.

## 1. Luồng nghiệp vụ và màn hình

```text
Admin đăng nhập → tạo/chọn course → phân công lecturer
                                     ↓
Lecturer đăng nhập → chọn course được phân công
                   ├─ quản lý rubric + tiêu chí
                   ├─ tạo/sửa câu hỏi thủ công hoặc import Excel
                   └─ upload tài liệu → kiểm tra READY → Generate AI
                                                        ↓
                                xem lại câu hỏi DRAFT → sửa nếu cần → Approve
                                                        ↓
                                                   APPROVED
```

| Màn hình/khối UI | Thành phần tối thiểu | API |
|---|---|---|
| Đăng nhập | Email, mật khẩu; lưu token; xử lý hết hạn token | `POST /api/auth/login`, `GET /api/auth/me` |
| Admin — course | Danh sách/tìm kiếm/phân trang; form tạo/sửa; chi tiết course | `/api/admin/courses` |
| Admin — phân công | Danh sách lecturer; danh sách lecturer của course; gán/gỡ | `GET /api/admin/users?role=LECTURER`, `/api/admin/courses/{courseId}/lecturers` |
| Workspace của course — Rubric | Danh sách, form tên/mô tả, các dòng tiêu chí và điểm tối đa; sửa/xóa | `/api/courses/{courseId}/rubrics` |
| Workspace — Tài liệu | Danh sách, trạng thái index/lỗi; upload PDF/DOCX/PPTX, xóa | `/api/courses/{courseId}/documents` |
| Workspace — Câu hỏi | Bảng lọc status/Bloom/từ khóa, phân trang; xem chi tiết, tạo/sửa/xóa | `/api/questions` |
| Import Excel | Tải mẫu, chọn `.xlsx`, rubric mặc định tùy chọn, bảng lỗi theo dòng | `/api/courses/{courseId}/questions/import-template`, `/import` |
| Tạo câu hỏi AI | Chọn số lượng, chủ đề, Bloom, rubric, tài liệu `READY`; loading/error | `/api/courses/{courseId}/questions/generate` |
| Duyệt câu hỏi | Xem lại nội dung, Bloom, rubric, nhãn AI; nút Approve; xác nhận | `GET /api/questions/{id}`, `POST /api/questions/{id}/approve` |

**Thiết kế trạng thái:** `DRAFT` là câu hỏi nháp, chưa được dùng cho thi. Chỉ `APPROVED` mới là câu hỏi đã duyệt. Câu hỏi tạo tay, import và AI đều bắt đầu ở `DRAFT`. FE **không tự động Approve** sau Generate/Import; giảng viên phải xem và quyết định.

## 2. Quy ước chung

- Trừ `POST /api/auth/login`, mọi request gửi `Authorization: Bearer <accessToken>`.
- JSON request dùng `Content-Type: application/json`. Upload dùng `FormData`; **không tự đặt** `Content-Type: multipart/form-data` vì browser cần tự tạo boundary.
- Actor/`createdById` được backend lấy từ JWT; FE không gửi `createdById`.
- `ADMIN` dùng được toàn bộ Question Bank. `LECTURER` chỉ thao tác trên course được phân công; course khác trả 403. `/api/admin/**` chỉ dành cho Admin.
- Ngày giờ trả dạng ISO-8601 UTC, ví dụ `2026-10-16T13:43:05Z`. Một vài response ngay sau khi tạo có thể trả `createdAt`/`updatedAt: null`; FE phải chấp nhận `null` và có thể GET lại bản ghi.
- Response JSON thông thường luôn bọc theo mẫu dưới. Riêng **Excel template** là file nhị phân, không phải JSON.

```json
{
  "success": true,
  "status": 200,
  "message": "Thành công",
  "data": {}
}
```

Danh sách phân trang (`GET /api/questions`, danh sách course/user của Admin) nằm ở `data`:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

`page` bắt đầu từ **0**; backend kẹp `size` trong khoảng 1–100. Enum phải gửi đúng giá trị:

```text
BloomLevel    = REMEMBER | UNDERSTAND | APPLY | ANALYZE | EVALUATE | CREATE
QuestionStatus = DRAFT | APPROVED
DocumentIndexStatus = PENDING | INDEXING | READY | FAILED
```

## 3. Đăng nhập, course và phân công

### Đăng nhập

`POST /api/auth/login` — không cần token:

```json
{ "email": "lecturer@example.com", "password": "mat-khau" }
```

HTTP 200, `data`:

```json
{
  "accessToken": "<JWT>",
  "tokenType": "Bearer",
  "expiresIn": 7200,
  "user": {
    "id": "11111111-1111-4111-8111-111111111111",
    "userCode": "GV001",
    "fullName": "Nguyễn Văn A",
    "email": "lecturer@example.com",
    "roleName": "LECTURER",
    "createdAt": "2026-10-16T13:00:00Z",
    "updatedAt": "2026-10-16T13:00:00Z"
  }
}
```

`GET /api/auth/me` trả `data` là object `user` tương tự, để khôi phục phiên và điều hướng theo `roleName`.

### Admin tạo course, phân công lecturer

| Method + path | Request | Response `data` |
|---|---|---|
| `GET /api/admin/courses?keyword=&page=0&size=10` | Query tùy chọn | `PageResponse<CourseResponse>` |
| `GET /api/admin/courses/{id}` | — | `CourseDetailResponse` có `lecturers[]` |
| `POST /api/admin/courses` | `CourseRequest` | `CourseDetailResponse`, HTTP 201 |
| `PUT /api/admin/courses/{id}` | `CourseRequest` | `CourseDetailResponse` |
| `DELETE /api/admin/courses/{id}` | — | `null`; có thể bị chặn nếu đã có dữ liệu liên quan |
| `GET /api/admin/users?role=LECTURER&keyword=&page=0&size=10` | Query tùy chọn | `PageResponse<UserResponse>` để chọn giảng viên |
| `GET /api/admin/courses/{courseId}/lecturers` | — | `LecturerResponse[]` |
| `PUT /api/admin/courses/{courseId}/lecturers/{lecturerId}` | Không body | `CourseDetailResponse` |
| `DELETE /api/admin/courses/{courseId}/lecturers/{lecturerId}` | Không body | `null` |

`CourseRequest` (`courseCode` và `courseName` bắt buộc; `department` tùy chọn):

```json
{
  "courseCode": "PRJ301",
  "courseName": "Lập trình Java",
  "department": "Software Engineering"
}
```

`CourseDetailResponse` mẫu:

```json
{
  "id": "22222222-2222-4222-8222-222222222222",
  "courseCode": "PRJ301",
  "courseName": "Lập trình Java",
  "department": "Software Engineering",
  "createdAt": "2026-10-16T13:00:00Z",
  "lecturers": [
    {
      "id": "11111111-1111-4111-8111-111111111111",
      "userCode": "GV001",
      "fullName": "Nguyễn Văn A",
      "email": "lecturer@example.com"
    }
  ]
}
```

Nếu cần tạo tài khoản lecturer từ FE Admin: `POST /api/admin/users` nhận `userCode`, `fullName`, `email`, `password` (6–100 ký tự), `roleName: "LECTURER"`; trả HTTP 201 với `UserResponse` trong `data`.

> **Khoảng trống API hiện tại:** chưa có endpoint không thuộc `/api/admin/**` để lecturer tự lấy danh sách course mình được phân công. FE lecturer không thể gọi `GET /api/admin/users/{id}/courses` vì bị 403. Cần backend bổ sung endpoint “my courses”, hoặc có cơ chế cung cấp `courseId` hợp lệ từ nơi khác; đừng giả định lecturer gọi được API Admin.

## 4. Rubric theo course

| Method + path | Ý nghĩa |
|---|---|
| `GET /api/courses/{courseId}/rubrics` | Trả `RubricResponse[]` |
| `GET /api/courses/{courseId}/rubrics/{id}` | Chi tiết một rubric |
| `POST /api/courses/{courseId}/rubrics` | Tạo, HTTP 201 |
| `PUT /api/courses/{courseId}/rubrics/{id}` | Sửa toàn bộ rubric/criteria |
| `DELETE /api/courses/{courseId}/rubrics/{id}` | Xóa, bị 409 nếu đã có câu hỏi dùng rubric |

Body của POST/PUT (`criteria` phải có ít nhất 1 dòng; `maxScore` > 0):

```json
{
  "rubricName": "Rubric vấn đáp Java",
  "description": "Đánh giá kiến thức và lập luận",
  "criteria": [
    { "criterionName": "Kiến thức", "description": "Trả lời đúng", "maxScore": 6 },
    { "criterionName": "Giải thích", "description": "Lập luận rõ", "maxScore": 4 }
  ]
}
```

`RubricResponse` trong `data`:

```json
{
  "id": "33333333-3333-4333-8333-333333333333",
  "courseId": "22222222-2222-4222-8222-222222222222",
  "rubricName": "Rubric vấn đáp Java",
  "description": "Đánh giá kiến thức và lập luận",
  "criteria": [
    { "id": "44444444-4444-4444-8444-444444444444", "criterionName": "Kiến thức", "description": "Trả lời đúng", "maxScore": 6 },
    { "id": "55555555-5555-4555-8555-555555555555", "criterionName": "Giải thích", "description": "Lập luận rõ", "maxScore": 4 }
  ],
  "totalMaxScore": 10,
  "createdAt": "2026-10-16T13:00:00Z"
}
```

Form câu hỏi/Generate chỉ cho chọn rubric của **cùng course**. `rubricId` có thể là `null`.

## 5. Tài liệu nguồn

| Method + path | Ý nghĩa |
|---|---|
| `GET /api/courses/{courseId}/documents` | Trả `DocumentResponse[]`, mới nhất trước |
| `POST /api/courses/{courseId}/documents` | Upload **một** file, HTTP 200 |
| `DELETE /api/courses/{courseId}/documents/{documentId}` | Xóa; bị 409 nếu tài liệu là nguồn của câu hỏi AI |

Upload bằng `FormData`:

```ts
const form = new FormData();
form.append('title', 'Tài liệu Java'); // không bắt buộc
form.append('file', selectedFile);     // bắt buộc: File thật, không phải tên/path
await fetch(`/api/courses/${courseId}/documents`, {
  method: 'POST',
  headers: { Authorization: `Bearer ${token}` }, // KHÔNG tự thêm Content-Type
  body: form,
});
```

Chỉ nhận `.pdf`, `.docx`, `.pptx`, giới hạn mặc định `APP_MAX_UPLOAD_BYTES=26214400` (25 MiB), có thể thay đổi theo môi trường. Ví dụ `DocumentResponse`:

```json
{
  "id": "66666666-6666-4666-8666-666666666666",
  "title": "Tài liệu Java",
  "originalFilename": "java.pdf",
  "contentType": "application/pdf",
  "fileSize": 82202,
  "indexingStatus": "READY",
  "indexingError": null,
  "createdAt": "2026-10-16T13:00:00Z"
}
```

**Rất quan trọng:** API upload hiện có thể trả `success: true`, HTTP 200 và lời nhắn “thành công” **ngay cả khi** `data.indexingStatus === "FAILED"`. FE phải nhìn vào `indexingStatus`, hiển thị `indexingError` nếu `FAILED`, và chỉ cho dùng tài liệu `READY` để Generate. Không có API retry index hay API tải lại nội dung tài liệu trong module này.

## 6. Câu hỏi: danh sách, tạo, sửa, xóa, duyệt

| Method + path | Request | Response `data` |
|---|---|---|
| `GET /api/questions?courseId={uuid}&status=DRAFT&bloomLevel=UNDERSTAND&keyword=java&page=0&size=20` | `courseId` bắt buộc; các filter khác tùy chọn | `PageResponse<QuestionResponse>`; sắp theo `updatedAt` giảm dần |
| `GET /api/questions/{id}` | — | `QuestionResponse` |
| `POST /api/questions` | `QuestionRequest` | `QuestionResponse` với `DRAFT`, HTTP 201 |
| `PUT /api/questions/{id}` | `QuestionRequest` | `QuestionResponse`, luôn chuyển về `DRAFT` |
| `POST /api/questions/{id}/approve` | Không body | `QuestionResponse` với `APPROVED`, HTTP 200 |
| `DELETE /api/questions/{id}` | — | `null`; bị 409 nếu đã cấp vào một lần thi |

Body tạo/sửa câu hỏi thủ công:

```json
{
  "courseId": "22222222-2222-4222-8222-222222222222",
  "rubricId": "33333333-3333-4333-8333-333333333333",
  "content": "Phân biệt abstraction và encapsulation trong Java.",
  "bloomLevel": "UNDERSTAND",
  "aiGenerated": false
}
```

`courseId`, `content` không rỗng, `bloomLevel` bắt buộc; `rubricId` tùy chọn. Dù `aiGenerated` có trong DTO request hiện tại, FE tạo thủ công nên gửi `false`; **không dùng field này để giả mạo câu hỏi AI**. Không gửi `status` hay `createdById` trong body.

`QuestionResponse` mẫu:

```json
{
  "id": "77777777-7777-4777-8777-777777777777",
  "courseId": "22222222-2222-4222-8222-222222222222",
  "courseCode": "PRJ301",
  "rubricId": "33333333-3333-4333-8333-333333333333",
  "rubricName": "Rubric vấn đáp Java",
  "createdById": "11111111-1111-4111-8111-111111111111",
  "content": "Phân biệt abstraction và encapsulation trong Java.",
  "bloomLevel": "UNDERSTAND",
  "aiGenerated": false,
  "status": "DRAFT",
  "createdAt": "2026-10-16T13:00:00Z",
  "updatedAt": "2026-10-16T13:00:00Z"
}
```

### Luồng Approve bắt buộc thể hiện trên UI

1. Sau tạo/import/Generate, mở danh sách lọc `status=DRAFT`.
2. Cho giảng viên xem đầy đủ nội dung, Bloom, rubric, nhãn `aiGenerated`; có thể `PUT` để chỉnh sửa trước khi duyệt.
3. Khi nhấn **Approve**, gọi `POST /api/questions/{id}/approve` và kiểm tra `data.status === "APPROVED"`.
4. Chuyển câu hỏi sang danh sách `APPROVED` hoặc cập nhật cache. Đây mới là trạng thái sử dụng cho thi.
5. Nếu sửa câu hỏi đã `APPROVED` bằng `PUT`, backend đưa về `DRAFT`. UI cần cảnh báo trước khi lưu và cập nhật cả hai tab. Không có endpoint “unapprove” riêng.

## 7. Import Excel `.xlsx`

`GET /api/courses/{courseId}/questions/import-template` trả file `.xlsx` (MIME `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`). FE tải dưới dạng Blob/file; **không parse như JSON**. Sheet đầu có header `content`, `bloomLevel`, `rubricId`. `rubricId` trong từng dòng có thể trống nếu chọn rubric mặc định hoặc muốn không gắn rubric.

`POST /api/courses/{courseId}/questions/import` dùng `multipart/form-data`:

- `file`: một file `.xlsx` (bắt buộc).
- `defaultRubricId`: UUID tùy chọn, gửi như field form hoặc query parameter. Giá trị `rubricId` không trống ở dòng Excel sẽ ưu tiên hơn mặc định.

Ví dụ `data` khi thành công:

```json
{ "importedCount": 3, "errors": [] }
```

Ví dụ **HTTP 200 nhưng import thất bại** do dữ liệu từng dòng:

```json
{
  "success": true,
  "status": 200,
  "message": "Import thất bại; không có dòng nào được lưu",
  "data": {
    "importedCount": 0,
    "errors": [
      { "row": 3, "field": "bloomLevel", "message": "Bloom level không hợp lệ" },
      { "row": 5, "field": "content", "message": "Không được để trống" }
    ]
  }
}
```

FE quyết định import thành công bằng `data.errors.length === 0`, **không chỉ** bằng HTTP 200/`success`. Backend kiểm tra toàn workbook trước; có lỗi thì không lưu dòng nào. Dòng `row` là số dòng Excel để FE hiển thị/sửa. Thiếu header hoặc file không hợp lệ thường trả 400.

## 8. Generate AI từ tài liệu

`POST /api/courses/{courseId}/questions/generate` — JSON:

```json
{
  "count": 1,
  "topic": "Lập trình hướng đối tượng Java",
  "bloomLevels": ["UNDERSTAND", "APPLY"],
  "rubricId": "33333333-3333-4333-8333-333333333333",
  "documentIds": ["66666666-6666-4666-8666-666666666666"]
}
```

| Field | Kiểu / kiểm tra | UI |
|---|---|---|
| `count` | `integer`, bắt buộc, 1–20 | Ô số; nên thử 1 trước |
| `topic` | `string`, tùy chọn | Ô chủ đề; bỏ trống để bao quát tài liệu |
| `bloomLevels` | `BloomLevel[]`, tùy chọn; mặc định `UNDERSTAND` | Multi-select |
| `rubricId` | UUID hoặc `null`, tùy chọn; phải cùng course | Select từ rubric của course |
| `documentIds` | UUID[] tùy chọn; nếu bỏ trống backend chọn tất cả tài liệu của course | Multi-select chỉ tài liệu `READY` |

HTTP 200 trả `data: QuestionResponse[]`. Tất cả kết quả hợp lệ được lưu với `aiGenerated: true`, `status: "DRAFT"`; FE đưa người dùng sang màn review rồi mới Approve. Khi Gemini lỗi/timeout, backend trả lỗi 5xx và **không lưu câu hỏi nháp dở dang**.

Lưu ý kỹ thuật hiện tại: backend xác thực `documentIds` thuộc course và đều `READY`, rồi ghi chúng làm nguồn của câu hỏi; tuy nhiên truy vấn Gemini File Search dùng **store chung của course**, chưa filter thật sự theo từng `documentId`. FE không nên hứa rằng AI chỉ đọc đúng các file được tick nếu trong course còn file khác.

Generate có thể chậm: hiển thị loading, khóa nút để tránh gửi trùng. Với lỗi 504 có interaction ID, đừng tự động retry ngay vì tác vụ phía Gemini có thể còn chạy. FE chỉ cần hiển thị lỗi từ backend; không gọi Gemini bằng API key ở browser.

## 9. Xử lý lỗi và checklist nghiệm thu FE

Lỗi thông thường:

```json
{ "success": false, "status": 403, "message": "Bạn không được phân công quản lý ngân hàng câu hỏi của môn này", "data": null }
```

Validation 400 có thể trả `data` là object lỗi theo field, ví dụ `{ "email": "Email không được để trống" }`. Xử lý 401 bằng đăng nhập lại; 403 bằng màn không đủ quyền; 409 bằng thông báo ràng buộc; 502/503/504 bằng thông báo lỗi AI, không coi là câu hỏi đã tạo. Không dựa vào `message` để suy ra thành công của **upload/import**; kiểm tra field dữ liệu như các mục trên.

Checklist trước khi bàn giao FE:

- [ ] Admin tạo course, phân công lecturer; lecturer khác course nhận 403.
- [ ] Rubric có ít nhất một tiêu chí; chỉ gắn rubric cùng course.
- [ ] Upload file thật qua `FormData`; chỉ cho Generate khi `indexingStatus === "READY"`.
- [ ] Import hiển thị lỗi từng dòng và không báo thành công khi `errors.length > 0`.
- [ ] Câu hỏi tạo tay/import/AI xuất hiện ở `DRAFT`, có nút review/Approve.
- [ ] Approve đổi sang `APPROVED`; sửa câu hỏi đã duyệt làm quay lại `DRAFT`.
- [ ] Danh sách câu hỏi có lọc course/status/Bloom/từ khóa và phân trang từ page 0.
- [ ] Không gửi API key Gemini, không gửi `createdById`, không dùng API Admin từ phiên lecturer.
