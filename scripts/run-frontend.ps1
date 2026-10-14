$ErrorActionPreference = "Stop"

$dir = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location (Join-Path $dir "frontend")

if (-not (Test-Path "node_modules")) {
  Write-Host "Installing frontend dependencies..."
  npm install
}

npm run dev -- @args
