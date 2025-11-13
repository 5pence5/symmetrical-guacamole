package com.symguac.int128.bench;

import com.symguac.int128.api.Int128Arithmetic;
import com.symguac.int128.impl.twolongs.TwoLongsBaselineArithmetic;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Registry that keeps track of the Int128 implementations that the benchmarks can instantiate.
 */
public final class Int128BenchmarkRegistry {
    private static final Map<String, Supplier<? extends Int128Arithmetic>> ENTRIES = new LinkedHashMap<>();

    static {
        register(Int128ImplementationIds.TWO_LONGS_BASELINE, TwoLongsBaselineArithmetic::new);
    }

    private Int128BenchmarkRegistry() {
    }

    public static void register(String id, Supplier<? extends Int128Arithmetic> factory) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(factory, "factory");
        Supplier<? extends Int128Arithmetic> previous = ENTRIES.putIfAbsent(id, factory);
        if (previous != null) {
            throw new IllegalArgumentException("An Int128 implementation has already been registered under id '" + id + "'.");
        }
    }

    public static Int128Arithmetic create(String id) {
        Supplier<? extends Int128Arithmetic> factory = ENTRIES.get(id);
        if (factory == null) {
            throw new IllegalArgumentException("No Int128 implementation registered under id '" + id + "'. Known ids: " + ENTRIES.keySet());
        }
        return factory.get();
    }

    public static Set<String> registeredIds() {
        return Collections.unmodifiableSet(ENTRIES.keySet());
    }
}
