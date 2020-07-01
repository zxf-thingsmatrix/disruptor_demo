package lost.canvas.disruptor;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import lost.canvas.disruptor.event.SimpleEvent;
import lost.canvas.disruptor.event.factory.SimpleEventFactory;
import lost.canvas.disruptor.event.handler.SimpleEventWorkerHandler;
import lost.canvas.disruptor.event.handler.thread_factory.SimpleEventHandlerThreadFactory;
import lost.canvas.disruptor.event.publisher.SimpleEventPublisher;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.locks.LockSupport;

/**
 * @author xinfan.zhou@things-matrix.com
 * @date 2020/06/30 15:43
 */
public class WorkerHandlerTest {

    @Test
    public void testworkerHandler() {

        // 消费者阻塞策略
        BlockingWaitStrategy strategy = new BlockingWaitStrategy();
        // 指定RingBuffer的大小
        int bufferSize = 16;
        // 创建disruptor，采用单生产者模式
        Disruptor<SimpleEvent<Integer>> disruptor = new Disruptor(SimpleEventFactory.INSTANCE, bufferSize, SimpleEventHandlerThreadFactory.INSTANCE, ProducerType.SINGLE, strategy);
        //设置 workerHandler
        disruptor.handleEventsWithWorkerPool(new SimpleEventWorkerHandler<>("1"), new SimpleEventWorkerHandler<>("2")); // 1，2 为一组，组内不会重复消费
        disruptor.handleEventsWithWorkerPool(new SimpleEventWorkerHandler<>("3"), new SimpleEventWorkerHandler<>("4")); // 3，4 为一组，组内不会重复消费
        // 启动disruptor的线程
        disruptor.start();

        RingBuffer<SimpleEvent<Integer>> ringBuffer = disruptor.getRingBuffer();
        SimpleEventPublisher<Integer> publisher = new SimpleEventPublisher<>(ringBuffer);
        for (int l = 0; ; l++) {
            publisher.publish(l);
            LockSupport.parkNanos(Duration.ofMillis(3).toNanos());
        }

    }
}
