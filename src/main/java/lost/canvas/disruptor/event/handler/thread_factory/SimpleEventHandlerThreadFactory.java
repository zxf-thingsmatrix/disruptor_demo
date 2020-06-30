package lost.canvas.disruptor.event.handler.thread_factory;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author xinfan.zhou@things-matrix.com
 * @date 2020/06/30 12:17
 */
public class SimpleEventHandlerThreadFactory implements ThreadFactory {

    private final AtomicInteger index = new AtomicInteger(1);

    public static SimpleEventHandlerThreadFactory INSTANCE = new SimpleEventHandlerThreadFactory();

    public Thread newThread(Runnable r) {
        return new Thread(r, "my_thread_" + index.getAndIncrement());
    }
}
