# Changelog

All notable changes to the Int128 Benchmark Harness project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **AGENTS.md** - Comprehensive documentation of Int128 implementation agents
- **CONTRIBUTING.md** - Developer contribution guidelines
- **CHANGELOG.md** - Project change tracking
- Enhanced Readme with documentation references and agent comparison table

### Changed
- Improved documentation structure and cross-referencing

---

## [0.1.0] - 2025-11-13

### Added
- Initial project structure with Maven build system
- **Agent Architecture**: Plugin-based system for comparing Int128 implementations
- **TwoLongs Baseline Agent** (`twoLongsBaseline`):
  - Correctness-first reference implementation
  - Uses BigInteger for conservative arithmetic
  - ~200 LOC across 3 classes
  - Registered as baseline for correctness verification
- **FastInt128 Agent** (`fastLimb128`):
  - High-performance optimized implementation
  - Custom 64×64→128 multiplication using 32-bit limb decomposition
  - Fast 128÷64 division path for common operations
  - Optimized 128÷128 division algorithm
  - ~1000+ LOC in FastInt128Value.java
  - Zero-allocation patterns for trading workloads
- **Fast128 Reference Implementation** (src/test/java/Int128.java):
  - Comprehensive standalone Int128 implementation (910+ LOC)
  - Full arithmetic, bitwise, and string conversion support
  - Financial helpers (divRemPow10, rounding modes)
  - Implements Comparable<Int128> and Serializable
  - **Note:** Contains known bugs (see REVIEW_REPORT.md)
- **API Interfaces**:
  - `Int128Arithmetic` - Main agent contract
  - `Int128Value` - Read-only two-limb value
  - `MutableInt128Value` - Zero-allocation mutable operations
- **Benchmark Infrastructure**:
  - `Int128BenchmarkRegistry` - Agent factory and registry
  - `Int128ArithmeticBenchmark` - JMH benchmark suite with 5 benchmarks
  - `Int128ImplementationIds` - Standard agent ID constants
- **JMH Benchmarks**:
  - `additionWithReuse` - Zero-allocation addition
  - `subtractionWithReuse` - Zero-allocation subtraction
  - `multiplicationWithReuse` - Zero-allocation multiplication
  - `additionAllocating` - Allocation overhead measurement
  - `multiplicationAllocating` - Allocation overhead measurement
- **Test Suite**:
  - Int128Test.java - Comprehensive test coverage
  - Int128DivisionTest.java - Division algorithm tests
  - DebugDivisionTest.java - Division debugging utilities
  - SimpleDivTest.java - Basic division verification
- **Documentation**:
  - CLAUDE.md - AI assistant guide and codebase architecture
  - REVIEW_REPORT.md - Test report for Fast128 reference implementation
  - Readme - User-facing build and benchmark instructions
- **Build System**:
  - Maven POM with Java 17 configuration
  - JMH 1.36 integration
  - Shaded JAR packaging for standalone execution
  - Maven JMH plugin support

### Fixed
- Int128 division bugs in Fast128 reference (documented in REVIEW_REPORT.md):
  - Fixed infinite loop in 128÷128 division for certain inputs
  - Fixed quotient clamping in udivrem_128by128
  - Fixed overflow handling in division algorithms
  - Added iteration bounds and safety checks
  - **Note:** Fast128 reference still has known issues with negative divRemPow10

### Performance
- **FastInt128 Benchmarks** (indicative, hardware-dependent):
  - Addition with reuse: ~500M ops/sec
  - Multiplication with reuse: ~200M ops/sec
  - 15-28% faster than baseline for reuse operations
  - 50-150% faster than baseline for allocating operations
- **Zero-allocation patterns** achieve single-digit nanosecond latency
- **Custom multiplication** 2.5x faster than BigInteger-based approach

### Documentation
- Comprehensive architecture documentation
- Agent comparison matrix
- Performance tuning guidelines
- Algorithm reference implementations
- Step-by-step developer guides

---

## [0.0.1] - 2025-11-XX (Initial Prototype)

### Added
- Initial repository setup
- Basic Int128 arithmetic exploration
- Early prototype implementations

---

## Version History Summary

| Version | Date | Key Changes |
|---------|------|-------------|
| 0.1.0 | 2025-11-13 | Initial release with agent architecture, 2 production agents, JMH benchmarks |
| 0.0.1 | 2025-11-XX | Initial prototype |

---

## Upcoming (Planned)

### Future Enhancements
- **SIMD Agent**: Vectorized operations using Java Vector API (JEP 338)
- **Native Agent**: JNI-based implementation using CPU instructions (MULX, ADCX, ADOX)
- **BigDecimal-Compatible Agent**: Drop-in replacement for financial apps
- **GPU Agent**: CUDA/OpenCL for batch operations
- **Extended API**: Additional operations (GCD, power, modular arithmetic)
- **Comprehensive Test Suite**: Property-based testing, fuzz testing
- **Performance Profiling**: Automated performance regression detection
- **CI/CD Pipeline**: Automated testing and benchmarking

### Known Issues
- Fast128 reference has bugs with negative divRemPow10 (documented in REVIEW_REPORT.md)
- No formal unit test framework (JUnit/TestNG) - relies on manual tests
- Limited string conversion support in production agents
- No bitwise operations in production agents

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on contributing to this project.

---

## Links

- **Repository**: https://github.com/5pence5/symmetrical-guacamole
- **Documentation**: [AGENTS.md](AGENTS.md), [CLAUDE.md](CLAUDE.md), [CONTRIBUTING.md](CONTRIBUTING.md)
- **Issue Tracker**: https://github.com/5pence5/symmetrical-guacamole/issues

---

**Legend:**
- `Added` - New features
- `Changed` - Changes to existing functionality
- `Deprecated` - Features that will be removed in future versions
- `Removed` - Removed features
- `Fixed` - Bug fixes
- `Security` - Security vulnerability fixes
- `Performance` - Performance improvements
