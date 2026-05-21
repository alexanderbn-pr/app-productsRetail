# Tech Stack — app-productsRetail

> Documento preparado para entrevistas técnicas. Cada tecnología incluye:
> **¿Qué problema resuelve?**, **¿Por qué esta opción?**, **Alternativas** y **Cómo lo usamos**.

---

## Java 21

**Problema**: Necesitamos un lenguaje maduro, con tipado estático, ecosistema enterprise y buen rendimiento para una API REST.

**Por qué Java 21**: Es la LTS más reciente de Java. Aporta:
- **Virtual Threads** (Project Loom) — hilos ligeros para I/O concurrente sin el overhead de los hilos del sistema.
- **Records** — DTOs inmutables con constructor, getters, `equals`, `hashCode` y `toString` en una línea.
- **Pattern Matching** — `instanceof` y `switch` más expresivos.
- **Text Blocks** — strings multilínea limpios para SQL, JSON, logs.

**Cómo lo usamos**: Records para DTOs (`ProductDetailResponse`, `ErrorResponse`), `CompletableFuture` con virtual threads ready, `List.of()` para colecciones inmutables.

---

## Spring Boot 3.3

**Problema**: Configurar un servidor web, DI, validación, caché, AOP y métricas desde cero requeriría cientos de líneas de boilerplate.

**Por qué Spring Boot**: Es el estándar de facto para APIs REST en Java. Auto-configuration reduce el setup a un `@SpringBootApplication`. La integración con Resilience4j, Caffeine, Actuator y OpenAPI es nativa.

**Cómo lo usamos**: `spring-boot-starter-web` (servidor embebido Tomcat), `spring-boot-starter-validation` (Jakarta Bean Validation), `spring-boot-starter-aop` (necesario para `@Retry`, `@CircuitBreaker`, `@Cacheable`), `spring-boot-starter-cache` (abstracción de caché), `spring-boot-starter-actuator` (health checks y métricas).

---

## Resilience4j 2.2

**Problema**: Las llamadas HTTP a la API externa pueden fallar por timeouts, la API puede estar caída, o pueden saturarse las conexiones. Sin resiliencia, un fallo externo tumba el servicio entero.

**Por qué Resilience4j**: Es la librería de resiliencia moderna para Spring Boot 3. Sustituye a Hystrix (que entró en modo mantenimiento). Ofrece:
- **Retry**: Reintenta llamadas fallidas con backoff.
- **Circuit Breaker**: Abre el circuito si la tasa de fallo supera un umbral, evitando cascadas.
- **Bulkhead**: Limita el número de llamadas concurrentes, protegiendo los recursos del sistema.
- Integración nativa con Spring Boot 3 vía `resilience4j-spring-boot3`.

**Cómo lo usamos**:

```java
@Retry(name = "similarProductsRetry")
@CircuitBreaker(name = "similarProducts", fallbackMethod = "fallbackSimilarProducts")
@Bulkhead(name = "similarProductsBulkhead")
public List<ProductDetail> getSimilarProducts(String productId) {
    return productApiClient.getSimilarProducts(productId);
}
```

- **Retry**: 3 intentos, 500ms entre intentos. Ignora `ProductNotFoundException` (no reintentar 404s).
- **Circuit Breaker**: Ventana deslizante de 10 llamadas, umbral 50%, espera 10s, 3 llamadas half-open.
- **Bulkhead**: Máximo 10 llamadas concurrentes, 500ms de espera máxima.

---

## Caffeine Cache 3.1

**Problema**: Las mismas peticiones a la API externa se repiten constantemente. Sin caché, cada request golpea la API externa, aumentando latencia y consumo.

**Por qué Caffeine**: Es el caché en memoria de más alto rendimiento para Java. Supera a Guava Cache y Ehcache en benchmarks de throughput y latencia. Integración directa con `spring-boot-starter-cache` vía `CaffeineCacheManager`.

**Cómo lo usamos**:

```java
@Cacheable(value = "similarIds", unless = "#result.isEmpty()")
public List<ProductDetail> getSimilarProducts(String productId) { ... }
```

Dos caches:
- **similarIds**: TTL 10 minutos, máximo 10.000 entradas.
- **productDetails**: TTL 5 minutos, máximo 10.000 entradas.

---

## Apache HttpClient 5

**Problema**: `RestTemplate` por defecto usa `SimpleClientHttpRequestFactory` que crea una conexión por request — sin pooling, sin timeouts configurables, sin reuso de conexiones.

**Por qué Apache HC5**: Es el cliente HTTP más maduro para Java. Ofrece connection pooling (reutiliza conexiones TCP), timeouts configurables (`connectTimeout`, `readTimeout`), y mejor manejo de errores HTTP.

**Cómo lo usamos**:

```java
PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
connectionManager.setMaxTotal(20);

RequestConfig requestConfig = RequestConfig.custom()
    .setConnectTimeout(timeout.connect())
    .setResponseTimeout(timeout.read())
    .build();
```

Pool de 20 conexiones, connect timeout 2s, read timeout 5s.

---

## SpringDoc OpenAPI 2.5

**Problema**: Sin documentación de API, los consumidores no saben qué endpoints existen, qué parámetros esperan, ni qué formato tienen las respuestas.

**Por qué SpringDoc**: Genera la especificación OpenAPI 3 automáticamente a partir de anotaciones Spring. Incluye Swagger UI para explorar y probar los endpoints desde el navegador. Es el sustituto moderno de Springfox (que dejó de mantener OpenAPI 3).

**Cómo lo usamos**:

```java
@OpenAPIDefinition(info = @Info(title = "Retail Products API", version = "1.0"))
```

Accesible en `/swagger-ui.html` y `/v3/api-docs`.

---

## Logstash Logback Encoder 8.0

**Problema**: Los logs en texto plano son difíciles de parsear por herramientas como Elasticsearch, Datadog o Grafana Loki. En producción necesitas logs estructurados en JSON.

**Por qué Logstash Logback Encoder**: Es la librería estándar para emitir logs JSON desde Logback (el logger por defecto de Spring Boot). Incluye el MDC automáticamente en el JSON, lo que permite correlacionar logs por `correlationId`.

**Cómo lo usamos**:

```xml
<appender name="CONSOLE_JSON" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <includeMdcKeyName>correlationId</includeMdcKeyName>
    </encoder>
</appender>
```

Cada log incluye `@timestamp`, `level`, `logger_name`, `message`, `correlationId`, y stack traces completos.

---

## Micrometer + Actuator

**Problema**: En producción necesitas saber si la aplicación está viva, cuántas peticiones recibe, cuánto tardan, y si hay errores. Sin métricas, operas a ciegas.

**Por qué Micrometer**: Es la fábrica de métricas de Spring Boot. Expone métricas de JVM, caché, HTTP, y personalizadas en formato Prometheus o OpenTelemetry. Actuator añade endpoints REST para health checks, info y metrics.

**Cómo lo usamos**:

```yaml
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=always
```

Endpoints: `/actuator/health`, `/actuator/metrics`. Health check personalizado para la API externa.

---

## Correlation ID + Structured Logging

**Problema**: Sin un ID de correlación, es imposible seguir el rastro de una petición a través de logs cuando hay múltiples requests concurrentes.

**Por qué esta implementación**: Un `Filter` de Jakarta Servlet intercepta cada request, extrae `X-Correlation-ID` del header (si existe) o genera un UUID, y lo inyecta en **MDC** (Mapped Diagnostic Context). Esto hace que TODOS los logs de esa petición incluyan automáticamente el correlationId sin tener que pasarlo manualmente.

**Cómo lo usamos**: Cada log incluye `"correlationId":"..."` en el JSON, permitiendo filtrar todos los eventos de una misma petición en Elasticsearch o Grafana.

---

## ProblemDetail (RFC 7807)

**Problema**: Sin un formato estándar de error, cada equipo inventa su propia estructura (`{code, message, timestamp}`), obligando a los consumidores a adaptarse a cada API.

**Por qué RFC 7807**: Es un estándar IETF que define la estructura de errores HTTP: `type`, `title`, `status`, `detail`, `instance`. Spring Boot 3 lo implementa nativamente con `ProblemDetail`. Cualquier cliente HTTP puede parsearlo sin documentación específica.

**Cómo lo usamos**:

```java
@ExceptionHandler(ProductNotFoundException.class)
public ProblemDetail handleProductNotFound(ProductNotFoundException ex) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    pd.setTitle("PRODUCT_NOT_FOUND");
    pd.setDetail(ex.getMessage());
    return pd;
}
```

Cubre 6 tipos de error: 404 (not found, product not found), 400 (bad request), 503 (circuit breaker), 429 (bulkhead), 500 (internal).

---

## Tests: JUnit 5 + Mockito + AssertJ + MockMvc

**Problema**: Sin tests automatizados, cada cambio manual rompe funcionalidad existente sin que nadie lo sepa.

**Por qué este stack**:
- **JUnit 5**: Framework de testing estándar en Java. `@ExtendWith(MockitoExtension.class)` para inyección de mocks.
- **Mockito**: Mocks de dependencias externas (HTTP, servicios). Aísla la unidad bajo test.
- **AssertJ**: Assertions fluidas y legibles (`assertThat(result).hasSize(2).containsExactly(...)`).
- **MockMvc**: Test de controladores sin levantar el servidor completo.

**Cómo lo usamos**:

| Test | Estrategia |
|------|-----------|
| `SimilarProductsServiceTest` | Mockito: mockea `ProductApiClient`, testea lógica de negocio + fallbacks |
| `SimilarProductsControllerTest` | MockMvc con `@WebMvcTest`: testea endpoints, validación, errores |
| `GlobalExceptionHandlerTest` | MockMvc standalone: testea cada excepción → código HTTP + ProblemDetail |
| `SimilarProductsControllerIntegrationTest` | `@SpringBootTest` + MockMvc: testea contexto completo |
| `ProductApiClientTest` | Mockito + `CompletableFuture`: testea timeouts, 404s, fallos parciales |

---

## k6 + InfluxDB + Grafana

**Problema**: Sin pruebas de carga, no sabes cómo se comporta la API bajo estrés ni dónde están los cuellos de botella.

**Por qué k6**: Es la herramienta de load testing más moderna. Scriptable en JavaScript, integración nativa con InfluxDB para almacenar métricas y Grafana para visualizarlas. Ejecuta 200 VUs concurrentes en 5 escenarios.

**Cómo lo usamos**: 5 escenarios (normal, slow, verySlow, 404, error) que cubren todos los casos de la API externa. Las métricas se almacenan en InfluxDB y se visualizan en Grafana con 3 paneles: Requests, http-req-duration, VUs.
