# CLAUDE.md - AI Assistant Guide for Int128 Benchmark Harness

## Repository Overview

This repository hosts **experimental infrastructure for high-performance 128-bit signed integer implementations** optimized for low-latency financial trading workloads. The project provides a pluggable benchmark harness using JMH (Java Microbenchmark Harness) to compare different Int128 arithmetic strategies.

**Primary Goals:**
- High-performance 128-bit signed integer arithmetic (addition, subtraction, multiplication, division)
- Exact precision for financial calculations
- Low-latency operations suitable for trading systems
- Zero-allocation patterns where possible
- No BigInteger in hot paths (only for string conversions)

**Key Requirements:**
- Java 17
- Maven 3.x
- JMH 1.36 for benchmarking
- JUnit 5.10.1 for testing
- Performance prioritized over code simplicity
- Correctness is non-negotiable

---

## Codebase Structure

```
symmetrical-guacamole/
├── pom.xml                          # Maven configuration (Java 17, JMH, JUnit 5)
├── Readme                           # User-facing documentation
├── CLAUDE.md                        # This file - AI assistant guide
├── REVIEW_REPORT.md                 # Comprehensive code review with known bugs
└── src/
    ├── main/java/com/symguac/int128/
    │   ├── api/                     # Core interfaces (plugin contract)
    │   │   ├── Int128Arithmetic.java          # Main arithmetic interface
    │   │   ├── Int128Value.java               # Read-only value interface
    │   │   └── MutableInt128Value.java        # Mutable value (zero-alloc)
    │   ├── bench/                   # Benchmark infrastructure
    │   │   ├── Int128BenchmarkRegistry.java   # Plugin registry
    │   │   └── Int128ImplementationIds.java   # ID constants
    │   └── impl/                    # Concrete implementations
    │       ├── twolongs/            # Baseline (correctness-first)
    │       │   ├── TwoLongsBaselineArithmetic.java
    │       │   ├── TwoLongsBaselineValue.java
    │       │   └── MutableTwoLongsBaselineValue.java
    │       └── highperf/            # Optimized (performance-first)
    │           ├── FastInt128Arithmetic.java      (~68 LOC)
    │           └── FastInt128Value.java          (~1647 LOC)
    ├── jmh/java/com/symguac/int128/bench/
    │   └── Int128ArithmeticBenchmark.java     # JMH benchmark suite
    └── test/java/
        ├── Int128.java              # Reference implementation (1002 LOC)
        └── Int128Test.java          # Comprehensive JUnit test suite (806 LOC)
```

**Key Files:**
- **FastInt128Value.java**: 1647 lines, production-quality high-performance implementation
- **Int128.java**: 1002 lines, standalone reference with division (⚠️ contains known bugs)
- **Int128Test.java**: 806 lines, comprehensive JUnit 5 test suite
- **REVIEW_REPORT.md**: Detailed code review documenting 3 critical bugs in Int128.java

---

## Architecture & Design Patterns

### Plugin-Based Architecture

The codebase uses a **plugin pattern** to enable comparing different Int128 implementations:

```
┌─────────────────────────────────────┐
│   Int128Arithmetic Interface        │  ← Plugin contract
│   (add, sub, mul, fromLong, etc.)   │
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
- Represents 128-bit value as two 64-bit limbs: `value = (high << 64) + (low as unsigned)`
- Provides default `toHexString()` method for debugging

**MutableInt128Value** (`api/MutableInt128Value.java`)
- Extends Int128Value with mutability
- Enables zero-allocation patterns in tight loops
- Provides `set(high, low)`, `immutableCopy()`, and `copy()` methods

**Int128Arithmetic** (`api/Int128Arithmetic.java`)
- Main contract for implementations
- Provides both mutable (`addInto`, `subtractInto`, `multiplyInto`) and immutable (`add`, `multiply`) variants
- Factory methods: `fromParts(high, low)`, `fromLong(value)`, `createMutable()`
- Default implementations of immutable methods delegate to mutable + copy

### Two-Limb Representation

All implementations represent 128-bit values as **two 64-bit signed longs**:
- `high`: upper 64 bits (signed, contains sign bit at position 127)
- `low`: lower 64 bits (treated as unsigned for composition)
- Value = `(high << 64) + (low & 0xFFFF_FFFF_FFFF_FFFFL)`

---

## Current Implementations

### 1. TwoLongs Baseline (`impl/twolongs/`)

**Philosophy:** Correctness and readability over performance

**Characteristics:**
- Uses `BigInteger` for multiplication (conservative approach)
- Straightforward, easy-to-verify logic
- Serves as reference for correctness testing
- ~200 LOC total across 3 files

**Files:**
- `TwoLongsBaselineArithmetic.java` (72 LOC) - Main arithmetic provider
- `TwoLongsBaselineValue.java` (77 LOC) - Immutable value with BigInteger interop
- `MutableTwoLongsBaselineValue.java` (47 LOC) - Mutable variant

**When to use as reference:**
- Verifying new implementations
- Understanding the API contract
- Teaching/explaining Int128 arithmetic

### 2. FastInt128 (`impl/highperf/`)

**Philosophy:** Maximum performance without sacrificing correctness

**Characteristics:**
- No BigInteger in arithmetic hot paths
- Custom 64×64→128 multiplication using 16-bit limb decomposition (schoolbook algorithm)
- Inline hot-path operations in FastInt128Arithmetic
- Factory method reuses singleton constants (ZERO, ONE, MAX_VALUE, MIN_VALUE)
- **1647 LOC** in FastInt128Value.java (extremely comprehensive)

**Files:**
- `FastInt128Arithmetic.java` (68 LOC) - Arithmetic provider with inlined operations
- `FastInt128Value.java` (1647 LOC) - Comprehensive immutable value class

**Optimizations:**
- Portable 16-bit limb split for multiplication (JIT-friendly on x86_64 and AArch64)
- Nested `MutableFastInt128Value` class for zero-allocation patterns
- Static `MathOps` utility class for operations on generic Int128Value interfaces
- Extensive helper methods (100+ methods total)

**Features in FastInt128Value:**
- Basic arithmetic: add, subtract, multiply, negate, abs, increment, decrement
- Comparisons: compareTo, equals, hashCode, unsigned comparisons
- Bitwise: and, or, xor, not, bit manipulation
- Shifts: left, right (arithmetic), rotate left/right
- Utilities: bit counting, zero detection, sign queries
- String conversion: toString (decimal), fromString, parseHex, toHexString
- Byte serialization: toByteArray, writeBigEndian
- Advanced helpers: clamp, average, absoluteDifference, multiplyAdd, etc.

**Note:** FastInt128Value does NOT implement division/remainder. For division, see Int128.java.

### 3. Int128 Reference (`test/Int128.java`)

**Status:** Standalone reference implementation with **KNOWN CRITICAL BUGS** (see REVIEW_REPORT.md)

**⚠️ WARNING:** This implementation has 3 critical bugs that make it unsuitable for production:
1. **Infinite loop in 128÷128 division** for certain inputs
2. **Incorrect divRemPow10 for negative numbers** (data corruption in financial operations)
3. **Incomplete 128÷128 division algorithm** (lacks proper convergence guarantees)

**Characteristics:**
- Comprehensive 1002-line standalone implementation
- Full arithmetic: add, sub, mul, div, rem
- Bitwise operations: and, or, xor, not, shifts
- String conversions: toString, fromString (decimal, radix)
- Hex support: parseHex, toHexString, toDebugHex
- Financial helpers: divRemPow10, divRoundHalfEvenPow10, floorDivPow10, ceilDivPow10
- Constants: ZERO, ONE, DECIMAL_BASE, MIN_VALUE, MAX_VALUE
- Implements Comparable<Int128> and Serializable
- BigInteger used ONLY for string conversions
- Includes `quickSelfCheck()` method for smoke testing

**Key features:**
- Fast 128÷64 division path (common in finance: 10^k for k≤19)
- Optimized 128÷128 division using two-limb approximation (⚠️ buggy)
- Decimal and hexadecimal parsing
- Byte serialization (big-endian)
- Self-contained, immutable, thread-safe design

**Use cases:**
- Study reference for division algorithms
- Cross-validation of arithmetic operations
- **DO NOT** use in production without fixing bugs
- See REVIEW_REPORT.md for detailed bug descriptions and test cases

---

## Testing Infrastructure

### JUnit Test Suite (`test/Int128Test.java`)

**Comprehensive test suite with 806 lines covering:**

**Test Categories:**
1. **Basic Construction** - Constants, valueOf, construction methods
2. **Arithmetic** - Add, subtract, multiply, negate, abs, increment/decrement
3. **Comparison** - compareTo, compareUnsigned, predicates, signum
4. **Bitwise** - AND, OR, XOR, NOT, bit manipulation
5. **Shifts** - Left, right (arithmetic/unsigned), rotations
6. **Division** - Signed division, remainder, divRem identity
7. **String Conversion** - toString, fromString, parseHex, radix support
8. **Edge Cases** - MIN_VALUE, MAX_VALUE, overflow, underflow
9. **Financial Operations** - divRemPow10, rounding modes
10. **Serialization** - Byte array conversion, ByteBuffer operations
11. **Performance Critical Paths** - Zero-allocation patterns

**Running Tests:**
```bash
# Run all tests via Maven
mvn clean test

# Run specific test class
mvn test -Dtest=Int128Test

# Run with verbose output
mvn test -Dtest=Int128Test -X
```

**Test Execution:**
- Tests are discovered automatically by Maven Surefire (matches `*Test.java` pattern)
- Comprehensive report printed to stdout
- Issues tracked and reported with detailed failure messages
- Cross-validates against BigInteger for arithmetic correctness

**Test Output Example:**
```
================================================================================
COMPREHENSIVE INT128 REVIEW AND TEST REPORT
================================================================================

[✓] Basic Construction & Constants (9/9 passed)
[✓] Arithmetic Operations (13/13 passed)
[✓] Comparison & Predicates (14/14 passed)
[✗] Division (2/3 failed - infinite loop, negative divRemPow10)
...
```

### Known Test Failures

See `REVIEW_REPORT.md` for detailed documentation of test failures:
- **128÷128 division infinite loop** (hangs indefinitely on specific inputs)
- **divRemPow10 incorrect for negative numbers** (critical financial bug)
- **divRoundHalfEvenPow10 wrong for negative values** (depends on divRemPow10)

---

## Development Workflow

### Building the Project

```bash
# Clean build with shaded JAR
mvn clean package

# Output: target/int128-0.1.0-SNAPSHOT-shaded.jar
```

### Running Tests

```bash
# Run all JUnit tests
mvn clean test

# Run tests with detailed output
mvn test -X

# Run specific test
mvn test -Dtest=Int128Test
```

### Running Benchmarks

```bash
# Method 1: Via Maven (requires separate JMH plugin config)
mvn clean install
# Note: mvn jmh:jmh requires maven-jmh-plugin (not currently in pom.xml)

# Method 2: Direct JAR execution (all benchmarks)
java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar

# Method 3: Specific benchmark
java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar Int128ArithmeticBenchmark.additionWithReuse

# Method 4: Specific implementation
java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar -p implId=fastLimb128

# Method 5: With profiling
java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar -prof gc  # GC profiling
java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar -prof stack  # Stack profiling
```

### Git Workflow

**Current Branch:** Check your session context for the active branch (format: `claude/*`)

**Commit Message Style:**
- Clear, concise descriptions
- Examples from history:
  - "Fix udivrem_128by128 quotient clamping and overflow handling"
  - "Configure Surefire 3.2.5 for JUnit 5 test discovery"
  - "Implement Int128 class for 128-bit integer operations"

**Push Command:**
```bash
# Always use -u for tracking
git push -u origin <branch-name>
```

**Git Safety:**
- Branch names start with `claude/` for this project
- Never force push without explicit permission
- Always run tests before committing (`mvn test`)

---

## Key Conventions & Standards

### Code Style

**Java Conventions:**
- Java 17 language features encouraged (records, pattern matching, text blocks)
- UTF-8 source encoding
- Package structure: `com.symguac.int128.{api|impl|bench}`
- Final classes for implementations (immutability)
- Use `@Override` annotations consistently

**Performance Conventions:**
- Minimize allocations in hot paths
- Prefer primitive types over boxed types
- Use `final` for immutable fields
- Inline small helper methods (JIT-friendly)
- Avoid branches in critical loops when possible
- Use `Long.compareUnsigned()` for unsigned comparisons

**Documentation:**
- JavaDoc on all public APIs
- Class-level design documentation
- Performance notes on critical sections
- Explain non-obvious optimizations
- Document complexity (time/space) when relevant

### Naming Conventions

**Implementations:**
- Pattern: `{Strategy}{Concept}{Type}`
- Examples: `TwoLongsBaselineArithmetic`, `FastInt128Value`

**Constants:**
- ALL_CAPS for static finals: `ZERO`, `ONE`, `MIN_VALUE`, `MAX_VALUE`, `DECIMAL_BASE`
- Implementation IDs: camelCase strings like `"twoLongsBaseline"`, `"fastLimb128"`

**Method naming:**
- Mutable operations: `{verb}Into` - e.g., `addInto`, `subtractInto`, `multiplyInto`
- Immutable operations: `{verb}` - e.g., `add`, `multiply`, `negate`
- Factories: `from{Source}` - e.g., `fromLong`, `fromParts`, `fromString`
- Queries: `is{Property}` - e.g., `isZero`, `isNegative`, `isPowerOfTwo`

### Testing Philosophy

**Current Approach:**
- **JUnit 5** test framework (see Int128Test.java)
- Comprehensive test suite with 100+ test cases
- Cross-validation against BigInteger for correctness
- Smoke tests in Int128.java (`quickSelfCheck()`)
- Benchmarks serve as integration tests for performance
- Correctness verified by comparing against baseline implementation

**What to Test:**
- Basic arithmetic (0+1=1, 1-1=0, identity operations)
- Boundary conditions (MIN_VALUE, MAX_VALUE, zero, one)
- Overflow/underflow behavior (wrapping semantics)
- String round-trip (toString/fromString consistency)
- Division identity (a = q*d + r)
- Sign extension (fromLong correctness)
- Unsigned vs signed operations
- Edge cases (division by zero, MIN_VALUE negation)

**Test Categories in Int128Test.java:**
1. Basic construction & constants
2. Arithmetic operations
3. Comparison & predicates
4. Bitwise operations
5. Shift operations
6. Division & remainder
7. String conversion
8. Edge cases
9. Financial operations
10. Serialization
11. Performance-critical paths

---

## Performance Considerations

### Critical Hot Paths

**Top Priority (Zero Allocation):**
1. Addition with reuse (`addInto`)
2. Subtraction with reuse (`subtractInto`)
3. Multiplication with reuse (`multiplyInto`)

**High Priority:**
4. Division by small divisors (≤64 bits)
5. Division by powers of 10 (10^k for k≤19)
6. Mutable arithmetic in tight loops

**Medium Priority:**
7. General 128÷128 division
8. String conversions (toString, fromString)
9. Byte serialization

### Optimization Techniques Used

**16-bit Limb Multiplication (FastInt128Value):**
```java
// Schoolbook multiplication using 8 limbs of 16 bits each
private static final int LIMB_BITS = 16;
private static final int LIMB_MASK = (1 << LIMB_BITS) - 1;
// Decomposes 128-bit value into 8×16-bit limbs
// Performs schoolbook multiplication: O(n²) limb multiplications
// Carries are propagated in second pass
```

**32-bit Split Multiplication (Int128.java):**
```java
// Portable 32-bit split (JIT optimizes well)
long a_lo = a & 0xFFFFFFFFL;
long a_hi = a >>> 32;
long b_lo = b & 0xFFFFFFFFL;
long b_hi = b >>> 32;
// Four partial products: (a_hi*b_hi)<<64 + (a_hi*b_lo + a_lo*b_hi)<<32 + a_lo*b_lo
```

**Fast 128÷64 Division:**
```java
// When divisor fits in 64 bits, use optimized path
if (divisor_high == 0 || divisor_high == -1) {
    // Specialized bit-by-bit or limb-by-limb division
    // Avoids full 128÷128 algorithm complexity
}
```

**Singleton Reuse:**
```java
// Avoid allocations for common constants
if (hi == 0 && lo == 0) return ZERO;
if (hi == 0 && lo == 1) return ONE;
if (hi == Long.MAX_VALUE && lo == -1L) return MAX_VALUE;
if (hi == Long.MIN_VALUE && lo == 0L) return MIN_VALUE;
```

### Performance Anti-Patterns to Avoid

**Never do this:**
- ❌ Use BigInteger in arithmetic hot paths (multiplication, division)
- ❌ Allocate new objects in tight loops (use mutable variants)
- ❌ Box primitives unnecessarily (Long.valueOf, etc.)
- ❌ Use exception handling for control flow
- ❌ Add unnecessary bounds checks (trust two's complement wrapping)
- ❌ Call toString() in performance-critical code
- ❌ Perform division when multiplication by inverse would work

**Always prefer:**
- ✅ Reuse mutable instances in benchmarks
- ✅ Inline small helper methods (let JIT decide)
- ✅ Use unsigned comparisons for low limb (`Long.compareUnsigned`)
- ✅ Leverage JIT optimizations (straight-line code, loop unrolling)
- ✅ Minimize branches in hot paths
- ✅ Use local variables instead of field access in loops

---

## Adding New Implementations

### Step-by-Step Guide

**1. Create implementation classes in `src/main/java/com/symguac/int128/impl/{yourname}/`:**

```java
package com.symguac.int128.impl.yourname;

import com.symguac.int128.api.*;

public class YourInt128Arithmetic implements Int128Arithmetic {
    @Override
    public String id() {
        return "yourImplementation";
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
    public void addInto(Int128Value left, Int128Value right, MutableInt128Value dest) {
        // Your optimized addition logic
        long leftLow = left.low();
        long rightLow = right.low();
        long low = leftLow + rightLow;
        long carry = (Long.compareUnsigned(low, leftLow) < 0) ? 1L : 0L;
        long high = left.high() + right.high() + carry;
        dest.set(high, low);
    }

    // ... implement other methods
}
```

**2. Register in `Int128BenchmarkRegistry.java`:**

```java
static {
    register(Int128ImplementationIds.TWO_LONGS_BASELINE, TwoLongsBaselineArithmetic::new);
    register(Int128ImplementationIds.FAST_LIMB_BASED, FastInt128Arithmetic::new);
    register("yourImplementation", YourInt128Arithmetic::new);  // Add this
}
```

**3. Add ID constant to `Int128ImplementationIds.java`:**

```java
public static final String YOUR_IMPLEMENTATION = "yourImplementation";
```

**4. Enable in benchmark suite (`Int128ArithmeticBenchmark.java`):**

```java
@Param({
    Int128ImplementationIds.TWO_LONGS_BASELINE,
    Int128ImplementationIds.FAST_LIMB_BASED,
    "yourImplementation"  // Add this
})
private String implementation;
```

**5. Rebuild and benchmark:**

```bash
mvn clean package
java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar
```

**6. Verify correctness:**

Create a test that compares your implementation against the baseline:
```java
@Test
public void testYourImplementation() {
    Int128Arithmetic baseline = new TwoLongsBaselineArithmetic();
    Int128Arithmetic yours = new YourInt128Arithmetic();

    // Test addition
    Int128Value a = baseline.fromLong(12345);
    Int128Value b = baseline.fromLong(67890);
    Int128Value expected = baseline.add(a, b);
    Int128Value actual = yours.add(yours.fromLong(12345), yours.fromLong(67890));

    assertEquals(expected.high(), actual.high());
    assertEquals(expected.low(), actual.low());
}
```

---

## Benchmarking Guide

### Available Benchmarks

The `Int128ArithmeticBenchmark` class provides 5 benchmarks:

| Benchmark | Description | Pattern | Measures |
|-----------|-------------|---------|----------|
| `additionWithReuse` | Addition reusing mutable instance | Zero-allocation | Throughput (ops/sec) |
| `subtractionWithReuse` | Subtraction reusing mutable instance | Zero-allocation | Throughput (ops/sec) |
| `multiplicationWithReuse` | Multiplication reusing mutable instance | Zero-allocation | Throughput (ops/sec) |
| `additionAllocating` | Addition creating new objects | Allocating | Throughput (ops/sec) |
| `multiplicationAllocating` | Multiplication creating new objects | Allocating | Throughput (ops/sec) |

### Benchmark Configuration

**JMH Settings (configured in annotations):**
- Mode: Throughput (operations/second)
- Warmup: 3 iterations × 1 second
- Measurement: 5 iterations × 1 second
- Forks: 1
- Time Unit: Seconds
- Dataset: 1024 random 128-bit values (cycling)
- RNG Seed: 0x9E3779B97F4A7C15L (deterministic)

**Benchmark State:**
- Thread-scoped state
- Parameters: `implementation` (impl ID), `datasetSize` (1024)
- Pre-allocated arrays: `left[]`, `right[]`
- Pre-allocated scratch space: `addScratch`, `multiplyScratch`
- Index cycles through dataset (modulo)

### Interpreting Results

**Good performance indicators:**
- Higher ops/sec for reuse benchmarks (indicates efficient zero-allocation paths)
- Small gap between reuse and allocating benchmarks (efficient object creation)
- Consistent results across iterations (low variance, stable JIT)
- Competitive with baseline for simple operations (add/sub within 10-20%)
- Significant speedup over baseline for complex operations (mul 2-5x faster)

**Red flags:**
- Large performance gap vs baseline for simple ops (regression in add/sub)
- High variance in results (>10% coefficient of variation)
- Much slower multiplication than baseline (algorithm issue)
- Significant GC activity in reuse benchmarks (unexpected allocations)
- Performance degrades with larger datasets (cache issues)

**Example output:**
```
Benchmark                                    (implementation)  Mode  Cnt         Score        Error  Units
Int128ArithmeticBenchmark.additionWithReuse  twoLongsBaseline  thrpt    5  45678901.234 ± 123456.789  ops/s
Int128ArithmeticBenchmark.additionWithReuse     fastLimb128    thrpt    5  56789012.345 ± 234567.890  ops/s
```

**Analysis:**
- FastLimb128 is ~24% faster than baseline for addition
- Low error margin indicates stable performance
- Reuse benchmarks should show higher throughput than allocating variants

### Profiling Benchmarks

```bash
# GC profiling (check for unexpected allocations)
java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar -prof gc

# Stack profiling (identify hot methods)
java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar -prof stack

# Assembly profiling (see generated machine code)
java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar -prof perfasm

# All profilers combined
java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar -prof gc:stack
```

---

## Common Tasks for AI Assistants

### Task 1: Analyzing Performance Issues

**When asked to investigate slow performance:**

1. **Check the implementation**
   - Read the relevant `*Arithmetic.java` file
   - Look for allocations in `addInto`, `subtractInto`, `multiplyInto`
   - Verify no BigInteger usage in hot paths
   - Check for unnecessary method calls or branches

2. **Run benchmarks**
   ```bash
   mvn clean package
   java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar {specific-benchmark}
   ```

3. **Compare against baseline**
   - Run same benchmark with `-p implementation=twoLongsBaseline`
   - Calculate performance ratio
   - Expect 10-50% improvement for add/sub, 2-5x for mul

4. **Profile if needed**
   - Add JMH profilers: `-prof gc` (GC), `-prof stack` (hotspots)
   - Check for unexpected allocations in reuse benchmarks
   - Identify bottleneck methods with stack profiler

5. **Check common issues**
   - Missing singleton reuse (ZERO, ONE constants)
   - Inefficient bit manipulation
   - Excessive branching
   - Cache misses from poor data locality

### Task 2: Adding Required Constants

**When asked to ensure constants exist:**

Check for these in the implementation:
```java
public static final Int128 ZERO = new Int128(0L, 0L);
public static final Int128 ONE = new Int128(0L, 1L);
public static final Int128 DECIMAL_BASE = Int128.valueOf(10);
public static final Int128 MIN_VALUE = new Int128(Long.MIN_VALUE, 0L);
public static final Int128 MAX_VALUE = new Int128(Long.MAX_VALUE, 0xFFFF_FFFF_FFFF_FFFFL);
```

**Verification:**
- MIN_VALUE: `high = Long.MIN_VALUE (0x8000_0000_0000_0000L)`, `low = 0`
- MAX_VALUE: `high = Long.MAX_VALUE (0x7FFF_FFFF_FFFF_FFFFL)`, `low = 0xFFFF_FFFF_FFFF_FFFFL`
- ZERO: `high = 0`, `low = 0`
- ONE: `high = 0`, `low = 1`
- DECIMAL_BASE: equals valueOf(10)

**Additional helpful constants:**
```java
// Powers of 10 for financial operations
private static final Int128[] TEN_POW;  // 10^0 through 10^38
private static final long[] TEN_POW_64;  // 10^0 through 10^19 (fits in long)
```

### Task 3: Implementing Missing Operations

**When asked to add division, modulo, or bitwise ops:**

1. **Check Int128.java reference**
   - Located at `src/test/java/Int128.java`
   - Contains implementations of division, remainder, bitwise operations
   - **WARNING:** Division has known bugs (see REVIEW_REPORT.md)
   - Use as algorithmic reference but verify correctness

2. **For division implementation:**
   - **DO NOT** copy buggy 128÷128 division from Int128.java
   - Implement 128÷64 fast path first (handles most financial cases)
   - For 128÷128, consider using BigInteger temporarily or design correct algorithm
   - Add convergence bounds/iteration limits to prevent infinite loops
   - Handle sign separately (work with magnitudes, restore sign)

3. **Maintain performance standards**
   - No BigInteger in division hot paths (except as fallback for correctness)
   - Implement fast 128÷64 path for divisors ≤ 64 bits
   - Add constants for powers of 10 (financial operations)
   - Consider lookup tables for small divisors

4. **Add to interface if needed**
   - Update `Int128Arithmetic.java` with new method signature
   - Implement in all existing implementations
   - Add benchmark if performance-critical
   - Add comprehensive tests in Int128Test.java

**Example division interface addition:**
```java
// In Int128Arithmetic.java
void divideInto(Int128Value dividend, Int128Value divisor,
                MutableInt128Value quotient, MutableInt128Value remainder);

default Int128Value divide(Int128Value dividend, Int128Value divisor) {
    MutableInt128Value quotient = createMutable();
    MutableInt128Value remainder = createMutable();
    divideInto(dividend, divisor, quotient, remainder);
    return quotient.immutableCopy();
}
```

### Task 4: Fixing Correctness Issues

**When arithmetic produces wrong results:**

1. **Identify the failing operation**
   - Run Int128Test.java: `mvn test -Dtest=Int128Test`
   - Check console output for failed tests
   - Look for failed assertions with expected vs actual values
   - Run quickSelfCheck() if available: `Int128.quickSelfCheck()`

2. **Create minimal reproduction case**
   ```java
   @Test
   public void testSpecificFailure() {
       Int128 a = Int128.of(0x1234_5678_9ABC_DEF0L, 0xFEDC_BA98_7654_3210L);
       Int128 b = Int128.of(0x0000_0000_0000_0001L, 0x0000_0000_0000_0001L);
       Int128 result = a.add(b);
       // Print actual values
       System.out.println("Result: " + result.toHexString());
       // Assert expected
       assertEquals(0x1234_5678_9ABC_DEF1L, result.hi());
       assertEquals(0xFEDC_BA98_7654_3211L, result.lo());
   }
   ```

3. **Compare against baseline**
   ```java
   // In test
   TwoLongsBaselineArithmetic baseline = new TwoLongsBaselineArithmetic();
   YourImplementation yours = new YourImplementation();

   Int128Value baseResult = baseline.add(a, b);
   Int128Value yourResult = yours.add(a, b);

   // Compare high() and low()
   assertEquals(baseResult.high(), yourResult.high(),
                "High word mismatch for " + a.toHexString() + " + " + b.toHexString());
   assertEquals(baseResult.low(), yourResult.low(),
                "Low word mismatch for " + a.toHexString() + " + " + b.toHexString());
   ```

4. **Check two's complement semantics**
   - Overflow should wrap (not throw)
   - Sign extension must be correct in fromLong
   - Low limb is always treated as unsigned in composition
   - Carry/borrow must propagate correctly between limbs

5. **Verify edge cases**
   ```java
   // Wrapping behavior
   assertEquals(MAX_VALUE, MIN_VALUE.add(Int128.of(-1)));  // Wraps to MAX
   assertEquals(MIN_VALUE, MAX_VALUE.add(Int128.ONE));     // Wraps to MIN
   assertEquals(MIN_VALUE, Int128.ZERO.sub(MIN_VALUE));    // -(-2^127) = -2^127 (wraps)

   // Sign extension
   assertEquals(0xFFFF_FFFF_FFFF_FFFFL, Int128.valueOf(-1L).hi());  // -1L sign-extends
   assertEquals(0x0000_0000_0000_0000L, Int128.valueOf(1L).hi());   // 1L zero-extends
   ```

6. **Cross-validate with BigInteger**
   ```java
   BigInteger a = new BigInteger("170141183460469231731687303715884105727");  // MAX_VALUE
   BigInteger b = BigInteger.ONE;
   BigInteger expected = a.add(b).and(MASK_128);  // Wrap to 128 bits

   Int128 result = Int128.MAX_VALUE.add(Int128.ONE);
   assertEquals(expected, result.toBigInteger());
   ```

### Task 5: Updating Documentation

**When asked to document changes:**

1. **Update CLAUDE.md** (this file)
   - Add new implementations to "Current Implementations"
   - Update file line counts if changed significantly
   - Document new benchmarks added
   - Update benchmark results if available
   - Document new conventions or patterns
   - Update version history at bottom

2. **Update Readme**
   - User-facing documentation
   - Build instructions (if changed)
   - Benchmark examples
   - Quick start guide
   - Keep it concise (defer details to CLAUDE.md)

3. **JavaDoc**
   - All public methods need JavaDoc
   - Explain performance characteristics (O-notation)
   - Document preconditions/postconditions
   - Provide code examples for complex methods
   - Document thread-safety guarantees
   - Note any known limitations or bugs

4. **REVIEW_REPORT.md**
   - Update if bugs are fixed
   - Add new findings from code reviews
   - Document test results
   - Track issue resolution

**JavaDoc Example:**
```java
/**
 * Divides this value by the divisor and returns quotient and remainder.
 *
 * <p>Computes {@code q} and {@code r} such that {@code this = q * divisor + r}
 * where {@code 0 <= |r| < |divisor|} (Euclidean division).
 *
 * <p><b>Performance:</b> O(1) for 128÷64 division (divisor fits in 64 bits),
 * O(log n) for general 128÷128 division using binary search algorithm.
 *
 * <p><b>Thread-safety:</b> This method is thread-safe and allocation-free.
 *
 * @param divisor the value to divide by (must not be zero)
 * @return array of [quotient, remainder]
 * @throws ArithmeticException if divisor is zero
 *
 * @see #divide(Int128)
 * @see #remainder(Int128)
 */
public Int128[] divRem(Int128 divisor) {
    // Implementation...
}
```

### Task 6: Understanding Existing Code

**When asked to explain or review code:**

1. **Start with architecture**
   - Identify which implementation (baseline, highperf, test)
   - Understand the plugin pattern and interfaces
   - Map out data flow and dependencies

2. **Analyze algorithms**
   - Identify the mathematical algorithm used
   - Check for edge case handling
   - Verify correctness properties (invariants, postconditions)
   - Look for potential overflow or underflow

3. **Review performance**
   - Count allocations in hot paths
   - Identify branching patterns
   - Check for opportunities to inline or optimize
   - Verify zero-allocation claims

4. **Check test coverage**
   - Run `mvn test` and review results
   - Identify untested edge cases
   - Cross-reference with Int128Test.java test categories
   - Check REVIEW_REPORT.md for known issues

5. **Document findings**
   - Summarize architecture and design decisions
   - List strengths and weaknesses
   - Identify bugs or potential improvements
   - Provide specific code examples

**Review Checklist:**
- [ ] Correctness: Does it produce correct results?
- [ ] Edge cases: MIN_VALUE, MAX_VALUE, zero, overflow
- [ ] Performance: Allocations, complexity, hot paths
- [ ] Thread-safety: Immutability, no shared mutable state
- [ ] Documentation: JavaDoc, comments, examples
- [ ] Tests: Coverage, edge cases, cross-validation
- [ ] Code style: Consistent, readable, maintainable

---

## File Locations Quick Reference

| Need to... | Check these files |
|------------|-------------------|
| Understand the API | `src/main/java/com/symguac/int128/api/*.java` |
| See baseline implementation | `src/main/java/com/symguac/int128/impl/twolongs/*.java` |
| See optimized implementation | `src/main/java/com/symguac/int128/impl/highperf/*.java` |
| Reference standalone implementation | `src/test/java/Int128.java` ⚠️ (has bugs) |
| Run comprehensive tests | `src/test/java/Int128Test.java` + `mvn test` |
| Review known bugs | `REVIEW_REPORT.md` |
| Add new implementation | `src/main/java/com/symguac/int128/impl/{newname}/` |
| Register implementation | `src/main/java/com/symguac/int128/bench/Int128BenchmarkRegistry.java` |
| Add implementation ID | `src/main/java/com/symguac/int128/bench/Int128ImplementationIds.java` |
| Configure benchmarks | `src/jmh/java/com/symguac/int128/bench/Int128ArithmeticBenchmark.java` |
| Understand build | `pom.xml` |
| Read user docs | `Readme` |
| Read AI assistant guide | `CLAUDE.md` (this file) |

---

## Important Reminders

### For Performance Work

- **Always benchmark before claiming improvements** (use JMH, not microbenchmarks)
- **Compare against baseline for correctness** (TwoLongsBaseline is reference)
- **Profile before optimizing** (don't guess what's slow)
- **Document why an optimization works** (help future maintainers)
- **Verify zero-allocation claims** (use `-prof gc`)
- **Test on representative data** (financial workloads, not sequential numbers)

### For Correctness Work

- **Two's complement semantics everywhere** (value = (hi << 64) + (lo unsigned))
- **Overflow wraps, doesn't throw** (modulo 2^128 arithmetic)
- **Low limb is unsigned in value composition** (use Long.compareUnsigned)
- **Sign bit is bit 127** (high bit of high limb)
- **Carry/borrow propagates correctly** (check Long.compareUnsigned for carry detection)
- **Division identity: a = q*d + r** (verify in tests)
- **Cross-validate with BigInteger** (ultimate source of truth)

### For New Implementations

- **Start from FastInt128Value or Int128.java as reference** (don't reinvent)
- **Implement complete API** (no partial implementations in production)
- **Register in benchmark registry** (enable comparison)
- **Test edge cases** (MIN_VALUE, MAX_VALUE, zero, overflow)
- **Document performance characteristics** (O-notation, allocation behavior)
- **Add to Int128Test.java** (comprehensive test coverage)
- **Verify against baseline** (correctness before performance)

### For Code Changes

- **Never commit without clear description** (future you will thank you)
- **Run tests before committing** (`mvn test` must pass)
- **Push to the correct branch** (starts with `claude/` for this project)
- **Maintain existing code style** (follow Java conventions)
- **Update this file when conventions change** (keep docs in sync)
- **Update REVIEW_REPORT.md if fixing bugs** (track issue resolution)

### Critical Warnings

⚠️ **Int128.java has KNOWN BUGS:**
1. **Infinite loop in 128÷128 division** - can hang indefinitely
2. **Incorrect divRemPow10 for negative numbers** - data corruption risk
3. **DO NOT use in production** without fixing (see REVIEW_REPORT.md)

⚠️ **FastInt128Value missing division:**
- No division or remainder operations implemented
- Use Int128.java as algorithm reference (but fix bugs first)
- Consider BigInteger fallback for correctness until division is fixed

⚠️ **Benchmark implementations only provide add/sub/mul:**
- Current Int128Arithmetic interface is limited
- Division requires extending the interface
- Or use standalone Int128.java (with bug fixes)

---

## Version History

- **2025-11-13 (Latest Update)**: Comprehensive review and documentation update
  - ✅ Fixed all references from Fast128.java → Int128.java
  - ✅ Added comprehensive testing section (Int128Test.java, 806 LOC)
  - ✅ Documented REVIEW_REPORT.md and 3 critical bugs
  - ✅ Added JUnit 5 testing infrastructure details
  - ✅ Clarified relationship between implementations
  - ✅ Updated file line counts (FastInt128Value: 1647 LOC, Int128: 1002 LOC)
  - ✅ Added warnings about known bugs in Int128.java
  - ✅ Expanded Common Tasks section with detailed examples
  - ✅ Added profiling and debugging guidance
  - ✅ Improved code examples throughout
  - ⚠️ Removed incorrect git branch reference (session-specific)

- **2025-11-13 (Initial)**: Initial CLAUDE.md created based on codebase state
  - Initial Fast128.java merged to src/test/java/ (later renamed to Int128.java)
  - Two implementations registered: twoLongsBaseline, fastLimb128
  - JMH benchmark suite with 5 benchmarks
  - Java 17, Maven 3.x, JMH 1.36

---

## Questions & Support

### For AI Assistants

**Primary References:**
- **Int128.java** (`test/Int128.java`) - Comprehensive standalone implementation (⚠️ has bugs)
- **FastInt128Value.java** (`impl/highperf/FastInt128Value.java`) - Production-quality high-performance implementation (no division)
- **Int128Test.java** (`test/Int128Test.java`) - Comprehensive test suite
- **REVIEW_REPORT.md** - Known bugs and code review findings

**Best Practices:**
- Check Int128Test.java first to understand expected behavior
- Use TwoLongsBaseline for correctness verification
- Use FastInt128Value as performance reference
- Consult REVIEW_REPORT.md before using Int128.java division code
- Prioritize performance measurements over theoretical optimization
- Always maintain correctness - performance improvements must not break arithmetic
- Document non-obvious design decisions
- Cross-validate with BigInteger when in doubt

**Common Pitfalls:**
- Don't copy buggy division code from Int128.java without fixes
- Don't forget to handle carry/borrow in multi-limb arithmetic
- Don't use signed comparison for low limb (use Long.compareUnsigned)
- Don't allocate in hot paths (use mutable variants)
- Don't forget to test edge cases (MIN_VALUE, MAX_VALUE, zero)

### For Humans

**Getting Started:**
- See `Readme` for user-facing documentation
- Run `mvn test` to verify setup
- Run `mvn package` to build benchmarks
- Check `REVIEW_REPORT.md` for known issues

**Development:**
- Use `mvn test` for unit testing
- Use `java -jar target/*.jar` for benchmarking
- Check git history for implementation evolution
- Run benchmarks to validate performance claims

**Contributing:**
- Follow Java 17 conventions
- Add tests to Int128Test.java
- Register new implementations in Int128BenchmarkRegistry
- Update this file (CLAUDE.md) with significant changes
- Run `mvn test` before committing

**Support:**
- File issues on GitHub (check project README for link)
- Consult REVIEW_REPORT.md for known bugs
- Review git commit history for context
- Check benchmark results for performance baselines

---

**End of CLAUDE.md**
