# ============================================================
#  Aerospace ERP - Start All Backend Services
#  Save this as start-all.ps1 in the project root.
#  Run from the project root folder in PowerShell:
#     .\start-all.ps1          -> start all services
#     .\start-all.ps1 -Stop    -> kill all Java processes
# ============================================================

param(
    [switch]$Stop   # Pass -Stop flag to stop all services instead of starting them
)

$BackendRoot = Join-Path $PSScriptRoot "backend"
$DelaySeconds = 8  # Wait between each service start (seconds). Increase if your PC is slow.

# ---- Colour helpers ----------------------------------------
function Write-Step { param($msg) Write-Host "`n>> $msg" -ForegroundColor Cyan }
function Write-OK   { param($msg) Write-Host "   [OK]  $msg" -ForegroundColor Green }
function Write-Warn { param($msg) Write-Host "   [!]   $msg" -ForegroundColor Yellow }

# ---- Service definitions in startup ORDER ------------------
# auth-service MUST start before downstream services.
# api-gateway MUST start LAST (it depends on all others).
$Services = @(
    [PSCustomObject]@{ Name = "service-registry";     Port = 8761; Dir = "service-registry" },
    [PSCustomObject]@{ Name = "auth-service";         Port = 8087; Dir = "auth-service"         },
    [PSCustomObject]@{ Name = "inventory-service";    Port = 8081; Dir = "inventory-service"    },
    [PSCustomObject]@{ Name = "qa-service";           Port = 8082; Dir = "qa-service"           },
    [PSCustomObject]@{ Name = "notification-service"; Port = 8083; Dir = "notification-service" },
    [PSCustomObject]@{ Name = "order-service";        Port = 8084; Dir = "order-service"        },
    [PSCustomObject]@{ Name = "production-service";   Port = 8085; Dir = "production-service"   },
    [PSCustomObject]@{ Name = "procurement-service";  Port = 8086; Dir = "procurement-service"  },
    [PSCustomObject]@{ Name = "reporting-service";    Port = 8088; Dir = "reporting-service"    },
    [PSCustomObject]@{ Name = "api-gateway";          Port = 8089; Dir = "api-gateway"          }
)

# ---- STOP MODE: kill all running java processes ------------
if ($Stop) {
    Write-Host "`n======================================================" -ForegroundColor Red
    Write-Host "  Stopping all Aerospace ERP services..." -ForegroundColor Red
    Write-Host "======================================================`n" -ForegroundColor Red

    $javaPIDs = Get-Process -Name "java" -ErrorAction SilentlyContinue
    if ($javaPIDs) {
        $javaPIDs | Stop-Process -Force
        Write-Host "  Stopped $($javaPIDs.Count) Java process(es)." -ForegroundColor Green
    } else {
        Write-Warn "No running Java processes found."
    }
    exit 0
}

# ---- START MODE --------------------------------------------
Write-Host "`n======================================================" -ForegroundColor Cyan
Write-Host "  Aerospace ERP - Starting All Backend Services" -ForegroundColor Cyan
Write-Host "======================================================`n" -ForegroundColor Cyan

foreach ($svc in $Services) {
    $svcDir = Join-Path $BackendRoot $svc.Dir

    if (-not (Test-Path "$svcDir\pom.xml")) {
        Write-Warn "Skipping '$($svc.Name)' - pom.xml not found at $svcDir"
        continue
    }

    Write-Step "Starting $($svc.Name) on port $($svc.Port)..."

    # Each service gets its own PowerShell window with a descriptive title
    $cmd = @"
`$host.UI.RawUI.WindowTitle = '$($svc.Name) [:$($svc.Port)]'
Set-Location '$svcDir'
Write-Host 'Starting $($svc.Name) on port $($svc.Port)...' -ForegroundColor Cyan
mvn spring-boot:run
Read-Host 'Service stopped. Press Enter to close this window.'
"@
    Start-Process powershell -ArgumentList "-NoExit", "-Command", $cmd

    Write-OK "$($svc.Name) window opened. Waiting $DelaySeconds seconds before next service..."
    Start-Sleep -Seconds $DelaySeconds
}

Write-Host "`n======================================================" -ForegroundColor Green
Write-Host "  All services launched!" -ForegroundColor Green
Write-Host "======================================================" -ForegroundColor Green
Write-Host ""
Write-Host "  Service URLs:" -ForegroundColor White
foreach ($svc in $Services) {
    Write-Host "    $($svc.Name.PadRight(26)) -> http://localhost:$($svc.Port)" -ForegroundColor Gray
}
Write-Host ""
Write-Host "  To STOP all services, run:  .\start-all.ps1 -Stop" -ForegroundColor Yellow
Write-Host ""
