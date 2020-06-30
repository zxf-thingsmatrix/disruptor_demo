package lost.canvas.disruptor;

import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import lost.canvas.disruptor.event.SimpleEvent;
import lost.canvas.disruptor.event.factory.SimpleEventFactory;
import lost.canvas.disruptor.event.handler.SimpleEventHandler;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;

/**
 * @author xinfan.zhou@things-matrix.com
 * @date 2020/06/30 14:57
 */
public class DeadLockByUsingBoundExecutorTest {
    /**
     * 死锁原因
     * 消费者线程数 < EventHandler 个数 -> 部分 EventHandler 无法执行 -> min(消费者 seqs)=0 -> ringBuffer 被填满，生产者无法获取 seq -> 消费者无法获取 seq
     */
    @Test
    public void testDeadLock() {
        int bufferSize = 16;
        ExecutorService boundExectuor = Executors.newFixedThreadPool(2);
        Disruptor<SimpleEvent> disruptor = new Disruptor(SimpleEventFactory.INSTANCE, bufferSize, boundExectuor, ProducerType.SINGLE, new BusySpinWaitStrategy());

        disruptor.handleEventsWith(new SimpleEventHandler("1"), new SimpleEventHandler("2"), new SimpleEventHandler("3"), new SimpleEventHandler("4"));
//        disruptor.handleEventsWithWorkerPool(new SimpleEventWorkerHandler("1"), new SimpleEventWorkerHandler("2"), new SimpleEventWorkerHandler("3"));
        disruptor.start();

        RingBuffer<SimpleEvent> ringBuffer = disruptor.getRingBuffer();
        for (int l = 0; true; l++) {
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
