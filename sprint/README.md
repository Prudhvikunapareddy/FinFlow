# FinFlow Sprint Evaluation Study Pack

This folder is a complete reading guide for the FinFlow project. It is written for sprint evaluation preparation, with extra focus on the Angular frontend and enough backend detail to explain the full system confidently.

## Recommended Reading Order

1. `01-project-overview-and-architecture.md`
   Start here. It explains what FinFlow is, the user roles, the microservices, infrastructure, data ownership, and why the system is split this way.

2. `02-frontend-angular-detailed.md`
   Main Angular guide. It explains app bootstrapping, routing, guards, interceptors, services, models, components, forms, signals, templates, and page behavior.

3. `03-backend-microservices-detailed.md`
   Backend guide. It explains each Spring Boot service, controllers, services, repositories, entities, DTOs, security, RabbitMQ, Eureka, Config Server, PostgreSQL, and Swagger.

4. `04-end-to-end-flows.md`
   Use this to explain real user journeys: signup, login, create draft, upload document, submit application, admin verification, approval or rejection, status sync.

5. `05-file-by-file-guide.md`
   Use this when the evaluator points to a file and asks, "What does this do?" It gives a concise explanation of every important source, config, test, and deployment file.

6. `06-sprint-viva-cheatsheet.md`
   Fast revision sheet with likely evaluation questions, answers, demo flow, strengths, limitations, and improvement points.

7. `07-scenario-based-questions.md`
   Scenario-based evaluation questions with simple Telugu-English answers. Use this to practice how to explain real flows when the evaluator asks "what happens if...?"

8. `08-final-important-points.md`
   Last-minute revision checklist with demo order, Docker/SonarQube commands, admin note sync explanation, troubleshooting, and high-value answers.

## One-Line Project Summary

FinFlow is a loan application platform where users sign up, create and submit loan applications, upload supporting documents, and track status, while admins review synced applications, verify documents, approve or reject requests, manage user roles, and view reports.

## Tech Stack

- Frontend: Angular 21, standalone components, Angular Router, Reactive Forms, Signals, HttpClient, CSS.
- Backend: Java 21, Spring Boot 3.5.12, Spring Cloud Gateway, Spring Cloud Config, Eureka, Spring Security, Spring Data JPA, RabbitMQ, PostgreSQL, Swagger/OpenAPI.
- Infrastructure: Docker Compose, Nginx for frontend hosting, Zipkin tracing, RabbitMQ management UI.

## Most Important Evaluation Point

The frontend does not directly talk to individual services. It talks to the API Gateway, usually `http://localhost:8083` locally or `http://localhost:9083` in Docker. The gateway validates JWT tokens, adds `X-User-Email` and `X-User-Role` headers, and routes requests to the correct microservice.
