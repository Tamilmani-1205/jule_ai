# Spring Boot Observability with OpenTelemetry, Prometheus, and Grafana

## Overview

This project demonstrates a Spring Boot application integrated with a full observability stack.
It uses OpenTelemetry for generating traces and metrics, an OpenTelemetry Collector for processing and exporting this data, Prometheus for metrics storage, and Grafana for visualization.

The setup includes:
- A simple Spring Boot application (`springboot-app`) with a `/hello` endpoint.
- OpenTelemetry Java agent auto-instrumentation via the Spring Boot starter.
- An OpenTelemetry Collector (`otel-collector`) configured to receive OTLP data and export metrics to Prometheus and traces to logging (for demonstration).
- Prometheus (`prometheus`) configured to scrape metrics from the Otel Collector and the Spring Boot app's Actuator endpoint.
- Grafana (`grafana`) pre-configured with a Prometheus datasource and a sample dashboard for JVM metrics.

## Prerequisites

- Docker
- Docker Compose
- Java JDK (e.g., Temurin 17 or later) - for building the Spring Boot app locally
- Apache Maven - for building the Spring Boot app locally

## Directory Structure

-   `springboot-app/`: Contains the Spring Boot application source code and Dockerfile.
    -   `src/main/java/`: Java source files.
    -   `src/main/resources/application.properties`: Spring Boot application configuration, including OpenTelemetry settings.
    -   `Dockerfile`: Used to build the Spring Boot application Docker image.
    -   `pom.xml`: Maven project configuration.
-   `otel-collector/`: Contains the OpenTelemetry Collector configuration.
    -   `otel-collector-config.yaml`: Configuration for the Otel Collector, defining receivers, processors, and exporters.
-   `prometheus/`: Contains the Prometheus configuration.
    -   `prometheus.yml`: Prometheus scrape configurations.
-   `grafana/`: Contains Grafana provisioning files for datasources and dashboards.
    -   `provisioning/datasources/`: Grafana datasource definitions (Prometheus).
    -   `provisioning/dashboards/`: Grafana dashboard provider configuration.
    -   `dashboards/`: Sample Grafana dashboard JSON files.
-   `docker-compose.yml`: Defines and configures all the services (Spring Boot app, Otel Collector, Prometheus, Grafana).
-   `README.md`: This file.

## Setup and Running

1.  **Build the Spring Boot Application (Optional if using pre-built image or Docker handles it)**:
    If you've made changes to the Spring Boot application code, you need to rebuild the JAR file.
    ```bash
    cd springboot-app
    mvn clean package
    cd ..
    ```
    The `docker-compose.yml` is configured to build the `springboot-app` image using the Dockerfile in the `springboot-app` directory, which expects the JAR in `springboot-app/target/`.

2.  **Run with Docker Compose**:
    This command will build the Spring Boot app image (if not already built) and start all defined services.
    ```bash
    docker-compose up -d
    ```

## Accessing Services

Once the services are up and running, you can access them via your browser:

-   **Spring Boot App**:
    -   Hello Endpoint: [http://localhost:8080/hello](http://localhost:8080/hello)
    -   Actuator (Prometheus metrics): [http://localhost:8080/actuator/prometheus](http://localhost:8080/actuator/prometheus)
-   **Prometheus**:
    -   Web UI: [http://localhost:9090](http://localhost:9090)
    -   Targets: Check `Status > Targets` to see if `otel-collector` and `springboot-app` are being scraped.
-   **Grafana**:
    -   Web UI: [http://localhost:3000](http://localhost:3000)
    -   Default Credentials: `admin` / `password` (as configured in `docker-compose.yml`)
    -   A sample dashboard "Spring Boot App Metrics" should be available.
-   **OpenTelemetry Collector Ports** (for reference, not typically accessed directly via browser):
    -   gRPC receiver: `localhost:4317`
    -   HTTP receiver: `localhost:4318`
    -   Prometheus exporter (scraped by Prometheus): `localhost:8889` (available at `otel-collector:8889` within Docker network)

## Testing Logs and Metrics for `/hello` API

1.  **Generate Traffic:**
    Access the `/hello` endpoint a few times using your browser or `curl`:
    ```bash
    curl http://localhost:8080/hello
    # or open http://localhost:8080/hello in your browser
    ```

2.  **Check Application Logs (via Otel Collector):**
    The Spring Boot application's logs (including SLF4J logs from `HelloController` and OpenTelemetry's span events if the logging exporter is configured verbosely enough) are sent to the Otel Collector. You can view them in the collector's output:
    ```bash
    docker-compose logs otel-collector
    ```
    Look for lines indicating logs from `springboot-app` or events from the `HelloController`'s custom span. Span events might be part of the trace logging.

3.  **Check Traces (Conceptual):**
    The custom span and its events created in `HelloController` are part of the trace data. This data is sent to `otel-collector`. While this setup doesn't include a dedicated trace visualization backend (like Jaeger or Zipkin), the `otel-collector` is configured to log traces it receives. You might see trace information in:
    ```bash
    docker-compose logs otel-collector
    ```
    *(Note: For a real setup, you'd add Jaeger/Zipkin to `docker-compose.yml` and configure the Otel collector to export traces to it.)*

4.  **Check Metrics in Prometheus:**
    - Navigate to Prometheus: `http://localhost:9090`
    - In the "Expression" browser, you can query for metrics related to the `/hello` endpoint. Examples:
        - Count of requests to `/hello`:
          `http_server_requests_seconds_count{service_name="springboot-app", uri="/hello"}` (Note: label `uri` might be `http.route` or `http.target` depending on exact OTel semantic conventions version)
        - Latency of requests to `/hello` (histogram):
          `http_server_requests_seconds_bucket{service_name="springboot-app", uri="/hello"}`
    - You should also see the custom span metrics if they are generated (though `spanBuilder` itself doesn't automatically create duration metrics in Prometheus without more config). Standard HTTP metrics are the primary focus here.

5.  **Check Metrics in Grafana:**
    - Navigate to Grafana: `http://localhost:3000` (admin/password as previously noted, or specific user/pass if changed)
    - The "Spring Boot App Metrics" dashboard should show JVM metrics. You can create new panels or a new dashboard to query and visualize the HTTP metrics from Prometheus (e.g., using the queries mentioned above).
    - The existing dashboard displays `jvm_memory_used_bytes`. You can add a new panel:
        - Title: "Hello API Requests"
        - Query: `sum(rate(http_server_requests_seconds_count{service_name="springboot-app", uri="/hello"}[1m]))` (Requests per second)
        - Or `sum(increase(http_server_requests_seconds_count{service_name="springboot-app", uri="/hello"}[5m]))` (Total requests in last 5m)

## Stopping the Setup

To stop and remove all the containers, networks, and volumes defined in `docker-compose.yml`:
```bash
docker-compose down
```
If you want to remove volumes defined in the `docker-compose.yml` file (e.g., to clear Grafana or Prometheus data if they were configured to use named volumes persistently, though this setup primarily uses bind mounts for configuration):
```bash
docker-compose down -v
```

## Customization

-   **Grafana Dashboards**: Add or modify dashboard JSON files in the `grafana/dashboards/` directory. Grafana is configured to automatically pick them up.
-   **OpenTelemetry Collector**: Modify `otel-collector/otel-collector-config.yaml` to change how traces and metrics are processed or to add new exporters (e.g., Jaeger, Zipkin, or a cloud-based observability platform).
-   **Prometheus Configuration**: Adjust `prometheus/prometheus.yml` to change scrape targets, intervals, or add alerting rules.
-   **Spring Boot Application**: Modify the code in `springboot-app/` to add more features or change OpenTelemetry instrumentation details in `application.properties`. Remember to rebuild the app (Step 1 in "Setup and Running") if you make code changes.
