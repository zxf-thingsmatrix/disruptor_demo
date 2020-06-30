package lost.canvas.disruptor;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import lost.canvas.disruptor.event.SimpleEvent;
import lost.canvas.disruptor.event.factory.SimpleEventFactory;
import lost.canvas.disruptor.event.handler.SimpleEventHandler;
import lost.canvas.disruptor.event.handler.SimpleEventWorkerHandler;
import lost.canvas.disruptor.event.handler.thread_factory.SimpleEventHandlerThreadFactory;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.locks.LockSupport;

/**
 * @author xinfan.zhou@things-matrix.com
 * @date 2020/06/30 15:55
 */
public class HandlerChainTest {


    /**
     * 保证消费者消费顺序
     * 直接依赖
     */
    @Test
    public void testHandlerChain() {
        // 消费者阻塞策略
        BlockingWaitStrategy strategy = new BlockingWaitStrategy();
        // 指定RingBuffer的大小
        int bufferSize = 16;
        // 创建disruptor，采用单生产者模式
        Disruptor<SimpleEvent> disruptor = new Disruptor(SimpleEventFactory.INSTANCE, bufferSize, SimpleEventHandlerThreadFactory.INSTANCE, ProducerType.SINGLE, strategy);

        // 设置EventHandler A -> B | A -> C
        disruptor.handleEventsWith(new SimpleEventHandler("A")).thenHandleEventsWithWorkerPool(new SimpleEventWorkerHandler("B"), new SimpleEventWorkerHandler("C"));
        // 启动disruptor的线程
        disruptor.start();

        RingBuffer<SimpleEvent> ringBuffer = disruptor.getRingBuffer();
        for (int l = 0; l < 3; l++) {
            // 获取下一个可用位置的下标
            long sequence = ringBuffer.next();
            try {
                // 返回可用位置的元素
                SimpleEvent event = ringBuffer.get(sequence);
                // 设置该位置元素的值
                event.value = l;
            } finally {
                ringBuffer.publish(sequence);
            }
            LockSupport.parkNanos(Duration.ofMillis(3).toNanos());
        }
        LockSupport.park();
    }


    /**
     * 菱形依赖
     */
    @Test
    public void testHandlerRhombus() {
        // 消费者阻塞策略
        BlockingWaitStrategy strategy = new BlockingWaitStrategy();
        // 指定RingBuffer的大小
        int bufferSize = 16;
        // 创建disruptor，采用单生产者模式
        Disruptor<SimpleEvent> disruptor = new Disruptor(SimpleEventFactory.INSTANCE, bufferSize, SimpleEventHandlerThreadFactory.INSTANCE, ProducerType.SINGLE, strategy);

        // 设置EventHandler
        disruptor.handleEventsWith(new SimpleEventHandler("A")).then(new SimpleEventHandler("B"), new SimpleEventHandler("C")).then(new SimpleEventHandler("D"));
        // 启动disruptor的线程
        disruptor.start();

        RingBuffer<SimpleEvent> ringBuffer = disruptor.getRingBuffer();
        for (int l = 0; l < 3; l++) {
            // 获取下一个可用位置的下标
            long sequence = ringBuffer.next();
            try {
                // 返回可用位置的元素
                SimpleEvent event = ringBuffer.get(sequence);
                // 设置该位置元素的值
                event.value = l;
            } finally {
                ringBuffer.publish(sequence);
            }
            LockSupport.parkNanos(Duration.ofMillis(3).toNanos());
        }
    }
}
