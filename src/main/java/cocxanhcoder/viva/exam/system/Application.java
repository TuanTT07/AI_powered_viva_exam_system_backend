package cocxanhcoder.viva.exam.system;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class Application {

    public static void main(String[] args) {
        // Một số máy/mạng không route được IPv6 tới smtp.gmail.com -> JVM thử IPv6 trước
        // sẽ treo tới khi hết timeout rồi mới fallback IPv4. Ép dùng IPv4 để kết nối ngay.
        System.setProperty("java.net.preferIPv4Stack", "true");

        // Tự động nạp file .env từ thư mục gốc dự án (nếu tồn tại)
//        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
//        dotenv.entries().forEach(entry -> {
//            if (System.getProperty(entry.getKey()) == null) {
//                System.setProperty(entry.getKey(), entry.getValue());
//            }
//        });

        SpringApplication.run(Application.class, args);
    }

}
