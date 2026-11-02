package cocxanhcoder.viva.exam.system.speech.provider;

public interface SpeechToTextClient {
    RecognitionSession open(Listener listener) throws Exception;

    interface Listener {
        void partial(String segmentId, String text);
        void recognized(String segmentId, String text);
        void failed();
    }

    interface RecognitionSession extends AutoCloseable {
        void write(byte[] pcm);
        /** End audio and wait for all final recognition results. */
        void finish() throws Exception;
        @Override void close();
    }
}
