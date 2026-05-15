# FinFlow backend deployment

Vercel can host the Angular frontend, but this backend needs a Docker-capable host because it runs Spring Boot microservices plus Postgres, RabbitMQ, Eureka, and config-server.

## Deploy backend on a Docker host

1. Copy `.env.backend.example` to `.env.backend`.
2. Replace every `change-this...` value with a real secret.
3. Start the backend:

```powershell
docker compose --env-file .env.backend -f docker-compose.backend.yml up -d --build
```

4. Open the API gateway publicly on port `8083`, or map it through your hosting provider's HTTPS domain.

Your public API base URL should point to the gateway, for example:

```text
https://finflow-api.example.com
```

## Connect Vercel frontend

In Vercel, add this environment variable to the frontend project:

```text
FINFLOW_API_BASE_URL=https://your-public-api-gateway-url
```

Then redeploy the frontend.

For local development, keep using:

```text
http://localhost:8083
```

The gateway CORS config allows localhost and Vercel URLs by default. To override it in production, set:

```text
APP_CORS_ALLOWED_ORIGIN_PATTERNS=https://your-vercel-app.vercel.app,http://localhost:*
```
