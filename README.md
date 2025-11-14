# Int128 for Java (with a simple build & benchmark helper)

[![build](https://github.com/5pence5/symmetrical-guacamole/workflows/build/badge.svg)](https://github.com/5pence5/symmetrical-guacamole/actions)

A compact library for exact 128-bit signed two's-complement integers in Java.
It exposes a small plugin API so you can swap implementations (correctness-first baseline vs performance-oriented), and ships with a reference `Int128` (under `src/test/java`) used for verification.
A minimal JMH harness is included purely to make building and comparing implementations easy — it's optional.

---

## What's in this repository?

| Component | Where | Purpose |
|-----------|-------|---------|
| **Public API (stable)** | `src/main/java/com/symguac/int128/api/` | Interfaces used by clients and implementations: `Int128Arithmetic`, `Int128Value`, `MutableInt128Value`. |
| **Baseline implementation** | `impl/twolongs` | Clarity & correctness. Uses `BigInteger` internally for multiply/divide; good for validation. |
| **Fast implementation** | `impl/highperf` | Hot-path add/sub/mul are allocation-free. 128÷64 division fast-path is implemented. (Full 128÷128 division currently delegated or not supported; see matrix below.) |
| **Reference Int128 (tests)** | `src/test/java/Int128.java` | Self-contained, no-BigInteger arithmetic, used by tests & debugging. Not exported as API. |
| **Build/Benchmark helper** | `src/jmh/java/...` | Small JMH suite to sanity-check and compare implementations. Optional. |

### Division support matrix

| Implementation | Add/Sub/Mul | 128÷64 | 128÷128 | Notes |
|----------------|-------------|--------|---------|-------|
| `twoLongsBaseline` | ✅ | ✅ | ✅ | Uses `BigInteger` in arithmetic paths; easy to trust, not the fastest. |
| `fastLimb128` | ✅ | ✅ | ⚠️ | Fast 128÷64 path. 128÷128 currently not implemented in this class; use baseline for `/` and `%` or port the division from the reference `Int128`. |
| `Int128` (tests) | ✅ | ✅ | ✅ | Lives under tests; not part of the public API but used to verify behaviour. |

> **If your workload needs full 128÷128 in the high-perf path today**, either call the baseline from your code for those operations, or lift the tested divider from `src/test/java/Int128.java` into the fast implementation.

---

## Quick start

### Build (library JAR + tests)

```bash
mvn clean verify
# Produces: target/int128-0.1.0-SNAPSHOT.jar
# Also produces a shaded JAR with the JMH entry point
```

### Use in your code

Add the built JAR to your project (or `mvn install` it locally and depend on `com.symguac:int128:0.1.0-SNAPSHOT`).

**Minimal example:**

```java
import com.symguac.int128.api.*;
import com.symguac.int128.impl.highperf.FastInt128Arithmetic;

public class Example {
  public static void main(String[] args) {
    Int128Arithmetic a = new FastInt128Arithmetic();

    Int128Value x = a.fromLong(123);
    Int128Value y = a.fromLong(456);

    MutableInt128Value tmp = a.createMutable();
    a.multiplyInto(x, y, tmp);          // zero-allocation multiply
    System.out.println(tmp.toHexString());

    // Division: for full 128÷128 use baseline (or port division into fast impl)
    var baseline = new com.symguac.int128.impl.twolongs.TwoLongsBaselineArithmetic();
    MutableInt128Value q = baseline.createMutable(), r = baseline.createMutable();
    baseline.divideRemainderInto(x, y, q, r);
    System.out.println("q=" + q.toHexString() + " r=" + r.toHexString());
  }
}
```

---

## Running the (optional) benchmarks

The harness is only there to make it easy to build and sanity-check performance.
It uses a single parameter named **`implementation`** to switch between implementations.

```bash
# Run all benchmarks
java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar

# Specific benchmark + implementation (note the param name)
java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar \
  Int128ArithmeticBenchmark.additionWithReuse \
  -p implementation=fastLimb128
```

**Current built-in IDs:**
- `twoLongsBaseline`
- `fastLimb128`

**Discovering available implementations programmatically:**
```java
import com.symguac.int128.bench.Int128BenchmarkRegistry;

System.out.println(Int128BenchmarkRegistry.registeredIds());
// Output: [twoLongsBaseline, fastLimb128]
```

---

## Project layout

```
src/
├── main/java/com/symguac/int128/
│   ├── api/                    # Public interfaces
│   ├── bench/                  # Registry & ID constants
│   └── impl/
│       ├── twolongs/           # Baseline (correctness-first)
│       └── highperf/           # Fast hot-paths
├── jmh/java/com/symguac/int128/bench/
│   └── Int128ArithmeticBenchmark.java
└── test/java/
    ├── Int128.java             # Reference implementation (tests)
    └── ...                     # Property tests and division suites
```

---

## Adding a new implementation (optional)

1. Implement `Int128Arithmetic` in `src/main/java/com/symguac/int128/impl/<yours>/`.
2. Register it in `Int128BenchmarkRegistry`.
3. Add its ID to the `@Param({"..."})` in the JMH `BenchmarkState` as needed.
4. Build and test:

```bash
mvn clean verify
java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar -p implementation=<yourId>
```

---

## Notes on semantics

- Two's-complement wrap for overflow (mod 2¹²⁸).
- The low limb is treated as unsigned when composing `(high << 64) + (low & 0xFFFF...FFFF)`.
- Division is truncating toward zero; remainder carries the dividend's sign.

---

## Status

- **Stable public API** (`api/*`).
- **Baseline implementation** is complete and correctness-first.
- **Fast implementation** focuses on allocation-free hot paths; full 128÷128 division is planned/optional.

---

## Documentation

- **[Assistant Guide](docs/assistant-guide.md)** - Comprehensive guide for AI assistants working with this codebase
- **[Review History](docs/history/REVIEW_REPORT.md)** - Historical review report and issue resolutions

---

## Licence

Licensed under **MIT**. See [LICENSE](LICENSE) for full text.

---

## Contributing

Issues and PRs welcome. Please include:
- A failing test (if reporting a correctness issue).
- JMH results (if claiming a performance change) against `twoLongsBaseline`.
