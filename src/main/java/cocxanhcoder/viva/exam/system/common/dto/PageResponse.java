package cocxanhcoder.viva.exam.system.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Format trả về chung cho API có phân trang.
 * Không trả thẳng Page của Spring ra JSON vì nó chứa nhiều field thừa và format không ổn định.
 * JSON: { "content": [...], "page": 0, "size": 10, "totalElements": 25, "totalPages": 3 }
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
