# LogMonito Autonomous AI Debugger & SRE Rule

This rule guides Antigravity when diagnosing runtime errors captured by the LogMonito telemetry platform.

## When Activated
Apply this rule whenever the user asks about:
- Runtime errors, exceptions, or crashes in their Spring Boot services
- Diagnosing a specific LogMonito incident or inspecting recent telemetry logs
- Fixing bugs captured by the `monitor-spring-boot-starter`

## Autonomous Diagnosis Workflow

1. **Fetch Incident Telemetry**:
   - Call MCP tool `logmonito_get_recent_errors(limit=5, app_name=...)` to retrieve the latest error logs and stack traces from MongoDB.
   - If diagnosing performance or memory issues, call `logmonito_get_metrics(app_name=...)`.

2. **Structure the Diagnosis**:
   Always present the diagnosis in 3 clean, distinct sections:
   - 📌 **Incident Summary**: State which microservice was affected, the timestamp, and the high-level description of what failed.
   - 🔍 **Root Cause Analysis**: Pinpoint the technical cause—exact class name, line number, missing Spring bean, exhausted thread pool, or invalid network address.
   - 🛠️ **Step-by-Step Solution & Code Fix**: Provide the exact Java code fix, RFC 9457 GlobalExceptionHandler block, or `application.properties` configuration snippet.

3. **Sync with React Web Dashboard**:
   - Call MCP tool `logmonito_save_diagnosis(log_id, summary, cause, solution)` with the structured analysis.
   - This writes the insight directly into MongoDB (`telemetry_logs`), allowing developers viewing the React UI at `http://localhost:5173` to see the diagnosis immediately!

4. **Proactive Workspace Patching**:
   - Identify the client Java file from the stack trace and locate it in the workspace.
   - Offer to apply the bug fix directly to the source code.
