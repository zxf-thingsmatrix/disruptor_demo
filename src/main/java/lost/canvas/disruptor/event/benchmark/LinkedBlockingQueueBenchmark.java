package lost.canvas.disruptor.event.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.IntStream;

/**
 * @author xinfan.zhou@things-matrix.com
 * @date 2020/07/02 10:41
 */
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 1)
@Measurement(iterations = 3)
@Threads(1)
@Fork(1)
@State(Scope.Benchmark)
public class LinkedBlockingQueueBenchmark {
    /**
     * 模拟消费一个 event 的耗时,单位 ms
     */
    @Param({"0"})
    private long ioTimeUse;
    /**
     * 消费者个数
     */
    private int consumerNum = Runtime.getRuntime().availableProcessors() * 2;
    /**
     * 生产一个 event 的时间间隔
     */
    private Duration produceTimeInterval = Duration.ofMillis(1);
    /**
     * 生产 event 数
     */
//    private int eventSize = 10_000;
    @Param({"1000", "10000", "100000"})
    private int eventSize;
    //    private CountDownLatch latch = new CountDownLatch(eventSize); //用于通知消费完毕
    private AtomicInteger count = new AtomicInteger(0);

    private ExecutorService producerExecutor;

    private LinkedBlockingQueue<Integer> linkedBlockingQueue;


    @Setup
    public void initConcurrentLinkedQueue() {
        linkedBlockingQueue = new LinkedBlockingQueue<>();
        //消费线程启动
        IntStream.range(0, consumerNum).mapToObj(i -> {
            return new Thread(() -> {
                for (; ; ) {
                    try {
                        Integer take = linkedBlockingQueue.take();
//                        System.out.println("========== LinkedBlockingQueue take:" + take);
//                        latch.countDown();
                        count.incrementAndGet();
                        //模拟消费后io耗时
                        LockSupport.parkNanos(Duration.ofMillis(ioTimeUse).toNanos());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }).peek(thread -> thread.setDaemon(true)).forEach(Thread::start);
    }

    @Setup
    public void initProducers() {
        producerExecutor = Executors.newFixedThreadPool(100);
    }

    @TearDown
    public void destory() {
        producerExecutor.shutdown();
    }

    @Benchmark
    public void useLinkedBlockingQueue() {
        System.out.printf(">>> ioTimeUse: %d, eventSize: %d\n",ioTimeUse,eventSize);
        for (int i = 0; i < eventSize; i++) {
            final int v = i;
            producerExecutor.execute(() -> {
                try {
                    linkedBlockingQueue.put(v);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
            //1000 qps
            LockSupport.parkNanos(produceTimeInterval.toNanos());
        }
        //等待消费完成
        if (count.get() < eventSize) {
            Thread.yield();
        } else {
            count.set(0);
            System.out.println("<<< finished");
        }
//        try {
//            latch.await();
//            latch = new CountDownLatch(eventSize);
//            System.out.println("<<<< finished");
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
    }

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(LinkedBlockingQueueBenchmark.class.getSimpleName())
                .output("./benchmark/linked_blocking_queue_benchmark.txt")
                .build();
        new Runner(options).run();
    }
}
