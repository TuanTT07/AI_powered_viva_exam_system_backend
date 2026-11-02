# Cấu trúc chức năng 3: interview và speech

Đọc [bàn giao cho nhóm](TEAM_HANDOFF.md) trước để biết người phụ trách, phần đã có
và phần chưa triển khai. Interview thuộc Backend 1; speech thuộc Backend 2.
Các lớp nền interview đang có được chuẩn bị khi tích hợp speech và bàn giao cho BE1.

## Quyết định

Giữ kiến trúc phân lớp theo feature của dự án. Tách `speech` thành package/module
logic ngang hàng `interview`, giống `academic`, `questionbank`, `exammgt`, `grading`.
Chạy chung một ứng dụng Spring Boot, một Maven project, không thêm server hay
microservice. Chức năng 3 được thực hiện bởi cả hai module.

```text
src/main/java/cocxanhcoder/viva/exam/system/
├── academic/
├── auth/
├── common/
├── exammgt/
├── questionbank/
├── interview/                         # Backend 1: nghiệp vụ phỏng vấn
│   ├── dto/
│   │   ├── InterviewPrompt.java       # Câu hỏi hợp lệ và thời gian cho phép
│   │   ├── AnswerReceipt.java         # Xác nhận câu trả lời đã lưu
│   │   ├── AnswerFinalizedEvent.java
│   │   └── AnswerDeadlineReachedEvent.java
│   ├── entity/                       # TranscriptTurn, Speaker, TurnType
│   ├── repository/                   # TranscriptTurnRepository
│   └── service/
│       ├── InterviewAccess.java      # Contract kiểm tra lượt thi/lượt hỏi
│       ├── InterviewAccessService.java
│       ├── InterviewAnswers.java     # Contract nhận/tra cứu câu trả lời
│       └── TranscriptService.java    # Lưu hội thoại, thứ tự và idempotency
├── speech/                            # Backend 2: vận chuyển/xử lý giọng nói
│   ├── config/                       # SpeechProperties, SpeechWebSocketConfig
│   ├── controller/                   # SpeechController
│   ├── dto/                          # SpeechProtocol: REST/WebSocket payloads
│   ├── websocket/                    # Handler và handshake interceptor
│   ├── provider/                     # STT/TTS interfaces, Deepgram adapter
│   └── service/
│       ├── SpeechTicketService.java
│       ├── SpeechSessionService.java
│       ├── TranscriptAccumulator.java
│       ├── SpeechInterviewEventListener.java
│       └── QuestionSpeechService.java
└── grading/
```

Không tạo controller/repository/entity rỗng chỉ để mọi module có cùng số thư mục.
Khi BE1 bổ sung API điều phối, controller của API đó nằm trong interview/controller.
Speech chưa có dữ liệu persistence riêng nên không cần entity/repository.
QuestionSpeechService giữ đường mở rộng server TTS; demo hiện tại đọc bằng Browser TTS.

## Sở hữu dữ liệu và trách nhiệm

| Module | Phụ trách | Dữ liệu |
|---|---|---|
| exammgt | Kỳ thi, ca thi, câu hỏi được giao | Exam, ExamAttempt, QuestionAttempt |
| interview | Kiểm tra lượt hỏi, lưu hội thoại; nền để BE1 thêm AI hỏi xoáy/điều phối | TranscriptTurn |
| speech | Ticket, WebSocket, PCM, Deepgram STT, partial transcript và phát lại câu hỏi | Ticket/kết nối/buffer tạm trong RAM |
| grading | AI gợi ý điểm, giảng viên quyết định điểm cuối | Evaluation, CriterionScore |

Speech không tự chọn câu hỏi, chuyển câu, sửa trạng thái kỳ thi, ghi trực tiếp
transcript hoặc quyết định điểm. Backend 1 chưa được triển khai đầy đủ chỉ bởi việc
tách package này; service sinh follow-up/điều phối sẽ do BE1 phát triển trong interview.

## Hướng phụ thuộc

```text
SpeechController / SpeechWebSocketHandler
                 ↓
        speech.service
          ├── provider interface → DeepgramSpeechClient → Deepgram
          └── InterviewAccess / InterviewAnswers (interview.service)
                                ↓
             InterviewAccessService / TranscriptService
                                ↓
             interview / exammgt / academic repositories
                                ↓
                            PostgreSQL
```

- Speech chỉ import interface public từ interview/service và record từ interview/dto.
- Speech không import entity/repository hoặc implementation service của interview.
- Interview không import speech. Business DTO/event không nằm trong SpeechProtocol.
- Config JWT/security tiếp tục thuộc common/config như dự án hiện tại.
- Interface provider giữ giao tiếp Deepgram ra khỏi nghiệp vụ interview.

Hai interface là ranh giới nhỏ phục vụ tách trách nhiệm, không thêm tầng kiến trúc
mới hay framework. Controller → Service → Repository vẫn được giữ nguyên.

## Hợp đồng tích hợp

### Speech gọi interview

`InterviewAccess`:

- requireActiveAttempt(studentId, attemptId): kiểm tra chủ sở hữu, mật khẩu và trạng thái.
- requirePrompt(studentId, attemptId, questionTurnId): trả InterviewPrompt, không trả Entity.

`InterviewAnswers`:

- complete(...): nhận câu trả lời STT hoàn chỉnh, lưu một lần và trả AnswerReceipt.
- findAnswer(...): khôi phục kết quả khi FE mất thông báo xác nhận.

TranscriptService còn có recordQuestion(...) cho BE1 ghi câu hỏi AI. Phương thức
này không nằm trong contract InterviewAnswers vì speech không được tạo câu hỏi.
Cả ghi câu hỏi và câu trả lời dùng chung khóa QuestionAttempt để cấp turnOrder.

### Interview thông báo hết giờ

Timer BE1 phát event ngoài transaction đang giữ khóa:

```java
events.publishEvent(new AnswerDeadlineReachedEvent(examAttemptId, questionTurnId));
```

SpeechInterviewEventListener nhận event và yêu cầu chốt stream. Interview không
cần gọi SpeechSessionService hoặc phụ thuộc module speech.

### Sau khi câu trả lời được lưu

TranscriptService phát AnswerFinalizedEvent. BE1 có thể nhận bằng
`@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` để giao việc
cho executor/job của mình, rồi quyết định hỏi thêm/chuyển câu. Dùng answerId để
chống xử lý lại. Không gọi LLM lâu trong callback commit.

Spring events hiện là best-effort trong một process, không phải durable queue.
Nếu cần khôi phục điều phối sau restart, đọc transcript đã commit; thêm outbox khi
nhóm thực sự cần đảm bảo giao nhận event bền vững.

## Các thay đổi đã thực hiện

| Trước | Sau |
|---|---|
| interview/speech/** | speech/** |
| SpeechAccessService | interview/service/InterviewAccessService |
| SpeechTranscriptService | interview/service/TranscriptService |
| SpeechAccessService.Prompt | interview/dto/InterviewPrompt |
| SpeechProtocol.FinalAnswer | interview/dto/AnswerReceipt |
| SpeechProtocol.AnswerFinalizedEvent | interview/dto/AnswerFinalizedEvent |

Test cũng được chuyển về đúng module. API URL, WebSocket protocol, tên biến môi trường
Deepgram và schema database giữ nguyên. Việc tách package không cần migration mới.

Xem [cấu hình và chạy thử](MODULE3_SPEECH.md).
