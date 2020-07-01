package lost.canvas.disruptor.event.handler;

import com.lmax.disruptor.EventHandler;
import lost.canvas.disruptor.event.SimpleEvent;

/**
 * @author xinfan.zhou@things-matrix.com
 * @date 2020/06/30 12:13
 */
public class SimpleEventHandler<T> implements EventHandler<SimpleEvent<T>>
//,LifecycleAware
{

    private final String name;

    public SimpleEventHandler(String name) {
        this.name = name;
    }

    @Override
    public void onEvent(SimpleEvent<T> event, long sequence, boolean endOfBatch) throws Exception {
        System.out.printf("[thread:%s] [SimpleEventHandler:%s] handle [SimpleEvent:%s]\n", Thread.currentThread().getName(), name, event.value.toString());
    }

//    @Override
//    public void onStart() {
//        System.out.printf(">>>>>>>>>> [SimpleEventHandler:%s] start\n", name);
//    }
//
//    @Override
//    public void onShutdown() {
//        System.out.printf(">>>>>>>>>> [SimpleEventHandler:%s] shutdown\n", name);
//    }
}
