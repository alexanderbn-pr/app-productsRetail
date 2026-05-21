# app-productsRetail

API REST para consultar productos similares a partir de un ID de producto y obtener los detalles de cada uno. Construida con **Java 21** y **Spring Boot 3.3**.

## Stack Tecnológico

| Categoría | Tecnologías |
|-----------|-------------|
| **Lenguaje** | Java 21 |
| **Framework** | Spring Boot 3.3, Spring MVC, Spring AOP |
| **HTTP Client** | Apache HttpClient 5 con connection pooling |
| **Resiliencia** | Resilience4j (Circuit Breaker, Retry, Bulkhead) |
| **Caché** | Caffeine (en memoria) |
| **Documentación API** | SpringDoc OpenAPI 2.5 + Swagger UI |
| **Observabilidad** | Spring Actuator, Micrometer, Logstash JSON logging |
| **Tests** | JUnit 5, Mockito, AssertJ, MockMvc, SpringBootTest |
| **Build** | Maven |
| **Infra Docker** | Simulado (mock API), InfluxDB + Grafana, k6 |

## Requisitos

- **Java 21** (el proyecto usa features de Java 21)
- **Docker** y **Docker Compose** (para infraestructura)
- **Maven** (wrapper incluido: `./mvnw`)

## Ejecución

### 1. Levantar infraestructura

```bash
cd retail
docker-compose up -d simulado influxdb grafana
```

Esto inicia:
- **simulado** — mock HTTP de la API externa de productos (puerto 3001)
- **InfluxDB** — almacenamiento de métricas para k6 (puerto 8086)
- **Grafana** — dashboards de rendimiento (puerto 3000)

### 2. Ejecutar la aplicación

```bash
cd retail
./mvnw spring-boot:run
```

La API arranca en `http://localhost:5000`.

### 3. Ejecutar tests

```bash
cd retail
./mvnw test              # Tests unitarios + integración
```

### 4. Pruebas de carga (opcional)

```bash
cd retail
docker-compose run --rm k6 run /scripts/test.js
```

Los resultados se almacenan en InfluxDB y se visualizan en Grafana.

## Endpoints

| Método | URL | Descripción |
|--------|-----|-------------|
| `GET` | `/product/{productId}/similar` | Lista productos similares al ID dado |

### Documentación interactiva

- **Swagger UI**: http://localhost:5000/swagger-ui.html
- **OpenAPI JSON**: http://localhost:5000/v3/api-docs

### Health checks

- **Health**: http://localhost:5000/actuator/health
- **Health externa**: http://localhost:5000/actuator/health/externalApi

## Arquitectura

El proyecto sigue una **arquitectura limpia por capas**:

```
Controller → Service → Client (HTTP externo)
     │           │
     │           └── Caché (Caffeine)
     │
     └── Error handling → GlobalExceptionHandler (ProblemDetail RFC 7807)
```

### Capas

| Capa | Responsabilidad | Tecnología |
|------|----------------|------------|
| **Controller** | Validación de entrada, mapeo a DTOs de respuesta | Spring MVC `@RestController` |
| **Service** | Orquestación, resiliencia, caché | Spring `@Service`, Resilience4j, `@Cacheable` |
| **Client** | Comunicación HTTP con API externa, fetching paralelo | Apache HttpClient 5, `CompletableFuture` |
| **Exception Handler** | Mapeo de excepciones a respuestas HTTP estructuradas | `@RestControllerAdvice`, `ProblemDetail` |

## Tests

| Tipo | Framework | Cobertura |
|------|-----------|-----------|
| Unitarios | JUnit 5 + Mockito + AssertJ | Service, Controller, Client, ExceptionHandler |
| Integración | SpringBootTest + MockMvc | Contexto completo, controller end-to-end |
| Carga | k6 + InfluxDB + Grafana | 5 escenarios concurrentes (200 VUs) |

**40 tests**: 0 failures.

## Decisiones Técnicas

Consulta [`tech-stack.md`](tech-stack.md) para una explicación detallada de cada librería, por qué se eligió, qué alternativas se consideraron, y cómo aplica al código real — preparado para entrevistas técnicas.
