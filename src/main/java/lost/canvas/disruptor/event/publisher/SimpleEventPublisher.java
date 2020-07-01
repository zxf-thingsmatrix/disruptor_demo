package lost.canvas.disruptor.event.publisher;

import com.lmax.disruptor.InsufficientCapacityException;
import com.lmax.disruptor.RingBuffer;
import lost.canvas.disruptor.event.SimpleEvent;

/**
 * @author xinfan.zhou@things-matrix.com
 * @date 2020/07/01 11:00
 */
public class SimpleEventPublisher<T> {

    private final RingBuffer<SimpleEvent<T>> ringBuffer;

    public SimpleEventPublisher(RingBuffer<SimpleEvent<T>> ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    public boolean tryPublish(T value) {
        long sequence = 0;
        try {
            sequence = ringBuffer.tryNext();
        } catch (InsufficientCapacityException e) {
            return false;
        }
        try {
            // 返回可用位置的元素
            SimpleEvent<T> event = ringBuffer.get(sequence);
            // 设置该位置元素的值
            event.value = value;
            return true;
        } finally {
            ringBuffer.publish(sequence);
        }
    }

    public void publish(T value) {
        long sequence = ringBuffer.next();
        try {
            // 返回可用位置的元素
            SimpleEvent<T> event = ringBuffer.get(sequence);
            // 设置该位置元素的值
            event.value = value;
        } finally {
            ringBuffer.publish(sequence);
        }
    }
}
