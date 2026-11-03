# API bổ sung cho FE

## Bằng chứng chấm điểm

`GET /api/question-attempts/{questionAttemptId}/evaluation`

Giữ nguyên `ApiResponse<EvaluationResponse>`, thêm trong `data`:

```json
{
  "transcript": [
    {
      "id": "<UUID>",
      "turnOrder": 1,
      "speaker": "AI",
      "textContent": "Nội dung câu hỏi",
      "audioUrl": null,
      "turnType": "MAIN_QUESTION",
      "createdAt": "2026-11-03T10:00:00+07:00"
    }
  ],
  "audioUrl": "/path/to/exam-recording.wav"
}
```

- `transcript` sắp xếp theo `turnOrder`, gồm AI và STUDENT, chỉ lấy lần chạy hiện tại sau khi reset câu hỏi. Chưa có hội thoại trả `[]`.
- `audioUrl` cấp evaluation là `audioRecordUrl` của toàn lượt thi. URL trong mỗi turn là file âm thanh riêng của turn đó. Chưa có file đã lưu thì trả `null`; API này không tự tạo bản ghi âm.
- Các response evaluation khác dùng chung DTO cũng có hai trường này.
- Chỉ Admin hoặc giảng viên được phân công môn học được xem.

## Kết quả theo kỳ thi

`GET /api/exams/{examId}/evaluations?page=0&size=20&status=GRADED`

Response tương tự API theo môn học: `ApiResponse<PageResponse<EvaluationResponse>>`.
`status` không bắt buộc; bỏ qua để lấy mọi evaluation đã có (bao gồm `PENDING_REVIEW`), dùng `GRADED` để chỉ lấy bài đã chốt điểm. Sắp xếp `createdAt` giảm dần. Mỗi phần tử là kết quả của một câu hỏi (`questionAttemptId`), giống API theo môn học.

## Cấu hình câu hỏi

`GET /api/v1/exams/{examId}/question-config`

`PUT /api/v1/exams/{examId}/question-config`

PUT nhận và trả trực tiếp JSON sau (không bọc `ApiResponse`):

```json
{
  "questionIds": ["<UUID>"],
  "timeLimitPerTurnSeconds": 60,
  "maxFollowUpQuestions": 2
}
```

- Cả ba trường đều bắt buộc khi PUT. Thời gian tối thiểu 10 giây, số lượt hỏi thêm >= 0.
- Câu hỏi không được trùng, phải tồn tại, thuộc môn học của kỳ thi và ở trạng thái `APPROVED`.
- `questionIds` là tập câu hỏi để bốc cho các ca được sinh mới; số câu chính, chống trùng và bốc ngẫu nhiên vẫn theo cấu hình kỳ thi hiện có. `[]` dùng toàn bộ ngân hàng đã duyệt của môn học.
- Không thay đổi các câu hỏi đã được gán cho ca thi trước đó.
- Chỉ cho sửa kỳ thi `DRAFT` hoặc `PUBLISHED`; chỉ Admin hoặc giảng viên được phân công môn học được đọc/sửa.
- Chưa có cấu hình: `questionIds=[]`, thời gian 60 giây, tối đa 2 lượt hỏi thêm.

## Tổng thí sinh

`GET /api/v1/exams` thêm `totalCandidates` trong mỗi phần tử `content`.
Giá trị là số sinh viên phân biệt đã được gán ca thi cho kỳ thi, không phải tổng sinh viên đăng ký môn học.

Không cần migration: câu hỏi được lưu trong JSONB `exams.exam_config`; transcript và URL âm thanh dùng dữ liệu hiện có.
