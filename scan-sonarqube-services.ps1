$ErrorActionPreference = "Stop"

if (-not $env:SONAR_TOKEN) {
    throw "Set SONAR_TOKEN first. Example: `$env:SONAR_TOKEN='your-token'"
}

$services = @(
    "admin-service",
    "api-gateway",
    "application-service",
    "auth-service",
    "config-server",
    "document-service",
    "service-registry"
)

$fallbackMaven = Get-ChildItem ".m2/wrapper/dists/apache-maven-*" -Recurse -Filter "mvn.cmd" |
    Sort-Object FullName -Descending |
    Select-Object -First 1

foreach ($service in $services) {
    Write-Host "Building $service..." -ForegroundColor Cyan
    $wrapper = Join-Path $service "mvnw.cmd"
    if (Test-Path $wrapper) {
        Push-Location $service
        try {
            & ".\mvnw.cmd" "-Dmaven.repo.local=../.m2/repository" clean verify
        } finally {
            Pop-Location
        }
    } elseif ($fallbackMaven) {
        & $fallbackMaven.FullName "-Dmaven.repo.local=.m2/repository" -f "$service/pom.xml" clean verify
    } else {
        throw "Maven is not installed and no Maven wrapper/local Maven was found for $service."
    }

    Write-Host "Scanning $service..." -ForegroundColor Cyan
    docker compose --profile tools run --rm sonar-scanner -Dsonar.projectBaseDir="/usr/src/$service"
}

Write-Host "Done. Open http://localhost:9000/projects to see each service." -ForegroundColor Green
