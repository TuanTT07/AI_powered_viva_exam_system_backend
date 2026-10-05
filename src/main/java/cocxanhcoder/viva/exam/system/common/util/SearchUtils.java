package cocxanhcoder.viva.exam.system.common.util;

/** Hàm hỗ trợ tìm kiếm. */
public final class SearchUtils {

    private SearchUtils() {
    }

    /**
     * Chuyển từ khoá người dùng nhập thành pattern cho LIKE:
     * "  Nguyen " -> "%nguyen%",  null hoặc "   " -> null (= không lọc).
     */
    public static String toLikePattern(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return "%" + keyword.trim().toLowerCase() + "%";
    }
}
