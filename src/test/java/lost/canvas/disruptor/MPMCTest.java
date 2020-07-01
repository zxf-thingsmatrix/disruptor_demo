package lost.canvas.disruptor;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import lost.canvas.disruptor.event.SimpleEvent;
import lost.canvas.disruptor.event.factory.SimpleEventFactory;
import lost.canvas.disruptor.event.handler.SimpleEventHandler;
import lost.canvas.disruptor.event.handler.thread_factory.SimpleEventHandlerThreadFactory;
import lost.canvas.disruptor.event.publisher.SimpleEventPublisher;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.locks.LockSupport;

/**
 * 多生产者广播消息给多消费者
 *
 * @author xinfan.zhou@things-matrix.com
 * @date 2020/07/01 10:27
 */
public class MPMCTest {
    @Test
    public void testMultiProducerMultiConsumer() {
        // 消费者阻塞策略
        BlockingWaitStrategy strategy = new BlockingWaitStrategy();
        // 指定RingBuffer的大小
        int bufferSize = 16;
        // 创建disruptor，采用多生产者模式,如果此处设置单生产者模式，则会导致多个生产者获取相同的 seq，导致部分数据丢失，无法被消费者消费
        Disruptor<SimpleEvent<Integer>> disruptor = new Disruptor(SimpleEventFactory.INSTANCE, bufferSize, SimpleEventHandlerThreadFactory.INSTANCE, ProducerType.MULTI, strategy);

        // 设置EventHandler, 必须在 start 之前，否则无效
        disruptor.handleEventsWith(new SimpleEventHandler("1"), new SimpleEventHandler("2"));
        // 启动disruptor的线程
        disruptor.start();

        //生产 event，必须在 start 之后，否则无效
        RingBuffer<SimpleEvent<Integer>> ringBuffer = disruptor.getRingBuffer();
        SimpleEventPublisher<Integer> publisher = new SimpleEventPublisher<>(ringBuffer);

        Thread pThread1 = new Thread(() -> publish(publisher),"publisher-1");
        Thread pThread2 = new Thread(() -> publish(publisher),"publisher-2");

        pThread1.start();
        pThread2.start();

        try {
            pThread1.join();
            pThread2.join();
        }catch (Exception e){}

    }

    public void publish(SimpleEventPublisher publisher) {
        String threadName = Thread.currentThread().getName();
        for (int l = 0; l<10 ; l++) {
            publisher.publish(threadName+":"+l);
            LockSupport.parkNanos(Duration.ofMillis(3).toNanos());
        }
    }
}
