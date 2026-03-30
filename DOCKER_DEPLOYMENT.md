# FinFlow Docker Deployment

## Start the full stack

```bash
docker compose up --build
```

## Services

- API Gateway: `http://localhost:8083`
- Swagger UI: `http://localhost:8083/swagger-ui.html`
- Eureka: `http://localhost:8761`
- Config Server: `http://localhost:8888`
- Zipkin: `http://localhost:9411`
- RabbitMQ Management: `http://localhost:15672`

## Stop the stack

```bash
docker compose down
```

## Stop and remove volumes

```bash
docker compose down -v
```

## Notes

- PostgreSQL is started once and creates `auth_db`, `application_db`, `document_db`, and `admin_db` automatically.
- `config-repo` is mounted into the config server container at `/config-repo`.
- Services use Docker DNS names such as `postgres`, `rabbitmq`, `service-registry`, `config-server`, and `zipkin`.
