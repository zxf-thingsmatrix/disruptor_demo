package lost.canvas.disruptor;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import lost.canvas.disruptor.event.SimpleEvent;
import lost.canvas.disruptor.event.factory.SimpleEventFactory;
import lost.canvas.disruptor.event.handler.SimpleEventHandler;
import lost.canvas.disruptor.event.handler.thread_factory.SimpleEventHandlerThreadFactory;
import lost.canvas.disruptor.event.translator.SimpleEventTranslator;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.locks.LockSupport;

/**
 * @author xinfan.zhou@things-matrix.com
 * @date 2020/06/30 17:06
 */
public class EventTranslatorTest {

    @Test
    public void testEventTranslator() {
        // 消费者阻塞策略
        BlockingWaitStrategy strategy = new BlockingWaitStrategy();
        // 指定RingBuffer的大小
        int bufferSize = 16;
        // 创建disruptor，采用单生产者模式
        Disruptor<SimpleEvent> disruptor = new Disruptor(SimpleEventFactory.INSTANCE, bufferSize, SimpleEventHandlerThreadFactory.INSTANCE, ProducerType.SINGLE, strategy);

        // 设置EventHandler
        disruptor.handleEventsWith(new SimpleEventHandler("1"), new SimpleEventHandler("2"));
        // 启动disruptor的线程
        disruptor.start();

        for (int l = 0; true; l++) {
            //其实不建议使用 translator 这种方式，这种方式需要创建大量对象，额外增加 gc 压力
            disruptor.publishEvent(new SimpleEventTranslator(l));
            LockSupport.parkNanos(Duration.ofMillis(2).toNanos());
        }
    }
}
