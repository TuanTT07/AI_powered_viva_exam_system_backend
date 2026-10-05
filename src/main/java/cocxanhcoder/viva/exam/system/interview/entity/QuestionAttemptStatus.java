package cocxanhcoder.viva.exam.system.interview.entity;

/** DB mặc định là PENDING; IN_PROGRESS / COMPLETED là các bước tiếp theo khi AI hỏi đáp. */
public enum QuestionAttemptStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED
}
