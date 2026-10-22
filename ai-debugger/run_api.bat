@echo off
cd /d "%~dp0"
echo Starting LogMonito AI Debugger API on port 8000...
.\.venv\Scripts\python.exe -m uvicorn api_service:app --host 0.0.0.0 --port 8000 --reload
