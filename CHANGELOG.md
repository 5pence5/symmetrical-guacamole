# Changelog

All notable changes to the Int128 Benchmark Harness project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Comprehensive documentation update (CLAUDE.md, Readme, CONTRIBUTING.md, CHANGELOG.md)
- CONTRIBUTING.md with detailed developer guidelines
- CHANGELOG.md for tracking version history

### Changed
- Updated CLAUDE.md to reflect current project state
- Enhanced Readme with comprehensive usage examples and implementation details
- Updated all documentation references from Fast128.java to Int128.java

## [0.1.0] - 2025-11-13

### Added
- JUnit 5 test framework integration (PR #7, #9)
- Comprehensive test suite with 100+ tests
  - Int128Test.java: Core arithmetic, comparisons, bitwise, shifts, string conversion
  - Int128DivisionTest.java: Division and remainder tests
  - SimpleDivTest.java: Basic division smoke tests
  - DebugDivisionTest.java: Debug utilities for division edge cases
- Maven Surefire 3.2.5 configuration for JUnit 5 test discovery (PR #9)
- REVIEW_REPORT.md documenting comprehensive bug analysis and test results (PR #5)
- CLAUDE.md comprehensive AI assistant guide (PR #5)

### Fixed
- **CRITICAL:** Fixed infinite loop in 128/128 division algorithm (PR #10, #13)
  - Added iteration bounds to prevent hangs
  - Corrected quotient clamping in Knuth algorithm
  - Fixed overflow handling in `udivrem_128by128`
- **CRITICAL:** Fixed incorrect signed division for negative numbers (PR #8)
  - Corrected `divRemPow10` to use signed division for negative dividends
  - Fixed sign handling throughout division operations
- **CRITICAL:** File naming issue - renamed Fast128.java to Int128.java (PR #6)
  - Fixed Java compilation requirement (public class name must match filename)
- Division edge cases in 128/64 division path (PR #13)
- Test discovery pattern (PR #7)

### Changed
- Renamed Int128Tester.java to Int128Test.java for JUnit compliance (PR #7)
- Enhanced Int128.java reference implementation to production-ready status
- Improved documentation and JavaDoc comments

### Security
- Fixed potential DoS vulnerability from infinite loop in division (PR #10, #13)
- Fixed data corruption bug in financial operations with negative values (PR #8)

## [0.0.1] - 2025-11-13 (Initial State)

### Added
- Initial project structure with Maven build system
- Plugin-based architecture for Int128 implementations
- Core API interfaces:
  - Int128Arithmetic: Main arithmetic contract
  - Int128Value: Read-only value interface
  - MutableInt128Value: Mutable value for zero-allocation patterns
- Two baseline implementations:
  - TwoLongsBaseline: Correctness-focused reference implementation
  - FastInt128: Performance-optimized implementation
- JMH benchmark suite with 5 benchmarks:
  - Addition with reuse (zero-allocation)
  - Subtraction with reuse (zero-allocation)
  - Multiplication with reuse (zero-allocation)
  - Addition allocating (allocation overhead measurement)
  - Multiplication allocating (allocation overhead measurement)
- Int128 reference implementation (Fast128.java, later renamed to Int128.java)
  - 1200+ lines of comprehensive 128-bit integer arithmetic
  - Full arithmetic: add, sub, mul, div, rem
  - Bitwise operations: and, or, xor, not, shifts
  - String conversions: decimal and hexadecimal
  - Financial helpers: division by powers of 10, rounding modes
  - Constants: ZERO, ONE, DECIMAL_BASE, MIN_VALUE, MAX_VALUE
  - Comparable and Serializable support

### Technical Details
- Java 17 minimum requirement
- Maven 3.x build system
- JMH 1.36 for microbenchmarking
- UTF-8 source encoding
- Two-limb representation (high/low 64-bit limbs)
- Two's complement wrap semantics for overflow
- Zero-allocation hot paths via mutable operations

---

## Version History Summary

| Version | Date | Highlights |
|---------|------|------------|
| 0.1.0 | 2025-11-13 | JUnit tests, critical bug fixes, production-ready |
| 0.0.1 | 2025-11-13 | Initial release with JMH benchmarks |

---

## Pull Request History

### PR #13 - Critical Division Bug Fixes
**Date:** 2025-11-13
**Type:** Bug Fix (Critical)
**Description:** Fixed Knuth algorithm implementation in 128/128 division and 128/64 division edge cases
**Impact:** Resolves infinite loop vulnerability and incorrect division results

### PR #10 - Fixed Quotient Clamping and Overflow Handling
**Date:** 2025-11-13
**Type:** Bug Fix (Critical)
**Description:** Corrected quotient clamping in `udivrem_128by128`, added iteration bounds
**Impact:** Prevents infinite loops and improves division algorithm robustness

### PR #9 - Testing Infrastructure Improvements
**Date:** 2025-11-13
**Type:** Infrastructure
**Description:** Configured Maven Surefire 3.2.5 for JUnit 5
**Impact:** Enables automatic test discovery and proper test execution

### PR #8 - Division and Sign Handling Fixes
**Date:** 2025-11-13
**Type:** Bug Fix (Critical)
**Description:** Fixed `divRemPow10` for negative numbers, corrected signed division
**Impact:** Resolves data corruption in financial operations with negative values

### PR #7 - Test Suite Reorganization
**Date:** 2025-11-13
**Type:** Infrastructure
**Description:** Renamed Int128Tester to Int128Test, added proper JUnit 5 support
**Impact:** Improves test organization and JUnit compatibility

### PR #6 - File Naming Correction
**Date:** 2025-11-13
**Type:** Bug Fix (Build Blocker)
**Description:** Renamed Fast128.java to Int128.java
**Impact:** Fixes Java compilation requirement (public class name must match filename)

### PR #5 - Initial CLAUDE.md and Review Report
**Date:** 2025-11-13
**Type:** Documentation
**Description:** Added comprehensive CLAUDE.md guide and REVIEW_REPORT.md
**Impact:** Provides detailed documentation for contributors and AI assistants

### PR #4 - Organize Fast128 File
**Date:** 2025-11-13
**Type:** Refactoring
**Description:** Moved Fast128.java to test directory
**Impact:** Better project organization

---

## Known Issues

### Fixed (See PR History Above)
- ✅ Infinite loop in 128/128 division
- ✅ Incorrect signed division for negative numbers
- ✅ File naming (Fast128.java vs Int128.java)
- ✅ Quotient clamping in Knuth algorithm

### Currently None
All critical bugs identified in REVIEW_REPORT.md have been resolved.

---

## Migration Guide

### From Fast128.java to Int128.java

**File renamed:** `src/test/java/Fast128.java` → `src/test/java/Int128.java`

**Action required:**
- Update import statements: no change needed (class name was already `Int128`)
- Update file references in documentation
- Update build scripts that directly reference the file

**Code compatibility:** 100% compatible, only file name changed

---

## Future Roadmap

### Potential Enhancements
- Division by constant optimization (compile-time divisors)
- SIMD-based batch operations (Project Panama / Vector API)
- Additional rounding modes for financial operations
- Performance comparison with alternative approaches (Graal native, C2 intrinsics)

### Under Consideration
- GraalVM native image support
- Additional benchmark scenarios (financial workload simulation)
- Property-based testing with QuickCheck-style framework
- Formal verification of critical algorithms

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on contributing to this project.

## Acknowledgments

- JMH team for the excellent benchmarking framework
- Contributors who identified and fixed critical division bugs
- Community testing and feedback

---

[Unreleased]: https://github.com/5pence5/symmetrical-guacamole/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/5pence5/symmetrical-guacamole/releases/tag/v0.1.0
[0.0.1]: https://github.com/5pence5/symmetrical-guacamole/releases/tag/v0.0.1
