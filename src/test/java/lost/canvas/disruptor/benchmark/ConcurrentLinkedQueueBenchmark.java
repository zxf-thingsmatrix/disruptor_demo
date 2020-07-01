package lost.canvas.disruptor.benchmark;

import com.lmax.disruptor.dsl.Disruptor;
import lost.canvas.disruptor.event.SimpleEvent;
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

@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@Threads(1)
@Fork(1)
@State(Scope.Thread)
public class ConcurrentLinkedQueueBenchmark {

    @Param({"10", "100", "1000"})
    private long ioTimeUse;

    private int size = 5;

    private CountDownLatch latch = new CountDownLatch(size);

    private ExecutorService producerExecutor;

    private ConcurrentLinkedQueue<Integer> concurrentLinkedQueue;

    @Setup
    public void initConcurrentLinkedQueue() {
        concurrentLinkedQueue = new ConcurrentLinkedQueue<>();
        //消费线程启动
        IntStream.range(0, 2).mapToObj(i -> {
            return new Thread(() -> {
                for (; ; ) {
                    Integer poll = concurrentLinkedQueue.poll();
                    if (poll != null) {
                        System.out.println("========== ConcurrentLinkedQueue poll:" + poll);
                        latch.countDown();
                        //模拟消费耗时
                        LockSupport.parkNanos(Duration.ofMillis(ioTimeUse).toNanos());
                    }
                }
            });
        }).peek(thread -> thread.setDaemon(true)).forEach(Thread::start);
    }

    @Setup
    public void initProducers() {
        producerExecutor = Executors.newFixedThreadPool(200);
    }

    @TearDown
    public void destory() {
        producerExecutor.shutdown();
    }

    @Benchmark
    public void useConcurrentLinkedQueue() {
        System.out.println(">>> ioTimeUse: " + ioTimeUse);
        for (int i = 0; i < size; i++) {
            final int v = i;
            producerExecutor.execute(() -> {
                while (!concurrentLinkedQueue.offer(v)) ;
            });
            LockSupport.parkNanos(Duration.ofMillis(1).toNanos()); //1000 qps
        }
        //等待消费完成
        try {
            latch.await();
            latch = new CountDownLatch(size);
            System.out.println("<<<< finished");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(ConcurrentLinkedQueueBenchmark.class.getSimpleName())
                .output("/benchmark/concurrent_linked_queue_benchmark.txt")
                .build();
        new Runner(options).run();
    }
}
