# Contributing to Int128 Benchmark Harness

Thank you for your interest in contributing to the Int128 Benchmark Harness! This document provides guidelines for contributing new implementations, improvements, and bug fixes.

## Table of Contents

- [Getting Started](#getting-started)
- [Types of Contributions](#types-of-contributions)
- [Development Workflow](#development-workflow)
- [Adding New Agents](#adding-new-agents)
- [Coding Standards](#coding-standards)
- [Testing Requirements](#testing-requirements)
- [Performance Benchmarking](#performance-benchmarking)
- [Pull Request Process](#pull-request-process)
- [Community Guidelines](#community-guidelines)

---

## Getting Started

### Prerequisites

- **Java 17** or later
- **Maven 3.x** or later
- Git for version control
- Understanding of two's complement arithmetic
- Familiarity with JMH (Java Microbenchmark Harness) for performance work

### Initial Setup

```bash
# Clone the repository
git clone https://github.com/5pence5/symmetrical-guacamole.git
cd symmetrical-guacamole

# Build the project
mvn clean package

# Run the benchmarks to verify setup
java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar
```

### Required Reading

Before contributing, please read:

1. **[Readme](Readme)** - Project overview and basic usage
2. **[AGENTS.md](AGENTS.md)** - Agent architecture and implementation guide
3. **[CLAUDE.md](CLAUDE.md)** - Comprehensive codebase documentation
4. **[REVIEW_REPORT.md](REVIEW_REPORT.md)** - Known issues in reference implementation

---

## Types of Contributions

### 1. New Agent Implementations

Contribute new Int128 arithmetic strategies optimized for specific use cases:

- **SIMD/Vectorized agents** using Java Vector API
- **Native agents** using JNI for platform-specific instructions
- **Specialized agents** for domain-specific optimizations
- **Research agents** exploring novel algorithms

See [Adding New Agents](#adding-new-agents) below.

### 2. Performance Improvements

Enhance existing agents:

- Algorithm optimizations
- Reduced allocations
- JIT compiler-friendly patterns
- Cache optimization

**Requirements:**
- Must not break correctness
- Must include benchmark results showing improvement
- Must maintain or improve thread safety

### 3. Bug Fixes

Fix correctness issues:

- Arithmetic errors
- Overflow handling bugs
- Sign extension issues
- Edge case failures

**Requirements:**
- Include test case demonstrating the bug
- Verify fix against baseline implementation
- Document the root cause

### 4. Documentation

Improve project documentation:

- Code examples
- Algorithm explanations
- Performance tuning guides
- API clarifications

### 5. Testing

Expand test coverage:

- Edge case testing
- Property-based tests
- Fuzz testing
- Cross-verification tests

---

## Development Workflow

### Branch Strategy

1. **Main branch** (`main`): Stable, production-ready code
2. **Feature branches**: `feature/{descriptive-name}`
3. **Bug fix branches**: `fix/{issue-description}`
4. **Agent branches**: `agent/{agent-name}`

### Creating a Feature Branch

```bash
git checkout main
git pull origin main
git checkout -b feature/my-new-feature
```

### Committing Changes

**Commit message format:**
```
<type>: <short summary>

<detailed description>

<footer>
```

**Types:**
- `feat`: New feature or agent
- `fix`: Bug fix
- `perf`: Performance improvement
- `docs`: Documentation changes
- `test`: Test additions or modifications
- `refactor`: Code refactoring
- `style`: Code style changes (formatting)

**Examples:**
```
feat: Add SIMD-based Int128 agent using Vector API

Implements vectorized operations for batch arithmetic using JEP 338.
Achieves 3x speedup for addition and 2x for multiplication in batch mode.

Benchmark results:
- Batch addition: 1500M ops/sec (vs 500M baseline)
- Batch multiplication: 600M ops/sec (vs 200M baseline)
```

```
fix: Correct overflow handling in FastInt128 multiplication

Fixed edge case where multiplication of MAX_VALUE * 2 produced
incorrect results. Added test case and verified against baseline.

Fixes #42
```

---

## Adding New Agents

### Step-by-Step Guide

**1. Create implementation package:**

```bash
mkdir -p src/main/java/com/symguac/int128/impl/youragent
```

**2. Implement required classes:**

Create three classes:
- `YourAgentArithmetic.java` (implements Int128Arithmetic)
- `YourAgentValue.java` (implements Int128Value)
- `MutableYourAgentValue.java` (implements MutableInt128Value)

See [AGENTS.md - Developing New Agents](AGENTS.md#developing-new-agents) for complete code examples.

**3. Register in benchmark registry:**

Edit `src/main/java/com/symguac/int128/bench/Int128BenchmarkRegistry.java`:

```java
static {
    register(Int128ImplementationIds.TWO_LONGS_BASELINE, TwoLongsBaselineArithmetic::new);
    register(Int128ImplementationIds.FAST_LIMB_BASED, FastInt128Arithmetic::new);
    register("yourAgent", YourAgentArithmetic::new);  // Add this
}
```

**4. Add ID constant (optional but recommended):**

Edit `src/main/java/com/symguac/int128/bench/Int128ImplementationIds.java`:

```java
public static final String YOUR_AGENT = "yourAgent";
```

**5. Enable in benchmarks:**

Edit `src/jmh/java/com/symguac/int128/bench/Int128ArithmeticBenchmark.java`:

```java
@Param({
    Int128ImplementationIds.TWO_LONGS_BASELINE,
    Int128ImplementationIds.FAST_LIMB_BASED,
    "yourAgent"  // Add this
})
private String implId;
```

**6. Build and verify:**

```bash
mvn clean package
java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar -p implId=yourAgent
```

### Agent Implementation Checklist

Before submitting your agent:

- [ ] Implements all methods in `Int128Arithmetic` interface
- [ ] Provides both immutable and mutable operations
- [ ] Handles sign extension correctly in `fromLong`
- [ ] Uses unsigned comparison for low limb carry detection
- [ ] Implements proper two's complement overflow (wrapping)
- [ ] Equals/hashCode consistent with two-limb representation
- [ ] Thread-safe (immutable values)
- [ ] Zero-allocation patterns in mutable operations
- [ ] Registered in `Int128BenchmarkRegistry`
- [ ] Added to benchmark `@Param` annotation
- [ ] Benchmark results included in PR
- [ ] Verified correctness against `twoLongsBaseline`
- [ ] Documentation updated (JavaDoc + AGENTS.md)

---

## Coding Standards

### Java Style

**Follow standard Java conventions:**
- 4-space indentation (no tabs)
- Opening braces on same line
- Maximum line length: 120 characters
- UTF-8 source encoding

**Naming:**
- Classes: `PascalCase`
- Methods: `camelCase`
- Constants: `ALL_CAPS_WITH_UNDERSCORES`
- Packages: `lowercase`

### Performance Guidelines

**Do:**
- ✅ Minimize allocations in hot paths
- ✅ Use primitive types (avoid boxing)
- ✅ Mark classes `final` when appropriate
- ✅ Use `final` for immutable fields
- ✅ Inline small helper methods
- ✅ Write JIT-friendly code (straight-line, minimal branches)

**Don't:**
- ❌ Use BigInteger in arithmetic hot paths
- ❌ Create new objects in loops (use mutable variants)
- ❌ Box primitives unnecessarily
- ❌ Use exceptions for control flow
- ❌ Add unnecessary synchronization

### Documentation Requirements

**All public APIs require JavaDoc:**

```java
/**
 * Adds two Int128 values and stores the result in a mutable destination.
 *
 * <p>This is a zero-allocation operation suitable for high-frequency trading
 * scenarios. Overflow wraps according to two's complement semantics.
 *
 * @param left the first operand (not modified)
 * @param right the second operand (not modified)
 * @param dest the destination for the sum (modified in place)
 * @throws NullPointerException if any argument is null
 */
void addInto(Int128Value left, Int128Value right, MutableInt128Value dest);
```

**Performance-critical sections need comments:**

```java
// Fast path for 128÷64 division (common in financial applications)
// Avoids expensive 128÷128 algorithm when divisor fits in 64 bits
if (divisor_high == 0 || divisor_high == -1) {
    return fastDivide128by64(dividend, divisor_low);
}
```

### Correctness Requirements

**Two's Complement Semantics:**

```java
// ✅ Correct sign extension
public Int128Value fromLong(long value) {
    long high = (value < 0) ? -1L : 0L;  // Sign extend
    return fromParts(high, value);
}

// ❌ Incorrect (doesn't handle negative values)
public Int128Value fromLong(long value) {
    return fromParts(0L, value);  // BUG: negative values broken
}
```

**Unsigned Low Limb Comparison:**

```java
// ✅ Correct carry detection
long sum = left.low() + right.low();
long carry = (Long.compareUnsigned(sum, left.low()) < 0) ? 1L : 0L;

// ❌ Incorrect (signed comparison)
long carry = (sum < left.low()) ? 1L : 0L;  // BUG: fails for large values
```

---

## Testing Requirements

### Correctness Verification

**All agents must pass correctness tests:**

```java
// Test against baseline for verification
Int128Arithmetic baseline = new TwoLongsBaselineArithmetic();
Int128Arithmetic yourAgent = new YourAgentArithmetic();

// Test addition
Int128Value a = baseline.fromLong(12345);
Int128Value b = baseline.fromLong(67890);

Int128Value expected = baseline.add(a, b);
Int128Value actual = yourAgent.add(a, b);

assertEquals(expected.high(), actual.high());
assertEquals(expected.low(), actual.low());
```

### Edge Cases to Test

**Required test cases:**

1. **Zero operations:**
   - `0 + 0 = 0`
   - `0 * 123 = 0`
   - `123 - 123 = 0`

2. **Identity operations:**
   - `123 + 0 = 123`
   - `123 * 1 = 123`
   - `123 / 1 = 123`

3. **Sign handling:**
   - Positive + Positive
   - Positive + Negative
   - Negative + Negative
   - `fromLong(-1)` → `high=-1, low=-1`

4. **Overflow/Underflow:**
   - `MAX_VALUE + 1` → wraps to `MIN_VALUE`
   - `MIN_VALUE - 1` → wraps to `MAX_VALUE`
   - `MAX_VALUE * 2` → wraps correctly

5. **Boundary values:**
   - `MIN_VALUE` (`-2^127`)
   - `MAX_VALUE` (`2^127 - 1`)
   - Values near zero
   - Large positive and negative values

### Property-Based Testing

**Recommended properties to verify:**

```java
// Commutative property
assertEquals(agent.add(a, b), agent.add(b, a));

// Associative property
assertEquals(agent.add(agent.add(a, b), c), agent.add(a, agent.add(b, c)));

// Identity property
assertEquals(a, agent.add(a, ZERO));
assertEquals(a, agent.multiply(a, ONE));

// Division identity
Int128Value quotient = agent.divide(a, b);
Int128Value remainder = agent.remainder(a, b);
assertEquals(a, agent.add(agent.multiply(quotient, b), remainder));
```

---

## Performance Benchmarking

### Running Benchmarks

**Before and after your changes:**

```bash
# Baseline measurement (before changes)
git checkout main
mvn clean package
java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar > results-before.txt

# Your changes (after)
git checkout feature/my-optimization
mvn clean package
java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar > results-after.txt

# Compare
diff results-before.txt results-after.txt
```

### Benchmark Requirements for PRs

**Performance improvements must include:**

1. **Before/after benchmark results**
2. **Statistical significance** (low variance, consistent improvements)
3. **Multiple runs** (at least 3 complete runs)
4. **Hardware specs** (CPU, RAM, OS, JVM version)

**Example benchmark report:**

```markdown
## Benchmark Results

### Environment
- CPU: Intel Xeon E5-2690 v4 @ 2.60GHz
- RAM: 64GB DDR4
- OS: Ubuntu 22.04 LTS
- JVM: OpenJDK 17.0.8

### Results (3 runs averaged)

| Benchmark | Before | After | Improvement |
|-----------|--------|-------|-------------|
| additionWithReuse | 450M ops/sec | 520M ops/sec | +15.6% |
| multiplicationWithReuse | 180M ops/sec | 230M ops/sec | +27.8% |

### Analysis
The optimization reduces unnecessary allocations in the multiplication
hot path, resulting in 27% throughput improvement with no correctness
impact (verified against baseline).
```

### Profiling

**For deep performance analysis:**

```bash
# GC profiling
java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar \
    -p implId=yourAgent -prof gc

# Hotspot profiling
java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar \
    -p implId=yourAgent -prof stack

# JIT compilation
java -jar target/int128-0.1.0-SNAPSHOT-shaded.jar \
    -p implId=yourAgent -prof comp
```

---

## Pull Request Process

### Before Submitting

1. **Verify correctness:** Test against `twoLongsBaseline`
2. **Run benchmarks:** Include results in PR description
3. **Update documentation:** AGENTS.md if adding new agent
4. **Clean code:** No debug prints, commented-out code, or TODOs
5. **Format code:** Follow Java style guidelines
6. **Write tests:** Add test cases for new functionality
7. **Update CHANGELOG:** Document your changes

### PR Checklist

- [ ] Code follows project style guidelines
- [ ] All tests pass (`mvn test`)
- [ ] Benchmarks run successfully
- [ ] Documentation updated (JavaDoc + markdown files)
- [ ] Commit messages are clear and descriptive
- [ ] PR description explains motivation and approach
- [ ] Benchmark results included (for performance PRs)
- [ ] Correctness verification included (for new agents)
- [ ] Breaking changes documented (if any)

### PR Template

```markdown
## Description
Brief description of your changes.

## Motivation
Why is this change needed? What problem does it solve?

## Type of Change
- [ ] New agent implementation
- [ ] Performance improvement
- [ ] Bug fix
- [ ] Documentation update
- [ ] Test addition

## Checklist
- [ ] Verified correctness against baseline
- [ ] Benchmark results included
- [ ] Documentation updated
- [ ] Tests added/updated
- [ ] Code follows style guidelines

## Benchmark Results
(Include for performance-related PRs)

## Test Results
(Include for correctness-related changes)

## Breaking Changes
(List any API changes or behavioral changes)

## Additional Notes
(Any additional context or screenshots)
```

### Review Process

1. **Automated checks:** CI/CD pipeline runs tests and benchmarks
2. **Code review:** Maintainer reviews implementation
3. **Benchmark verification:** Performance claims are verified
4. **Correctness verification:** Arithmetic correctness is checked
5. **Approval:** PR is approved and merged

---

## Community Guidelines

### Code of Conduct

- **Be respectful:** Treat all contributors with respect
- **Be constructive:** Provide helpful feedback
- **Be patient:** Remember contributors are volunteers
- **Be collaborative:** Work together to improve the project

### Getting Help

**Questions about:**
- **Architecture:** See [AGENTS.md](AGENTS.md)
- **Implementation:** See [CLAUDE.md](CLAUDE.md)
- **Algorithms:** See `src/test/java/Int128.java` reference
- **Benchmarking:** See JMH documentation

**Stuck?**
- Open an issue with your question
- Include relevant code snippets
- Describe what you've already tried

### Reporting Issues

**Bug reports should include:**

1. **Description:** Clear description of the issue
2. **Steps to reproduce:** Minimal code to reproduce
3. **Expected behavior:** What should happen
4. **Actual behavior:** What actually happens
5. **Environment:** Java version, OS, JVM flags

**Example:**

```markdown
## Bug: Incorrect division result for negative numbers

### Description
FastInt128 produces incorrect quotient when dividing negative numbers.

### Steps to Reproduce
```java
Int128Arithmetic agent = new FastInt128Arithmetic();
Int128Value a = agent.fromLong(-100);
Int128Value b = agent.fromLong(10);
Int128Value result = agent.divide(a, b);
// Expected: -10, Actual: 429496729 (incorrect)
```

### Expected Behavior
Should return -10 (same as twoLongsBaseline)

### Actual Behavior
Returns 429496729

### Environment
- Java: OpenJDK 17.0.8
- OS: macOS 13.5
- Version: main branch (commit abc123)
```

---

## Release Process

### Version Numbering

We follow **Semantic Versioning** (SemVer):

- **MAJOR.MINOR.PATCH** (e.g., `0.1.0`)
- **MAJOR:** Breaking API changes
- **MINOR:** New features (backward-compatible)
- **PATCH:** Bug fixes (backward-compatible)

### Changelog Maintenance

Update `CHANGELOG.md` with your changes:

```markdown
## [Unreleased]

### Added
- New SIMD-based agent for vectorized operations

### Changed
- Improved FastInt128 multiplication performance by 15%

### Fixed
- Corrected overflow handling in edge case XYZ

### Deprecated
- (None)
```

---

## Recognition

Contributors are recognized in:

1. **Git commit history** (ensure your git config has correct name/email)
2. **CHANGELOG.md** (major contributions)
3. **AGENTS.md** (new agent implementations)

---

## License

By contributing, you agree that your contributions will be licensed under the same license as the project (see `LICENSE` file).

---

## Questions?

- **GitHub Issues:** https://github.com/5pence5/symmetrical-guacamole/issues
- **Documentation:** [AGENTS.md](AGENTS.md), [CLAUDE.md](CLAUDE.md)
- **Examples:** See existing implementations in `src/main/java/com/symguac/int128/impl/`

---

Thank you for contributing to Int128 Benchmark Harness! 🚀
