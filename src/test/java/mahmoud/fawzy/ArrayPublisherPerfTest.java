package mahmoud.fawzy;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import mahmoud.fawzy.ArrayPublisher;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.RunnerException;

@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 2)
@Measurement(iterations = 3, time = 3, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1)
@State(Scope.Thread)
public class ArrayPublisherPerfTest {
    @Param({ "1000000" })
    public int times;

    ArrayPublisherOptimized<Integer> arrayPublisherOptimized;
    ArrayPublisher<Integer> arrayPublisher;

    @Setup
    public void setup() {
        Integer[] array = new Integer[times];
        Arrays.fill(array, 777);
        arrayPublisherOptimized = new ArrayPublisherOptimized<>(array);
        arrayPublisher = new ArrayPublisher<>(array);
    }

    @Benchmark
    public Object publisherPerformance(Blackhole bh) {
        PerfSubscriber lo = new PerfSubscriber(bh);

        arrayPublisher.subscribe(lo);

        return lo;
    }

    @Benchmark
    public Object optimizedPerformance(Blackhole bh) {
        PerfSubscriber lo = new PerfSubscriber(bh);

        arrayPublisherOptimized.subscribe(lo);

        return lo;
    }

    public static void main(String[] args) throws IOException, RunnerException {
        org.openjdk.jmh.Main.main(args);
    }
}