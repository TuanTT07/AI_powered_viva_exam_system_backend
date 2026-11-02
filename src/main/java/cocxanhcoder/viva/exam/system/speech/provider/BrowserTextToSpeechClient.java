package cocxanhcoder.viva.exam.system.speech.provider;

import cocxanhcoder.viva.exam.system.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Vietnamese demo playback is performed by the frontend's SpeechSynthesis API. */
@Component
public class BrowserTextToSpeechClient implements TextToSpeechClient {
    @Override public byte[] synthesize(String text) {
        throw new BusinessException(HttpStatus.NOT_IMPLEMENTED,
                "TTS dùng giọng của trình duyệt. Lấy nội dung câu hỏi qua API playback và dùng speechSynthesis với vi-VN.");
    }
}
