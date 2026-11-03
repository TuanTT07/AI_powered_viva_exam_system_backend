# Bàn giao chức năng 3 và 4 cho nhóm
## API phòng thi BE1 hiện có

Các mục bàn giao bên dưới ghi lại trạng thái lúc BE2 chuyển giao. FE1 tích hợp với hai
API đã triển khai sau đây, dùng JWT STUDENT của chủ lượt thi.

| API | Hành vi |
|---|---|
| `POST /api/interviews/{attemptId}/start` | Bắt đầu lượt thi READY với bộ câu đã giao. Gọi lại để lấy trạng thái hiện hành. |
| `GET /api/interviews/{attemptId}/current` | Đọc trạng thái khi mở phòng thi, tải lại trang hoặc nối lại sau lỗi. |

`data.status` là `READY`, `AWAITING_ANSWER`, `PROCESSING` hoặc `COMPLETED`.
Khi `AWAITING_ANSWER`, `data.prompt` chứa `questionAttemptId`, `questionTurnId`,
`text` và `deadlineAt` (UTC, ISO 8601). Dùng `questionTurnId` với playback và
WebSocket speech. Khi bắt đầu thu âm, speech có thể trả deadline sớm hơn theo
giới hạn STT. FE dùng deadline sớm hơn trong lúc thu.

Sau `answer.finalized` hoặc hết giờ, gọi `current` mỗi giây trong khi trạng thái
là `PROCESSING`. Dừng polling khi nhận câu mới hoặc `COMPLETED`. Sau khi mất
kết nối, gọi `current` trước khi mở lại stream. BE1 giữ nguyên deadline của
câu hiện hành, kể cả khi sinh viên tải lại trang.

BE1 dùng cấu hình `timeLimitPerTurnSeconds` và `maxFollowUpQuestions`. Nếu quá
deadline 10 giây mà speech chưa lưu câu trả lời, hệ thống ghi nhận hết giờ trên
lượt hỏi và chuyển câu chính. Khi AI lỗi hoặc quá 10 giây, hệ thống chuyển câu.
Để chạy test tích hợp, trỏ datasource đến PostgreSQL thử nghiệm rồi chạy
`./mvnw.cmd '-Dinterview.pg.test=true' test`.


Tài liệu này mô tả code hiện có và phần cần làm tiếp. Các mục **đề xuất/chưa có**
không phải API hoặc chức năng đã triển khai. Ưu tiên hoàn thiện chức năng 3 trước.

## 1. Thứ tự đọc

1. Tài liệu này: phân công, trạng thái và thứ tự tích hợp.
2. [Kiến trúc interview/speech](MODULE3_ARCHITECTURE.md): ranh giới module.
3. [Speech và protocol](MODULE3_SPEECH.md): cấu hình Deepgram, REST, WebSocket, lỗi.
4. [Quản lý kỳ thi hiện có](../MODULE2_EXAM_MANAGEMENT.md): đối chiếu ExamAttempt/QuestionAttempt.

Code dùng Java 21, Spring Boot, kiến trúc phân lớp theo feature. Mọi thay đổi schema
cần migration Flyway mới; không sửa migration đã chạy. Không commit `.env`.

## 2. Phân công và sở hữu code

| Thành viên | Module/phần chính | Trách nhiệm bàn giao |
|---|---|---|
| Backend 1 | interview | Bắt đầu/resume lượt thi, chọn câu chính, AI hỏi xoáy, chuyển câu, timer nghiệp vụ, lưu hội thoại |
| Backend 2 | speech | Ticket, WebSocket, audio PCM, Deepgram STT, partial/final transcript, lỗi kết nối; hợp đồng Browser TTS |
| Backend 3 | grading | Chấm theo rubric, điểm/nhận xét AI, giảng viên điều chỉnh và chốt |
| Frontend 1 | Phòng thi sinh viên | API interview, microphone/resample PCM, WebSocket, Browser TTS, transcript, timer, reconnect |
| Frontend 2 | Chuẩn bị thi và giao diện chấm | Cấu hình lượt thi tối thiểu; transcript/rubric/điểm AI; sửa và chốt điểm |

**Interview thuộc Backend 1.** InterviewAccessService, TranscriptService, interface,
DTO/event hiện có được tạo sẵn trong quá trình tích hợp Backend 2 để speech có điểm
kiểm tra và lưu kết quả. Backend 1 tiếp nhận, duy trì và phát triển phần nền này.
Đây chưa phải implementation đầy đủ lõi phỏng vấn AI.

Speech chỉ dùng InterviewAccess/InterviewAnswers và DTO của interview; không truy
cập repository/entity của interview. Backend 1 không gọi implementation speech;
phát AnswerDeadlineReachedEvent để yêu cầu kết thúc audio theo timer nghiệp vụ.

## 3. Trạng thái theo code hiện tại

| Hạng mục | Trạng thái |
|---|---|
| Speech REST và WebSocket, ticket/Origin/quyền sinh viên | Đã có code và test tự động |
| Deepgram Listen v1, PCM 16 kHz mono, partial/final, CloseStream | Đã có adapter và test transport giả; cần thử provider thật |
| Lưu transcript, thứ tự lượt, answerId chống trùng | Đã có TranscriptService và migration V7; cần kiểm tra DB thật |
| Browser TTS | Đã có API playback và trang smoke; phụ thuộc giọng Việt trên máy |
| API bắt đầu/resume/đọc trạng thái phỏng vấn | Chưa có trong module interview |
| AI hỏi xoáy, chọn/chuyển câu, giới hạn follow-up | Chưa có lõi điều phối hoàn chỉnh; Backend 1 làm tiếp |
| Đưa câu hỏi tiếp theo đến frontend | Chưa có hợp đồng hoàn chỉnh; BE1 + FE1 cần thống nhất |
| Grading | Có entity/repository nền; chưa có service/controller chấm và duyệt điểm |
| Trang phòng thi thu microphone | Chưa có trong repo backend; smoke chỉ gửi file PCM |
| Toàn luồng PostgreSQL + Deepgram + frontend | Chưa được xác minh bởi các unit test hiện tại |

72 test đã pass tại thời điểm bàn giao gần nhất, không gồm ApplicationTests cần
database thật. Các test này không gọi Deepgram và không chứng minh chất lượng STT
thực tế. Chạy lại test khi thay đổi code.

## 4. Backend 1: bắt đầu từ đâu

Đọc các lớp:

- [InterviewAccess](../src/main/java/cocxanhcoder/viva/exam/system/interview/service/InterviewAccess.java)
  và [InterviewAccessService](../src/main/java/cocxanhcoder/viva/exam/system/interview/service/InterviewAccessService.java).
- [InterviewAnswers](../src/main/java/cocxanhcoder/viva/exam/system/interview/service/InterviewAnswers.java)
  và [TranscriptService](../src/main/java/cocxanhcoder/viva/exam/system/interview/service/TranscriptService.java).
- [TranscriptTurn](../src/main/java/cocxanhcoder/viva/exam/system/interview/entity/TranscriptTurn.java)
  và [QuestionAttempt](../src/main/java/cocxanhcoder/viva/exam/system/exammgt/entity/QuestionAttempt.java).

Thứ tự triển khai:

1. Tạo service điều phối trong interview/service và controller/DTO tương ứng.
2. Xác thực sinh viên, điều kiện bắt đầu thi và tập câu hỏi đã được giao.
3. Bắt đầu ExamAttempt; chỉ cho một QuestionAttempt hiện hành IN_PROGRESS trong
   một lượt thi. Chống start/next đồng thời từ nhiều request.
4. Lưu câu chính bằng TranscriptService.recordQuestion(..., MAIN_QUESTION), trả
   questionTurnId cho FE. Không gửi đáp án/rubric nội bộ xuống phòng thi sinh viên.
5. Nhận AnswerFinalizedEvent sau commit, dùng câu trả lời cùng câu hỏi/rubric và
   lịch sử để quyết định hỏi thêm hoặc chuyển câu. Chặn xử lý trùng theo answerId.
6. Lưu follow-up bằng recordQuestion(..., FOLLOW_UP) trên cùng QuestionAttempt;
   kiểm soát số lượt và tránh hỏi lặp. AI trả lời thiếu cấu trúc/timeout cần có
   hành vi xác định, ví dụ chuyển câu hoặc báo lỗi có thể khôi phục.
7. Khi xong câu chính và toàn bộ follow-up, đánh dấu QuestionAttempt COMPLETED,
   mở câu tiếp theo; cuối bài đánh dấu ExamAttempt COMPLETED.
8. Thêm API đọc trạng thái hiện hành/resume để FE khôi phục khi reload/mất mạng.

### Lưu câu hỏi hiện có

```java
UUID questionTurnId = transcriptService.recordQuestion(
    studentId, examAttemptId, questionAttemptId, questionText, TurnType.MAIN_QUESTION);
```

Phương thức yêu cầu lượt thi và câu được giao đang IN_PROGRESS; không cho thêm
câu hỏi khi lượt cuối cùng của QuestionAttempt vẫn là AI chưa được trả lời.
Giới hạn số follow-up, lựa chọn câu đúng nội dung môn học và khả năng idempotent
của thao tác điều phối vẫn do Backend 1 thực hiện.

InterviewAccess hiện kiểm tra lượt AI mới nhất **trong QuestionAttempt được chỉ
định**. Nó chưa bảo đảm duy nhất một QuestionAttempt đang hoạt động trên toàn bài.
Backend 1 phải quản lý invariant này khi mở/chuyển câu.

### Nhận câu trả lời đã lưu

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onAnswer(AnswerFinalizedEvent event) {
    // Giao công việc cho executor/job, chống trùng bằng event.answerId().
    // Gọi service điều phối có transaction riêng để ghi trạng thái/lượt tiếp theo.
}
```

Không chạy LLM lâu trong callback commit. Ghi database sau AFTER_COMMIT cần
transaction mới/phương thức service được gọi qua Spring proxy; tránh tự gọi method
@Transactional trong cùng đối tượng rồi nghĩ đã tạo transaction mới.

Spring event chỉ tồn tại trong process. Resume cần đối chiếu transcript và trạng
thái đã lưu; không giả định mọi event luôn được giao sau restart.

### Hết giờ

```java
events.publishEvent(new AnswerDeadlineReachedEvent(examAttemptId, questionTurnId));
```

Phát ngoài transaction đang giữ khóa QuestionAttempt. Speech chốt bất đồng bộ;
**publish event không có nghĩa câu trả lời đã lưu xong**. Chờ xác nhận kết quả trước
khi đóng QuestionAttempt, nếu không complete(...) sẽ từ chối vì câu không còn hoạt động.
Nếu không có stream/sinh viên chưa bắt đầu/NO_SPEECH/provider lỗi, có thể không có
AnswerFinalizedEvent: BE1 cần timeout và quy tắc bỏ qua/không trả lời riêng, tránh chờ vô hạn.

## 5. Hợp đồng hiện có giữa Backend 1 và Backend 2

| Thành phần | Dữ liệu/hành vi |
|---|---|
| InterviewAccess.requireActiveAttempt | studentId, attemptId; ném lỗi khi không có quyền hoặc không IN_PROGRESS |
| InterviewAccess.requirePrompt | Trả InterviewPrompt(questionTurnId, questionAttemptId, text, timeLimitSeconds) |
| InterviewAnswers.complete | student, attempt, prompt, questionAttempt, answer, text, started, ended theo đúng thứ tự signature |
| InterviewAnswers.findAnswer | student, attempt, answer; trả AnswerReceipt(transcriptTurnId, text) |
| AnswerDeadlineReachedEvent | examAttemptId, questionTurnId |
| AnswerFinalizedEvent | answerId, questionTurnId, questionAttemptId, examAttemptId, studentId, transcriptTurnId, text, startedAt, endedAt |

Chỉ gọi complete sau khi STT đã xử lý hết audio. answerId là ID một lần thu câu
trả lời, do speech cấp; không dùng questionTurnId làm answerId.
Backend 2 đã gọi các interface này, Backend 1 không cần tạo endpoint HTTP riêng
để speech gọi ngược về cùng ứng dụng.

startedAt/endedAt có trong event nhưng **chưa có cột riêng trên TranscriptTurn**.
Nếu BE3 cần số liệu thời gian bền vững sau restart, thống nhất DTO và migration mới;
không suy ra độ trôi chảy hoặc thời gian nói chính xác từ createdAt.

## 6. Backend 3: đầu vào và việc cần làm

- Bắt đầu tại grading/entity/Evaluation.java, CriterionScore.java và EvaluationRepository.
- Đầu vào chấm cho một QuestionAttempt: câu chính, toàn bộ follow-up, các câu trả lời
  đã chốt theo turnOrder, đáp án/rubric tương ứng từ questionbank.
- Không chấm trên transcript.partial. Một AnswerFinalizedEvent có thể chỉ là một
  lượt trả lời, chưa chắc kết thúc nhóm câu hỏi chính và follow-up.
- BE1 + BE3 cần thống nhất tín hiệu hoàn tất QuestionAttempt hoặc API/trạng thái để
  kích hoạt chấm. **Chưa có QuestionCompletedEvent/InterviewCompletedEvent trong code.**
- Evaluation có UNIQUE question_attempt_id: thiết kế retry/upsert không tạo hai bản
  đánh giá. Giữ riêng aiSuggestedScore và finalScore; giảng viên mới được chốt điểm.
- Bổ sung service gọi AI qua interface, kiểm tra output theo rubric/thang điểm,
  controller/DTO đọc bài, chấm lại, điều chỉnh và chốt điểm; kiểm tra quyền giảng viên.
- Thống nhất cách cố định câu hỏi/rubric tại lúc thi. Hiện chưa có đủ snapshot để
  bảo đảm chỉnh ngân hàng câu hỏi sau đó không đổi căn cứ chấm bài cũ.

## 7. Frontend 1: thứ tự tích hợp

1. Gọi API start/current do BE1 sẽ cung cấp để lấy attemptId và questionTurnId.
2. GET playback hiện có, đọc câu hỏi bằng Browser TTS; kiểm tra giọng vi trên máy.
3. POST speech-ticket có JWT, mở WebSocket bằng ticket, đợi session.ready.
4. Gửi answer.start, đợi answer.started rồi mới thu/gửi PCM.
5. Hiển thị transcript theo answerId/segmentId/revision. Partial là nội dung có thể sửa.
6. Khi bấm kết thúc, dừng audio rồi gửi answer.end. Khóa gửi trong FINALIZING.
7. Đợi answer.finalized. Khi mất xác nhận, dùng API tra cứu answerId.
8. Lấy trạng thái/câu hỏi tiếp từ cơ chế BE1 cung cấp; không tự chọn câu hoặc đếm
   follow-up để thay quyền quyết định của backend.

**WebSocket speech hiện chưa phát event câu hỏi tiếp theo.** BE1 + FE1 cần chốt
cơ chế lấy câu tiếp, ví dụ gọi API current và polling giới hạn trong lúc AI xử lý.
Đây là đề xuất; endpoint cụ thể chưa được triển khai. Không tự dùng event
`question.next` vì protocol hiện không có event này.

Microphone thực tế cần chuyển/resample về PCM S16LE 16 kHz mono. Audio MediaRecorder
thường có container/codec khác; không đổi tên WebM thành PCM rồi gửi.
Browser TTS không tạo MP3/audioId; không gọi endpoint /audio cũ trong luồng demo.
Đọc [protocol chi tiết](MODULE3_SPEECH.md) trước khi triển khai.

Frontend 2 phối hợp BE1 cho màn hình cấu hình tối thiểu, BE3 cho giao diện transcript,
rubric, điểm AI, sửa và chốt điểm. Màn hình giảng viên không dùng JWT STUDENT để
gọi speech API; cần API đọc bài có phân quyền giảng viên do BE3 triển khai.

## 8. ID và liên kết hội thoại

| ID | Ý nghĩa |
|---|---|
| questionId | Câu hỏi trong ngân hàng |
| examAttemptId / attemptId | Lượt thi của một sinh viên |
| questionAttemptId | Một câu chính được giao trong lượt thi; chứa cả chuỗi follow-up |
| questionTurnId | TranscriptTurn.id của lượt AI đang cần được trả lời |
| answerId | Khóa idempotency do speech cấp cho một lần thu câu trả lời |
| transcriptTurnId | ID bản ghi câu trả lời đã lưu |

TranscriptTurn.responseToTurnId của ANSWER trỏ về lượt AI. Follow-up AI hiện được
nhóm với câu chính bằng questionAttemptId, turnType và turnOrder; chưa có cột parent
riêng cho lượt FOLLOW_UP. Không giả định answerId là ID bản ghi TranscriptTurn.

## 9. Thứ tự hoàn thiện và nghiệm thu chung

1. BE1 + FE1 chốt start/current/resume và trả được một câu chính bằng text.
2. BE2 + FE1 thử STT với key thật, mic thật; thử sai Origin/key, mất mạng và chốt trùng.
3. BE1 thêm AI hỏi xoáy: câu trả lời khác nhau phải dẫn tới câu phụ phù hợp khác nhau;
   không vượt giới hạn, không lặp, có timeout/fallback và xử lý không trả lời.
4. BE1 hoàn tất trạng thái câu/bài thi; BE3 + FE2 tích hợp chấm và duyệt điểm.
5. Chạy chung: câu chính → nói → follow-up → nói → kết thúc → AI gợi ý → giảng viên chốt.

Kiểm tra transcript và thứ tự trong DB; không chỉ kiểm tra text đang hiển thị trên FE.
Đo độ trễ từ kết thúc trả lời đến câu hỏi tiếp theo, tách STT/LLM/TTS để biết điểm chậm.

```powershell
.\mvnw.cmd '-Dtest=!*ApplicationTests' test
.\mvnw.cmd -DskipTests package
```

ApplicationTests cần database thật. Trang smoke và unit test không thay thế nghiệm
thu trên PostgreSQL + Deepgram + máy/trình duyệt demo. Không đưa key thật vào tài liệu.
