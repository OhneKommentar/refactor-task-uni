#!/usr/bin/env pwsh
$ErrorActionPreference = 'Stop'

$root = (Resolve-Path "$PSScriptRoot/..").Path
$bootstrapDir = Join-Path $root '.gradle/bootstrap'
$gradleVersion = '8.9'
$gradleDist = "gradle-$gradleVersion-bin.zip"
$gradleUrl = "https://services.gradle.org/distributions/$gradleDist"

if (!(Test-Path $bootstrapDir)) {
    New-Item -ItemType Directory -Path $bootstrapDir | Out-Null
}

$zipPath = Join-Path $bootstrapDir $gradleDist
if (!(Test-Path $zipPath)) {
    Write-Host "Downloading Gradle $gradleVersion..."
    Invoke-WebRequest -Uri $gradleUrl -OutFile $zipPath
}

$unzipDir = Join-Path $bootstrapDir "gradle-$gradleVersion"
if (!(Test-Path $unzipDir)) {
    Write-Host 'Unpacking Gradle...'
    Expand-Archive -Path $zipPath -DestinationPath $bootstrapDir -Force
}

Set-Location (Join-Path $root 'backend')
& "$unzipDir/bin/gradle" wrapper --gradle-version $gradleVersion
Write-Host 'Gradle wrapper generated. You can now run ./gradlew tasks.'
