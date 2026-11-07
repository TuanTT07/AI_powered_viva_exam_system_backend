# Backend 2 — Deepgram STT và Deepgram Flux TTS

Xem [bàn giao cho nhóm](TEAM_HANDOFF.md) để phân biệt phần nền interview thuộc BE1,
speech thuộc BE2 và các API/event còn cần thống nhất. Tài liệu này tập trung vào
phần speech đã có; chưa mô tả một buổi thi hoàn chỉnh đã hoạt động xuyên suốt.

## Cấu hình nhanh

Backend mặc định chọn **Deepgram Nova-3** cho STT streaming tiếng Việt.
Không cần Azure subscription, Azure key, region hay native Speech SDK.
TTS tạo MP3 bằng Deepgram Flux, mặc định giọng nam Minh `flux-minh-vi` (Early Access).

Trong `.env` ở thư mục gốc, điền:

```properties
SPEECH_ENABLED=true
SPEECH_STT_PROVIDER=deepgram
DEEPGRAM_API_KEY=<api-key-cua-ban>
DEEPGRAM_STT_MODEL=nova-3
DEEPGRAM_STT_LANGUAGE=vi
DEEPGRAM_TTS_MODEL=flux-minh-vi
SPEECH_LANGUAGE=vi-VN
```

`DEEPGRAM_STT_LANGUAGE=vi` là ngôn ngữ STT; `SPEECH_LANGUAGE=vi-VN` là locale cho
Deepgram Flux TTS. Không commit `.env`, không gửi key vào frontend hoặc URL.
Key được tạo ở [Deepgram Console](https://console.deepgram.com/), mục API keys.
Thiếu key chỉ khiến phần speech trả lỗi 503, không ngăn các module khác khởi động.
`.env.example` mặc định tắt speech để thành viên khác chủ động cấu hình key.

```powershell
.\mvnw.cmd spring-boot:run
# Hoặc khi có Docker:
docker compose up -d --build
```

## Luồng xử lý

```text
FE -- WebSocket: raw PCM --> Spring Boot -- WebSocket --> Deepgram Listen v1
FE <-- partial/final text -- Spring Boot <-- Results ---- Deepgram
FE -- answer.end ----------> Spring Boot -- CloseStream -> Deepgram
                            Chờ final Results + Metadata + normal close
                            Lưu transcript một lần, phát AnswerFinalizedEvent
FE <-- answer.finalized ---- Spring Boot
```

Backend dùng Java 21 HttpClient/WebSocket, không cần thêm SDK Deepgram.
Xác thực đến provider bằng `Authorization: Token <key>`.
Model `nova-3`, language `vi`, audio `linear16`, sample_rate `16000`, channels `1`,
interim_results `true`, punctuate `true`, smart_format `true`, endpointing `300`.
`is_final` chốt một đoạn; `speech_final` không tự kết thúc toàn bộ câu trả lời.
Backend gửi KeepAlive định kỳ khi stream đang mở.

Tài liệu: [Listen streaming](https://developers.deepgram.com/reference/speech-to-text/listen-streaming),
[CloseStream](https://developers.deepgram.com/docs/close-stream),
[xác thực](https://developers.deepgram.com/guides/fundamentals/authenticating).

## Cấu trúc module

Xem [thiết kế module](MODULE3_ARCHITECTURE.md): `speech` ngang hàng `interview`.
Speech chỉ dùng `InterviewAccess`, `InterviewAnswers` và DTO do interview cung cấp.
Không gọi trực tiếp repository/entity của interview.

## Backend 1 cần tích hợp

Repo chưa có đầy đủ service điều phối AI phỏng vấn. Backend 1 cần:

1. Xác thực bắt đầu thi, chuyển ExamAttempt và QuestionAttempt sang IN_PROGRESS.
2. Lưu câu hỏi chính/phụ qua đầu mối dùng chung:

```java
UUID questionTurnId = transcriptService.recordQuestion(
    studentId, examAttemptId, questionAttemptId,
    questionText, TurnType.MAIN_QUESTION); // hoặc FOLLOW_UP
```

3. Trả questionTurnId cho frontend. Đây là **TranscriptTurn.id của AI**,
   không phải Question.id hoặc QuestionAttempt.id.
4. Nhận kết quả sau commit:

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onAnswer(AnswerFinalizedEvent event) {
    // Giao cho executor/job BE1; xử lý idempotent theo answerId.
    // Quyết định hỏi thêm/chuyển câu và dùng recordQuestion để ghi lượt AI.
}
```

Event nội bộ là best-effort, không phải durable queue. BE1 cần đọc transcript DB
khi khôi phục sau restart; thêm transactional outbox nếu cần giao nhận bền vững.

Khi timer nghiệp vụ hết hạn, gọi bên ngoài transaction đang giữ khóa:
`events.publishEvent(new AnswerDeadlineReachedEvent(examAttemptId, questionTurnId))`.
SpeechInterviewEventListener sẽ yêu cầu chốt audio; interview không gọi implementation speech trực tiếp.
Chờ chốt câu trả lời trước khi đánh dấu câu hỏi COMPLETED.
Speech có timer bảo vệ từ answer.started: min(timeLimitPerTurnSeconds của kỳ thi,
SPEECH_MAX_ANSWER_SECONDS); mặc định cấu hình kỳ thi là 60 giây khi thiếu giá trị.

TranscriptService là đầu mối ghi transcript dùng chung, khóa QuestionAttempt
để cấp turnOrder. Không ghi transcript ở service khác mà không dùng cùng khóa.
Migration V7 thêm answer_id và response_to_turn_id UNIQUE để chống ghi trùng.

## REST API và giọng đọc

Mọi API yêu cầu JWT STUDENT, mật khẩu đã đổi, quyền sở hữu lượt thi.

| Method | Path | Kết quả |
|---|---|---|
| POST | `/api/interview/attempts/{attemptId}/speech-ticket` | ticket, expiresAt, websocketPath |
| GET | `/api/interview/attempts/{attemptId}/speech/questions/{questionTurnId}/playback` | questionTurnId, text, ttsProvider=deepgram, language=vi-VN |
| POST | `/api/interview/attempts/{attemptId}/speech/questions/{questionTurnId}/audio` | audioId, contentType, expiresAt, audioPath |
| GET | `/api/interview/speech/audio/{audioId}` | MP3 riêng tư, JWT bắt buộc |
| GET | `/api/interview/attempts/{attemptId}/speech/answers/{answerId}` | transcriptTurnId, text; 404 nếu chưa lưu |

Ví dụ response playback (ID minh họa):

```json
{
  "success": true,
  "status": 200,
  "message": "Lấy nội dung câu hỏi để phát audio",
  "data": {
    "questionTurnId": "11111111-1111-4111-8111-111111111111",
    "text": "Giải thích dependency injection và cho một ví dụ.",
    "ttsProvider": "deepgram",
    "language": "vi-VN"
  }
}
```

HTTP lỗi dùng ApiResponse với success=false, status và message. 401 là thiếu/sai
JWT, 403 là không có quyền, 428 là bắt buộc đổi mật khẩu, 409 là trạng thái lượt
thi/lượt hỏi không phù hợp, 503 là speech chưa bật hoặc thiếu cấu hình key.

Endpoint playback chỉ trả câu hỏi AI mới nhất đang hoạt động, không trả đáp án/rubric.
Frontend gọi POST `/api/interview/attempts/{attemptId}/speech/questions/{questionTurnId}/audio`
để nhận `{audioId, contentType, expiresAt, audioPath}`. Sau đó GET audioPath kèm JWT,
đọc Blob và phát bằng HTML audio. Không gắn JWT vào URL; không mở microphone khi đang phát.
Dừng phát và thu hồi Blob URL khi chuyển câu/rời phòng. Lỗi TTS cho phép thử lại và vẫn đọc câu hỏi trên màn hình.

Backend cache MP3 riêng tư trong RAM (TTL mặc định 600 giây, tổng 16 MB), mất khi restart.
API tải audio kiểm tra chủ sở hữu và trạng thái bài thi. Không cần migration DB.

Dùng cùng DEEPGRAM_API_KEY với STT. Cần bật SPEECH_ENABLED=true.
Tài khoản phải có quyền sử dụng model Flux tiếng Việt; lỗi provider trả 503, không lộ key.
Tiếng Việt đang Early Access và chưa tự chuẩn hóa số/ngày tháng: cần kiểm tra phát âm trên câu hỏi thực tế.
Tài liệu: https://developers.deepgram.com/reference/speak/v-2/audio/generate

## WebSocket FE–BE

Xin ticket bằng POST có JWT, rồi mở ngay:

```javascript
const response = await fetch(`${base}/api/interview/attempts/${attemptId}/speech-ticket`, {
  method: 'POST', headers: { Authorization: `Bearer ${jwt}` }
});
const body = await response.json();
if (!response.ok) throw new Error(body.message);
const url = new URL(body.data.websocketPath, base);
url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:';
url.searchParams.set('ticket', body.data.ticket);
const ws = new WebSocket(url);
```

Ticket dùng một lần, mặc định 30 giây và không vượt hạn JWT. Ticket mới thu hồi
vé cũ chưa dùng của cùng sinh viên. Origin phải khớp CORS_ALLOWED_ORIGINS;
không mở trang thử bằng file:// hoặc bỏ Origin. Không log query chứa ticket.
Mỗi lượt thi chỉ có một kết nối speech trên process. Quyền được kiểm tra lại
khi start/finalize và định kỳ 5 giây. JWT hết hạn, lượt thi kết thúc, idle timeout
hoặc quá thời lượng kết nối sẽ đóng WebSocket.

Sau session.ready:

```json
{
  "type": "answer.start",
  "requestId": "<uuid>",
  "questionTurnId": "<uuid-luot-AI>",
  "audio": { "encoding": "PCM_S16LE", "sampleRate": 16000, "channels": 1 }
}
```

Đợi answer.started chứa answerId/deadline rồi gửi binary audio frames.
Raw PCM signed 16-bit little-endian, mono, 16 kHz; khuyến nghị 20–100ms/frame
(640–3200 bytes), tối đa 32768 bytes, số byte chẵn, không header WAV.
Không gửi WebM/Opus/base64. FE phải chuyển codec/resample thực sự.
Backend giới hạn audio gửi trước thời gian thực tối đa 2 giây; FE theo dõi
ws.bufferedAmount để không xếp hàng audio vô hạn.

Ngừng gửi audio rồi gửi:

```json
{ "type": "answer.end", "requestId": "<uuid-moi>", "answerId": "<uuid-da-nhan>" }
```

| Server event | Ý nghĩa |
|---|---|
| session.ready | Thông số PCM, sttProvider=deepgram, ttsProvider=deepgram, ttsLanguage |
| answer.started | answerId và deadline; được phép gửi audio |
| transcript.partial | data: segmentId, revision, text; thay đoạn tạm |
| transcript.final | Chốt một đoạn theo segmentId, không nhân đôi |
| answer.finalizing | Khóa audio, đợi xử lý cuối |
| answer.finalized | Toàn bộ câu trả lời đã commit; data: transcriptTurnId, text |
| speech.error | data: code, message, retryable |
| pong | Trả lời ping ứng dụng |

Envelope luôn có type, requestId (có thể null), answerId (có thể null), data.

Ví dụ hai event trong một câu trả lời:

```json
{
  "type": "transcript.partial",
  "requestId": null,
  "answerId": "22222222-2222-4222-8222-222222222222",
  "data": { "segmentId": "0", "revision": 1, "text": "Dependency injection là" }
}
```

```json
{
  "type": "answer.finalized",
  "requestId": "33333333-3333-4333-8333-333333333333",
  "answerId": "22222222-2222-4222-8222-222222222222",
  "data": {
    "transcriptTurnId": "44444444-4444-4444-8444-444444444444",
    "text": "Dependency injection là cơ chế cung cấp dependency từ bên ngoài."
  }
}
```

requestId của answer.finalized là requestId của answer.end, có thể null khi timer
phía server chốt. Sau đó BE1 vẫn có thể đang sinh câu hỏi tiếp; event này không
chứa câu hỏi mới hoặc điểm số.

SegmentId là offset numeric tính từ đầu audio, đổi sang đơn vị 100ns để giữ
protocol cũ; sắp theo số. Bỏ event không thuộc answerId đang hiển thị.
Không nối tất cả partial vào nhau. Partial không ghi DB.
Giữa các câu có thể gửi `{ "type":"ping", "requestId":"<uuid>" }` mỗi 10 giây.

## Lỗi / reconnect

- STT_BUSY: chưa mở stream; thử start lại sau với cùng requestId.
- INVALID_AUDIO_FORMAT / INVALID_AUDIO: sửa PCM trước khi thử lại.
- NO_SPEECH: không ghi câu trả lời rỗng; thu lại nếu BE1 còn cho phép.
- STT_UNAVAILABLE: kiểm tra enabled, key, credit/quota, model/ngôn ngữ và mạng backend.
- STT_FAILED: stream bị lỗi, câu chưa chốt không được giả vờ coi là đã lưu.
- FINALIZE_FAILED: tra cứu GET answerId trước khi thu lại, có retry giới hạn vì
  transaction cũ có thể đang hoàn tất.
- Mất mạng/JWT hết hạn: xin ticket mới. Không resume/replay audio giữa câu.

Chỉ bắt đầu lại khi câu chưa lưu và lượt AI vẫn còn được phép trả lời.
Transcript đã commit vẫn ở PostgreSQL sau disconnect/restart.

## Trang thử và test

```powershell
python -m http.server 5173 --directory doc
```

Mở http://localhost:5173/speech-smoke.html từ origin trong CORS; điền JWT sinh viên,
attemptId, questionTurnId do BE1 đã khởi tạo. Thử đọc câu hỏi bằng Deepgram Flux TTS.
Chọn raw PCM đã ghi sẵn để kiểm tra STT. Nếu có FFmpeg:

```powershell
ffmpeg -i answer.wav -ac 1 -ar 16000 -c:a pcm_s16le -f s16le answer.pcm
.\mvnw.cmd '-Dtest=!*ApplicationTests' test
```

Trang smoke không thay thế phòng thi có microphone. Các test dùng WebSocket/provider
mock, không gọi Deepgram và không tiêu credit. Có test URI/auth, fragmented JSON,
partial/final, CloseStream drain, unexpected close, ticket, quyền REST/WebSocket,
chốt trùng, cache và persistence. Mockito agent được cấu hình trong Maven.
ApplicationTests cần database thật nên loại khỏi lệnh trên. Flyway V7 và toàn bộ
luồng thực tế vẫn cần kiểm tra với PostgreSQL + Deepgram key thật.

## Giới hạn demo

Ticket, connection và cache ở một instance. Nhiều replica cần shared store/routing
và quota chung. Không log key/JWT/ticket/audio/nội dung transcript. Log chỉ ghi ID,
byte count và thời gian xử lý. Không có barge-in hay resume giữa câu.
Đo riêng STT, sinh câu hỏi BE1 và thời điểm Deepgram Flux TTS phát ở FE.
