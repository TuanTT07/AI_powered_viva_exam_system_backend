package cocxanhcoder.viva.exam.system.speech.provider;

public interface TextToSpeechClient {
    byte[] synthesize(String text) throws Exception;
}
