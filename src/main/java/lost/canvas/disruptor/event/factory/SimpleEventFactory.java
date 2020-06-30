package lost.canvas.disruptor.event.factory;

import com.lmax.disruptor.EventFactory;
import lost.canvas.disruptor.event.SimpleEvent;

/**
 * @author xinfan.zhou@things-matrix.com
 * @date 2020/06/30 12:20
 */
public class SimpleEventFactory implements EventFactory<SimpleEvent> {

    public static SimpleEventFactory INSTANCE = new SimpleEventFactory();
    public SimpleEvent newInstance() {
        return new SimpleEvent();
    }
}
