import uvicorn
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional, Dict, Any
from diagnostics import diagnose_spring_error

app = FastAPI(title="LogMonito AI Debugger API", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class DiagnoseRequest(BaseModel):
    logId: Optional[str] = None
    appName: Optional[str] = None
    message: str
    logLevel: Optional[str] = "ERROR"

@app.get("/health")
def health() -> Dict[str, str]:
    return {"status": "UP", "service": "LogMonito AI Debugger"}

@app.post("/diagnose")
def diagnose(req: DiagnoseRequest) -> Dict[str, Any]:
    diagnosis = diagnose_spring_error(req.message, req.appName)
    return {
        "logId": req.logId,
        "appName": req.appName,
        "summary": diagnosis["summary"],
        "cause": diagnosis["cause"],
        "solution": diagnosis["solution"],
        "insight": diagnosis["insight"]
    }

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
