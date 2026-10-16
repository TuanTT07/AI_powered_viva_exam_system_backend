package cocxanhcoder.viva.exam.system.common.mail;

/**
 * Gửi email, dùng qua interface để service không gọi thẳng JavaMailSender / SDK.
 * Gửi lỗi (SMTP chưa cấu hình, sai mật khẩu app...) chỉ log, KHÔNG ném exception,
 * vì gửi mail là tác vụ phụ, không được làm rollback nghiệp vụ chính (tạo tài khoản...).
 */
public interface MailService {

    void send(String to, String subject, String body);
}
