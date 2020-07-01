package lost.canvas.disruptor.benchmark;

import java.util.concurrent.ThreadFactory;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.WorkHandler;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import lost.canvas.disruptor.event.SimpleEvent;
import lost.canvas.disruptor.event.factory.SimpleEventFactory;
import lost.canvas.disruptor.event.handler.thread_factory.SimpleEventHandlerThreadFactory;
import lost.canvas.disruptor.event.publisher.SimpleEventPublisher;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.Duration;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.IntStream;

/**
 * @author xinfan.zhou@things-matrix.com
 * @date 2020/07/01 12:21
 */
//@BenchmarkMode(Mode.Throughput)
//@Warmup(iterations = 0)
//@Measurement(iterations = 1)
//@Threads(4)
//@Fork(2)
@State(value = Scope.Thread)
public class SimpleBenchmark {

    @Param({"10"})
    private long ioTimeUse;


    private Disruptor<SimpleEvent<Integer>> disruptor;

    private ConcurrentLinkedQueue<Integer> concurrentLinkedQueue;

    private ExecutorService producerExecutor;

    private CountDownLatch latch = new CountDownLatch(10);

    @Setup
    public void initDisruptor() {
        // 消费者阻塞策略
        BlockingWaitStrategy strategy = new BlockingWaitStrategy();
        // 指定RingBuffer的大小
        int bufferSize = 16;
        // 创建disruptor，采用多生产者模式,如果此处设置单生产者模式，则会导致多个生产者获取相同的 seq，导致部分数据丢失，无法被消费者消费
        disruptor = new Disruptor(SimpleEventFactory.INSTANCE, bufferSize, DaemonThreadFactory.INSTANCE, ProducerType.MULTI, strategy);

        // 设置EventHandler, 必须在 start 之前，否则无效
        BenchmarkSimpleEventWorkerHandler<Integer>[] consumers = new BenchmarkSimpleEventWorkerHandler[]{
                new BenchmarkSimpleEventWorkerHandler<>(),
                new BenchmarkSimpleEventWorkerHandler<>(),
//                new BenchmarkSimpleEventWorkerHandler<>(),
//                new BenchmarkSimpleEventWorkerHandler<>()
        };
        disruptor.handleEventsWithWorkerPool(consumers);
        // 启动disruptor的线程
        disruptor.start();
    }
//
//    @Setup
//    public void initConcurrentLinkedQueue() {
//        concurrentLinkedQueue = new ConcurrentLinkedQueue<>();
//        //消费线程启动
//        IntStream.range(0, 2).mapToObj(i -> {
//            return new Thread(() -> {
//                for (; ; ) {
//                    Integer poll = concurrentLinkedQueue.poll();
//                    if (poll != null) {
//                        System.out.println(">>>>>>>>>> ConcurrentLinkedQueue poll:" + poll);
//                        latch.countDown();
//                        //模拟消费耗时
//                        LockSupport.parkNanos(Duration.ofMillis(ioTimeUse).toNanos());
//                    }
//                }
//            });
//        }).peek(thread -> thread.setDaemon(true)).forEach(Thread::start);
//    }

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
        SimpleEventPublisher<Integer> publisher = new SimpleEventPublisher<>(disruptor.getRingBuffer());
        for (int i = 0; i < 10; i++) {
            final int v = i;
            producerExecutor.execute(() -> publisher.publish(v));
            LockSupport.parkNanos(Duration.ofMillis(1).toNanos()); //1000 qps
        }
        //等待消费完成
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

//    @Benchmark
//    public void useConcurrentLinkedQueue() {
//        for (int i = 0; i < size; i++) {
//            final int v = i;
//            producerExecutor.execute(() -> {
//                while (!concurrentLinkedQueue.offer(v)) ;
//            });
//            LockSupport.parkNanos(Duration.ofMillis(1).toNanos()); //1000 qps
//        }
//        //等待消费完成
//        try {
//            latch.await();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//    }

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(SimpleBenchmark.class.getSimpleName())
                .output("D:/disruptor_benchmark.txt")
                .build();
        new Runner(options).run();
    }

    public class BenchmarkSimpleEventWorkerHandler<T> implements WorkHandler<SimpleEvent<T>> {

        @Override
        public void onEvent(SimpleEvent<T> event) throws Exception {
            System.out.println("========== Disruptor handle:" + event.value);
            latch.countDown();
            //模拟消费耗时
            LockSupport.parkNanos(Duration.ofMillis(ioTimeUse).toNanos());
        }
    }

    public static class DaemonThreadFactory implements ThreadFactory {

        public static final DaemonThreadFactory INSTANCE = new DaemonThreadFactory();

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        }
    }
}
