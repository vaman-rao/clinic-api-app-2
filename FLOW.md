# Clinic API + Load Simulator — Docker Flow

Everything runs in Docker. No local Java, Python, or database setup required beyond one Maven build step.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                     docker-compose network                          │
│                                                                     │
│  ┌──────────────┐    SQL     ┌──────────────┐                       │
│  │  clinic-mysql│◄───────── │  clinic-api  │◄── REST :9999         │
│  │  MySQL 8.0   │            │  Java 17     │                       │
│  │  :3306       │            │  Spring Boot │──► /actuator/prometheus│
│  └──────────────┘            └──────────────┘         │            │
│                                                        │ scrape/15s │
│                                               ┌────────▼──────┐    │
│                                               │  prometheus   │    │
│                                               │  :9090        │    │
│                                               └───────────────┘    │
└─────────────────────────────────────────────────────────────────────┘

┌───────────────────────┐
│  clinic-load-simulator│  ──── HTTP calls ──►  localhost:9999
│  Python 3.11 (Docker) │       (--network host)
│  6 concurrent threads │
└───────────────────────┘
```

---

## Observability Architecture (Inside the App)

Business code and observability code are fully decoupled.
Services never import `io.micrometer`. The metrics layer never contains business logic.

```
HTTP Request
     │
     ▼
Controller
     │
     ▼
Service method ──@Timed──► AOP Aspect (automatic) ──► Prometheus histogram
     │
     │  (pure business logic)
     │
     ├──► repository.save(...)
     │
     └──► eventPublisher.publishEvent(AppointmentCreatedEvent)
                               │
                               │  Spring event bus
                               ▼
                   ClinicMetricsListener
                   @EventListener
                   appointmentCreated.increment() ──► Prometheus counter
```

### Package responsibilities

| Package / File | Role | Knows about Micrometer? |
|---|---|---|
| `service/` | Business logic only. Publishes events. | ✗ No |
| `events/` | Plain data carriers. No framework coupling. | ✗ No |
| `metrics/ClinicMetricsListener` | Listens to events → increments counters | ✓ Yes (intentionally) |
| `metrics/ClinicGaugesRegistrar` | Registers live-state DB gauges | ✓ Yes (intentionally) |
| `config/MetricsConfig` | Wires `TimedAspect` only | ✓ Yes (infrastructure) |

**Rule:** `io.micrometer` imports are allowed only in the `metrics/` and `config/` packages.

---

## Step-by-Step: Running the Full Stack

### Step 1 — Build the Spring Boot jar

```bash
cd clinic-api-app
mvn clean package -DskipTests
```

Produces `target/clinic-api-2.0.0.jar`.

---

### Step 2 — Start the core stack

```bash
docker-compose up --build
```

| Container | Image | Port | Starts when |
|---|---|---|---|
| `clinic-mysql` | `mysql:8.0` | 3306 | Immediately |
| `clinic-api` | Built from `Dockerfile` | 9999 | After MySQL healthcheck passes |
| `prometheus` | `prom/prometheus:latest` | 9090 | After `clinic-api` starts |

**Verify:**
```bash
curl http://localhost:9999/actuator/health
# open http://localhost:9090/targets  ← clinic-api should show state: UP
```

On first startup, `DataInitializer` seeds the DB (dev profile):
- 4 doctors, 3 patients, 4 appointments

---

### Step 3 — Build the simulator image

```bash
cd clinic-api-app/clinic-load-simulator
docker build -t clinic-load-simulator .
```

---

### Step 4 — Run the simulator

```bash
docker run --network host clinic-load-simulator
```

The simulator runs with `--network host` so it can reach `localhost:9999`.

**More load:**
```bash
docker run --network host clinic-load-simulator \
  python clinic_load_simulator.py --url http://localhost:9999 --workers 10
```

**Specific scenarios:**
```bash
docker run --network host clinic-load-simulator \
  python clinic_load_simulator.py --scenarios happy_path,cancel_flood
```

**Stop:** `Ctrl+C` — graceful shutdown with final stats.

---

## The 6 Simulator Scenarios

| Scenario | What it does | HTTP responses | Metric driven |
|---|---|---|---|
| `happy_path` | Register doctor → patient → book → confirm → complete | 201, 200 | `clinic_appointments_created_total` |
| `slot_conflict` | Books same doctor + date + slot twice | 201, then **409** | 4xx error rate |
| `bad_doctor` | Books with a non-existent doctorId | **404** | 404 spike |
| `bad_validation` | Sends malformed payloads | **400** | 4xx spike |
| `read_traffic` | Floods GET endpoints — lists, filters, name search | 200 | Low-latency histogram buckets |
| `cancel_flood` | Creates appointments then immediately cancels | 201, 200 | `clinic_appointments_cancelled_total` |

---

## Prometheus Metrics Reference

### Business Counters — event-driven, defined in `ClinicMetricsListener`

| Prometheus name | Triggered when |
|---|---|
| `clinic_appointments_created_total` | `AppointmentCreatedEvent` fires (on successful booking) |
| `clinic_appointments_cancelled_total` | `AppointmentCancelledEvent` fires (status → CANCELLED) |
| `clinic_doctors_created_total` | `DoctorCreatedEvent` fires |
| `clinic_doctors_deleted_total` | `DoctorDeletedEvent` fires |
| `clinic_patients_created_total` | `PatientCreatedEvent` fires |

### Live-State Gauges — DB-queried on each scrape, defined in `ClinicGaugesRegistrar`

| Prometheus name | What it reflects |
|---|---|
| `clinic_appointments_scheduled_total` | Count of appointments currently in SCHEDULED state |
| `clinic_appointments_today_total` | Appointments booked for today's date |

### Service Latency Histograms — automatic via `@Timed`

Every service method annotated with `@Timed` produces three Prometheus series automatically.
No code in the metrics layer needed — AOP intercepts the call.

Examples:
```
clinic_service_appointments_create_seconds_bucket{le="0.1"}
clinic_service_appointments_create_seconds_count
clinic_service_appointments_create_seconds_sum

clinic_service_doctors_create_seconds_bucket{le="0.25"}
clinic_service_patients_getAll_seconds_count
```

### Auto-instrumented — free with Spring Boot Actuator + Micrometer

| Metric | What it measures |
|---|---|
| `http_server_requests_seconds_*` | Every HTTP request — latency, status, URI, method |
| `jvm_memory_used_bytes` | JVM heap and non-heap |
| `hikaricp_connections_*` | DB connection pool state |
| `jvm_gc_pause_seconds_*` | Garbage collection pause durations |

---

## Useful PromQL Queries

```promql
# HTTP request rate per second
rate(http_server_requests_seconds_count[1m])

# p95 end-to-end latency
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# Error rate %
rate(http_server_requests_seconds_count{status=~"4..|5.."}[1m])
  / rate(http_server_requests_seconds_count[1m]) * 100

# p99 service-level latency for appointment creation only
histogram_quantile(0.99, rate(clinic_service_appointments_create_seconds_bucket[5m]))

# Business: appointment booking rate
rate(clinic_appointments_created_total[5m])

# Business: cancellation rate
rate(clinic_appointments_cancelled_total[5m])

# Business: live scheduled count
clinic_appointments_scheduled_total
```

---

## Startup Sequence

```
docker-compose up --build
  │
  ├─► clinic-mysql starts
  │       └─► healthcheck passes (mysqladmin ping)
  │
  ├─► clinic-api starts
  │       ├─► Hibernate creates/updates schema
  │       ├─► ClinicGaugesRegistrar registers DB-backed gauges (@PostConstruct)
  │       ├─► DataInitializer seeds demo data (dev profile)
  │       └─► /actuator/prometheus becomes available
  │
  └─► prometheus starts
          └─► scrapes clinic-api:9999 every 15s

docker run --network host clinic-load-simulator
  └─► waits for /actuator/health
  └─► launches 6 scenario threads
  └─► business events fire → ClinicMetricsListener increments counters
  └─► @Timed AOP intercepts service calls → records latency histograms
  └─► Prometheus scrapes and stores everything
```

---

## Stopping Everything

```bash
docker-compose down          # stop stack, keep DB data
docker-compose down -v       # stop stack + wipe DB volume (clean slate)
```

---

## Key URLs

| URL | What it is |
|---|---|
| `http://localhost:9999/swagger-ui.html` | Interactive API docs |
| `http://localhost:9999/actuator/health` | App health check |
| `http://localhost:9999/actuator/prometheus` | Raw Prometheus metrics |
| `http://localhost:9090` | Prometheus UI |
| `http://localhost:9090/targets` | Confirm scrape targets are UP |
