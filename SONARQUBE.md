# SonarQube

FinFlow includes a local SonarQube service in `docker-compose.yml` and a repository-level scanner configuration in `sonar-project.properties`.

## Start SonarQube

```powershell
docker compose up -d sonarqube
```

Open `http://localhost:9000`.

Default local credentials:

```text
username: admin
password: admin
```

SonarQube asks you to change the password on first login.

## Run Service-Level Analysis

Create `YOUR_TOKEN` in SonarQube under **My Account > Security**.

Then scan each backend service as a separate SonarQube project:

```powershell
$env:SONAR_TOKEN="YOUR_TOKEN"
docker compose --profile tools run --rm sonar-analysis
```

If you do not want to create a token manually, provide your SonarQube admin credentials and the container will generate a temporary token:

```powershell
$env:SONAR_ADMIN_LOGIN="admin"
$env:SONAR_ADMIN_PASSWORD="YOUR_SONAR_PASSWORD"
docker compose --profile tools run --rm sonar-analysis
```

This builds each Maven service, generates JaCoCo coverage at `target/site/jacoco/jacoco.xml`, then scans each service into its own project card in SonarQube.

If you prefer running the same flow directly on Windows PowerShell:

```powershell
$env:SONAR_TOKEN="YOUR_TOKEN"
.\scan-sonarqube-services.ps1
```

## Run Aggregate Analysis

You can still scan the whole repository as one project:

```powershell
$env:SONAR_TOKEN="YOUR_TOKEN"
docker compose --profile tools run --rm sonar-scanner
```

Or, if `sonar-scanner` is installed on your machine, run:

```powershell
sonar-scanner -Dsonar.host.url=http://localhost:9000 -Dsonar.token=YOUR_TOKEN
```
