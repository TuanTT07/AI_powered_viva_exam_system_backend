package cocxanhcoder.viva.exam.system.questionbank.entity;

/**
 * Thang Bloom: mức độ tư duy mà câu hỏi yêu cầu (thấp -> cao).
 * Lưu trong DB dạng chuỗi (VARCHAR) nhờ @Enumerated(EnumType.STRING).
 */
public enum BloomLevel {
    REMEMBER,
    UNDERSTAND,
    APPLY,
    ANALYZE,
    EVALUATE,
    CREATE
}
