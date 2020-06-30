package lost.canvas.disruptor.event.translator;

import com.lmax.disruptor.EventTranslator;
import lost.canvas.disruptor.event.SimpleEvent;

/**
 * @author xinfan.zhou@things-matrix.com
 * @date 2020/06/30 17:45
 */
public class SimpleEventTranslator implements EventTranslator<SimpleEvent> {

    private final long value;

    public SimpleEventTranslator(long value) {
        this.value = value;
    }

    @Override
    public void translateTo(SimpleEvent event, long sequence) {
        event.value = this.value;
    }
}
