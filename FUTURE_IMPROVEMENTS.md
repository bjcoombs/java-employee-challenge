# Future Improvements & Technical Debt

This document consolidates unimplemented suggestions from PR code reviews into actionable items for future development. Items are categorized by priority and type.

> **Last Updated**: Generated from analysis of PRs #5, #6, #7, #8, #9, and #11

## Table of Contents

- [Summary](#summary)
- [Critical Priority](#critical-priority)
  - [1. Circuit Breaker Pattern](#1-circuit-breaker-pattern)
  - [2. Add 5xx Error Retry Support](#2-add-5xx-error-retry-support) ✅
- [High Priority](#high-priority)
  - [Testing Gaps](#testing-gaps)
  - [Architecture](#architecture)
- [Medium Priority](#medium-priority)
  - [Observability & Monitoring](#observability--monitoring)
  - [Testing Enhancements](#testing-enhancements)
  - [Code Quality](#code-quality)
  - [Documentation & DX](#documentation--dx)
- [Low Priority / Future Enhancements](#low-priority--future-enhancements)
- [Implementation Notes](#implementation-notes)
  - [Already Implemented](#already-implemented-removed-from-list)
  - [Suggested Implementation Order](#suggested-implementation-order)
- [Contributing](#contributing)

---

## Summary

| Priority | Count | Status |
|----------|-------|--------|
| Critical | 1 | Needs immediate attention |
| High | 12 | Should address before production |
| Medium | 27 | Address as capacity allows |
| Low | 40+ | Nice-to-have / Future enhancements |

---

## Critical Priority

These items represent potential production issues that should be addressed immediately.

### 1. Circuit Breaker Pattern
**Source**: PR #5
**Category**: Resilience
**Status**: ❌ Not Implemented

Add Resilience4j circuit breaker to prevent cascade failures when the downstream employee service is consistently failing.

```java
@CircuitBreaker(name = "employeeService", fallbackMethod = "fallbackFindAll")
public List<Employee> findAll() { ... }
```

**Why Critical**: Without circuit breaker, a failing downstream service can exhaust thread pools and cause cascading failures across the application.

---

### ~~2. Add 5xx Error Retry Support~~
**Source**: PR #5
**Category**: Resilience
**Status**: ✅ Implemented (PR #14)

~~Currently only retries on 429 (Too Many Requests). Should also retry on transient 5xx errors:~~
- ~~502 Bad Gateway~~
- ~~503 Service Unavailable~~
- ~~504 Gateway Timeout~~

Now retries on all 5xx errors via `RetryableServerException` with full integration test coverage.

---

## High Priority

Items that should be addressed before production deployment.

### Testing Gaps

#### 3. Integration Tests with MockMvc
**Source**: PR #8
**Category**: Testing

Use `@WebMvcTest` to verify full request/response cycle including validation, serialization, and content negotiation.

#### 4. Correlation ID Filter Tests
**Source**: PR #8
**Category**: Testing

Test scenarios:
- Header present/missing
- Blank value handling
- Response header inclusion
- MDC cleanup after request

#### 5. Add Test for Empty Validation Errors
**Source**: PR #7
**Category**: Testing

Test the "Validation failed" fallback branch when `MethodArgumentNotValidException` has no field errors.

#### 6. Retry Behavior Integration Test
**Source**: PR #5
**Category**: Testing

Add test that returns 429 twice then success, verifying exactly 3 requests were made.

#### 7. Timeout Scenario Tests
**Source**: PR #5
**Category**: Testing

Test connection timeout and read timeout behavior with WireMock delays.

### Architecture

#### 8. Use Locale.ROOT for Case-Insensitive Matching
**Source**: PR #6
**Category**: Code Quality
**Status**: ❌ Not Implemented

`toLowerCase()` without Locale can produce unexpected results (e.g., Turkish 'i' → 'İ').

```java
// Before
searchString.toLowerCase()

// After
searchString.toLowerCase(Locale.ROOT)
```

**Files**: `EmployeeService.java:31`

#### 9. Filter Null Names in getTopTenHighestEarningNames
**Source**: PR #6
**Category**: Code Quality

After filtering null salaries, result could still have employees with null names.

```java
.filter(e -> e.name() != null)
.map(Employee::name)
```

#### 10. Extract Duplicated Error Handling
**Source**: PR #5
**Category**: Code Quality

Error handling logic is duplicated across all four adapter methods. Extract to reusable method.

#### 11. Make Retry-After Header Configurable
**Source**: PR #7
**Category**: Configuration

`"5"` seconds is hardcoded. Externalize to `application.yml`.

#### 12. Catch-All Exception Handler
**Source**: PR #8
**Category**: Architecture

Prevent leaking internal details for unexpected exceptions.

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
    logger.error("Unexpected error", ex);
    return ResponseEntity.status(500)
        .body(ErrorResponse.of(500, "Internal Server Error", "An unexpected error occurred"));
}
```

---

## Medium Priority

Items to address in upcoming development cycles.

### Observability & Monitoring

#### 13. Enable Cache Metrics
**Source**: PR #11
**Category**: Performance
**Status**: ❌ Not Implemented

Add `.recordStats()` to CacheConfig for Micrometer metrics.

```java
Caffeine.newBuilder()
    .recordStats()  // Enable statistics
    .expireAfterWrite(...)
```

#### 14. Add Micrometer Metrics for HTTP Client
**Source**: PR #5
**Category**: Observability

Track request counts per operation, error rates, and response times.

#### 15. Add Cache Eviction Logging
**Source**: PR #11
**Category**: Observability

Currently only "Cache MISS" is logged. Add logging for evictions.

### Testing Enhancements

#### 16. Test for Concurrent Access
**Source**: PR #11
**Category**: Testing

Verify cache thread safety under concurrent load.

#### 17. Test for TTL Expiration
**Source**: PR #11
**Category**: Testing

Use Caffeine's `Ticker` for time-based testing.

#### 18. Test searchByName Uses Cached Data
**Source**: PR #11
**Category**: Testing

Verify that `searchByName()` benefits from caching.

#### 19. Null Salary Edge Case Tests
**Source**: PR #6
**Category**: Testing

Test `getHighestSalary` and `getTopTenHighestEarningNames` with null salaries.

#### 20. Validation Boundary Tests
**Source**: PR #8
**Category**: Testing

Test age = 16, age = 75, salary = 1 (boundary values).

#### 21. Malformed JSON Response Test
**Source**: PR #5
**Category**: Testing

Test handling of malformed JSON from external API.

### Code Quality

#### 22. Return All Validation Errors
**Source**: PR #7
**Category**: Code Quality

Currently only returns first field error. Consider returning all.

#### 23. Standardize on SLF4J
**Source**: PR #8
**Category**: Code Quality

Mixing Log4j2 and SLF4J. Use SLF4J facade throughout.

#### 24. Use Collectors.joining for Validation Errors
**Source**: PR #8
**Category**: Code Quality

```java
// Before
.reduce((a, b) -> a + ", " + b).orElse("Validation failed")

// After
.collect(Collectors.joining(", "))
```

### Documentation & DX

#### 25. Add OpenAPI/Swagger Documentation
**Source**: Task Master #11
**Category**: Documentation
**Complexity**: 3 points

Integrate springdoc-openapi to provide automatic API documentation with interactive Swagger UI:

- Add `springdoc-openapi-starter-webmvc-ui` dependency
- Create `OpenApiConfig` with API metadata
- Add `@Operation` and `@ApiResponse` annotations to controller
- Add `@Schema` annotations to domain models with examples
- Accessible at `/swagger-ui.html` and `/api-docs`

**Why Medium**: Improves evaluator experience and API discoverability for coding challenge review.

#### 26. Add Spring Boot Actuator Health Endpoints
**Source**: Task Master #12
**Category**: Observability
**Complexity**: 3 points

Add production-readiness features with health checks:

- Add `spring-boot-starter-actuator` dependency
- Expose `/actuator/health` and `/actuator/info` endpoints
- Create custom `MockServerHealthIndicator` to check downstream connectivity
- Configure health check details visibility

**Why Medium**: Demonstrates production-readiness practices for coding challenge.

#### 27. Document Race Condition in deleteById
**Source**: PR #6
**Category**: Documentation

Time-of-check to time-of-use issue between find and delete operations.

#### 28. Document Why 0 is Default for Highest Salary
**Source**: PR #6
**Category**: Documentation

Clarify business requirement for empty employee list case.

---

## Low Priority / Future Enhancements

These items improve code quality but are not blocking issues.

### Code Organization

- Extract magic number `10` to `TOP_EARNERS_LIMIT` constant
- Extract cache key `'all'` to constant
- Extract cache name `"employees"` to prevent drift
- Add `@DisplayName` annotations for test readability
- Use `@Nested` classes for test organization
- Consider `@Slf4j` Lombok annotation

### Testing Improvements

- Use parameterized tests for similar test patterns
- Create test data builder pattern
- Add test fixtures directory structure
- Consider property-based testing with jqwik

### Logging Enhancements

- Use structured logging with key-value pairs
- Configure log pattern for MDC correlation ID
- Add null safety for MDC.get("correlationId")

### HTTP Semantics

- Consider 204 No Content for DELETE operations
- Add path field to ErrorResponse for debugging

### Security Considerations

- Add input length validation for search strings
- Consider rate limiting at API layer
- Log sanitization for user input

### Performance

- Consider cache warming on startup
- Remove misleading `@Retryable` on `findById`
- Add explicit timeout to `.block()` calls

---

## Implementation Notes

### Already Implemented (Removed from List)

The following suggestions from PR reviews have already been addressed:

- ✅ Make `webClient` field final (PR #9)
- ✅ Simplify CGLIB proxy constructor (PR #9)
- ✅ Return 201 Created for POST (PR #8)
- ✅ Add ServiceUnavailableException handler (PR #7)
- ✅ Add correlation ID propagation tests (PR #9)
- ✅ Use WireMock dynamic port (PR #9)
- ✅ Add timeout behavior test (PR #9)
- ✅ Add concurrent request test (PR #9)
- ✅ Add cache eviction verification test (PR #9)
- ✅ Add malformed JSON handling tests (PR #9)
- ✅ Add delete race condition test (PR #9)
- ✅ Add 5xx error retry support (PR #14)

### Suggested Implementation Order

1. **Critical (8+ points)**: Circuit breaker *(5xx retry support now complete)*
2. **High Priority (5-8 points)**: Testing gaps, Locale.ROOT fix, error handling
3. **Medium Priority (3-5 points)**: Observability, remaining tests, documentation
4. **Backlog (1-2 points)**: Low priority items as capacity allows

---

## Contributing

When addressing items from this list:

1. Create a branch: `fix/improvement-name` or `feat/feature-name`
2. Reference this document in PR description
3. Mark item as completed with PR number
4. Update this document to remove addressed items

---

*This document is auto-generated from PR review analysis. Last analyzed PRs: #5, #6, #7, #8, #9, #11*
