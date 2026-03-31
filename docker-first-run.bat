@echo off
setlocal EnableExtensions

REM First-time Docker DB bootstrap for Windows
REM Usage:
REM   docker-first-run.bat          -> init DB once (keep existing volume)
REM   docker-first-run.bat --reset  -> remove volume and re-import SQL dump

set "COMPOSE_FILE=docker-compose.yml"
set "SERVICE=mysql"
set "CONTAINER=family-app-mysql"
set "DB_NAME=family_management_db"
set "DB_USER=root"
set "DB_PASS=Mp7124002@"

where docker >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Docker is not installed or not in PATH.
  exit /b 1
)

docker info >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Docker Desktop is not running.
  echo         Please start Docker Desktop, then run this script again.
  exit /b 1
)

if /I "%~1"=="--reset" (
  echo [INFO] Reset mode: removing old container and volume...
  docker compose -f "%COMPOSE_FILE%" down -v
  if errorlevel 1 (
    echo [ERROR] Failed to reset docker resources.
    exit /b 1
  )
)

echo [INFO] Starting MySQL container...
docker compose -f "%COMPOSE_FILE%" up -d
if errorlevel 1 (
  echo [ERROR] Failed to start docker compose services.
  exit /b 1
)

echo [INFO] Waiting for MySQL to become ready...
set /a RETRIES=30
:wait_loop
docker exec "%CONTAINER%" mysqladmin --protocol=TCP -h 127.0.0.1 -u"%DB_USER%" -p"%DB_PASS%" ping --silent >nul 2>nul
if %errorlevel%==0 goto ready

set /a RETRIES-=1
if %RETRIES% LEQ 0 (
  echo [ERROR] MySQL is not ready after waiting.
  echo         Check logs with: docker logs %CONTAINER%
  exit /b 1
)

timeout /t 2 /nobreak >nul
goto wait_loop

:ready
echo [INFO] MySQL is ready.
echo [INFO] Quick data check from %DB_NAME%.users ...
docker exec "%CONTAINER%" mysql --protocol=TCP -h 127.0.0.1 -u"%DB_USER%" -p"%DB_PASS%" -e "SELECT COUNT(*) AS users_count FROM %DB_NAME%.users;"
if errorlevel 1 (
  echo [WARN] Could not query users table. The DB may still be initializing.
  exit /b 1
)

echo [DONE] Docker DB is up and initialized.
echo        App can connect via: jdbc:mysql://localhost:3306/%DB_NAME%
exit /b 0
