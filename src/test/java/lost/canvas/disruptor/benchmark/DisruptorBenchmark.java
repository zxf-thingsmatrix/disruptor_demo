package lost.canvas.disruptor.benchmark;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.WorkHandler;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * @author xinfan.zhou@things-matrix.com
 * @date 2020/07/01 12:21
 */
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 1, timeUnit = TimeUnit.SECONDS, time = 5)
@Measurement(iterations = 5, timeUnit = TimeUnit.SECONDS, time = 5)
@Threads(1)
@Fork(1)
@State(Scope.Benchmark)
public class DisruptorBenchmark {

    @Param({"1"})
    private long consumeTimeUse; //模拟消费一个 event 的耗时,单位 ms

    private Duration produceTimeInterval = Duration.ofMillis(1); //生产一个 event 的时间间隔

    private int eventSize = 10_000; //生产 event 数

//    private CountDownLatch latch = new CountDownLatch(eventSize); //用于通知消费完毕

    private AtomicInteger count = new AtomicInteger(0);
    private ExecutorService producerExecutor;

    private Disruptor<SimpleEvent<Integer>> disruptor;

    @Setup
    public void initDisruptor() {
        // 消费者阻塞策略
//        BlockingWaitStrategy strategy = new BlockingWaitStrategy();
        BusySpinWaitStrategy strategy = new BusySpinWaitStrategy();
        // 指定RingBuffer的大小
        int bufferSize = 1024 * 8; //x32
        // 创建disruptor，采用多生产者模式,如果此处设置单生产者模式，则会导致多个生产者获取相同的 seq，导致部分数据丢失，无法被消费者消费
        disruptor = new Disruptor(SimpleEventFactory.INSTANCE, bufferSize, DaemonThreadFactory.INSTANCE, ProducerType.MULTI, strategy);

        // 设置EventHandler, 必须在 start 之前，否则无效
        BenchmarkSimpleEventWorkerHandler<Integer>[] consumers = new BenchmarkSimpleEventWorkerHandler[]{
                new BenchmarkSimpleEventWorkerHandler<>(),
                new BenchmarkSimpleEventWorkerHandler<>(),
                new BenchmarkSimpleEventWorkerHandler<>(),
                new BenchmarkSimpleEventWorkerHandler<>()
        };
        disruptor.handleEventsWithWorkerPool(consumers);
        // 启动disruptor的线程
        disruptor.start();
    }

    @Setup
    public void initProducers() {
        producerExecutor = Executors.newFixedThreadPool(200);
    }

    @TearDown
    public void destory() {
        producerExecutor.shutdown();
        disruptor.shutdown();
    }

    @Benchmark
    public void useDisruptor() {
        System.out.println(">>> consumeTimeUse: " + consumeTimeUse);
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
            //模拟消费耗时
            LockSupport.parkNanos(Duration.ofMillis(consumeTimeUse).toNanos());
//            latch.countDown();
            count.incrementAndGet();

        }
    }

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(DisruptorBenchmark.class.getSimpleName())
                .output("./benchmark/disruptor_benchmark_spin.txt")
                .build();
        new Runner(options).run();
    }


}
