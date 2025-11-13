# Contributing to Int128 Benchmark Harness

Thank you for your interest in contributing! This document provides guidelines for contributing to the Int128 Benchmark Harness project.

## Table of Contents

- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Code Standards](#code-standards)
- [Testing Requirements](#testing-requirements)
- [Pull Request Process](#pull-request-process)
- [Implementation Guidelines](#implementation-guidelines)
- [Performance Considerations](#performance-considerations)

## Getting Started

### Prerequisites

- Java 17 or later
- Maven 3.x
- Git
- A good understanding of two's complement arithmetic
- Familiarity with JMH for performance work

### Setting Up Your Environment

1. Fork the repository
2. Clone your fork:
   ```bash
   git clone https://github.com/your-username/symmetrical-guacamole.git
   cd symmetrical-guacamole
   ```
3. Build the project:
   ```bash
   mvn clean package
   ```
4. Run tests to verify setup:
   ```bash
   mvn test
   ```

## Development Workflow

### Branch Naming

- Feature branches: `feature/<feature-name>`
- Bug fixes: `fix/<bug-description>`
- Claude AI branches: `claude/<description>-<session-id>` (auto-generated)

### Commit Messages

Write clear, concise commit messages:

**Good examples:**
- "Fix critical Int128 division bugs in Knuth algorithm and 128/64 division"
- "Add comprehensive test suite for division edge cases"
- "Optimize multiplication using 32-bit limb decomposition"

**Avoid:**
- "Fixed stuff"
- "WIP"
- "Update"

### Pull Request Workflow

1. Create a feature branch from `main`
2. Make your changes
3. Add tests for new functionality
4. Run full test suite: `mvn test`
5. Run benchmarks if performance-related: `mvn clean package && java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar`
6. Commit with clear messages
7. Push to your fork
8. Open a pull request against `main`

## Code Standards

### Java Style

**General conventions:**
- Java 17 language features encouraged
- UTF-8 source encoding
- 4-space indentation (no tabs)
- Line length: 120 characters (soft limit)
- Package structure: `com.symguac.int128.{api|impl|bench}`

**Naming conventions:**
- Classes: `PascalCase` (e.g., `FastInt128Arithmetic`)
- Methods: `camelCase` (e.g., `addInto`, `fromLong`)
- Constants: `ALL_CAPS` (e.g., `ZERO`, `ONE`, `MIN_VALUE`)
- Implementation IDs: `camelCase` strings (e.g., `"twoLongsBaseline"`, `"fastLimb128"`)

**Method naming patterns:**
- Mutable operations: `{verb}Into` (e.g., `addInto`, `subtractInto`)
- Immutable operations: `{verb}` (e.g., `add`, `multiply`)
- Factory methods: `from{Source}` (e.g., `fromLong`, `fromParts`)
- Predicates: `is{Property}` (e.g., `isZero`, `isNegative`)

### Documentation Requirements

**All public APIs must have JavaDoc:**

```java
/**
 * Adds two 128-bit values and stores the result in a mutable destination.
 *
 * <p>This operation is allocation-free and suitable for hot paths.
 * Overflow wraps according to two's complement semantics.
 *
 * @param left the left operand
 * @param right the right operand
 * @param dest the destination for the result (modified in place)
 * @throws NullPointerException if any parameter is null
 */
void addInto(Int128Value left, Int128Value right, MutableInt128Value dest);
```

**Document non-obvious optimizations:**

```java
// Use 32-bit split for portable 64x64→128 multiplication.
// The JIT optimizes this well on both x86_64 and AArch64.
long a_lo = a & 0xFFFFFFFFL;
long a_hi = a >>> 32;
```

### Code Quality

**Required:**
- No compiler warnings
- No unused imports
- Final fields for immutable classes
- Null checks for public methods
- Proper exception handling

**Prohibited:**
- BigInteger in arithmetic hot paths (only for string I/O)
- Boxing/unboxing primitives unnecessarily
- Exception handling for control flow
- Mutable static state

## Testing Requirements

### Test Coverage

All new code must include tests:

**For new arithmetic operations:**
```java
@Test
void testOperation() {
    Int128 a = Int128.valueOf(100);
    Int128 b = Int128.valueOf(50);
    Int128 result = a.operation(b);

    // Verify with BigInteger
    BigInteger expected = BigInteger.valueOf(100).operation(BigInteger.valueOf(50));
    assertEquals(expected, result.toBigInteger());
}
```

**For edge cases:**
```java
@Test
void testOperationEdgeCases() {
    // Test MIN_VALUE
    Int128 result1 = Int128.MIN_VALUE.operation(Int128.ONE);
    assertEquals(expectedValue, result1);

    // Test MAX_VALUE
    Int128 result2 = Int128.MAX_VALUE.operation(Int128.ONE);
    assertEquals(expectedValue, result2);

    // Test zero
    Int128 result3 = Int128.ZERO.operation(Int128.valueOf(42));
    assertEquals(expectedValue, result3);

    // Test negative numbers
    Int128 result4 = Int128.valueOf(-1234).operation(Int128.valueOf(-5678));
    assertEquals(expectedValue, result4);
}
```

### Running Tests

Before submitting a PR:

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=Int128Test

# Run with verbose output
mvn test -X
```

### Performance Testing

For performance-critical changes:

```bash
# Build benchmarks
mvn clean package

# Run all benchmarks
java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar

# Run specific benchmark
java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar Int128ArithmeticBenchmark.additionWithReuse

# Run with GC profiling
java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar -prof gc

# Compare implementations
java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar -p implId=twoLongsBaseline,fastLimb128
```

## Pull Request Process

### Before Submitting

1. **Verify tests pass:**
   ```bash
   mvn clean test
   ```

2. **Check code compiles without warnings:**
   ```bash
   mvn clean compile
   ```

3. **Run benchmarks (if performance-related):**
   ```bash
   mvn clean package
   java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar
   ```

4. **Update documentation:**
   - Update JavaDoc for public APIs
   - Update `CLAUDE.md` for architectural changes
   - Update `Readme` for user-facing changes
   - Update `CHANGELOG.md` with your changes

### PR Description Template

```markdown
## Description
Brief description of what this PR does.

## Type of Change
- [ ] Bug fix (non-breaking change fixing an issue)
- [ ] New feature (non-breaking change adding functionality)
- [ ] Breaking change (fix or feature causing existing functionality to change)
- [ ] Performance improvement
- [ ] Documentation update

## Testing
- [ ] Unit tests added/updated
- [ ] All tests pass (`mvn test`)
- [ ] Benchmarks run (if performance-related)
- [ ] Manual testing performed

## Checklist
- [ ] Code follows project style guidelines
- [ ] JavaDoc added for public APIs
- [ ] Tests cover new functionality
- [ ] Documentation updated
- [ ] No compiler warnings
- [ ] Commit messages are clear and descriptive
```

### Review Process

1. Automated checks must pass (tests, compilation)
2. Code review by maintainer(s)
3. Address review feedback
4. Approval and merge

## Implementation Guidelines

### Adding a New Int128 Implementation

See `CLAUDE.md` for detailed step-by-step guide. Summary:

1. Create implementation in `src/main/java/com/symguac/int128/impl/{yourname}/`
2. Implement `Int128Arithmetic` interface
3. Register in `Int128BenchmarkRegistry`
4. Add ID constant to `Int128ImplementationIds`
5. Enable in `Int128ArithmeticBenchmark`
6. Add comprehensive tests
7. Run benchmarks to verify performance

### Two's Complement Semantics

**Critical rules:**
- Overflow **wraps**, does not throw exceptions
- The sign bit is bit 127 (high bit of `high` limb)
- Low limb is treated as **unsigned** for value composition
- Value = `(high << 64) + (low & 0xFFFF_FFFF_FFFF_FFFFL)`

**Examples:**
```java
// MAX_VALUE + 1 wraps to MIN_VALUE
Int128.MAX_VALUE.add(Int128.ONE) == Int128.MIN_VALUE

// MIN_VALUE - 1 wraps to MAX_VALUE
Int128.MIN_VALUE.sub(Int128.ONE) == Int128.MAX_VALUE

// Negating MIN_VALUE wraps to itself
Int128.MIN_VALUE.negate() == Int128.MIN_VALUE
```

### Division Algorithm Notes

Division is complex and error-prone. **Required reading:**

- Review `REVIEW_REPORT.md` for historical bugs
- Study `Int128.java` reference implementation
- Test extensively with negative numbers
- Test the 128÷64 fast path separately from 128÷128
- Verify division identity: `a == (a/b)*b + (a%b)` for all test cases

**Common pitfalls:**
- Infinite loops in quotient approximation
- Incorrect sign handling for negative dividends/divisors
- Overflow in intermediate calculations
- Wrong quotient when divisor is close to dividend

## Performance Considerations

### Hot Path Optimization

**Zero-allocation operations** are critical:

```java
// GOOD: Zero allocation
MutableInt128Value scratch = arithmetic.createMutable();
for (int i = 0; i < 1_000_000; i++) {
    arithmetic.addInto(values[i], values[i+1], scratch);
    // Use scratch...
}

// BAD: Allocates 1 million objects
for (int i = 0; i < 1_000_000; i++) {
    Int128Value result = arithmetic.add(values[i], values[i+1]);
    // Use result...
}
```

### Benchmark-Driven Optimization

**Never optimize without benchmarks:**

1. Write a benchmark
2. Run baseline: `java -jar target/benchmarks.jar YourBenchmark -p implId=twoLongsBaseline`
3. Implement optimization
4. Run optimized: `java -jar target/benchmarks.jar YourBenchmark -p implId=yourOptimized`
5. Compare results (require >10% improvement for added complexity)

### Common Optimizations

**Singleton reuse:**
```java
// Return shared instances for common values
if (hi == 0 && lo == 0) return ZERO;
if (hi == 0 && lo == 1) return ONE;
```

**JIT-friendly code:**
```java
// GOOD: Straight-line code
long carry = (aLo + bLo < aLo) ? 1L : 0L;
long resultHi = aHi + bHi + carry;

// AVOID: Branches in tight loops
if (someCondition) {
    // Complex branching logic
}
```

## Getting Help

- **Architecture questions:** See `CLAUDE.md`
- **Bug reports:** Check `REVIEW_REPORT.md` for known issues
- **Implementation guidance:** Review `Int128.java` reference implementation
- **Performance questions:** Run benchmarks and compare

## Recognition

Contributors will be acknowledged in:
- Git commit history
- Pull request comments
- Release notes (for significant contributions)

Thank you for contributing to Int128 Benchmark Harness!
