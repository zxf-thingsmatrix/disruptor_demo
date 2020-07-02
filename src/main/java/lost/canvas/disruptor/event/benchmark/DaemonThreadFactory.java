package lost.canvas.disruptor.event.benchmark;

import java.util.concurrent.ThreadFactory;

public class DaemonThreadFactory implements ThreadFactory {
    public static final DaemonThreadFactory INSTANCE = new DaemonThreadFactory();

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        return thread;
    }
}
