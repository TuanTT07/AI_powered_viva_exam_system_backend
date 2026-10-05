# Question Bank MVP API

Các API này giả định `courseId` và `createdById` đã tồn tại trong bảng `courses` và `users`.
Authentication hiện đang `permitAll` theo cấu hình development của dự án; khi thêm JWT, `createdById` phải lấy từ user đăng nhập thay vì request body.

## Rubrics

- `POST /api/rubrics`
- `GET /api/rubrics`
- `GET /api/rubrics/{id}`
- `PUT /api/rubrics/{id}`
- `DELETE /api/rubrics/{id}`

Ví dụ tạo rubric:

```json
{
  "rubricName": "Trả lời vấn đáp Java",
  "description": "Đánh giá kiến thức và khả năng diễn đạt",
  "criteria": [
    { "criterionName": "Đúng kiến thức", "description": "Nêu đúng khái niệm", "maxScore": 6 },
    { "criterionName": "Lập luận", "description": "Giải thích mạch lạc", "maxScore": 4 }
  ]
}
```

Không thể xoá rubric đang được gán cho một hay nhiều câu hỏi; API trả `409 Conflict`.

## Questions

- `POST /api/questions` tạo câu hỏi nháp.
- `GET /api/questions?courseId=&status=&bloomLevel=&keyword=&page=0&size=20` tìm/lọc có phân trang.
- `GET /api/questions/{id}`
- `PUT /api/questions/{id}`
- `POST /api/questions/{id}/approve` duyệt câu hỏi.
- `DELETE /api/questions/{id}`

Ví dụ tạo câu hỏi:

```json
{
  "courseId": "<uuid-course>",
  "rubricId": "<uuid-rubric>",
  "createdById": "<uuid-lecturer>",
  "content": "Giải thích sự khác nhau giữa authentication và authorization.",
  "bloomLevel": "UNDERSTAND",
  "aiGenerated": false
}
```

Các Bloom level hỗ trợ: `REMEMBER`, `UNDERSTAND`, `APPLY`, `ANALYZE`, `EVALUATE`, `CREATE`.
