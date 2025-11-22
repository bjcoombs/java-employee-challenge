# Future Improvements & Technical Debt

This document consolidates unimplemented suggestions from PR code reviews into actionable items for future development.

> **Last Updated**: Generated from analysis of PRs #5, #6, #7, #8, #9, and #11

---

## Summary

| Priority | Count | Status |
|----------|-------|--------|
| Critical | 3 | Needs immediate attention |
| High | 10 | Should address before production |
| Medium | 16 | Address as capacity allows |
| Low | 20+ | Nice-to-have |

---

## Critical Priority

Items that could cause production issues.

### Circuit Breaker Pattern

**Source**: PR #5 | **Category**: Resilience

Add Resilience4j circuit breaker to prevent cascade failures when the downstream employee service is consistently failing.

```java
@CircuitBreaker(name = "employeeService", fallbackMethod = "fallbackFindAll")
public List<Employee> findAll() { ... }
```

**Why Critical**: Without circuit breaker, a failing downstream service can exhaust thread pools and cause cascading failures.

---

### Bulkhead Pattern for Request Queueing

**Source**: Rate limiting analysis | **Category**: Resilience

Add a semaphore/bulkhead to prevent request pile-up when the mock server is rate-limiting.

```java
@Bulkhead(name = "employeeService", type = Bulkhead.Type.SEMAPHORE)
@Retryable(...)
public List<Employee> getAllEmployees() { ... }
```

**Why Critical**: The mock server's aggressive rate limiting (30-90s) combined with exponential backoff means multiple concurrent requests could each wait 150+ seconds.

---

### 5xx Error Retry Support (Deliberately Omitted)

**Source**: PR #5 | **Category**: Resilience

Currently only retries on 429. Could also retry on transient 5xx errors (502, 503, 504).

**Why Omitted**: See README "Why NOT Retry on 5xx Server Errors" section. For this challenge, 5xx errors indicate server bugs rather than rate limiting.

---

## High Priority

Items to address before production deployment.

### Testing Gaps

| Item | Source | Description |
|------|--------|-------------|
| Integration Tests with MockMvc | PR #8 | Use `@WebMvcTest` to verify full request/response cycle |
| Correlation ID Filter Tests | PR #8 | Test header presence, blank values, MDC cleanup |
| Empty Validation Errors Test | PR #7 | Test fallback branch when no field errors |
| Retry Behavior Integration Test | PR #5 | Verify 429 → 429 → success makes exactly 3 requests |
| Timeout Scenario Tests | PR #5 | Test connection and read timeout with WireMock delays |

### Architecture & Code Quality

| Item | Source | Description |
|------|--------|-------------|
| Use Locale.ROOT | PR #6 | `toLowerCase()` without Locale causes issues (Turkish 'i' → 'İ'). File: `EmployeeService.java:31` |
| Filter Null Names | PR #6 | `getTopTenHighestEarningNames` could return null names |
| Extract Duplicated Error Handling | PR #5 | Error handling duplicated across adapter methods |
| Make Retry-After Configurable | PR #7 | `"5"` seconds hardcoded, externalize to config |
| Catch-All Exception Handler | PR #8 | Prevent leaking internal details for unexpected exceptions |

---

## Medium Priority

Items to address in upcoming development cycles.

### Observability & Monitoring

| Item | Source | Description |
|------|--------|-------------|
| Enable Cache Metrics | PR #11 | Add `.recordStats()` to CacheConfig for Micrometer |
| Micrometer Metrics for HTTP Client | PR #5 | Track request counts, error rates, response times |
| Cache Eviction Logging | PR #11 | Currently only "Cache MISS" is logged |

### Testing Enhancements

| Item | Source | Description |
|------|--------|-------------|
| Concurrent Access Test | PR #11 | Verify cache thread safety under load |
| TTL Expiration Test | PR #11 | Use Caffeine's `Ticker` for time-based testing |
| searchByName Cache Test | PR #11 | Verify method benefits from caching |
| Null Salary Edge Cases | PR #6 | Test with null salaries in highest salary methods |
| Validation Boundary Tests | PR #8 | Test age = 16/75, salary = 1 |
| Malformed JSON Response Test | PR #5 | Test handling of malformed JSON from API |

### Code Quality

| Item | Source | Description |
|------|--------|-------------|
| Return All Validation Errors | PR #7 | Currently only returns first field error |
| Standardize on SLF4J | PR #8 | Mixing Log4j2 and SLF4J |
| Use Collectors.joining | PR #8 | Replace `.reduce()` with `.collect(Collectors.joining(", "))` |

### Documentation

| Item | Source | Description |
|------|--------|-------------|
| OpenAPI/Swagger | Task Master #11 | Add springdoc-openapi for API documentation |
| Actuator Health Endpoints | Task Master #12 | Add `/actuator/health` with custom health indicator |
| Document deleteById Race Condition | PR #6 | Time-of-check to time-of-use issue |
| Document Default Salary Behavior | PR #6 | Clarify why 0 is default for empty list |

---

## Low Priority

Nice-to-have improvements.

### Code Organization
- Extract magic number `10` to `TOP_EARNERS_LIMIT` constant
- Extract cache key `'all'` and cache name `"employees"` to constants
- Add `@DisplayName` and `@Nested` for test organization
- Consider `@Slf4j` Lombok annotation

### Testing
- Use parameterized tests for similar patterns
- Create test data builder pattern
- Consider property-based testing with jqwik

### Logging
- Use structured logging with key-value pairs
- Configure log pattern for MDC correlation ID
- Add null safety for MDC.get("correlationId")

### HTTP Semantics
- Consider 204 No Content for DELETE
- Add path field to ErrorResponse

### Security
- Add input length validation for search strings
- Consider rate limiting at API layer
- Log sanitization for user input

### Performance
- Consider cache warming on startup
- Remove misleading `@Retryable` on `findById`
- Add explicit timeout to `.block()` calls

---

## Already Implemented

These items from PR reviews have been addressed:

- Make `webClient` field final (PR #9)
- Simplify CGLIB proxy constructor (PR #9)
- Return 201 Created for POST (PR #8)
- Add ServiceUnavailableException handler (PR #7)
- Add correlation ID propagation tests (PR #9)
- Use WireMock dynamic port (PR #9)
- Add timeout behavior test (PR #9)
- Add concurrent request test (PR #9)
- Add cache eviction verification test (PR #9)
- Add malformed JSON handling tests (PR #9)
- Add delete race condition test (PR #9)

---

## Contributing

When addressing items from this list:

1. Create a branch: `fix/improvement-name` or `feat/feature-name`
2. Reference this document in PR description
3. Update this document to remove addressed items

---

*Generated from PR review analysis. Last analyzed PRs: #5, #6, #7, #8, #9, #11*
