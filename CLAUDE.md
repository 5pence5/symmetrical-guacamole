# CLAUDE.md - AI Assistant Guide for Int128 Library

## Repository Overview

This repository provides **high-performance 128-bit signed integer arithmetic for Java**, designed for low-latency financial trading workloads. The project uses a pluggable architecture with JMH (Java Microbenchmark Harness) to compare different Int128 implementations.

**Primary Goals:**
- Exact 128-bit signed integer arithmetic (addition, subtraction, multiplication, division)
- Exact precision for financial calculations requiring values beyond 64-bit range
- Low-latency operations suitable for trading systems
- Zero-allocation patterns in hot paths
- No BigInteger in performance-critical code (only for string conversions)

**Key Requirements:**
- Java 17+
- Maven 3.x for builds
- JMH 1.36 for benchmarking
- Performance prioritized without sacrificing correctness
- Correctness is non-negotiable

---

## Quick Repository Stats

- **Total Java files:** 17
- **Total lines of code:** ~4,600 LOC
- **Package:** `com.symguac.int128`
- **Version:** 0.1.0-SNAPSHOT
- **License:** MIT
- **Build system:** Maven
- **CI/CD:** GitHub Actions (automated build + JMH smoke test)

---

## Codebase Structure

```
symmetrical-guacamole/
├── pom.xml                          # Maven configuration
├── README.md                        # User-facing documentation
├── CLAUDE.md                        # This file - AI assistant guide
├── LICENSE                          # MIT license
├── .editorconfig                    # Code style (4 spaces, UTF-8, LF)
├── .github/workflows/
│   └── build.yml                    # CI: build + test + JMH smoke
├── docs/
│   ├── assistant-guide.md           # Detailed technical guide
│   └── history/
│       └── REVIEW_REPORT.md         # Historical review and resolutions
└── src/
    ├── main/java/com/symguac/int128/
    │   ├── api/                     # Public interfaces (stable API)
    │   │   ├── Int128Arithmetic.java          # Main arithmetic contract
    │   │   ├── Int128Value.java               # Read-only value interface
    │   │   └── MutableInt128Value.java        # Mutable value (zero-alloc)
    │   ├── bench/                   # Benchmark infrastructure
    │   │   ├── Int128BenchmarkRegistry.java   # Implementation registry
    │   │   └── Int128ImplementationIds.java   # ID constants
    │   └── impl/                    # Concrete implementations
    │       ├── twolongs/            # Baseline (correctness-first)
    │       │   ├── TwoLongsBaselineArithmetic.java
    │       │   ├── TwoLongsBaselineValue.java
    │       │   └── MutableTwoLongsBaselineValue.java
    │       └── highperf/            # Optimized (performance-first)
    │           ├── FastInt128Arithmetic.java
    │           └── FastInt128Value.java
    ├── jmh/java/com/symguac/int128/bench/
    │   └── Int128ArithmeticBenchmark.java     # JMH benchmark suite
    └── test/java/
        ├── Int128.java              # Reference implementation (910+ LOC)
        ├── Int128Test.java          # Basic tests
        ├── Int128DivisionTest.java  # Division-specific tests
        ├── Int128PropertyTest.java  # Property-based tests
        ├── DebugDivisionTest.java   # Debug helpers
        └── SimpleDivTest.java       # Simple division tests
```

---

## Architecture & Design Patterns

### Plugin-Based Architecture

The codebase uses a **plugin pattern** to enable comparing different Int128 implementations:

```
┌─────────────────────────────────────┐
│   Int128Arithmetic Interface        │  ← Plugin contract
│   (add, sub, mul, div, fromLong)    │
└─────────────────────────────────────┘
              ▲
              │ implements
    ┌─────────┴─────────┐
    │                   │
┌───────────────┐  ┌──────────────────┐
│ Baseline      │  │ FastInt128       │
│ (Correct)     │  │ (Optimized)      │
└───────────────┘  └──────────────────┘
    │                   │
    └─────────┬─────────┘
              │
    ┌─────────▼──────────┐
    │ Registry           │  ← Factory pattern
    │ + JMH Benchmarks   │
    └────────────────────┘
```

### Key Interfaces

**Int128Value** (`api/Int128Value.java`)
- Read-only interface with `high()` and `low()` accessors
- Represents 128-bit value as two 64-bit limbs
- Value computation: `(high << 64) + (low as unsigned)`

**MutableInt128Value** (`api/MutableInt128Value.java`)
- Extends Int128Value with mutability
- Enables zero-allocation patterns in tight loops
- Methods: `set(high, low)`, `immutableCopy()`

**Int128Arithmetic** (`api/Int128Arithmetic.java`)
- Main contract for all implementations
- Mutable operations: `addInto()`, `subtractInto()`, `multiplyInto()`, `divideRemainderInto()`
- Immutable operations: `add()`, `multiply()`, `divide()`, `remainder()`
- Factory methods: `fromParts(high, low)`, `fromLong(value)`, `createMutable()`
- Division methods added with default implementation throwing UnsupportedOperationException

### Two-Limb Representation

All implementations represent 128-bit values as **two 64-bit signed longs**:
- `high`: upper 64 bits (signed, contains sign bit at position 127)
- `low`: lower 64 bits (treated as unsigned for composition)
- Value = `(high << 64) + (low & 0xFFFF_FFFF_FFFF_FFFFL)`

---

## Current Implementations

### 1. TwoLongs Baseline (`impl/twolongs/`)

**Registry ID:** `twoLongsBaseline`

**Philosophy:** Correctness and readability over performance

**Characteristics:**
- Uses `BigInteger` for multiplication and division (conservative approach)
- Straightforward, easy-to-verify logic
- Serves as reference for correctness testing
- ~200 LOC total across 3 classes
- Fully implements all arithmetic operations including division

**When to use:**
- Verifying new implementations
- Understanding the API contract
- Reference for correctness in tests
- When reliability matters more than speed

### 2. FastInt128 (`impl/highperf/`)

**Registry ID:** `fastLimb128`

**Philosophy:** Maximum performance without sacrificing correctness

**Characteristics:**
- No BigInteger in arithmetic hot paths
- Custom 64×64→128 multiplication using 32-bit limb decomposition
- Inline hot-path operations
- Factory method reuses singleton constants (ZERO, ONE)
- ~1000+ LOC in FastInt128Value.java

**Optimizations:**
- Portable 32-bit split for multiplication (JIT-friendly on x86_64 and AArch64)
- 128÷64 fast path for common financial operations
- Zero-allocation arithmetic in mutable mode
- Constant folding for common values

**Current limitation:** Full 128÷128 division not yet implemented in this class

### 3. Int128 Reference (`test/Int128.java`)

**Status:** Comprehensive standalone implementation used for testing

**Characteristics:**
- Complete 910+ line implementation
- Full arithmetic: add, sub, mul, div, rem
- Bitwise operations: and, or, xor, not, shifts
- String conversions: toString, fromString (decimal and hex)
- Financial helpers: division by powers of 10, rounding
- Constants: ZERO, ONE, DECIMAL_BASE, MIN_VALUE, MAX_VALUE
- Implements `Comparable<Int128>` and `Serializable`
- BigInteger used ONLY for string conversions, not arithmetic

**Key features:**
- Fast 128÷64 division path
- Optimized 128÷128 division (two-limb approximation + correction)
- Decimal and hexadecimal parsing
- Self-contained, immutable, thread-safe
- Used as reference for verifying other implementations

### Division Support Matrix

| Implementation | Add/Sub/Mul | 128÷64 | 128÷128 | Notes |
|----------------|-------------|--------|---------|-------|
| `twoLongsBaseline` | ✅ | ✅ | ✅ | Uses `BigInteger`; easy to trust, not the fastest |
| `fastLimb128` | ✅ | ✅ | ⚠️ | Fast hot paths; 128÷128 not yet implemented in this class |
| `Int128` (tests) | ✅ | ✅ | ✅ | Reference implementation in test suite; not exported as API |

**Recommendation:** For full 128÷128 division:
- Use `twoLongsBaseline` for division operations (hybrid approach)
- Or port the tested division algorithm from `src/test/java/Int128.java` into fast implementation

---

## Development Workflow

### Building the Project

```bash
# Clean build with all artifacts
mvn clean verify

# Outputs:
# - target/int128-0.1.0-SNAPSHOT.jar           (library)
# - target/int128-0.1.0-SNAPSHOT-shaded.jar    (runnable JMH)
# - target/int128-0.1.0-SNAPSHOT-sources.jar   (sources)
# - target/int128-0.1.0-SNAPSHOT-javadoc.jar   (javadoc)
```

### Running Tests

```bash
# Run all JUnit tests
mvn test

# Run tests with verbose output
mvn test -X

# Run specific test class
mvn test -Dtest=Int128DivisionTest
```

### Running Benchmarks

```bash
# Method 1: Via Maven plugin
mvn jmh:benchmark

# Method 2: Run all benchmarks via shaded JAR
java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar

# Method 3: Specific benchmark
java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar \
  Int128ArithmeticBenchmark.additionWithReuse

# Method 4: Specific implementation
java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar \
  -p implementation=fastLimb128

# Method 5: Quick smoke test (fast iteration)
java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar \
  -bm thrpt -f 1 -wi 1 -i 1 -r 100ms \
  Int128ArithmeticBenchmark.additionWithReuse

# Method 6: With profiling
java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar \
  -prof gc \
  Int128ArithmeticBenchmark.multiplicationWithReuse
```

**Available profilers:**
- `-prof gc` - Garbage collection profiling
- `-prof stack` - Stack profiling (hotspots)
- `-prof perf` - Linux perf events (requires setup)

### Git Workflow for AI Assistants

**Current branch:** Check with `git branch --show-current`

**Branch naming convention:**
- All feature branches MUST start with `claude/`
- Format: `claude/{description}-{session-id}`
- Example: `claude/add-division-012345abcdef`

**Commit message style:**
- Clear, concise descriptions focusing on "what" and "why"
- Use imperative mood ("Add feature" not "Added feature")
- Examples from history:
  - "Fix shaded JMH jar packaging"
  - "Completely redo technical review report with current state"
  - "Add quick polish improvements for shareability"

**Pushing changes:**
```bash
# Always push to current branch with -u flag
git push -u origin $(git branch --show-current)

# The branch name MUST start with 'claude/' and end with session ID
# or push will fail with 403 error

# If network errors occur, retry up to 4 times with exponential backoff:
# - Wait 2s, retry
# - Wait 4s, retry
# - Wait 8s, retry
# - Wait 16s, retry
```

**Making commits:**
```bash
# Stage relevant files
git add src/main/java/com/symguac/int128/impl/yourchange/

# Commit with descriptive message
git commit -m "$(cat <<'EOF'
Add optimized division implementation

Implements fast 128÷64 and 128÷128 division paths
without BigInteger in hot paths.
EOF
)"

# Push to remote
git push -u origin $(git branch --show-current)
```

### Continuous Integration

**GitHub Actions workflow** (`.github/workflows/build.yml`):
1. Checkout code
2. Setup Java 17 (Temurin distribution)
3. Build & run unit tests: `mvn clean verify`
4. JMH smoke test: Quick benchmark run (100ms warmup/measurement)
5. Triggers on: push to any branch, pull requests

**CI expectations:**
- All tests must pass
- Build must succeed
- JMH smoke test must complete without errors

---

## Key Conventions & Standards

### Code Style

**EditorConfig settings** (`.editorconfig`):
- Line endings: LF (Unix-style)
- Charset: UTF-8
- Trim trailing whitespace: Yes
- Insert final newline: Yes
- Java indent: 4 spaces (not tabs)

**Java conventions:**
- Java 17 language features encouraged
- Package structure: `com.symguac.int128.{api|impl|bench}`
- Final classes for implementations (prevent subclassing)
- Use `@Override` annotations consistently

**Performance conventions:**
- Minimize allocations in hot paths
- Prefer primitive types over boxed types
- Use `final` for immutable fields
- Inline small helper methods (JIT-friendly)
- Avoid branches in critical loops when possible
- Never use exceptions for control flow

**Documentation:**
- JavaDoc required on all public APIs
- Class-level design documentation
- Performance notes on critical sections
- Explain non-obvious optimizations with comments

### Naming Conventions

**Implementation classes:**
- Pattern: `{Strategy}{Concept}{Type}`
- Examples: `TwoLongsBaselineArithmetic`, `FastInt128Value`, `MutableTwoLongsBaselineValue`

**Constants:**
- Static finals: `ALL_CAPS` - e.g., `ZERO`, `ONE`, `MIN_VALUE`, `MAX_VALUE`, `DECIMAL_BASE`
- Implementation IDs: camelCase strings - e.g., `"twoLongsBaseline"`, `"fastLimb128"`

**Methods:**
- Mutable operations: `{verb}Into` - e.g., `addInto(a, b, dest)`, `multiplyInto(x, y, result)`
- Immutable operations: `{verb}` - e.g., `add(a, b)`, `multiply(x, y)`
- Factory methods: `from{Source}` - e.g., `fromLong(123)`, `fromParts(hi, lo)`
- Query methods: `is{Property}` or `has{Property}` - e.g., `isZero()`, `isNegative()`

### Testing Philosophy

**Current approach:**
- JUnit 5 for unit tests
- Property-based tests for arithmetic operations
- Comprehensive division test suite
- Reference Int128 implementation used for verification
- Benchmarks serve as integration tests

**What to test:**
- Basic arithmetic correctness (0+1=1, 1-1=0)
- Boundary conditions (MIN_VALUE, MAX_VALUE)
- Overflow/underflow behavior (wrapping, not throwing)
- String round-trip (toString/fromString consistency)
- Division identity (a = q*d + r)
- Edge cases (division by zero, negative numbers, powers of 10)

**Test organization:**
- `Int128Test.java` - Basic operations
- `Int128DivisionTest.java` - Division-specific tests
- `Int128PropertyTest.java` - Property-based tests
- `DebugDivisionTest.java` - Debug and edge cases
- `SimpleDivTest.java` - Simple division scenarios

---

## Performance Considerations

### Critical Hot Paths (Priority Order)

**Top Priority (Zero Allocation Required):**
1. Addition with reuse (`addInto`)
2. Subtraction with reuse (`subtractInto`)
3. Multiplication with reuse (`multiplyInto`)

**High Priority:**
4. Division by small divisors (≤64 bits)
5. Division by powers of 10 (10^k for k≤19)

**Medium Priority:**
6. General 128÷128 division
7. Remainder operations
8. String conversions (toString, fromString)

### Optimization Techniques Used

**64×64→128 Multiplication:**
```java
// Portable 32-bit split (JIT optimizes well on all platforms)
long a_lo = a & 0xFFFFFFFFL;
long a_hi = a >>> 32;
long b_lo = b & 0xFFFFFFFFL;
long b_hi = b >>> 32;

// Four partial products with carry handling
long lo_lo = a_lo * b_lo;
long lo_hi = a_lo * b_hi;
long hi_lo = a_hi * b_lo;
long hi_hi = a_hi * b_hi;
// ... combine with carry propagation
```

**Fast 128÷64 Division:**
```java
// When divisor fits in 64 bits, use optimized path
if (divisor_high == 0 || divisor_high == -1) {
    // Specialized division algorithm
    // Approximately 3-5x faster than BigInteger
}
```

**Singleton Reuse:**
```java
// Avoid allocations for common constants
if (hi == 0 && lo == 0) return ZERO;
if (hi == 0 && lo == 1) return ONE;
if (hi == -1 && lo == -1) return MINUS_ONE;
```

### Performance Anti-Patterns to Avoid

**Never do this:**
- ❌ Use `BigInteger` in arithmetic hot paths (only OK in toString/fromString)
- ❌ Allocate new objects in tight loops (use mutable variants)
- ❌ Box primitives unnecessarily (`Long` instead of `long`)
- ❌ Use exceptions for control flow (expensive)
- ❌ Add unnecessary bounds checks (trust two's complement wrapping)
- ❌ Call virtual methods in innermost loops (prefer `final` classes)

**Always prefer:**
- ✅ Reuse mutable instances in benchmarks
- ✅ Inline small helper methods (let JIT optimize)
- ✅ Use unsigned comparisons for low limb (`Long.compareUnsigned`)
- ✅ Leverage JIT optimizations (straight-line code, no branches)
- ✅ Profile before optimizing (use `-prof gc` and `-prof stack`)
- ✅ Measure with JMH (don't guess performance)

---

## Adding New Implementations

### Step-by-Step Guide

**1. Create implementation package:**

```bash
mkdir -p src/main/java/com/symguac/int128/impl/yourname
```

**2. Implement the arithmetic class:**

```java
package com.symguac.int128.impl.yourname;

import com.symguac.int128.api.*;

public class YourInt128Arithmetic implements Int128Arithmetic {

    private static final String IMPL_ID = "yourImplementation";

    @Override
    public String id() {
        return IMPL_ID;
    }

    @Override
    public Int128Value fromParts(long high, long low) {
        return new YourInt128Value(high, low);
    }

    @Override
    public Int128Value fromLong(long value) {
        long high = (value < 0) ? -1L : 0L;
        return fromParts(high, value);
    }

    @Override
    public MutableInt128Value createMutable() {
        return new YourMutableInt128Value();
    }

    @Override
    public void addInto(Int128Value left, Int128Value right,
                        MutableInt128Value dest) {
        long lh = left.high(), ll = left.low();
        long rh = right.high(), rl = right.low();

        // Your optimized addition logic
        long sumLow = ll + rl;
        long carry = (Long.compareUnsigned(sumLow, ll) < 0) ? 1 : 0;
        long sumHigh = lh + rh + carry;

        dest.set(sumHigh, sumLow);
    }

    @Override
    public void subtractInto(Int128Value left, Int128Value right,
                             MutableInt128Value dest) {
        // Your optimized subtraction logic
    }

    @Override
    public void multiplyInto(Int128Value left, Int128Value right,
                             MutableInt128Value dest) {
        // Your optimized multiplication logic
    }

    @Override
    public void divideRemainderInto(Int128Value dividend, Int128Value divisor,
                                     MutableInt128Value quotient,
                                     MutableInt128Value remainder) {
        // Your optimized division logic (optional)
        // Or throw UnsupportedOperationException
    }
}
```

**3. Implement value classes:**

Create `YourInt128Value.java` (immutable) and `YourMutableInt128Value.java` (mutable).

**4. Register in benchmark registry:**

Edit `src/main/java/com/symguac/int128/bench/Int128BenchmarkRegistry.java`:

```java
static {
    register(Int128ImplementationIds.TWO_LONGS_BASELINE,
             TwoLongsBaselineArithmetic::new);
    register(Int128ImplementationIds.FAST_LIMB_BASED,
             FastInt128Arithmetic::new);
    register("yourImplementation", YourInt128Arithmetic::new);  // Add this
}
```

**5. Add ID constant:**

Edit `src/main/java/com/symguac/int128/bench/Int128ImplementationIds.java`:

```java
public static final String YOUR_IMPLEMENTATION = "yourImplementation";
```

**6. Enable in benchmarks (optional):**

Edit `src/jmh/java/com/symguac/int128/bench/Int128ArithmeticBenchmark.java`:

```java
@Param({
    Int128ImplementationIds.TWO_LONGS_BASELINE,
    Int128ImplementationIds.FAST_LIMB_BASED,
    "yourImplementation"  // Add to compare with others
})
private String implementation;
```

**7. Build and test:**

```bash
mvn clean verify
java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar \
  -p implementation=yourImplementation
```

---

## Benchmarking Guide

### Available Benchmarks

The `Int128ArithmeticBenchmark` class provides these benchmarks:

| Benchmark | Description | Pattern | What it measures |
|-----------|-------------|---------|------------------|
| `additionWithReuse` | Addition reusing mutable instance | Zero-allocation | Hot-path add performance |
| `subtractionWithReuse` | Subtraction reusing mutable instance | Zero-allocation | Hot-path subtract performance |
| `multiplicationWithReuse` | Multiplication reusing mutable instance | Zero-allocation | Hot-path multiply performance |
| `additionAllocating` | Addition creating new objects | Allocating | Immutable add + allocation cost |
| `multiplicationAllocating` | Multiplication creating new objects | Allocating | Immutable multiply + allocation cost |

### Benchmark Configuration

**JMH settings (in code):**
- Mode: Throughput (operations/second)
- Warmup: 3 iterations × 1 second each
- Measurement: 5 iterations × 1 second each
- Forks: 1
- Dataset: 1024 random 128-bit values (pre-generated)
- Parameters: `implementation` (twoLongsBaseline, fastLimb128, etc.)

**Dataset characteristics:**
- Random values across full 128-bit range
- Includes edge cases (MIN_VALUE, MAX_VALUE, ZERO, ONE)
- Same dataset used for all implementations (fair comparison)

### Interpreting Results

**Good performance indicators:**
- ✅ Higher ops/sec for reuse benchmarks (ideally >100M ops/sec for add)
- ✅ Small gap between reuse and allocating benchmarks (<2x)
- ✅ Consistent results across iterations (std dev <5%)
- ✅ Competitive with baseline for simple operations (add/sub within 20%)
- ✅ Multiply faster than baseline (goal: 2-3x faster)

**Red flags:**
- 🚩 Large performance gap vs baseline for simple ops (>50% slower)
- 🚩 High variance in results (std dev >10%)
- 🚩 Much slower multiplication than baseline
- 🚩 Significant GC activity in reuse benchmarks
- 🚩 Allocation rate >0 in reuse benchmarks

**Example output interpretation:**
```
Benchmark                                        (implementation)   Mode  Cnt   Score    Error  Units
Int128ArithmeticBenchmark.additionWithReuse      twoLongsBaseline  thrpt    5  85.234 ± 2.145  ops/us
Int128ArithmeticBenchmark.additionWithReuse      fastLimb128       thrpt    5  92.567 ± 1.823  ops/us
```
This shows fastLimb128 is ~8.6% faster than baseline for addition.

---

## Common Tasks for AI Assistants

### Task 1: Analyzing Performance Issues

**When asked to investigate slow performance:**

1. **Identify the operation:**
   - Which benchmark is slow? (add, sub, mul, div)
   - Which implementation? (check registry ID)

2. **Check the implementation:**
   ```bash
   # Read the relevant file
   cat src/main/java/com/symguac/int128/impl/{name}/*Arithmetic.java
   ```
   - Look for allocations in `*Into` methods (should be none)
   - Verify no BigInteger usage in hot paths
   - Check for unnecessary object creation

3. **Run benchmarks:**
   ```bash
   mvn clean package

   # Run specific benchmark for specific implementation
   java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar \
     Int128ArithmeticBenchmark.{operation}WithReuse \
     -p implementation={implId}

   # Compare with baseline
   java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar \
     Int128ArithmeticBenchmark.{operation}WithReuse \
     -p implementation=twoLongsBaseline
   ```

4. **Profile if needed:**
   ```bash
   # Check for GC pressure
   java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar \
     Int128ArithmeticBenchmark.{operation}WithReuse \
     -p implementation={implId} \
     -prof gc

   # Find hotspots
   java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar \
     Int128ArithmeticBenchmark.{operation}WithReuse \
     -p implementation={implId} \
     -prof stack
   ```

5. **Report findings:**
   - Quantify performance gap vs baseline
   - Identify root causes (allocations, branches, etc.)
   - Suggest specific optimizations

### Task 2: Implementing Missing Operations

**When asked to add division, modulo, or bitwise operations:**

1. **Check the reference implementation:**
   - `src/test/java/Int128.java` contains complete implementations
   - Algorithms are tested and verified correct
   - Copy and adapt to target implementation

2. **For division specifically:**
   ```java
   // See Int128.java methods:
   // - divide(Int128 divisor)
   // - remainder(Int128 divisor)
   // - divideAndRemainder(Int128 divisor)

   // Key techniques:
   // 1. Fast path for 128÷64 (divisor fits in 64 bits)
   // 2. Two-limb approximation for 128÷128
   // 3. Correction step for exact result
   // 4. No BigInteger (all arithmetic in longs)
   ```

3. **Update the interface:**
   - Add method signature to `Int128Arithmetic.java` if needed
   - Provide default implementation that throws UnsupportedOperationException
   - Implement in specific arithmetic classes

4. **Add tests:**
   - Create test cases for edge cases (MIN_VALUE, MAX_VALUE, zero, negative)
   - Verify division identity: `dividend = quotient * divisor + remainder`
   - Test against baseline implementation

5. **Add benchmark if performance-critical:**
   - Add new method to `Int128ArithmeticBenchmark.java`
   - Measure before/after performance

### Task 3: Fixing Correctness Issues

**When arithmetic produces wrong results:**

1. **Create minimal reproduction:**
   ```java
   // In a test file
   @Test
   void reproduceBug() {
       Int128Arithmetic impl = new YourImplementation();
       Int128Value a = impl.fromParts(highA, lowA);
       Int128Value b = impl.fromParts(highB, lowB);

       MutableInt128Value result = impl.createMutable();
       impl.addInto(a, b, result);  // Or other operation

       // Expected vs actual
       assertEquals(expectedHigh, result.high());
       assertEquals(expectedLow, result.low());
   }
   ```

2. **Compare against baseline:**
   ```java
   @Test
   void compareWithBaseline() {
       TwoLongsBaselineArithmetic baseline = new TwoLongsBaselineArithmetic();
       YourImplementation yours = new YourImplementation();

       Int128Value a = baseline.fromParts(highA, lowA);
       Int128Value b = baseline.fromParts(highB, lowB);

       Int128Value baseResult = baseline.add(a, b);
       Int128Value yourResult = yours.add(
           yours.fromParts(highA, lowA),
           yours.fromParts(highB, lowB)
       );

       assertEquals(baseResult.high(), yourResult.high());
       assertEquals(baseResult.low(), yourResult.low());
   }
   ```

3. **Check two's complement semantics:**
   - Overflow should wrap (not throw exception)
   - Sign extension must be correct in `fromLong()`
   - Low limb is ALWAYS treated as unsigned in composition
   - Sign bit is bit 127 (high bit of `high` limb)

4. **Verify edge cases:**
   ```java
   // MIN_VALUE + (-1) should wrap to MAX_VALUE
   Int128Value min = impl.fromParts(Long.MIN_VALUE, 0);
   Int128Value minusOne = impl.fromLong(-1);
   Int128Value result = impl.add(min, minusOne);
   assertEquals(Long.MAX_VALUE, result.high());
   assertEquals(0xFFFF_FFFF_FFFF_FFFFL, result.low());

   // MAX_VALUE + 1 should wrap to MIN_VALUE
   Int128Value max = impl.fromParts(Long.MAX_VALUE, 0xFFFF_FFFF_FFFF_FFFFL);
   Int128Value one = impl.fromLong(1);
   result = impl.add(max, one);
   assertEquals(Long.MIN_VALUE, result.high());
   assertEquals(0L, result.low());
   ```

5. **Fix and verify:**
   - Correct the implementation
   - Run all tests to ensure no regressions
   - Run benchmarks to ensure performance not degraded

### Task 4: Adding Required Constants

**When asked to ensure constants exist in an implementation:**

Check for these public static final fields:

```java
public static final YourInt128 ZERO = new YourInt128(0, 0);
public static final YourInt128 ONE = new YourInt128(0, 1);
public static final YourInt128 MINUS_ONE = new YourInt128(-1, -1);
public static final YourInt128 DECIMAL_BASE = new YourInt128(0, 10);

// MIN_VALUE = -2^127 = 0x8000_0000_0000_0000_0000_0000_0000_0000
public static final YourInt128 MIN_VALUE =
    new YourInt128(Long.MIN_VALUE, 0);

// MAX_VALUE = 2^127 - 1 = 0x7FFF_FFFF_FFFF_FFFF_FFFF_FFFF_FFFF_FFFF
public static final YourInt128 MAX_VALUE =
    new YourInt128(Long.MAX_VALUE, 0xFFFF_FFFF_FFFF_FFFFL);
```

**Verification:**
```java
@Test
void verifyConstants() {
    assertEquals(0, ZERO.high());
    assertEquals(0, ZERO.low());

    assertEquals(0, ONE.high());
    assertEquals(1, ONE.low());

    assertEquals(Long.MIN_VALUE, MIN_VALUE.high());
    assertEquals(0, MIN_VALUE.low());

    assertEquals(Long.MAX_VALUE, MAX_VALUE.high());
    assertEquals(0xFFFF_FFFF_FFFF_FFFFL, MAX_VALUE.low());
}
```

### Task 5: Updating Documentation

**When asked to document changes:**

1. **Update CLAUDE.md** (this file):
   - Add new implementations to "Current Implementations"
   - Update division support matrix if applicable
   - Document new conventions or patterns
   - Add version history entry at the bottom

2. **Update README.md**:
   - User-facing documentation
   - Build instructions if changed
   - New benchmark examples if added
   - Update feature matrix if capabilities changed

3. **Update JavaDoc**:
   - All public methods need JavaDoc comments
   - Explain performance characteristics
   - Document preconditions (e.g., "divisor must not be zero")
   - Document postconditions (e.g., "returns quotient truncated toward zero")
   - Add `@throws` tags for exceptions

4. **Update assistant-guide.md if needed**:
   - Located at `docs/assistant-guide.md`
   - More detailed technical guide
   - Keep in sync with CLAUDE.md

### Task 6: Creating Comprehensive Int128 Implementation

**When asked to create a complete, production-ready Int128 class:**

**Use `src/test/java/Int128.java` as the template** (910+ lines, battle-tested):

**1. Required components checklist:**

- [ ] **Constants:**
  - ZERO, ONE, MINUS_ONE
  - MIN_VALUE, MAX_VALUE
  - DECIMAL_BASE (value 10)
  - Powers of 10 (optional but useful)

- [ ] **Construction:**
  - Private constructor: `Int128(long high, long low)`
  - Static factory: `valueOf(long value)` with sign extension
  - Static factory: `fromParts(long high, long low)`
  - Reuse constants in factories (performance)

- [ ] **Arithmetic operations:**
  - `add(Int128 other)` - with carry handling
  - `subtract(Int128 other)` - with borrow handling
  - `multiply(Int128 other)` - 64×64→128 without BigInteger
  - `divide(Int128 divisor)` - with fast 128÷64 path
  - `remainder(Int128 divisor)` - efficient modulo
  - `negate()` - two's complement negation
  - `abs()` - absolute value

- [ ] **Comparisons:**
  - `equals(Object obj)` - proper equality
  - `hashCode()` - consistent with equals
  - `compareTo(Int128 other)` - implement Comparable
  - `signum()` - returns -1, 0, or 1

- [ ] **Bitwise operations:**
  - `and(Int128 other)`, `or(Int128 other)`, `xor(Int128 other)`
  - `not()` - bitwise complement
  - `shiftLeft(int bits)`, `shiftRight(int bits)` - arithmetic shift
  - `shiftRightUnsigned(int bits)` - logical shift

- [ ] **String conversions:**
  - `toString()` - decimal representation
  - `toHexString()` - hexadecimal (e.g., "0x1234567890abcdef...")
  - `valueOf(String s)` - parse decimal
  - `fromHexString(String s)` - parse hexadecimal
  - Use BigInteger ONLY in these methods, not in arithmetic

- [ ] **Query methods:**
  - `isZero()`, `isNegative()`, `isPositive()`
  - `high()`, `low()` - limb accessors
  - `toLong()` - extract low 64 bits (with overflow check)
  - `toDouble()` - best-effort floating point

- [ ] **Serialization:**
  - `implements Serializable`
  - Provide `serialVersionUID`

**2. Performance requirements:**

- No BigInteger in arithmetic operations (add, sub, mul, div)
- Implement 64×64→128 multiplication using 32-bit limb decomposition
- Fast 128÷64 division path (when divisor fits in 64 bits)
- Optimized 128÷128 division (two-limb approximation, not bit-by-bit)
- Factory methods reuse singleton constants (avoid allocation)
- Make class `final` (enables JIT optimizations)
- Make fields `private final` (immutability)

**3. Implementation algorithm examples:**

**Addition with carry:**
```java
public Int128 add(Int128 other) {
    long sumLow = this.low + other.low;
    // Carry occurs if unsigned sum is less than either operand
    long carry = (Long.compareUnsigned(sumLow, this.low) < 0) ? 1 : 0;
    long sumHigh = this.high + other.high + carry;
    return fromParts(sumHigh, sumLow);
}
```

**64×64→128 Multiplication:**
```java
private static long[] multiply64x64(long a, long b) {
    // Split into 32-bit limbs
    long a_lo = a & 0xFFFFFFFFL;
    long a_hi = a >>> 32;
    long b_lo = b & 0xFFFFFFFFL;
    long b_hi = b >>> 32;

    // Four partial products
    long p0 = a_lo * b_lo;
    long p1 = a_lo * b_hi;
    long p2 = a_hi * b_lo;
    long p3 = a_hi * b_hi;

    // Combine with carry propagation
    long mid = p1 + p2 + (p0 >>> 32);
    long high = p3 + (mid >>> 32);
    long low = (mid << 32) | (p0 & 0xFFFFFFFFL);

    return new long[]{high, low};
}
```

**4. Testing checklist:**

- [ ] All arithmetic operations tested with edge cases
- [ ] MIN_VALUE and MAX_VALUE handled correctly
- [ ] Overflow wraps correctly (two's complement)
- [ ] Division by zero throws ArithmeticException
- [ ] Division identity holds: `a = q*d + r` where `|r| < |d|`
- [ ] String round-trip works: `valueOf(x.toString()).equals(x)`
- [ ] Hex round-trip works: `fromHexString(x.toHexString()).equals(x)`
- [ ] Equals and hashCode contract satisfied
- [ ] compareTo consistent with equals
- [ ] At least 1000 lines of well-tested code

**5. Reference the existing Int128.java:**

The implementation at `src/test/java/Int128.java` is the gold standard:
- 910+ lines
- All operations implemented
- No BigInteger in arithmetic
- Fast division algorithm
- Comprehensive string handling
- Battle-tested through extensive test suite

---

## File Locations Quick Reference

| Need to... | Check these files |
|------------|-------------------|
| Understand public API | `src/main/java/com/symguac/int128/api/*.java` |
| See baseline (correct) | `src/main/java/com/symguac/int128/impl/twolongs/` |
| See optimized (fast) | `src/main/java/com/symguac/int128/impl/highperf/` |
| Reference complete impl | `src/test/java/Int128.java` |
| Add new implementation | `src/main/java/com/symguac/int128/impl/{newname}/` |
| Register implementation | `src/main/java/com/symguac/int128/bench/Int128BenchmarkRegistry.java` |
| Configure benchmarks | `src/jmh/java/com/symguac/int128/bench/Int128ArithmeticBenchmark.java` |
| Understand build | `pom.xml` |
| Read user docs | `README.md` |
| Detailed tech guide | `docs/assistant-guide.md` |
| Historical context | `docs/history/REVIEW_REPORT.md` |
| CI configuration | `.github/workflows/build.yml` |
| Code style settings | `.editorconfig` |

---

## Important Reminders for AI Assistants

### For Performance Work

- **Always benchmark before claiming improvements**
  - Use JMH, don't guess
  - Compare against baseline
  - Run multiple times to ensure consistency

- **Profile before optimizing**
  - Use `-prof gc` to check allocations
  - Use `-prof stack` to find hotspots
  - Measure, don't speculate

- **Document why optimizations work**
  - Explain the technique
  - Reference JIT behavior when relevant
  - Note any platform-specific considerations

### For Correctness Work

- **Two's complement semantics everywhere**
  - Overflow wraps, doesn't throw
  - Sign bit is bit 127 (high bit of high limb)
  - Low limb is ALWAYS unsigned in composition

- **Test edge cases thoroughly**
  - MIN_VALUE, MAX_VALUE, ZERO, ONE, MINUS_ONE
  - Overflow and underflow scenarios
  - Division by zero must throw ArithmeticException
  - Division identity must hold

- **Compare against baseline**
  - TwoLongsBaseline is the reference
  - If results differ, baseline is probably right
  - Investigate and fix discrepancies

### For New Implementations

- **Start from Int128.java reference**
  - Located at `src/test/java/Int128.java`
  - 910+ lines of battle-tested code
  - All algorithms implemented and verified

- **Implement complete API**
  - No partial implementations
  - All operations or throw UnsupportedOperationException
  - Register in benchmark registry

- **Test extensively**
  - Unit tests for all operations
  - Property-based tests (commutativity, associativity)
  - Compare results with baseline
  - Test MIN_VALUE, MAX_VALUE, zero, negative numbers

- **Document performance characteristics**
  - Time complexity of operations
  - Allocation behavior (zero-alloc vs allocating)
  - Known limitations (e.g., "128÷128 not yet optimized")

### For Code Changes

- **Never commit without clear description**
  - Use imperative mood ("Add feature" not "Added feature")
  - Explain what and why, not just what
  - Reference issue numbers if applicable

- **Push to correct branch**
  - MUST start with `claude/`
  - MUST end with matching session ID
  - Use `git push -u origin $(git branch --show-current)`

- **Maintain code style**
  - Follow .editorconfig settings
  - 4 spaces for Java indentation
  - UTF-8 encoding, LF line endings
  - Trim trailing whitespace

- **Update documentation when needed**
  - Update CLAUDE.md for conventions/architecture changes
  - Update README.md for user-facing changes
  - Update JavaDoc for API changes
  - Keep docs/ directory in sync

### For Division Operations

- **Division is complex and critical**
  - Test extensively with edge cases
  - Verify division identity: `dividend = quotient * divisor + remainder`
  - Handle negative numbers correctly (truncate toward zero)
  - Remainder has same sign as dividend

- **Fast paths are important**
  - 128÷64 fast path for common case (divisor fits in 64 bits)
  - Division by powers of 10 for financial calculations
  - Two-limb approximation for 128÷128 (avoid bit-by-bit)

- **Reference the existing Int128.java division**
  - Algorithm at `src/test/java/Int128.java`
  - Methods: `divide()`, `remainder()`, `divideAndRemainder()`
  - Well-tested and verified correct

---

## Version History

### 2025-11-14 (Current)
- ✅ Created comprehensive CLAUDE.md at repository root
- ✅ Documented current state with 17 Java files, ~4,600 LOC
- ✅ Added detailed workflows for AI assistants
- ✅ Included git workflow specifics for Claude Code
- ✅ Documented all implementations and their status
- ✅ Added comprehensive task guides for common operations
- ✅ Included benchmarking and performance guidelines

### 2025-11-13 (Recent Updates)
- ✅ Fixed shaded JMH jar packaging (commit af55017)
- ✅ Redid technical review report with current state (commit d2b3a84)
- ✅ Configured Maven JMH plugin (commit fd27fdc)
- ✅ Added polish improvements for shareability (commit 297e2d8)
- ✅ Switched license from Apache-2.0 to MIT (commit 01d4a34)

### Earlier (Foundation)
- ✅ Division edge case tests verified passing
- ✅ Division methods added to Int128Arithmetic interface
- ✅ Int128.java (formerly Fast128.java) moved to test suite
- ✅ Two implementations registered: twoLongsBaseline, fastLimb128
- ✅ JMH benchmark suite with 5 benchmarks
- ✅ Java 17, Maven 3.x, JMH 1.36 foundation established
- ✅ Critical division bugs fixed in commits 9d2d444, cc7caa9, 5897185

---

## Additional Resources

**For more details, see:**
- `docs/assistant-guide.md` - Detailed technical guide (original comprehensive doc)
- `docs/history/REVIEW_REPORT.md` - Historical review and issue resolutions
- `README.md` - User-facing quick start and examples
- `src/test/java/Int128.java` - Reference implementation (910+ LOC)

**For questions:**
- Check existing code first (especially Int128.java reference)
- Review commit history for context: `git log --oneline`
- Run benchmarks to validate performance claims
- Test against baseline to verify correctness

---

## Questions & Support

**For AI Assistants:**
- When in doubt, check `Int128.java` for reference implementation
- Prioritize correctness over performance (but measure both)
- Always test against baseline implementation
- Document non-obvious design decisions in code comments
- Use JMH for performance claims, don't guess
- Follow git workflow conventions strictly (branch naming, push with -u)

**For Humans:**
- See `README.md` for quick start guide
- Check `docs/assistant-guide.md` for detailed technical information
- Run `mvn clean verify` to build and test
- Run `java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar` for benchmarks
- Review git history for implementation evolution: `git log --oneline`

---

**Last updated:** 2025-11-14
**Repository:** symmetrical-guacamole
**Purpose:** High-performance 128-bit integer arithmetic for Java
