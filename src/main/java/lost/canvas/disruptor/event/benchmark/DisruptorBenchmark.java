package lost.canvas.disruptor.event.benchmark;

import com.lmax.disruptor.WorkHandler;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import lost.canvas.disruptor.event.SimpleEvent;
import lost.canvas.disruptor.event.factory.SimpleEventFactory;
import lost.canvas.disruptor.event.publisher.SimpleEventPublisher;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.IntStream;

/**
 * @author xinfan.zhou@things-matrix.com
 * @date 2020/07/01 12:21
 */
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 1)
@Measurement(iterations = 3)
@Threads(1)
@Fork(1)
@State(Scope.Benchmark)
public class DisruptorBenchmark {

    /**
     * 模拟消费一个 event 的耗时,单位 m
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
     * 生产 event
     */
//    private int eventSize = 10_000;
    @Param({"1000", "10000", "100000"})
    private int eventSize;

    /**
     * 用于通知消费完毕
     */
//    private CountDownLatch latch = new CountDownLatch(eventSize);
    private AtomicInteger count = new AtomicInteger(0);
    private ExecutorService producerExecutor;

    private Disruptor<SimpleEvent<Integer>> disruptor;
    /**
     * ringbuffer 大小
     */
    private int ringbufferSize = 1024;//x32

    @Setup
    public void initDisruptor() {
        // 消费者阻塞策略
//        BlockingWaitStrategy strategy = new BlockingWaitStrategy();
        YieldingWaitStrategy strategy = new YieldingWaitStrategy();
        // 创建disruptor，采用多生产者模式,如果此处设置单生产者模式，则会导致多个生产者获取相同的 seq，导致部分数据丢失，无法被消费者消费
        disruptor = new Disruptor(SimpleEventFactory.INSTANCE, ringbufferSize, DaemonThreadFactory.INSTANCE, ProducerType.MULTI, strategy);
        // 设置EventHandler, 必须在 start 之前，否则无效
        BenchmarkSimpleEventWorkerHandler<Integer>[] consumers = IntStream.range(0, consumerNum).mapToObj(i -> new BenchmarkSimpleEventWorkerHandler<>()).toArray(BenchmarkSimpleEventWorkerHandler[]::new);
        disruptor.handleEventsWithWorkerPool(consumers);
        // 启动disruptor的线程
        disruptor.start();
    }

    @Setup
    public void initProducers() {
        producerExecutor = Executors.newFixedThreadPool(100);
    }

    @TearDown
    public void destory() {
        producerExecutor.shutdown();
        disruptor.shutdown();
    }

    @Benchmark
    public void useDisruptor() {
        System.out.printf(">>> ioTimeUse: %d, eventSize: %d\n",ioTimeUse,eventSize);
        SimpleEventPublisher<Integer> publisher = new SimpleEventPublisher<>(disruptor.getRingBuffer());
        for (int i = 0; i < eventSize; i++) {
            final int v = i;
            producerExecutor.execute(() -> publisher.publish(v));
            LockSupport.parkNanos(produceTimeInterval.toNanos()); //1000 qps
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
//            System.out.println("<<< finished");
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
    }

    public class BenchmarkSimpleEventWorkerHandler<T> implements WorkHandler<SimpleEvent<T>> {
        @Override
        public void onEvent(SimpleEvent<T> event) throws Exception {
//            System.out.println("========== Disruptor handle:" + event.value);
//            latch.countDown();
            count.incrementAndGet();
            //模拟消费后io耗时
            LockSupport.parkNanos(Duration.ofMillis(ioTimeUse).toNanos());
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(DisruptorBenchmark.class.getSimpleName())
                .output("./benchmark/disruptor_benchmark.txt")
                .build();
        new Runner(options).run();
    }


}
