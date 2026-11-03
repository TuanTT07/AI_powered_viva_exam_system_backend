# Test toàn luồng phỏng vấn bằng Postman

Dùng backend và DB thử nghiệm. Backend cần ADMIN đã đổi mật khẩu, `SPEECH_ENABLED=true`, key Deepgram, key/model của AI provider đang dùng và CORS cho `http://localhost:5173`.

Import `postman_collection_interview_e2e.json`. Đặt `adminEmail`, `adminPassword`, `initialPassword` và `testPassword` trong Environment cục bộ. Hai mật khẩu thử cần khác nhau và dài ít nhất 6 ký tự. Đặt `baseUrl` nếu backend dùng port khác. Collection tự lưu ID và token. Chạy từng folder theo thứ tự.

1. **01** tạo giảng viên, sinh viên, môn học, phân công giảng viên và đổi mật khẩu.
2. Chuẩn bị trang audio. Chạy `python -m http.server 5173 --bind 127.0.0.1 --directory doc` từ repo và mở `http://localhost:5173/speech-smoke.html`.
3. **02** tạo rubric, duyệt câu chính, xếp ca, đưa lượt thi về READY và bắt đầu thi. Deadline câu đầu là 180 giây kể từ Start.
4. Điền `baseUrl`, `studentToken`, `attemptId` và `questionTurnId` từ Collection Variables vào trang smoke. Chọn `doc/test-audio/deepgram-test-vi.pcm`, bấm **Kết nối và gửi audio**, đợi `answer.finalized`.
5. **03** kiểm tra câu follow-up. Nếu `PROCESSING`, gửi lại sau khoảng một giây. Test đạt khi API trả lượt hỏi mới trên cùng `questionAttemptId`. Nếu AI quyết định chuyển câu hoặc fallback, API trả `COMPLETED` và lần thử follow-up báo lỗi.
6. Ngắt WebSocket cũ. Thay `questionTurnId` bằng ID mới đã lưu ở bước 03. Gửi PCM trả lời câu hỏi xoáy và đợi `answer.finalized`. Đổi WAV sang PCM bằng `ffmpeg -i answer.wav -ac 1 -ar 16000 -c:a pcm_s16le -f s16le answer.pcm`.
7. **04** xác nhận `COMPLETED`, AI chấm transcript, giảng viên chốt điểm và đọc lại trạng thái `GRADED`.

## Test riêng BE1 bằng text và AI thật

Chạy backend local với profile test và cấu hình key/model Gemini hoặc OpenAI.

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local-test'
```

Chạy folder 01 và 02. Gửi request **Nộp text — Câu chính** trong folder **Test riêng BE1 — Text + AI thật**. Chạy folder 03 để lấy câu hỏi mới, rồi gửi **Nộp text — Câu hỏi xoáy** với nội dung phù hợp. Chạy request **Xác nhận kết thúc lượt thi** trong folder 04. Hai request nộp text thay cho bước gửi PCM. Endpoint test chỉ có khi bật profile local-test.
