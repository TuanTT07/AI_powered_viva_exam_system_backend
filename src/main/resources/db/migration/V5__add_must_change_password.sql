-- Bắt buộc đổi mật khẩu lần đăng nhập đầu tiên khi Admin tạo tài khoản / đặt lại mật khẩu.
ALTER TABLE users
    ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT TRUE;

-- Tài khoản đã tồn tại từ trước tính năng này không bị bắt đổi mật khẩu (tránh khoá tài khoản đang dùng).
UPDATE users SET must_change_password = FALSE;
