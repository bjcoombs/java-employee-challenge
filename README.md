# ReliaQuest Coding Challenge - Solution

## Design Decisions

**This section explains what was built, what was deliberately omitted, and why.**

### Why Retry with Exponential Backoff

The mock server randomly rate-limits requests (429 responses). This is the core technical challenge of the assignment.

**Solution**: Implemented `@Retryable` with exponential backoff (3 attempts, 500ms → 1s → 2s).

**Why this is appropriate**: Rate limiting is explicitly mentioned in the requirements. Retry logic directly addresses the stated problem and demonstrates understanding of real-world resilience patterns.

### Why NOT Hexagonal Architecture

A hexagonal/ports-and-adapters architecture (separate Port interface + Adapter implementation) would add abstraction layers without benefit for this use case.

**What would justify hexagonal architecture**:
- Multiple data sources (database + external API)
- Need to swap implementations (test doubles, different vendors)
- Large team needing clear boundaries

**Why it's omitted here**:
- Single data source (one HTTP API)
- Service layer can call WebClient directly with no loss of clarity
- Tests use WireMock for HTTP-level mocking (industry standard)
- Fewer files = faster code review and easier understanding

**In production**: If this API needed to support multiple backends or required extensive unit testing with mocks, I would add the port/adapter abstraction.

### Why Consolidated Exception Hierarchy

The implementation uses 3 exception types:
- `EmployeeNotFoundException` - 404 for missing resources
- `TooManyRequestsException` - 429 for retry logic (needed for `@Retryable`)
- `EmployeeServiceException` - all other service errors with HTTP status

**Why not 5-6 separate exception types**: The `GlobalExceptionHandler` routes exceptions to HTTP responses. A single exception with an `HttpStatus` field achieves the same result with less code.

### Known Limitations

**getEmployeeById fetches all employees and filters**

The mock server doesn't provide a GET by ID endpoint. The implementation fetches all employees and filters client-side.

**In production**: Request a direct endpoint, or implement caching with proper invalidation.

**deleteEmployeeById has a race condition**

The mock server requires the employee name for deletion, but the controller receives an ID. The implementation:
1. Fetches the employee by ID
2. Deletes by name

Between steps 1 and 2, another process could delete or rename the employee.

**In production**: Request an ID-based delete endpoint, or implement optimistic locking.

### What Would Be Added in Production

| Feature | When Needed | Why Omitted |
|---------|-------------|-------------|
| Circuit breaker | When downstream failures cause cascading issues | Retry with timeout handles the mock server's rate limiting |
| OpenAPI/Swagger | When API is consumed by external teams | Self-documenting code sufficient for interview context |
| Metrics (Micrometer) | When operating at scale with monitoring | No observability stack to consume the metrics |
| Distributed tracing | When debugging spans multiple services | Single service, correlation IDs sufficient |

---

## Solution Overview

A REST API that consumes the Mock Employee API with emphasis on **appropriate scoping**, **resilience**, and **test coverage**.

### Architecture

```
EmployeeController → EmployeeService → WebClient → Mock Server
     (routing)         (business logic     (HTTP client)
                        + retry + cache)
```

The service layer handles:
- Business logic (search, filter, sort)
- HTTP client calls (WebClient)
- Retry logic (`@Retryable` with exponential backoff)
- Caching (`@Cacheable` for getAllEmployees)

### Resilience Features

**Rate Limiting Handling**
- **Retry**: 3 attempts with exponential backoff (500ms → 1s → 2s)
- **Caching**: In-memory cache for getAllEmployees (30s TTL)
- **Recovery**: Service unavailable (503) after retry exhaustion

**Error Handling**
- Correlation IDs in all log entries
- Structured JSON error responses
- Appropriate HTTP status codes (400, 404, 429, 500, 502, 503)

### All 7 Endpoints Implemented

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/employees` | GET | Returns all employees (cached) |
| `/employees/search/{searchString}` | GET | Case-insensitive name search |
| `/employees/{id}` | GET | Get employee by UUID |
| `/employees/highestSalary` | GET | Returns highest salary integer |
| `/employees/topTenHighestEarningEmployeeNames` | GET | Top 10 earners (names only) |
| `/employees` | POST | Create employee (returns 201) |
| `/employees/{id}` | DELETE | Delete by ID (returns name) |

### Testing Strategy

- **Integration tests**: WireMock-based tests verifying retry behavior, error handling, and full HTTP cycles
- **Unit tests**: Exception handler, controller routing, domain model validation

**Why WireMock instead of the Mock Server?**

The mock server randomly rate-limits requests, making tests flaky. WireMock provides deterministic control over responses for reliable CI.

---

## Getting Started

### Prerequisites

- Java 25+
- Gradle 9.2.1+ (wrapper included)

### Quick Start

```bash
# Check environment and optionally start services
./doctor.sh --start

# Or manually:
./gradlew server:bootRun  # Terminal 1: Start mock server
./gradlew api:bootRun     # Terminal 2: Start API
```

### Running Tests

```bash
./gradlew test            # All tests
./gradlew api:test        # API module only
```

### Code Formatting

```bash
./gradlew spotlessApply   # Apply Palantir Java Format
```

---

## Configuration

Key properties in `application.yml`:

```yaml
employee:
  client:
    base-url: http://localhost:8112/api/v1/employee
    connect-timeout: 5s
    read-timeout: 10s
    retry:
      max-attempts: 3
      delay: 500
      multiplier: 2.0
  cache:
    ttl-seconds: 30
```

---

## API Reference

### Mock Employee API (Port 8112)

The mock server generates random employee data on startup and randomly rate-limits requests.

See the original challenge documentation for endpoint details.
