@echo off
cd /d "%~dp0"
echo Starting Demo Client Service on port 8085...
call .\mvnw.cmd spring-boot:run
