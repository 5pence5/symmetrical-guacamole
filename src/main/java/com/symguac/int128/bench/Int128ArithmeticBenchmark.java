package com.symguac.int128.bench;

import com.symguac.int128.api.Int128Arithmetic;
import com.symguac.int128.api.Int128Value;
import com.symguac.int128.api.MutableInt128Value;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;

/**
 * Micro-benchmarks that exercise the Int128 arithmetic contract.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class Int128ArithmeticBenchmark {

    @Benchmark
    public void additionWithReuse(BenchmarkState state, Blackhole blackhole) {
        int index = state.nextIndex();
        state.arithmetic.addInto(state.left[index], state.right[index], state.addScratch);
        blackhole.consume(state.addScratch);
    }

    @Benchmark
    public void subtractionWithReuse(BenchmarkState state, Blackhole blackhole) {
        int index = state.nextIndex();
        state.arithmetic.subtractInto(state.left[index], state.right[index], state.addScratch);
        blackhole.consume(state.addScratch);
    }

    @Benchmark
    public void multiplicationWithReuse(BenchmarkState state, Blackhole blackhole) {
        int index = state.nextIndex();
        state.arithmetic.multiplyInto(state.left[index], state.right[index], state.multiplyScratch);
        blackhole.consume(state.multiplyScratch);
    }

    @Benchmark
    public Int128Value additionAllocating(BenchmarkState state) {
        int index = state.nextIndex();
        return state.arithmetic.add(state.left[index], state.right[index]);
    }

    @Benchmark
    public Int128Value multiplicationAllocating(BenchmarkState state) {
        int index = state.nextIndex();
        return state.arithmetic.multiply(state.left[index], state.right[index]);
    }

    @State(Scope.Thread)
    public static class BenchmarkState {
        @Param({Int128ImplementationIds.TWO_LONGS_BASELINE, Int128ImplementationIds.FAST_LIMB_BASED})
        public String implementation;

        @Param({"1024"})
        public int datasetSize;

        private SplittableRandom random;
        Int128Arithmetic arithmetic;
        Int128Value[] left;
        Int128Value[] right;
        MutableInt128Value addScratch;
        MutableInt128Value multiplyScratch;
        private int nextIndex;

        @Setup(Level.Trial)
        public void setup() {
            arithmetic = Int128BenchmarkRegistry.create(implementation);
            left = new Int128Value[datasetSize];
            right = new Int128Value[datasetSize];
            addScratch = arithmetic.createMutable();
            multiplyScratch = arithmetic.createMutable();
            random = new SplittableRandom(0x9E3779B97F4A7C15L);
            for (int i = 0; i < datasetSize; i++) {
                left[i] = randomValue();
                right[i] = randomValue();
            }
        }

        @Setup(Level.Iteration)
        public void resetIteration() {
            nextIndex = 0;
        }

        int nextIndex() {
            int current = nextIndex;
            nextIndex = (nextIndex + 1) % datasetSize;
            return current;
        }

        private Int128Value randomValue() {
            long high = random.nextLong();
            long low = random.nextLong();
            return arithmetic.fromParts(high, low);
        }
    }
}
