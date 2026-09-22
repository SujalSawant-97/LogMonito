import re
from typing import Dict, Any, Optional

def extract_stack_trace_info(message: str) -> Dict[str, Any]:
    lines = message.strip().split("\n")
    primary_line = lines[0] if lines else message
    
    # Try to extract exception class
    exc_match = re.search(r'([a-zA-Z0-9_.]+(?:Exception|Error)):?\s*(.*)', primary_line)
    exc_type = exc_match.group(1) if exc_match else "RuntimeException"
    exc_detail = exc_match.group(2) if exc_match else primary_line
    
    # Look for client code location: at com.something.Class.method(Class.java:123)
    code_location = None
    for line in lines:
        cleaned = line.strip()
        if cleaned.startswith("at ") and not any(pkg in cleaned for pkg in ["org.springframework", "java.lang", "sun.", "jdk.", "org.apache.tomcat", "jakarta."]):
            loc_match = re.search(r'at\s+([a-zA-Z0-9_.$]+)\(([a-zA-Z0-9_.]+\.java:\d+)\)', cleaned)
            if loc_match:
                code_location = f"{loc_match.group(1)} ({loc_match.group(2)})"
                break
                
    return {
        "exception_type": exc_type,
        "detail": exc_detail.strip(),
        "code_location": code_location,
        "raw": message
    }

def diagnose_spring_error(message: str, app_name: Optional[str] = None) -> Dict[str, str]:
    """
    Analyzes a Spring Boot runtime exception and returns a structured 3-part diagnosis:
    - summary: What broke and which service was affected
    - cause: Exact technical root cause (line, timeout, missing bean, constraint)
    - solution: Actionable resolution with exact code or config snippet
    """
    info = extract_stack_trace_info(message)
    exc = info["exception_type"]
    detail = info["detail"]
    loc = info["code_location"]
    service_label = f" in '{app_name}'" if app_name else ""

    # 1. MongoDB Connection / Socket Failures
    if "MongoSocketOpenException" in message or "MongoTimeoutException" in message or "ServerSelectionTimeoutException" in message:
        return {
            "summary": f"Database Connectivity Failure{service_label}: MongoDB instance unreachable.",
            "cause": "The driver failed to establish a socket connection to MongoDB. Either MongoDB is stopped, or the application is pointing to an invalid host/port (e.g. using 'localhost' instead of 'host.docker.internal' inside a container).",
            "solution": "1. Ensure MongoDB service is running: `net start MongoDB` or verify port 27017.\n2. In application.properties/docker-compose, verify connection URI: `${SPRING_DATA_MONGODB_URI:mongodb://localhost:27017/ai_monitoring_db}`.\n3. If running inside Docker, configure host gateway: `host.docker.internal:27017`.",
            "insight": "### 📌 Error Summary\nDatabase Connectivity Failure: MongoDB instance is unreachable.\n\n### 🔍 Root Cause\nThe MongoDB Java driver timed out attempting to open a cluster connection socket. The target instance at 27017 did not respond.\n\n### 🛠️ Recommended Solution\nEnsure MongoDB is active on port 27017 and verify `spring.data.mongodb.uri` points to the correct network host."
        }

    # 2. HikariCP / Database Pool Exhaustion
    if "HikariPool" in message or "CannotGetJdbcConnectionException" in message or "Connection is not available, request timed out" in message:
        return {
            "summary": f"Database Connection Pool Starvation{service_label}: HikariCP timed out waiting for an idle connection.",
            "cause": "All database connections in the Hikari pool are actively held or leaked by uncommitted long-running transactions, exceeding `connection-timeout`.",
            "solution": "1. Check for unclosed transactions or missing `@Transactional(readOnly = true)` on read-only queries.\n2. In `application.properties`, increase pool capacity and leak detection:\n   ```properties\n   spring.datasource.hikari.maximum-pool-size=20\n   spring.datasource.hikari.connection-timeout=30000\n   spring.datasource.hikari.leak-detection-threshold=5000\n   ```",
            "insight": "### 📌 Error Summary\nHikariCP Connection Pool Exhaustion.\n\n### 🔍 Root Cause\nActive database pool was depleted. Available pool threads reached maximum limit and incoming queries timed out waiting for a connection.\n\n### 🛠️ Recommended Solution\nAdd `leak-detection-threshold=5000` to locate uncommitted transactions, and scale `maximum-pool-size` according to load."
        }

    # 3. Validation / Bad Request Constraints
    if "MethodArgumentNotValidException" in message or "ConstraintViolationException" in message or "Validation Failure" in message:
        return {
            "summary": f"Request Payload Validation Failure{service_label}.",
            "cause": f"One or more incoming HTTP request fields failed DTO `@Valid` constraints (e.g. `@NotNull`, `@NotBlank`, `@Size`). {detail}",
            "solution": "1. Verify client request payload matches the DTO schema.\n2. Implement a global `@RestControllerAdvice` handling `MethodArgumentNotValidException` to return structured RFC 9457 `ProblemDetail` with field-level errors.",
            "insight": "### 📌 Error Summary\nRequest Payload Validation Failure.\n\n### 🔍 Root Cause\nIncoming request body failed validation constraints on DTO fields.\n\n### 🛠️ Recommended Solution\nInspect request payload fields against DTO constraints and ensure RFC 9457 GlobalExceptionHandler formats field errors."
        }

    # 4. Null Pointer Exception
    if "NullPointerException" in message:
        loc_str = f" at `{loc}`" if loc else ""
        return {
            "summary": f"Null Pointer Dereference{service_label}{loc_str}.",
            "cause": f"An uninitialized object reference was accessed without a null check{loc_str}. Detail: {detail or 'Attempted to invoke method on a null reference'}.",
            "solution": f"1. Inspect `{loc or 'the stack trace'}` and wrap access with `Optional.ofNullable(...)` or `Objects.requireNonNull(...)`.\n2. Use `@NonNull` annotations or default initialized collections/fields.",
            "insight": f"### 📌 Error Summary\nNull Pointer Dereference{loc_str}.\n\n### 🔍 Root Cause\nApplication attempted to call a method or access an attribute of an object evaluated to null.\n\n### 🛠️ Recommended Solution\nAdd defensive null-guards or use Java `Optional` to safely unwrap the value before invocation."
        }

    # 5. Kafka Deserialization or Consumer Error
    if "DeserializationException" in message or "KafkaException" in message or "ListenerExecutionFailedException" in message:
        return {
            "summary": f"Kafka Message Ingestion / Deserialization Failure{service_label}.",
            "cause": "Consumer failed to deserialize message payload from the Kafka topic. Typically caused by mismatched package names between producer DTO and consumer DTO, or missing trusted packages.",
            "solution": "1. In consumer `application.properties`, configure trusted packages and type mappings:\n   ```properties\n   spring.kafka.consumer.properties.spring.json.trusted.packages=*\n   spring.kafka.consumer.properties.spring.json.type.mapping=com.producer.dto.Payload:com.consumer.dto.Payload\n   ```\n2. Implement an `ErrorHandlingDeserializer` to route poisonous pills to a Dead Letter Topic (DLT).",
            "insight": "### 📌 Error Summary\nKafka Consumer Deserialization Failure.\n\n### 🔍 Root Cause\nConsumer received a message on the topic whose Java class type does not match the expected consumer DTO or is not in trusted packages.\n\n### 🛠️ Recommended Solution\nAdd `spring.json.trusted.packages=*` and declare `type.mapping` in consumer configuration."
        }

    # 6. HTTP / WebClient / RestTemplate Timeout or Connection Refused
    if "ResourceAccessException" in message or "ConnectException" in message or "WebClientRequestException" in message:
        return {
            "summary": f"Downstream Microservice Communication Failure{service_label}.",
            "cause": f"The service attempted to make an outbound HTTP call to a downstream dependency, but the connection was refused or timed out. {detail}",
            "solution": "1. Verify the downstream service is healthy and reachable from this network.\n2. Configure explicit timeouts and Circuit Breaker (Resilience4j):\n   ```java\n   @CircuitBreaker(name = \"downstreamService\", fallbackMethod = \"fallbackResponse\")\n   ```",
            "insight": "### 📌 Error Summary\nDownstream Microservice Communication Failure.\n\n### 🔍 Root Cause\nTarget downstream endpoint was unresponsive, resulting in a socket connection failure or read timeout.\n\n### 🛠️ Recommended Solution\nCheck downstream service status, configure connection/read timeouts, and implement Resilience4j fallback."
        }

    # 7. JWT / Security / Authentication
    if "ExpiredJwtException" in message or "SignatureException" in message or "BadCredentialsException" in message or "AccessDeniedException" in message:
        return {
            "summary": f"Security / Authentication Failure{service_label}.",
            "cause": f"Security token validation failed. {detail}",
            "solution": "1. Check token expiration timestamp and ensure client refreshes the JWT.\n2. Verify the shared HMAC secret in `application.yml` is at least 256 bits and identical between auth service and API gateway.",
            "insight": "### 📌 Error Summary\nAuthentication / Security Failure.\n\n### 🔍 Root Cause\nClient presented an expired, malformed, or unauthorized token signature.\n\n### 🛠️ Recommended Solution\nPrompt user to re-authenticate and verify shared JWT secrets across microservices."
        }

    # 8. Spring Bean Initialization / Unsatisfied Dependency
    if "NoSuchBeanDefinitionException" in message or "UnsatisfiedDependencyException" in message:
        return {
            "summary": f"Spring Context Bean Dependency Missing{service_label}.",
            "cause": f"Spring failed to auto-wire a required bean. {detail}",
            "solution": "1. Verify the target class has `@Component`, `@Service`, or `@Repository`.\n2. Ensure the package is included in `@ComponentScan` or `@SpringBootApplication` scan base packages.",
            "insight": "### 📌 Error Summary\nSpring Application Context Dependency Injection Failure.\n\n### 🔍 Root Cause\nRequired dependency bean could not be resolved during container initialization.\n\n### 🛠️ Recommended Solution\nEnsure class is annotated with `@Component`/`@Service` and scanned by Spring Boot."
        }

    # Fallback for general runtime errors
    loc_info = f" Location: `{loc}`." if loc else ""
    return {
        "summary": f"Unhandled Runtime Exception ({exc}){service_label}.",
        "cause": f"Encountered `{exc}` during execution. {detail}.{loc_info}",
        "solution": f"1. Review the execution flow leading up to the error.\n2. Check boundary inputs and wrap with appropriate try/catch or domain-specific exceptions.\n3. Return structured error response via GlobalExceptionHandler.",
        "insight": f"### 📌 Error Summary\nUnhandled Runtime Exception: `{exc}`.\n\n### 🔍 Root Cause\n{detail}.{loc_info}\n\n### 🛠️ Recommended Solution\nExamine stack trace frames and implement defensive exception handling."
    }
