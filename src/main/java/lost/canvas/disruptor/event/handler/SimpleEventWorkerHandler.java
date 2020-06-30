package lost.canvas.disruptor.event.handler;

import com.lmax.disruptor.LifecycleAware;
import com.lmax.disruptor.WorkHandler;
import lost.canvas.disruptor.event.SimpleEvent;

/**
 * @author xinfan.zhou@things-matrix.com
 * @date 2020/06/30 12:25
 */
public class SimpleEventWorkerHandler implements WorkHandler<SimpleEvent>, LifecycleAware {

    private final String name;

    public SimpleEventWorkerHandler(String name) {
        this.name = name;
    }

    @Override
    public void onEvent(SimpleEvent event) throws Exception {
        System.out.printf("[thread:%s] [SimpleEventWorkerHandler:%s] handle [SimpleEvent:%d]\n", Thread.currentThread().getName(), name, event.value);
    }

    @Override
    public void onStart() {
        System.out.printf(">>>>>>>>>> [SimpleEventWorkerHandler:%s] start\n", name);
    }

    @Override
    public void onShutdown() {
        System.out.printf(">>>>>>>>>> [SimpleEventWorkerHandler:%s] shutdown\n", name);
    }
}
