package lost.canvas.disruptor;

import org.junit.Test;

import java.util.concurrent.locks.LockSupport;

/**
 * @author xinfan.zhou@things-matrix.com
 * @date 2020/07/02 12:28
 */
public class LockSupportTest {
    @Test
    public void te() {
        System.out.println("<<<");
        LockSupport.parkNanos(0);
        System.out.println(">>>");
    }
}
