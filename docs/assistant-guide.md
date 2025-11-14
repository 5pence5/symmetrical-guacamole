# Assistant Guide

This guide helps AI assistants effectively contribute to the Int128 library. It provides context-specific guidance complementing the user-facing [README](../README.md).

---

## Repository Purpose

This is a **production-ready 128-bit signed integer library for Java** with a pluggable architecture. The library provides:

- **Stable public API** for 128-bit arithmetic operations
- **Multiple implementations** (baseline for correctness, optimized for performance)
- **JMH benchmark harness** for comparing implementations (optional)
- **Reference implementation** in tests for verification

**Key characteristics:**
- Two's-complement arithmetic (wraps on overflow)
- Zero-allocation patterns available via mutable operations
- Java 17+, built with Maven

---

## Working with this Codebase

### Architecture Overview

The codebase uses a **plugin pattern** with clear separation:

```
┌─────────────────────────┐
│ Int128Arithmetic API    │  ← Stable interface (src/main/java/*/api/)
└─────────────────────────┘
           ▲
           │ implements
    ┌──────┴──────┐
    │             │
┌───────────┐ ┌──────────┐
│ Baseline  │ │ Fast     │  ← Implementations (src/main/java/*/impl/)
└───────────┘ └──────────┘
```

**Key interfaces:**
- `Int128Arithmetic`: Main contract for implementations
- `Int128Value`: Read-only 128-bit value (two 64-bit limbs)
- `MutableInt128Value`: Mutable variant for zero-allocation patterns

**Representation:** All implementations use two 64-bit signed longs:
- `high`: upper 64 bits (contains sign bit)
- `low`: lower 64 bits (treated as unsigned for composition)
- Value = `(high << 64) + (low & 0xFFFF_FFFF_FFFF_FFFFL)`

### Current Implementations

| Implementation | Location | Philosophy | Division Support |
|----------------|----------|------------|------------------|
| **Baseline** | `impl/twolongs/` | Correctness-first; uses BigInteger for multiply/divide | Full (128÷128) |
| **Fast** | `impl/highperf/` | Performance-first; allocation-free hot paths | 128÷64 only* |
| **Reference** | `src/test/java/Int128.java` | Self-contained; used for tests and verification | Full (128÷128) |

*For full 128÷128 in the fast path, either use baseline for division ops or port the algorithm from the reference implementation.

### Finding Your Way Around

| Task | Key Files |
|------|-----------|
| Understanding the API contract | `src/main/java/com/symguac/int128/api/*.java` |
| Seeing correctness-first approach | `src/main/java/com/symguac/int128/impl/twolongs/*.java` |
| Seeing performance-first approach | `src/main/java/com/symguac/int128/impl/highperf/*.java` |
| Reference for complete algorithms | `src/test/java/Int128.java` (910+ LOC) |
| Registering new implementations | `src/main/java/com/symguac/int128/bench/Int128BenchmarkRegistry.java` |
| Build configuration | `pom.xml` |

---

## Common Assistant Tasks

### When Asked to Analyze Performance

1. **Check for common issues:**
   - Allocations in `*Into` methods (should be zero-allocation)
   - BigInteger usage in hot paths
   - Unnecessary boxing of primitives

2. **Run benchmarks to measure:**
   ```bash
   mvn clean package
   java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar
   ```

3. **Compare against baseline:**
   ```bash
   java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar -p implementation=twoLongsBaseline
   ```

### When Asked to Implement Missing Operations

1. **Check the reference implementation first** (`src/test/java/Int128.java`)
   - Contains complete, tested algorithms
   - No BigInteger in arithmetic paths (only for string conversions)
   - Good starting point for porting to other implementations

2. **Follow the existing patterns:**
   - Mutable operations: `{verb}Into` (e.g., `addInto`)
   - Immutable operations: `{verb}` (e.g., `add`)
   - Factory methods: `from{Source}` (e.g., `fromLong`)

3. **Update all implementations** if adding to the API interface

### When Asked to Add a New Implementation

**Quick checklist:**
1. Create classes in `src/main/java/com/symguac/int128/impl/{yourname}/`
2. Implement `Int128Arithmetic` interface with unique `id()`
3. Register in `Int128BenchmarkRegistry`
4. Add ID constant to `Int128ImplementationIds`
5. Rebuild and test: `mvn clean verify`

**Reference the README** for detailed step-by-step instructions.

### When Asked to Fix Correctness Issues

1. **Verify two's-complement semantics:**
   - Overflow/underflow must wrap (never throw)
   - Sign extension must be correct in `fromLong`
   - Low limb always unsigned in composition

2. **Test edge cases:**
   - `MIN_VALUE`, `MAX_VALUE`, `ZERO`, `ONE`
   - Overflow: `MAX_VALUE + 1` should wrap to `MIN_VALUE`
   - Underflow: `MIN_VALUE - 1` should wrap to `MAX_VALUE`

3. **Compare against baseline:**
   ```java
   TwoLongsBaselineArithmetic baseline = new TwoLongsBaselineArithmetic();
   YourImplementation yours = new YourImplementation();

   Int128Value expected = baseline.add(a, b);
   Int128Value actual = yours.add(a, b);
   // Verify: expected.high() == actual.high() && expected.low() == actual.low()
   ```

---

## Performance Guidance

### Critical Principles

**Zero-allocation hot paths:**
- Use mutable operations (`addInto`, `subtractInto`, `multiplyInto`) in tight loops
- Reuse `MutableInt128Value` instances
- Singleton reuse for common constants (ZERO, ONE)

**Performance anti-patterns to avoid:**
- ❌ BigInteger in arithmetic hot paths
- ❌ Allocating in `*Into` methods
- ❌ Boxing primitives unnecessarily
- ❌ Exception handling for control flow

**Preferred patterns:**
- ✅ Inline small helper methods (JIT-friendly)
- ✅ Straight-line code over complex branching
- ✅ Unsigned comparisons for low limb operations
- ✅ Portable bit operations (32-bit splits for multiplication)

### Optimization Reference

The reference implementation (`src/test/java/Int128.java`) demonstrates:
- 64×64→128 multiplication using 32-bit limb decomposition
- Fast 128÷64 division path
- Optimized 128÷128 division with approximation + correction
- No BigInteger except in string conversions

---

## Development Workflow

### Build Commands

```bash
# Full build with tests
mvn clean verify

# Build shaded JAR for benchmarks
mvn clean package

# Install to local Maven repo
mvn clean install
```

### Running Benchmarks

```bash
# All benchmarks
mvn jmh:benchmark

# Via shaded JAR (all benchmarks)
java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar

# Specific benchmark
java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar Int128ArithmeticBenchmark.additionWithReuse

# Specific implementation
java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar -p implementation=fastLimb128
```

### Git Workflow

**Current branch:** `claude/refresh-documentation-intro-012cAWTGbuoRsjAKw4vWbRaK`

**Commit style (from history):**
- Clear, concise descriptions
- Examples: "Move Int128.java to test directory for rigorous testing"
- Focus on what changed and why

**Push command:**
```bash
git push -u origin <branch-name>
```

Note: Branch must start with `claude/` and match session ID.

---

## Key Conventions

### Naming

- **Implementation classes:** `{Strategy}{Concept}{Type}` (e.g., `TwoLongsBaselineArithmetic`)
- **Constants:** `ALL_CAPS` (e.g., `ZERO`, `ONE`, `MIN_VALUE`, `MAX_VALUE`)
- **Implementation IDs:** camelCase strings (e.g., `"twoLongsBaseline"`, `"fastLimb128"`)

### Code Style

- Java 17+ language features encouraged
- UTF-8 source encoding
- `final` for immutable fields and classes where appropriate
- JavaDoc on all public APIs

### Testing Philosophy

- Benchmarks serve as integration tests
- Correctness verified by comparing against baseline
- Reference implementation (`Int128.java`) used for verification
- Edge cases: MIN_VALUE, MAX_VALUE, zero, overflow/underflow

---

## Important Reminders

**For any changes involving arithmetic:**
- Always verify two's-complement semantics
- Overflow wraps, never throws
- Test edge cases thoroughly
- Compare results against baseline

**For performance work:**
- Measure before claiming improvements
- Use JMH benchmarks as ground truth
- Profile before optimizing (don't guess)
- Document why optimizations work

**For new implementations:**
- Start from reference implementation (`Int128.java`)
- Implement complete API (no partial implementations)
- Register in benchmark registry
- Document performance characteristics

**For documentation updates:**
- Keep README user-facing and concise
- Keep this guide assistant-focused and complementary
- Update JavaDoc for public API changes
- Maintain consistency across docs

---

## Quick Reference

### Available Benchmark IDs

```java
import com.symguac.int128.bench.Int128BenchmarkRegistry;

System.out.println(Int128BenchmarkRegistry.registeredIds());
// Output: [twoLongsBaseline, fastLimb128]
```

### Constants to Expect

Well-formed implementations should define:
- `ZERO`: `high=0, low=0`
- `ONE`: `high=0, low=1`
- `MIN_VALUE`: `high=0x8000_0000_0000_0000L, low=0` (−2¹²⁷)
- `MAX_VALUE`: `high=0x7FFF_FFFF_FFFF_FFFFL, low=0xFFFF_FFFF_FFFF_FFFFL` (2¹²⁷−1)

### Division Semantics

- Truncating division (toward zero)
- Remainder carries dividend's sign
- `a = q*d + r` identity must hold

---

## Additional Resources

- **[README](../README.md)** - User-facing documentation, quick start, build instructions
- **[Review History](history/REVIEW_REPORT.md)** - Historical review report and issue resolutions

---

## Support

When uncertain about implementation details:
1. Check the reference implementation (`src/test/java/Int128.java`)
2. Compare behavior against baseline implementation
3. Run benchmarks to validate performance claims
4. Refer to README for user-facing guidance

Prioritize correctness over performance. Performance improvements must never break arithmetic semantics.
