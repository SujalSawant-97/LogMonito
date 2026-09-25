# LogMonito Demo Client Application

A standalone Spring Boot 3 test application simulating real-world enterprise runtime errors and streaming telemetry to the LogMonito platform via the `monitor-spring-boot-starter` SDK.

---

## 1. Setup & Configuration

1. **Start LogMonito**:
   Ensure Docker containers (`api-gateway`, `ingestion-service`, `storage-service`, `kafka`, `mongodb`) and the React UI (`http://localhost:5173`) are running.

2. **Generate API Key**:
   - Open `http://localhost:5173`
   - Click **"API Keys"** $\rightarrow$ Enter App Name `demo-order-service`
   - Copy the generated API Key.

3. **Configure [application.yml](file:///c:/Users/sujal/IdeaProjects/LogMonito/demo-client/src/main/resources/application.yml)**:
   ```yaml
   monitor:
     enabled: true
     server-url: http://localhost:8080
     api-key: "YOUR_PASTED_API_KEY"
     log-level: ERROR # Only monitor runtime crashes
   ```

4. **Run the Application**:
   Double click `run.bat` or run:
   ```powershell
   cd demo-client
   .\mvnw.cmd spring-boot:run
   ```
   (Runs on port **`8085`**).

---

## 2. Test Endpoints to Trigger Errors

| Method | Endpoint | Error Type Simulated | How to Trigger |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/test/null-pointer` | `NullPointerException` (Unhandled crash) | `curl http://localhost:8085/api/test/null-pointer` |
| `GET` | `/api/test/arithmetic` | `ArithmeticException: / by zero` | `curl http://localhost:8085/api/test/arithmetic` |
| `GET` | `/api/test/database-failure` | `MongoSocketOpenException` | `curl http://localhost:8085/api/test/database-failure` |
| `POST` | `/api/test/checkout` | Validation failure (Missing credit card) | `curl -X POST http://localhost:8085/api/test/checkout` |
| `GET` | `/api/test/payment-timeout` | Handled error with `log.error(...)` | `curl http://localhost:8085/api/test/payment-timeout` |
| `GET` | `/api/test/status` | System health and test endpoint guide | `curl http://localhost:8085/api/test/status` |

---

## 3. Verify on LogMonito Dashboard

1. Trigger any of the endpoints above.
2. Open `http://localhost:5173` and select `demo-order-service` from the project dropdown.
3. The error event appears immediately in the **Incident Error Logs** table.
4. Click **"Diagnose"** to view the AI SRE root-cause analysis and code fix!
