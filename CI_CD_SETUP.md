# FinFlow CI/CD

This repository uses GitHub Actions for CI and continuous delivery.

## What Runs

- Pull requests to `main`, `master`, or `develop`:
  - Build and test every Spring Boot service with Maven.
  - Build the Angular frontend with npm.
  - Upload test reports, backend jars, and frontend build output as workflow artifacts.

- Pushes to `main`, `master`, `develop`, or tags like `v1.0.0`:
  - Run the same CI checks.
  - Build Docker images for every backend service and the frontend.
  - Push images to GitHub Container Registry.

## Published Images

Images are published under:

```text
ghcr.io/<github-owner>/finflow-admin-service
ghcr.io/<github-owner>/finflow-api-gateway
ghcr.io/<github-owner>/finflow-application-service
ghcr.io/<github-owner>/finflow-auth-service
ghcr.io/<github-owner>/finflow-config-server
ghcr.io/<github-owner>/finflow-document-service
ghcr.io/<github-owner>/finflow-service-registry
ghcr.io/<github-owner>/finflow-frontend
```

Each image receives branch, tag, commit SHA, and default-branch `latest` tags.

## Required Setup

No custom secret is needed for publishing to GHCR. The workflow uses the built-in `GITHUB_TOKEN`.

In GitHub, make sure Actions can publish packages:

1. Open repository `Settings`.
2. Go to `Actions` -> `General`.
3. Under `Workflow permissions`, select `Read and write permissions`.

## Deployment

The workflow currently performs continuous delivery by publishing production-ready Docker images. A server deployment step can be added once the target environment is known, for example SSH to a VM, Kubernetes, Render, Railway, AWS ECS, or Azure Container Apps.
