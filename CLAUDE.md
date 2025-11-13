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
- Performance prioritized over code simplicity
- Correctness is non-negotiable

---

## Codebase Structure

```
symmetrical-guacamole/
├── pom.xml                          # Maven configuration (Java 17, JMH dependencies)
├── Readme                           # User-facing documentation
├── CLAUDE.md                        # This file - AI assistant guide
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
    │           ├── FastInt128Arithmetic.java
    │           └── FastInt128Value.java
    ├── jmh/java/com/symguac/int128/bench/
    │   └── Int128ArithmeticBenchmark.java     # JMH benchmark suite
    └── test/java/
        ├── Int128.java              # Reference implementation (1200+ LOC, formerly Fast128.java)
        ├── Int128Test.java          # JUnit 5 test suite
        ├── Int128DivisionTest.java  # Division-specific tests
        ├── DebugDivisionTest.java   # Debug utilities for division
        └── SimpleDivTest.java       # Simple division test cases
```

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

**MutableInt128Value** (`api/MutableInt128Value.java`)
- Extends Int128Value with mutability
- Enables zero-allocation patterns in tight loops
- Provides `set(high, low)` and `immutableCopy()` methods

**Int128Arithmetic** (`api/Int128Arithmetic.java`)
- Main contract for implementations
- Provides both mutable (`addInto`, `subtractInto`, `multiplyInto`) and immutable (`add`, `multiply`) variants
- Factory methods: `fromParts(high, low)`, `fromLong(value)`, `createMutable()`

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

**When to use as reference:**
- Verifying new implementations
- Understanding the API contract
- Teaching/explaining Int128 arithmetic

### 2. FastInt128 (`impl/highperf/`)

**Philosophy:** Maximum performance without sacrificing correctness

**Characteristics:**
- No BigInteger in arithmetic hot paths
- Custom 64×64→128 multiplication using 32-bit limb decomposition
- Inline hot-path operations
- Factory method reuses singleton constants (ZERO, ONE, etc.)
- ~1000+ LOC in FastInt128Value.java

**Optimizations:**
- Portable 32-bit split for multiplication (JIT-friendly on x86_64 and AArch64)
- 128÷64 fast path for common financial operations (division by powers of 10)
- Two-limb division approximation for 128÷128 (avoids bit-by-bit iteration)
- Zero-allocation arithmetic in mutable mode

### 3. Int128 Reference (`test/Int128.java`)

**Status:** Production-ready standalone implementation (formerly Fast128.java, renamed in PR #6)

**Characteristics:**
- Comprehensive 1200+ line implementation
- Full arithmetic: add, sub, mul, div, rem
- Bitwise operations: and, or, xor, shifts
- String conversions: toString, fromString (decimal and hex)
- Financial helpers: division by powers of 10, rounding modes
- Constants: ZERO, ONE, DECIMAL_BASE, MIN_VALUE, MAX_VALUE
- Implements Comparable<Int128> and Serializable
- BigInteger used ONLY for string conversions
- **Critical bugs fixed:** Division infinite loop (PR #10, #13), signed division issues (PR #8)

**Key features:**
- Fast 128÷64 division path (common in finance)
- Optimized 128÷128 division (Knuth algorithm with quotient clamping)
- Decimal and hexadecimal parsing with full negative number support
- Self-contained, immutable, thread-safe
- Comprehensive JUnit 5 test suite (100+ tests)

**Recent fixes:**
- Fixed infinite loop in `udivrem_128by128` (added iteration bounds and overflow handling)
- Fixed `divRemPow10` for negative numbers (proper signed division)
- Corrected quotient clamping in 128/128 division
- Added comprehensive test coverage for edge cases

---

## Development Workflow

### Building the Project

```bash
# Clean build with shaded JAR
mvn clean package

# Output: target/int128-0.1.0-SNAPSHOT-shaded.jar
```

### Running Benchmarks

```bash
# Method 1: Via Maven
mvn clean install
mvn jmh:jmh

# Method 2: Direct JAR execution (all benchmarks)
java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar

# Method 3: Specific benchmark
java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar Int128ArithmeticBenchmark.additionWithReuse

# Method 4: Specific implementation
java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar -p implId=fastLimb128
```

### Running Tests

```bash
# Run all JUnit tests
mvn test

# Run specific test class
mvn test -Dtest=Int128Test

# Run specific test method
mvn test -Dtest=Int128Test#testAddition
```

### Git Workflow

**Current Branch:** `claude/update-documentation-01FTgkFeSer2BnvV2N2ReJyt`

**Commit Message Style:**
- Clear, concise descriptions
- Examples from history:
  - "Fix critical Int128 division bugs in Knuth algorithm and 128/64 division"
  - "Fix file naming issue: rename Fast128.java to Int128.java"
  - "Add comprehensive review and test suite for Fast128.java"
  - "Move Fast128.java to test directory for rigorous testing"
  - "Implement Int128 class for 128-bit integer operations"

**Push Command:**
```bash
git push -u origin <branch-name>
# Note: Branch names must start with 'claude/' for CI/CD permissions
```

---

## Key Conventions & Standards

### Code Style

**Java Conventions:**
- Java 17 language features encouraged
- UTF-8 source encoding
- Package structure: `com.symguac.int128.{api|impl|bench}`
- Final classes for implementations (immutability)

**Performance Conventions:**
- Minimize allocations in hot paths
- Prefer primitive types over boxed types
- Use `final` for immutable fields
- Inline small helper methods (JIT-friendly)
- Avoid branches in critical loops when possible

**Documentation:**
- JavaDoc on all public APIs
- Class-level design documentation
- Performance notes on critical sections
- Explain non-obvious optimizations

### Naming Conventions

**Implementations:**
- Pattern: `{Strategy}{Concept}{Type}`
- Examples: `TwoLongsBaselineArithmetic`, `FastInt128Value`

**Constants:**
- ALL_CAPS for static finals: `ZERO`, `ONE`, `MIN_VALUE`, `MAX_VALUE`, `DECIMAL_BASE`
- Implementation IDs: camelCase strings like `"twoLongsBaseline"`, `"fastLimb128"`

**Method naming:**
- Mutable operations: `{verb}Into` - e.g., `addInto`, `subtractInto`
- Immutable operations: `{verb}` - e.g., `add`, `multiply`
- Factories: `from{Source}` - e.g., `fromLong`, `fromParts`

### Testing Philosophy

**Current approach:**
- **JUnit 5** test framework with comprehensive test suite
- 100+ unit tests covering all operations
- Property-based verification with BigInteger cross-validation
- Benchmarks serve as integration and performance tests
- Correctness verified by comparing against baseline and BigInteger

**Test organization:**
- `Int128Test.java`: Core arithmetic, comparisons, bitwise, shifts, string conversion
- `Int128DivisionTest.java`: Comprehensive division and remainder tests
- `SimpleDivTest.java`: Basic division smoke tests
- `DebugDivisionTest.java`: Debug utilities for investigating division edge cases

**What is tested:**
- Basic arithmetic (add, sub, mul, inc, dec, negate, abs)
- Boundary conditions (MIN_VALUE, MAX_VALUE, ZERO, ONE)
- Overflow/underflow behavior (wrapping semantics)
- String round-trip (toString/fromString for decimal and hex)
- Division identity (a = q*d + r)
- Division edge cases (negative numbers, powers of 10, 128/64 and 128/128 paths)
- Comparison operations (signed and unsigned)
- Bitwise operations (and, or, xor, not, shifts)
- Serialization (byte arrays, ByteBuffer)

---

## Performance Considerations

### Critical Hot Paths

**Top Priority (Zero Allocation):**
1. Addition with reuse
2. Subtraction with reuse
3. Multiplication with reuse

**High Priority:**
4. Division by small divisors (≤64 bits)
5. Division by powers of 10 (10^k for k≤19)

**Medium Priority:**
6. General 128÷128 division
7. String conversions (toString, fromString)

### Optimization Techniques Used

**64×64→128 Multiplication:**
```java
// Portable 32-bit split (JIT optimizes well)
long a_lo = a & 0xFFFFFFFFL;
long a_hi = a >>> 32;
long b_lo = b & 0xFFFFFFFFL;
long b_hi = b >>> 32;
// Four partial products...
```

**Fast 128÷64 Division:**
```java
// When divisor fits in 64 bits, use optimized path
if (divisor_high == 0 || divisor_high == -1) {
    // Specialized division algorithm
}
```

**Singleton Reuse:**
```java
// Avoid allocations for common constants
if (hi == 0 && lo == 0) return ZERO;
if (hi == 0 && lo == 1) return ONE;
```

### Performance Anti-Patterns to Avoid

**Never do this:**
- ❌ Use BigInteger in arithmetic hot paths
- ❌ Allocate new objects in tight loops (use mutable variants)
- ❌ Box primitives unnecessarily
- ❌ Use exception handling for control flow
- ❌ Add unnecessary bounds checks (trust two's complement wrapping)

**Always prefer:**
- ✅ Reuse mutable instances in benchmarks
- ✅ Inline small helper methods
- ✅ Use unsigned comparisons for low limb
- ✅ Leverage JIT optimizations (straight-line code)

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
private String implId;
```

**5. Rebuild and benchmark:**

```bash
mvn clean package
java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar
```

---

## Benchmarking Guide

### Available Benchmarks

The `Int128ArithmeticBenchmark` class provides 5 benchmarks:

| Benchmark | Description | Pattern |
|-----------|-------------|---------|
| `additionWithReuse` | Addition reusing mutable instance | Zero-allocation |
| `subtractionWithReuse` | Subtraction reusing mutable instance | Zero-allocation |
| `multiplicationWithReuse` | Multiplication reusing mutable instance | Zero-allocation |
| `additionAllocating` | Addition creating new objects | Measures allocation cost |
| `multiplicationAllocating` | Multiplication creating new objects | Measures allocation cost |

### Benchmark Configuration

**JMH Settings:**
- Mode: Throughput (operations/second)
- Warmup: 3 iterations × 1 second
- Measurement: 5 iterations × 1 second
- Forks: 1
- Dataset: 1024 random 128-bit values

### Interpreting Results

**Good performance indicators:**
- Higher ops/sec for reuse benchmarks
- Small gap between reuse and allocating benchmarks
- Consistent results across iterations (low variance)
- Competitive with baseline for simple operations

**Red flags:**
- Large performance gap vs baseline for simple ops (add/sub)
- High variance in results
- Much slower multiplication than baseline
- Significant GC activity in reuse benchmarks

---

## Common Tasks for AI Assistants

### Task 1: Analyzing Performance Issues

**When asked to investigate slow performance:**

1. **Check the implementation**
   - Read the relevant `*Arithmetic.java` file
   - Look for allocations in `addInto`, `subtractInto`, `multiplyInto`
   - Verify no BigInteger usage in hot paths

2. **Run benchmarks**
   ```bash
   mvn clean package
   java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar {specific-benchmark}
   ```

3. **Compare against baseline**
   - Run same benchmark with `-p implId=twoLongsBaseline`
   - Calculate performance ratio

4. **Profile if needed**
   - Add JMH profilers: `-prof gc` (GC), `-prof stack` (hotspots)

### Task 2: Adding Required Constants

**When asked to ensure constants exist:**

Check for these in the implementation:
```java
public static final Int128 ZERO = ...;
public static final Int128 ONE = ...;
public static final Int128 DECIMAL_BASE = valueOf(10);
public static final Int128 MIN_VALUE = ...;  // -2^127
public static final Int128 MAX_VALUE = ...;  // 2^127 - 1
```

**Verification:**
- MIN_VALUE: `high = Long.MIN_VALUE (0x8000_0000_0000_0000L)`, `low = 0`
- MAX_VALUE: `high = Long.MAX_VALUE (0x7FFF_FFFF_FFFF_FFFFL)`, `low = 0xFFFF_FFFF_FFFF_FFFFL`

### Task 3: Implementing Missing Operations

**When asked to add division, modulo, or bitwise ops:**

1. **Check Int128.java reference**
   - Located at `src/test/java/Int128.java`
   - Contains complete, production-ready implementations of all operations
   - Copy the algorithm, adapt to the target implementation
   - Note: Critical bugs have been fixed (division, sign handling)

2. **Maintain performance standards**
   - No BigInteger in division (use Fast128's algorithm)
   - Implement fast 128÷64 path for common cases
   - Add constants for powers of 10

3. **Add to interface if needed**
   - Update `Int128Arithmetic.java` with new method signature
   - Implement in all existing implementations
   - Add benchmark if performance-critical

### Task 4: Fixing Correctness Issues

**When arithmetic produces wrong results:**

1. **Identify the failing operation**
   - Run quickSelfCheck() if available
   - Create minimal reproduction case

2. **Compare against baseline**
   ```java
   // In test
   TwoLongsBaselineArithmetic baseline = new TwoLongsBaselineArithmetic();
   YourImplementation yours = new YourImplementation();

   Int128Value baseResult = baseline.add(a, b);
   Int128Value yourResult = yours.add(a, b);
   // Compare high() and low()
   ```

3. **Check two's complement semantics**
   - Overflow should wrap (not throw)
   - Sign extension must be correct in fromLong
   - Low limb is always treated as unsigned in composition

4. **Verify edge cases**
   - MIN_VALUE + (-1) should wrap to MAX_VALUE
   - MAX_VALUE + 1 should wrap to MIN_VALUE
   - 0 - MIN_VALUE should equal MIN_VALUE (wrapping)

### Task 5: Updating Documentation

**When asked to document changes:**

1. **Update CLAUDE.md** (this file)
   - Add new implementations to "Current Implementations"
   - Update benchmark results if available
   - Document new conventions

2. **Update Readme**
   - User-facing documentation
   - Build instructions
   - Benchmark examples

3. **JavaDoc**
   - All public methods need JavaDoc
   - Explain performance characteristics
   - Document preconditions/postconditions

### Task 6: Creating Comprehensive Int128 Implementation

**When asked to create a new 1000+ line implementation:**

**Use Int128.java as the template** (`src/test/java/Int128.java`):

1. **Required components:**
   - Constants: ZERO, ONE, DECIMAL_BASE, MIN_VALUE, MAX_VALUE
   - Construction: `fromLong`, `fromParts`, value constructors
   - Arithmetic: add, subtract, multiply, divide, remainder
   - Comparisons: equals, hashCode, compareTo
   - String conversion: toString, fromString (decimal)
   - Hex support: toHexString, fromHexString
   - Bitwise: and, or, xor, not, shiftLeft, shiftRight

2. **Performance requirements:**
   - No BigInteger except in toString/fromString
   - Implement 64×64→128 multiplication manually
   - Fast 128÷64 division path
   - Optimized 128÷128 division (no bit-by-bit)
   - Factory methods reuse constants

3. **Financial helpers (recommended):**
   - Division by powers of 10
   - Rounding modes
   - Scale conversion

4. **Implementation checklist:**
   - [ ] Two-limb representation (high, low)
   - [ ] Immutable and final class
   - [ ] All required constants
   - [ ] toString/fromString working
   - [ ] equals/hashCode correct
   - [ ] Division without BigInteger
   - [ ] Quick self-check passes
   - [ ] At least 1000 lines
   - [ ] Comparable<Int128> implemented
   - [ ] Serializable support

---

## File Locations Quick Reference

| Need to... | Check these files |
|------------|-------------------|
| Understand the API | `src/main/java/com/symguac/int128/api/*.java` |
| See baseline implementation | `src/main/java/com/symguac/int128/impl/twolongs/*.java` |
| See optimized implementation | `src/main/java/com/symguac/int128/impl/highperf/*.java` |
| Reference complete implementation | `src/test/java/Int128.java` (formerly Fast128.java) |
| Add new implementation | `src/main/java/com/symguac/int128/impl/{newname}/` |
| Register implementation | `src/main/java/com/symguac/int128/bench/Int128BenchmarkRegistry.java` |
| Configure benchmarks | `src/jmh/java/com/symguac/int128/bench/Int128ArithmeticBenchmark.java` |
| Run/write tests | `src/test/java/Int128Test.java`, `Int128DivisionTest.java` |
| Understand build | `pom.xml` |
| Read user docs | `Readme` |
| Read AI assistant guide | `CLAUDE.md` (this file) |
| Check bug fixes/review | `REVIEW_REPORT.md` |

---

## Important Reminders

### For Performance Work

- **Always benchmark before claiming improvements**
- **Compare against baseline for correctness**
- **Profile before optimizing** (don't guess)
- **Document why an optimization works**

### For Correctness Work

- **Two's complement semantics everywhere**
- **Overflow wraps, doesn't throw**
- **Low limb is unsigned in value composition**
- **Sign bit is bit 127 (high bit of high limb)**

### For New Implementations

- **Start from Int128.java reference** (`src/test/java/Int128.java`)
- **Implement complete API (no partial implementations)**
- **Register in benchmark registry**
- **Add comprehensive tests** (follow Int128Test.java pattern)
- **Test edge cases (MIN_VALUE, MAX_VALUE, zero, negative numbers)**
- **Document performance characteristics**
- **Verify division correctness** (many edge cases, see REVIEW_REPORT.md)

### For Code Changes

- **Never commit without clear description**
- **Push to the correct branch** (starts with `claude/`)
- **Maintain existing code style**
- **Update this file when conventions change**

---

## Version History

- **2025-11-13 (Latest)**: Major documentation update
  - Updated CLAUDE.md to reflect current state after bug fixes
  - All references to Fast128.java updated to Int128.java
  - Documented critical bug fixes (division infinite loop, signed division)
  - Added JUnit 5 test suite documentation
  - Updated git workflow and testing sections
  - Added REVIEW_REPORT.md to quick reference

- **2025-11-13 (PR #13)**: Critical division bug fixes
  - Fixed Knuth algorithm implementation in 128/128 division
  - Fixed 128/64 division edge cases
  - Added debug utilities for division testing

- **2025-11-13 (PR #10)**: Fixed quotient clamping and overflow handling
  - Corrected quotient clamping in `udivrem_128by128`
  - Added iteration bounds to prevent infinite loops
  - Improved overflow detection

- **2025-11-13 (PR #9)**: Testing infrastructure improvements
  - Configured Maven Surefire 3.2.5 for JUnit 5
  - Enabled automatic test discovery

- **2025-11-13 (PR #8)**: Division and sign handling fixes
  - Fixed `divRemPow10` for negative numbers
  - Corrected signed division throughout

- **2025-11-13 (PR #7)**: Test suite reorganization
  - Renamed Int128Tester to Int128Test
  - Added proper JUnit 5 support

- **2025-11-13 (PR #6)**: File naming correction
  - Renamed Fast128.java to Int128.java (matches public class name)
  - Required for proper Java compilation

- **2025-11-13 (PR #5)**: Initial CLAUDE.md created
  - Fast128.java recently merged to src/test/java/
  - Two implementations registered: twoLongsBaseline, fastLimb128
  - JMH benchmark suite with 5 benchmarks
  - Java 17, Maven 3.x, JMH 1.36

---

## Questions & Support

**For AI Assistants:**
- When in doubt, check Int128.java for reference implementation (`src/test/java/Int128.java`)
- Prioritize performance measurements over theoretical optimization
- Always maintain correctness - performance improvements must not break arithmetic
- Document non-obvious design decisions
- Review REVIEW_REPORT.md for known issues and fixes

**For Humans:**
- See `Readme` for user-facing documentation
- Check `REVIEW_REPORT.md` for comprehensive bug analysis and test results
- Check git history for implementation evolution
- Run benchmarks to validate performance claims
- Run tests with `mvn test` to verify correctness
