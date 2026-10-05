package cocxanhcoder.viva.exam.system.common.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Tạo Pageable an toàn từ tham số page/size client gửi lên. */
public final class PageableUtils {

    private static final int MAX_PAGE_SIZE = 100;

    private PageableUtils() {
    }

    /**
     * page < 0 -> 0, size ngoài [1, 100] -> kẹp lại, tránh client gửi size=1000000 làm nặng DB.
     */
    public static Pageable of(int page, int size, Sort sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(safePage, safeSize, sort);
    }
}
