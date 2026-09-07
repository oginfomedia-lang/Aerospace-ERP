# Loads secrets from a local, gitignored .env file (repo root) into the current
# PowerShell session's environment, then optionally launches whatever command
# follows.
#
# Usage:
#   . .\scripts\load-env.ps1                       # load vars into this session only
#   .\scripts\load-env.ps1 mvn spring-boot:run      # load vars, then run a command

$envFile = Join-Path $PSScriptRoot "..\.env"
if (-not (Test-Path $envFile)) {
    Write-Error "Missing $envFile - copy .env.example to .env and fill in real values first."
    exit 1
}

Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#")) {
        $parts = $line -split "=", 2
        if ($parts.Length -eq 2 -and $parts[1].Trim()) {
            [System.Environment]::SetEnvironmentVariable($parts[0].Trim(), $parts[1].Trim(), "Process")
        }
    }
}

if ($args.Count -gt 0) {
    & $args[0] $args[1..($args.Count - 1)]
}
