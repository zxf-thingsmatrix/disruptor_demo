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
 * 消息广播
 * @author xinfan.zhou@things-matrix.com
 * @date 2020/06/30 15:46
 */
public class EventHandlerTest {

    @Test
    public void testEventHandler() {
        // 消费者阻塞策略
        BlockingWaitStrategy strategy = new BlockingWaitStrategy();
        // 指定RingBuffer的大小
        int bufferSize = 16;
        // 创建disruptor，采用单生产者模式
        Disruptor<SimpleEvent<Integer>> disruptor = new Disruptor(SimpleEventFactory.INSTANCE, bufferSize, SimpleEventHandlerThreadFactory.INSTANCE, ProducerType.SINGLE, strategy);

        // 设置EventHandler, 必须在 start 之前，否则无效
        disruptor.handleEventsWith(new SimpleEventHandler("1"), new SimpleEventHandler("2"));
        // 启动disruptor的线程
        disruptor.start();

        //生产 event，必须在 start 之后，否则无效
        RingBuffer<SimpleEvent<Integer>> ringBuffer = disruptor.getRingBuffer();
        SimpleEventPublisher<Integer> publisher = new SimpleEventPublisher<>(ringBuffer);
        for (int l = 0; ; l++) {
            publisher.publish(l);
            LockSupport.parkNanos(Duration.ofMillis(3).toNanos());
        }

    }
}
