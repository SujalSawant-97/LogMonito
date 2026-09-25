# Spring Boot Enterprise Error Diagnostics & Resolution Knowledge Base

> **Target Audience**: SREs, DevOps, Java Backend Engineers, and AI RAG Systems  
> **Framework Target**: Spring Boot 3.x / Java 21  
> **Metadata Format**: Semantic Chunks optimized for Embedding & Vector Database Ingestion  

---

## 0. Architecture: Global Exception Handling (RFC 9457 / ProblemDetail)

In modern Spring Boot 3, error responses should be standardized using RFC 9457 `ProblemDetail`. Below is the central `@RestControllerAdvice` that catches and translates domain and system exceptions before returning structured HTTP responses.

```java
package com.example.common.exception;

import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnhandledException(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal error occurred.");
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("https://api.logmonito.com/errors/internal-server-error"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "One or more validation constraints failed.");
        problem.setTitle("Validation Failure");
        problem.setType(URI.create("https://api.logmonito.com/errors/validation-error"));

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err -> 
            fieldErrors.put(err.getField(), err.getDefaultMessage())
        );
        problem.setProperty("fieldErrors", fieldErrors);
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(status).body(problem);
    }
}
```

---

## 1. Application Startup & Context Lifecycle Errors

### ERR-START-001: NoSuchBeanDefinitionException
* **Category**: Startup & Dependency Injection
* **Severity**: CRITICAL / FATAL (Application fails to start)
* **Log Signature**:
  ```text
  org.springframework.beans.factory.NoSuchBeanDefinitionException: No qualifying bean of type 'com.example.service.PaymentService' available
  Action: Consider defining a bean of type 'com.example.service.PaymentService' in your configuration.
  ```
* **Root Cause**: Spring's `ApplicationContext` could not find any bean implementing or extending the requested type for injection into a constructor, field, or `@Bean` method.
* **Common Triggers**:
  1. Missing component annotation (`@Service`, `@Component`, `@Repository`) on the target class.
  2. Component scanning package mismatch (e.g. class is outside the `@SpringBootApplication` base package).
  3. Condition failure on conditional beans (e.g. `@ConditionalOnProperty` evaluated to false).
* **Remediation**:
  1. Verify the class has `@Service` or `@Component`.
  2. Verify the class is in a sub-package of the main class, or add `@ComponentScan("com.example")`.
  3. If created conditionally, verify `application.yml` has the required property set.

---

### ERR-START-002: NoUniqueBeanDefinitionException
* **Category**: Startup & Dependency Injection
* **Severity**: CRITICAL / FATAL
* **Log Signature**:
  ```text
  org.springframework.beans.factory.NoUniqueBeanDefinitionException: No qualifying bean of type 'com.example.NotificationService' available: expected single matching bean but found 2: emailNotificationService,smsNotificationService
  ```
* **Root Cause**: An injection point requested an interface or abstract class, but multiple concrete beans were registered without clarification on which one should be prioritized.
* **Remediation**:
  1. Add `@Qualifier("emailNotificationService")` to the injection site.
  2. Add `@Primary` to the default implementation bean.
  3. Use `@ConditionalOnProperty` to activate only one implementation per environment.

---

### ERR-START-003: BeanCurrentlyInCreationException (Circular Dependency)
* **Category**: Startup & Lifecycle
* **Severity**: CRITICAL / FATAL
* **Log Signature**:
  ```text
  The dependencies of some of the beans in the application context form a cycle:
  ┌─────┐
  |  serviceA (field private com.example.ServiceB com.example.ServiceA.serviceB)
  ↑     ↓
  |  serviceB (field private com.example.ServiceA com.example.ServiceB.serviceA)
  └─────┘
  ```
* **Root Cause**: Two or more beans mutually depend on each other during construction, causing an infinite instantiation loop. Spring Boot 2.6+ forbids circular references by default.
* **Remediation**:
  1. **Architectural Refactoring (Best)**: Extract shared logic into a third service (`ServiceC`) or use Spring Application Events (`ApplicationEventPublisher`).
  2. **Lazy Initialization**: Annotate one injection point with `@Lazy`:
     ```java
     public ServiceA(@Lazy ServiceB serviceB) { this.serviceB = serviceB; }
     ```
  3. **Config Override (Not recommended for prod)**: `spring.main.allow-circular-references=true`.

---

### ERR-START-004: PortInUseException
* **Category**: Startup / Web Server
* **Severity**: CRITICAL / FATAL
* **Log Signature**:
  ```text
  org.springframework.boot.web.server.PortInUseException: Port 8080 was already in use.
  Action: Identify and stop the process that's listening on port 8080 or configure this application to listen on another port.
  ```
* **Root Cause**: The embedded server (Tomcat, Netty, Undertow) failed to bind to the specified port because another process has already opened it.
* **Remediation**:
  1. Change the port in `application.properties`: `server.port=8085` or dynamic `server.port=0`.
  2. On Windows: `netstat -ano | findstr :8080`, then `taskkill /PID <PID> /F`.
  3. On Linux/Mac: `lsof -i :8080`, then `kill -9 <PID>`.

---

### ERR-START-005: ConfigurationPropertiesBindException
* **Category**: Startup / Configuration
* **Severity**: CRITICAL / FATAL
* **Log Signature**:
  ```text
  org.springframework.boot.context.properties.ConfigurationPropertiesBindException: Error creating bean with name 'monitor-com.example.monitor.MonitorProperties': Could not bind properties to 'MonitorProperties' : prefix=monitor
  ```
* **Root Cause**: A property in `application.yml` or environment variables cannot be coerced into the target field type (e.g. passing a string `"abc"` to an `Integer` or `Duration` field).
* **Remediation**:
  1. Check property types in `@ConfigurationProperties`.
  2. Ensure appropriate format for duration (`10s`, `5m`) or memory size (`500MB`).
  3. Ensure standard getters/setters or immutable record constructors exist.

---

## 2. Web, REST & Request Validation Errors

### ERR-WEB-001: MethodArgumentNotValidException
* **Category**: REST / Validation
* **Severity**: WARN (HTTP 400 Bad Request)
* **Log Signature**:
  ```text
  Resolved [org.springframework.web.bind.MethodArgumentNotValidException: Validation failed for argument [0] in public org.springframework.http.ResponseEntity...
  Field error in object 'userDto' on field 'email': rejected value [invalid-email]; default message [must be a well-formed email address]
  ```
* **Root Cause**: An incoming request payload failed validation annotations (`@NotNull`, `@NotBlank`, `@Email`, `@Min`, `@Size`) placed on DTO fields annotated with `@Valid`.
* **Remediation**:
  1. Catch via `ResponseEntityExceptionHandler.handleMethodArgumentNotValid` in `@RestControllerAdvice`.
  2. Extract all `FieldError` instances into a structured map `Map<String, String>`.
  3. Return HTTP 400 Bad Request with RFC 9457 `ProblemDetail`.

---

### ERR-WEB-002: HttpMessageNotReadableException
* **Category**: REST / Serialization
* **Severity**: WARN (HTTP 400 Bad Request)
* **Log Signature**:
  ```text
  org.springframework.http.converter.HttpMessageNotReadableException: JSON parse error: Cannot deserialize value of type `java.time.Instant` from String "2026-99-99": Failed to deserialize
  ```
* **Root Cause**: Jackson failed to deserialize the JSON request body. Triggers include malformed JSON syntax, invalid date/time formats, or unknown Enum constant values.
* **Remediation**:
  1. Catch `HttpMessageNotReadableException` in `@RestControllerAdvice`.
  2. For dates, configure `@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX")`.
  3. For enums, use `@JsonCreator` or `DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL`.

---

### ERR-WEB-003: MissingRequestHeaderException / MissingServletRequestParameterException
* **Category**: REST / Parameters
* **Severity**: WARN (HTTP 400 Bad Request)
* **Log Signature**:
  ```text
  org.springframework.web.bind.MissingRequestHeaderException: Required request header 'X-API-KEY' for method parameter type String is not present
  ```
* **Root Cause**: A controller method marked a header or query parameter as required (`@RequestHeader("X-API-KEY")` without `required = false`), and the caller omitted it.
* **Remediation**:
  1. Validate client sending logic (ensure headers are passed).
  2. If optional, mark `@RequestHeader(value = "X-API-KEY", required = false)`.
  3. Map in `@ExceptionHandler` to return friendly 400 message indicating the missing key.

---

### ERR-WEB-004: HttpRequestMethodNotSupportedException
* **Category**: REST / Routing
* **Severity**: WARN (HTTP 405 Method Not Allowed)
* **Log Signature**:
  ```text
  org.springframework.web.HttpRequestMethodNotSupportedException: Request method 'POST' is not supported
  ```
* **Root Cause**: The client sent an HTTP verb (e.g. POST) to a URL endpoint that only mapped GET or PUT.
* **Remediation**:
  1. Verify controller mappings (`@GetMapping` vs `@PostMapping`).
  2. Return standard 405 response with `Allow` header listing permitted methods.

---

### ERR-WEB-005: NoResourceFoundException / NoHandlerFoundException
* **Category**: REST / Routing
* **Severity**: WARN (HTTP 404 Not Found)
* **Log Signature**:
  ```text
  org.springframework.web.servlet.resource.NoResourceFoundException: No static resource api/v1/unknown.
  ```
* **Root Cause**: In Spring Boot 3.2+, requests to unmapped paths produce `NoResourceFoundException` instead of falling back to default error controller.
* **Remediation**:
  1. Handle in `@RestControllerAdvice` to produce clean RFC 9457 404 ProblemDetail.
  2. Verify gateway routing prefixes (`StripPrefix` or context path).

---

## 3. Database, Persistence & Transaction Errors

### ERR-DB-001: CannotGetJdbcConnectionException / HikariPool Timeout
* **Category**: Database / Connection Pool
* **Severity**: ERROR / CRITICAL (HTTP 500 / 503)
* **Log Signature**:
  ```text
  org.springframework.jdbc.CannotGetJdbcConnectionException: Failed to obtain JDBC Connection
  Caused by: java.sql.SQLTransientConnectionException: HikariPool-1 - Connection is not available, request timed out after 30000ms.
  ```
* **Root Cause**: All database connections in the Hikari connection pool are in use, and new threads waited longer than `connectionTimeout` (default 30s) without acquiring a connection.
* **Common Triggers**:
  1. Slow SQL queries holding connections open.
  2. Long-running external HTTP calls made inside a `@Transactional` block.
  3. Connection pool size too small for concurrent traffic.
  4. Connection leak (connections opened without being closed).
* **Remediation**:
  1. Move non-database work (e.g. external REST/Kafka calls) outside `@Transactional`.
  2. Increase pool size: `spring.datasource.hikari.maximum-pool-size=20`.
  3. Optimize slow queries with proper indexes.
  4. Enable leak detection: `spring.datasource.hikari.leak-detection-threshold=2000` (2s).

---

### ERR-DB-002: MongoSocketOpenException / MongoTimeoutException
* **Category**: NoSQL / MongoDB
* **Severity**: ERROR / CRITICAL
* **Log Signature**:
  ```text
  com.mongodb.MongoSocketOpenException: Exception opening socket
  Caused by: java.net.ConnectException: Connection refused: no further information
  com.mongodb.MongoTimeoutException: Timed out after 30000 ms while waiting for a server that matches ReadPreferenceServerSelector
  ```
* **Root Cause**: The Spring Data MongoDB driver cannot reach the MongoDB instance or replica set within the connection timeout.
* **Remediation**:
  1. Check if MongoDB daemon is running (`mongod` / Docker container).
  2. Verify `spring.data.mongodb.uri` host, port, and authentication credentials.
  3. In Docker, ensure services share the same Docker network.

---

### ERR-DB-003: DataIntegrityViolationException / DuplicateKeyException
* **Category**: Database / Constraints
* **Severity**: WARN / ERROR (HTTP 409 Conflict)
* **Log Signature**:
  ```text
  org.springframework.dao.DataIntegrityViolationException: could not execute statement [ERROR: duplicate key value violates unique constraint "idx_users_email"]
  // Or in MongoDB:
  com.mongodb.MongoServerException: E11000 duplicate key error collection: ai_monitoring_db.client_applications index: apiKey dup key: { apiKey: "mon_123" }
  ```
* **Root Cause**: Insertion or update violated a unique constraint, foreign key check, or NOT NULL column.
* **Remediation**:
  1. Catch `DataIntegrityViolationException` or `DuplicateKeyException` in `@RestControllerAdvice`.
  2. Return HTTP 409 Conflict with a message specifying which field is duplicated.
  3. Implement upsert or existence checks (`existsByApiKey()`) before saving.

---

### ERR-DB-004: LazyInitializationException
* **Category**: JPA / Hibernate
* **Severity**: ERROR (HTTP 500)
* **Log Signature**:
  ```text
  org.hibernate.LazyInitializationException: could not initialize proxy [com.example.entity.Order#1] - no Session
  ```
* **Root Cause**: Code attempted to access a lazy-loaded relationship (`fetch = FetchType.LAZY`) outside an active Hibernate session/transaction (e.g. during Jackson serialization in Controller).
* **Remediation**:
  1. **Best**: Use DTO projections and fetch joins (`JOIN FETCH o.items`) in repository queries.
  2. Use `@EntityGraph` to explicitly specify eager loading per query.
  3. Keep `@Transactional(readOnly = true)` on the Service method.
  4. Never use `spring.jpa.open-in-view=true` in production (anti-pattern).

---

### ERR-DB-005: OptimisticLockingFailureException
* **Category**: Concurrency / JPA
* **Severity**: WARN (HTTP 409 Conflict)
* **Log Signature**:
  ```text
  org.springframework.dao.OptimisticLockingFailureException: Row was updated or deleted by another transaction (or unsaved-value mapping was incorrect)
  ```
* **Root Cause**: Two concurrent transactions read the same entity with `@Version` and attempted to save changes. The second transaction failed because the version was incremented by the first.
* **Remediation**:
  1. Use Spring Retry: `@Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))`.
  2. Inform client with HTTP 409 Conflict so the UI can refresh and retry.

---

## 4. Security & Authentication Errors (Spring Security)

### ERR-SEC-001: BadCredentialsException
* **Category**: Security / Authentication
* **Severity**: WARN (HTTP 401 Unauthorized)
* **Log Signature**:
  ```text
  org.springframework.security.authentication.BadCredentialsException: Bad credentials
  ```
* **Root Cause**: Provided username/password, API key, or credential hash did not match the stored credentials.
* **Remediation**:
  1. Implement a custom `AuthenticationEntryPoint` to return a JSON ProblemDetail response.
  2. Never leak whether the username vs. password was incorrect (prevents user enumeration).

---

### ERR-SEC-002: AccessDeniedException
* **Category**: Security / Authorization
* **Severity**: WARN (HTTP 403 Forbidden)
* **Log Signature**:
  ```text
  org.springframework.security.access.AccessDeniedException: Access Denied
  ```
* **Root Cause**: The user is successfully authenticated, but lacks required roles or permissions (e.g. `@PreAuthorize("hasRole('ADMIN')")`).
* **Remediation**:
  1. Implement a custom `AccessDeniedHandler`.
  2. Return HTTP 403 Forbidden with clear permission context.

---

### ERR-SEC-003: JwtException / ExpiredJwtException
* **Category**: Security / Tokens
* **Severity**: WARN (HTTP 401 Unauthorized)
* **Log Signature**:
  ```text
  io.jsonwebtoken.ExpiredJwtException: JWT expired at 2026-09-15T12:00:00Z. Current time: 2026-09-15T12:05:00Z
  ```
* **Root Cause**: Bearer token signature validation failed, token structure is malformed, or token has expired.
* **Remediation**:
  1. In JWT authentication filter, catch `ExpiredJwtException` and write HTTP 401 response with header `WWW-Authenticate: Bearer error="invalid_token", error_description="The token has expired"`.
  2. Provide refresh token endpoint (`/api/v1/auth/refresh`).

---

## 5. Messaging & Event-Driven Errors (Apache Kafka)

### ERR-MSG-001: ListenerExecutionFailedException / RecordDeserializationException
* **Category**: Messaging / Kafka
* **Severity**: ERROR (Consumer Loop Stalled / Deadlock)
* **Log Signature**:
  ```text
  org.springframework.kafka.listener.ListenerExecutionFailedException: Listener failed; nested exception is org.apache.kafka.common.errors.RecordDeserializationException: Error deserializing key/value for partition raw-logs-topic-0 at offset 123
  Caused by: java.lang.ClassNotFoundException: com.example.IngestionService.dto.LogPayload
  ```
* **Root Cause**: Producer serialized an object using `JsonSerializer` with type headers, and the consumer cannot locate the exact class name, or JSON is malformed.
* **Remediation**:
  1. Configure `ErrorHandlingDeserializer` in Spring Kafka:
     ```properties
     spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
     spring.kafka.consumer.properties.spring.deserializer.value.delegate.class=org.springframework.kafka.support.serializer.JsonDeserializer
     ```
  2. Map types using `spring.json.type.mapping`:
     ```properties
     spring.kafka.consumer.properties.spring.json.type.mapping=com.example.IngestionService.dto.LogPayload:com.example.StorageService.dto.LogPayload
     ```
  3. Configure a Dead-Letter Topic (DLT) via `DefaultErrorHandler`.

---

### ERR-MSG-002: Kafka TimeoutException / DisconnectException
* **Category**: Messaging / Network
* **Severity**: ERROR
* **Log Signature**:
  ```text
  org.apache.kafka.common.errors.TimeoutException: Topic raw-logs-topic not present in metadata after 60000 ms.
  org.apache.kafka.clients.NetworkClient: [Producer clientId=producer-1] Connection to node -1 (localhost/127.0.0.1:9092) could not be established. Broker may not be available.
  ```
* **Root Cause**: Producer or consumer cannot connect to Kafka brokers defined in `bootstrap-servers`, or topic does not exist and `auto.create.topics.enable=false`.
* **Remediation**:
  1. Verify Kafka broker status.
  2. Set producer delivery timeout: `spring.kafka.producer.properties.delivery.timeout.ms=15000`.
  3. Pre-create required topics using a `NewTopic` `@Bean` definition in a configuration class.

---

## 6. Microservices, HTTP Client & Resilience Errors

### ERR-NET-001: ResourceAccessException / ConnectException
* **Category**: HTTP Client / Networking
* **Severity**: ERROR (HTTP 502 / 504)
* **Log Signature**:
  ```text
  org.springframework.web.client.ResourceAccessException: I/O error on POST request for "http://localhost:8082/api/v1/auth/validate": Connection refused
  ```
* **Root Cause**: Calling an external downstream REST API via `RestTemplate` or `RestClient`, and the target server is down, unreachable, or refused connection.
* **Remediation**:
  1. Configure explicit connect and read timeouts:
     ```java
     SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
     factory.setConnectTimeout(Duration.ofSeconds(3));
     factory.setReadTimeout(Duration.ofSeconds(5));
     RestTemplate restTemplate = new RestTemplate(factory);
     ```
  2. Implement Resilience4j Retry & Circuit Breaker.

---

### ERR-NET-002: CallNotPermittedException (Circuit Breaker OPEN)
* **Category**: Resilience / Fault Tolerance
* **Severity**: WARN / ERROR
* **Log Signature**:
  ```text
  io.github.resilience4j.circuitbreaker.CallNotPermittedException: CircuitBreaker 'externalService' is OPEN and does not permit further calls
  ```
* **Root Cause**: Downstream failure rate exceeded the configured threshold (e.g. 50%), tripping the circuit breaker to prevent cascading system failure.
* **Remediation**:
  1. Provide a fallback method using `@CircuitBreaker(name = "externalService", fallbackMethod = "fallbackResponse")`.
  2. Return cached data or degraded response while downstream recovers.

---

## 7. JVM, Memory & Runtime Errors

### ERR-JVM-001: OutOfMemoryError: Java heap space
* **Category**: JVM / Resource Exhaustion
* **Severity**: FATAL
* **Log Signature**:
  ```text
  java.lang.OutOfMemoryError: Java heap space
  Dumping heap to java_pid1234.hprof ...
  ```
* **Root Cause**: JVM cannot allocate an object because available heap memory is exhausted and garbage collection cannot reclaim space.
* **Common Triggers**:
  1. Large queries fetching thousands/millions of records without pagination (`findAll()`).
  2. Static memory leaks (unbounded maps, uncancelled listeners).
  3. Heavy in-memory caching without size or eviction limits.
* **Remediation**:
  1. Always enforce database pagination (`Pageable`, `PageRequest.of(0, 50)`).
  2. Stream large payloads instead of buffering in memory.
  3. Increase container heap limits (`-Xmx2g -Xms2g`).
  4. Enable heap dump on OOM: `-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/dumps/heap.hprof` and analyze via Eclipse Memory Analyzer (MAT).

---

### ERR-JVM-002: OutOfMemoryError: Metaspace
* **Category**: JVM / Class Loading
* **Severity**: FATAL
* **Log Signature**:
  ```text
  java.lang.OutOfMemoryError: Metaspace
  ```
* **Root Cause**: The Metaspace region (storing class metadata, reflection bytecode, dynamic proxies) has reached `-XX:MaxMetaspaceSize`.
* **Common Triggers**: Heavy runtime bytecode generation (CGLIB, reflection frameworks) or repeated hot reloading.
* **Remediation**:
  1. Increase metaspace: `-XX:MaxMetaspaceSize=512m`.
  2. Investigate classloader leaks.

---

### ERR-JVM-003: StackOverflowError
* **Category**: JVM / Thread Stack
* **Severity**: FATAL
* **Log Signature**:
  ```text
  java.lang.StackOverflowError: null
  at com.example.service.OrderService.calculateDiscount(OrderService.java:42)
  at com.example.service.OrderService.calculateDiscount(OrderService.java:42)
  ```
* **Root Cause**: Infinite recursion in method calls or circular `toString()`, `hashCode()`, or `equals()` generated by Lombok on bidirectional JPA relationships (`@OneToMany` $\leftrightarrow$ `@ManyToOne`).
* **Remediation**:
  1. Add `@ToString.Exclude` and `@EqualsAndHashCode.Exclude` on the child side of bidirectional JPA relationships.
  2. Verify base conditions in recursive algorithms.

---

## 8. Summary Diagnostic Matrix for AI / RAG Systems

| Error Code | Exception Class | HTTP Status | Primary Root Cause | First Diagnostic Step |
| :--- | :--- | :--- | :--- | :--- |
| **ERR-START-001** | `NoSuchBeanDefinitionException` | N/A (Startup) | Missing annotation or package mismatch | Check `@ComponentScan` & `@Service` |
| **ERR-START-003** | `BeanCurrentlyInCreationException` | N/A (Startup) | Circular dependency between beans | Refactor or add `@Lazy` |
| **ERR-START-004** | `PortInUseException` | N/A (Startup) | Port already bound by another process | Kill existing process or set `server.port` |
| **ERR-WEB-001** | `MethodArgumentNotValidException`| 400 Bad Request | Jakarta `@Valid` constraint failure | Inspect `fieldErrors` map in response |
| **ERR-WEB-002** | `HttpMessageNotReadableException` | 400 Bad Request | Malformed JSON / unparseable Enum | Check payload syntax & date formats |
| **ERR-DB-001** | `CannotGetJdbcConnectionException`| 500 / 503 | Hikari pool exhausted / slow queries | Check active queries & connection leaks |
| **ERR-DB-002** | `MongoSocketOpenException` | 500 / 503 | MongoDB daemon unreachable | Check mongo container & URI |
| **ERR-DB-003** | `DataIntegrityViolationException`| 409 Conflict | Unique key or FK violation | Inspect constraint name & duplicate key |
| **ERR-DB-004** | `LazyInitializationException` | 500 Internal Error| Lazy relation loaded without Session | Use DTO projection / `JOIN FETCH` |
| **ERR-SEC-001** | `BadCredentialsException` | 401 Unauthorized | Invalid credentials or API key | Validate client credentials |
| **ERR-MSG-001** | `RecordDeserializationException` | N/A (Consumer) | Kafka serializer type mismatch | Set `spring.json.type.mapping` |
| **ERR-NET-001** | `ResourceAccessException` | 502 / 504 | Downstream HTTP service unreachable | Configure timeouts & Circuit Breaker |
| **ERR-JVM-001** | `OutOfMemoryError` | N/A (Crash) | Heap exhaustion / memory leak | Analyze heap dump & enforce pagination |
