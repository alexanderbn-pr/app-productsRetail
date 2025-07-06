# app-productsRetail
Api para obtener productos similares respecto a un producto y los detalles de esos productos

## Ejecución

1. Desde el directorio donde se encuentra el archivo `docker-compose.yaml` (`/retail`), ejecuta el siguiente comando para iniciar los contenedores de **simulado**, **InfluxDB** y **Grafana**:

   ```bash
   docker-compose up -d simulado influxdb grafana

2. Ejecutar los tests

   ```bash
   docker-compose run --rm k6 run scripts/test.js

## Herramientas y Tecnologías Utilizadas

- **Java 17+**: Lenguaje principal para el desarrollo de la API.
- **Spring Boot**: Framework para la creación de aplicaciones web y microservicios.
- **JUnit**: Framework para pruebas unitarias.

## Funcionalidades
- Se ha creado un endpoint para recuperar los productos similares y detalles de los productos similares
- Se han creado los tests tanto del controller como del servicio
- La aplicación se ha dividido en tres directorios main (funcionalidad) retail (archivos de test del docker) test (los tests de la funcionalidad)

