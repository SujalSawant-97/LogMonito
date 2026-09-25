import os
import json
from typing import Optional, List, Dict, Any
from pymongo import MongoClient
from mcp.server.fastmcp import FastMCP
from diagnostics import diagnose_spring_error

MONGO_URI = os.getenv("MONGODB_URI", "mongodb://localhost:27017/ai_monitoring_db")
DB_NAME = "ai_monitoring_db"

mcp = FastMCP("logmonito")

def get_mongo_db():
    client = MongoClient(MONGO_URI, serverSelectionTimeoutMS=2000)
    return client[DB_NAME]

@mcp.tool()
def logmonito_get_recent_errors(limit: int = 5, app_name: str = "") -> str:
    """
    Fetches the latest runtime errors logged from client Spring Boot applications.
    Args:
        limit: Number of errors to retrieve (default: 5)
        app_name: Optional filter for a specific service name
    """
    try:
        db = get_mongo_db()
        query: Dict[str, Any] = {"logLevel": "ERROR"}
        if app_name:
            query["metadata.serviceName"] = app_name

        cursor = db.telemetry_logs.find(query).sort("timestamp", -1).limit(limit)
        results = []
        for doc in cursor:
            results.append({
                "id": str(doc.get("_id", doc.get("id"))),
                "timestamp": str(doc.get("timestamp")),
                "serviceName": doc.get("metadata", {}).get("serviceName", "unknown"),
                "logLevel": doc.get("logLevel"),
                "message": doc.get("message"),
                "has_diagnosis": "diagnosis" in doc
            })
        return json.dumps(results, indent=2)
    except Exception as e:
        return f"Error retrieving logs from MongoDB: {str(e)}"

@mcp.tool()
def logmonito_diagnose_error(error_message: str, app_name: str = "") -> str:
    """
    Diagnoses a Spring Boot runtime exception and returns a structured 3-part analysis:
    - 📌 Error Summary
    - 🔍 Root Cause
    - 🛠️ Actionable Solution & Code Patch
    """
    res = diagnose_spring_error(error_message, app_name if app_name else None)
    return (
        f"### 📌 Error Summary\n{res['summary']}\n\n"
        f"### 🔍 Root Cause\n{res['cause']}\n\n"
        f"### 🛠️ Solution & Code Fix\n{res['solution']}"
    )

@mcp.tool()
def logmonito_get_metrics(app_name: str = "", limit: int = 5) -> str:
    """
    Fetches latest CPU and memory telemetry to correlate runtime errors with system performance.
    """
    try:
        db = get_mongo_db()
        query = {}
        if app_name:
            query["metadata.serviceName"] = app_name
        cursor = db.telemetry_metrics.find(query).sort("timestamp", -1).limit(limit)
        results = []
        for doc in cursor:
            results.append({
                "id": str(doc.get("_id", doc.get("id"))),
                "timestamp": str(doc.get("timestamp")),
                "serviceName": doc.get("metadata", {}).get("serviceName", "unknown"),
                "cpuUsage": doc.get("cpuUsage"),
                "memoryUsage": doc.get("memoryUsage")
            })
        return json.dumps(results, indent=2)
    except Exception as e:
        return f"Error retrieving metrics: {str(e)}"

@mcp.tool()
def logmonito_save_diagnosis(log_id: str, summary: str, cause: str, solution: str) -> str:
    """
    Saves an Antigravity AI diagnosis into MongoDB under the specified error log ID so that
    it immediately displays in the React UI dashboard.
    """
    try:
        from bson import ObjectId
        from datetime import datetime, timezone
        db = get_mongo_db()
        
        query = {"_id": ObjectId(log_id)} if ObjectId.is_valid(log_id) else {"_id": log_id}
        update_doc = {
            "$set": {
                "diagnosis": {
                    "summary": summary,
                    "cause": cause,
                    "solution": solution,
                    "insight": f"### 📌 Error Summary\n{summary}\n\n### 🔍 Root Cause\n{cause}\n\n### 🛠️ Solution\n{solution}"
                },
                "diagnosedAt": datetime.now(timezone.utc)
            }
        }
        res = db.telemetry_logs.update_one(query, update_doc)
        if res.matched_count == 0:
            res = db.telemetry_logs.update_one({"id": log_id}, update_doc)
            
        return f"Diagnosis saved successfully for log ID '{log_id}' (matched: {res.matched_count}). Visible on React UI!"
    except Exception as e:
        return f"Failed to save diagnosis: {str(e)}"

if __name__ == "__main__":
    mcp.run()
