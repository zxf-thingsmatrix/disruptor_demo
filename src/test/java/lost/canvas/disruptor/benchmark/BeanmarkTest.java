package lost.canvas.disruptor.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
@Threads(1)
@Warmup(iterations = 1)
@Measurement(iterations = 1)
@State(Scope.Benchmark)
public class BeanmarkTest {

    private int size = 3;

    @Setup
    public void init() {
        System.out.println(">>> init...");
    }

    @TearDown
    public void destroy() {
        System.out.println(">>> destroy...");
    }


    @Benchmark
    public void test1(Blackhole blackhole) {
        blackhole.consume(1);
        CountDownLatch latch = new CountDownLatch(size);

        long start = System.currentTimeMillis();
        new Thread(() -> {
            for (int i = 0; i < size; i++) {
                System.out.println("=== test1...");
                latch.countDown();
                LockSupport.parkNanos(Duration.ofMillis(1).toNanos());
            }
        }).start();
        try {
            latch.await();
            System.out.println("test1 finished timeuse: " + (System.currentTimeMillis() - start));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

//    @Benchmark
//    public void test2() {
//        new Thread(()-> {
//            for (int i = 0; i < size; i++) {
//                System.out.println("=== test2...");
//                latch.countDown();
//                LockSupport.parkNanos(Duration.ofMillis(1).toNanos());
//            }
//        }).start();
//
//        try {
//            latch.await();
//            System.out.println("test2 finished");
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//    }

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(BeanmarkTest.class.getSimpleName())
                .output("D:/disruptor_benchmark.txt")
                .build();
        new Runner(options).run();
    }
}
