---
title: Feature Flag System — Architecture & Implementation Notes
summary: Pragmatic blueprint for a LaunchDarkly-style feature flag system using Spring Boot, MySQL, Redis, SSE/WebSocket, Kafka, and Java/Node SDKs.
---

# Feature Flag System — Architecture & Implementation Notes

This document is a clean, engineer-focused reference for implementing a production-ready feature flag system. It contains the essential design, data model, evaluation algorithm, API surface, caching strategy, SDK behaviour, testing checklist and deployment guidance.

Use this doc to:
- bootstrap implementation decisions
- create database migrations and API contracts
- implement deterministic evaluation logic to run in both server and SDKs

## Quick overview

Components:
- Flag API Service (Spring Boot): admin endpoints, CRUD, manage flags and rules.
- Evaluation/Cache Layer: Redis for low-latency reads; worker(s) to keep cache in sync.
- Real-time propagation: SSE (recommended) or WebSocket to push updates to SDKs.
- Event bus: Kafka/RabbitMQ for analytics, cache update events, and audit processing.
- Admin UI: Next.js + TypeScript for flag management and audit/analytics.
- SDKs: Java + Node (bootstrap snapshot, local evaluation, subscribe to updates).
- DB: MySQL for persistent storage of flags, rules, segments, and audit logs.

Flow (write path): Admin UI → Flag API writes MySQL → publish flag.updated → worker updates Redis → push flag.changed to SDKs.

Read path (hot): SDKs / API evaluate using Redis snapshot only.

## Contract: evaluation API

- Endpoint: POST /api/v1/evaluate
- Input JSON (EvalRequest):
  - flagKey: string
  - env: string
  - user: { key: string, attributes?: object }
- Output JSON (EvalResponse):
  - flagKey: string
  - value: boolean|string|number
  - variantKey?: string
  - reason?: string (e.g. "target-rule-match", "default")

Error modes:
- 400 Bad Request for malformed requests
- 404 Not Found if flag or environment unknown (optional: fall back to default)
- 429 Rate limit for high QPS from a single SDK key

Success criteria for local SDK evaluation:
- Deterministic result matching server evaluation for same inputs and snapshot

## Data model (concise)

Entities (primary fields):
- FeatureFlag: id, flag_key, flag_type (BOOLEAN/STRING/NUMBER), default_value, enabled, created_by, created_at, updated_at
- Environment: id, name (production/staging)
- FlagVariant: id, flag_id, env_id, variant_key, value, rollout_percentage
- TargetRule: id, flag_id, env_id, priority, condition_json, variant_key, active
- Segment: id, name, definition_json
- AuditLog: id, user, action, details, created_at
- UsageEvent: id, flag_id, env_id, user_key, variant_key, evaluated_at

Suggested SQL schemas (trimmed):

```sql
CREATE TABLE feature_flags (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  flag_key VARCHAR(255) NOT NULL UNIQUE,
  flag_type ENUM('BOOLEAN','STRING','NUMBER') NOT NULL DEFAULT 'BOOLEAN',
  default_value VARCHAR(255),
  enabled BOOLEAN DEFAULT TRUE,
  created_by VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE environments (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(64) UNIQUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE flag_variants (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  flag_id BIGINT NOT NULL,
  env_id INT NOT NULL,
  variant_key VARCHAR(255) NOT NULL,
  value VARCHAR(255),
  rollout_percentage INT DEFAULT 100,
  FOREIGN KEY (flag_id) REFERENCES feature_flags(id),
  FOREIGN KEY (env_id) REFERENCES environments(id)
);
```

Keep segment and rule tables using JSON for flexibility; validate complexity at write time.

## API surface (essential endpoints)

- GET /api/v1/flags?env={env} — list flags (include variants & rules when requested)
- GET /api/v1/flags/{flagKey}?env={env} — single flag
- POST /api/v1/flags — create flag
- PUT /api/v1/flags/{flagKey} — update flag
- DELETE /api/v1/flags/{flagKey} — delete flag
- POST /api/v1/flags/{flagKey}/variants — add/update variant
- POST /api/v1/flags/{flagKey}/rules — add/update target rule
- GET /api/v1/flags/snapshot?env={env}&sdkKey={key} — snapshot for SDK bootstrap
- POST /api/v1/evaluate — evaluation API (see contract above)
- GET /api/v1/stream?env={env}&sdkKey={key} — SSE endpoint for updates
- POST /api/v1/events — ingestion of impressions/usage events (async)
- GET /api/v1/audit — audit logs (paginated)

API design notes:
- Versioning (e.g., /api/v1) is required.
- Snapshot responses should be compact and include rules ordered by priority for fast client evaluation.

## Evaluation algorithm (deterministic & fast)

Requirements:
- Run entirely in memory using the Redis snapshot for hot-path evaluations
- Deterministic bucketing for percentage rollouts
- Support operators: equals, contains, startsWith, regex, in-segment

Algorithm (high-level):
1. Fetch flag definition (from Redis hash flags:{env} with field flagKey)
2. If no definition, either fallback to server DB read or return default
3. Iterate target rules in priority order
   - Evaluate condition JSON against user context
   - If rule matches and rollout < 100, compute bucket(user.key, flagKey) in 1..100
     - If bucket <= rollout_percentage, return the rule's variant
   - If rule matches and rollout == 100, return the rule's variant
4. If no rule matched, return flag.default_value or flag.enabled
5. Emit impression/event asynchronously (do not block evaluation)

Deterministic bucketing (Java example):

```java
int bucket(String userKey, String flagKey) throws NoSuchAlgorithmException {
    byte[] hash = MessageDigest.getInstance("SHA-1")
        .digest((userKey + ":" + flagKey).getBytes(StandardCharsets.UTF_8));
    int bucket = ByteBuffer.wrap(hash, 0, 4).getInt() & 0x7fffffff;
    return (bucket % 100) + 1; // 1..100
}
```

Edge cases to handle:
- Missing or blank user.key: treat as anonymous or use deterministic fallback id
- Overly complex condition JSON: enforce size and operator limits on write
- Segment resolution: resolve segments during cache build to avoid DB calls on evaluation

## Caching and consistency

Cache format:
- Redis key: flags:{env} (HASH)
- Field: flagKey → value: serialized JSON containing flag metadata, variants, and ordered rules

Update flow:
1. Admin writes to MySQL
2. API publishes flag.updated event to Kafka
3. Cache worker consumes events and updates Redis atomically (SET or HSET)
4. Worker publishes SSE/WebSocket notifications to connected clients

Consistency notes:
- Prefer event-driven updates over TTL-based invalidation
- Use Lua scripts or CAS where multiple keys must be updated together

## Real-time propagation

When to use SSE vs WebSocket:
- SSE: recommended for one-way config updates (browser and server SDKs). Simpler and robust.
- WebSocket: use when you need two-way communication (e.g., client acks or telemetry over the same channel).

SSE payload example:

```json
{ "type": "flag.changed", "env": "production", "flagKey": "new-checkout", "payload": { /* serialized flag */ } }
```

Spring Boot SSE snippet:

```java
@GetMapping("/stream")
public SseEmitter stream(@RequestParam String sdkKey) {
    SseEmitter emitter = new SseEmitter(0L);
    subscriptionService.register(sdkKey, emitter);
    emitter.onCompletion(() -> subscriptionService.unregister(sdkKey));
    emitter.onTimeout(() -> subscriptionService.unregister(sdkKey));
    return emitter;
}
```

## SDK design (Java / Node)

Responsibilities:
- Bootstrap by fetching a snapshot
- Start SSE/WebSocket to receive incremental updates
- Evaluate flags locally using the same algorithm as the service
- Fallback to last-known snapshot if offline

Java SDK skeleton:

```java
public class FFClient {
  private final ConcurrentMap<String, FlagDef> cache = new ConcurrentHashMap<>();
  public FFClient(String sdkKey, String env, String apiBase) {
    // fetch snapshot and start subscription
  }
  public <T> T evaluate(String flagKey, UserContext ctx) { ... }
}
```

Node SDK usage:

```js
const client = new FFClient({ sdkKey: 'abc', env: 'production', url: 'https://api' });
const val = client.evaluate('new-checkout', { key: 'user-222', email: 'x@y.com' });
```

Thread-safety and performance:
- Use lock-free concurrency primitives where possible (ConcurrentHashMap)
- Keep evaluation functions pure and side-effect free

## Admin UI (essential pages)

- Flags list (filters by env, tag, status)
- Flag editor (key, description, default, variants, rollout)
- Targeting rule builder (visual, operators: equals, contains, regex, segment)
- Segments management (static lists or rule-based)
- Audits (history + rollback button)
- Analytics (variant distribution, time-series)
- SDK keys management (scoped read-only SDK keys vs admin keys)

UI features to implement early:
- Rule preview tester: simulate an evaluation for a given user context
- Visual percentage slider and cohort preview

## Analytics & event processing

- Impression ingestion: POST /api/v1/events (SDKs can sample or batch)
- Pipeline: Kafka → stream processor → aggregation DB or timeseries DB
- Surfaces: variant distribution, trends, and top users/segments

Privacy: allow sampling and PII controls; hash or salt user keys before storing in analytics tables.

## Security & multi-tenant

- API keys: separate per environment; SDK keys are read-only
- Admin auth: OAuth2 / JWT and RBAC (admin/editor/auditor roles)
- Rate-limiting: per SDK key and per IP
- Data isolation: add tenant_id to tables for multi-tenant setups
- Secrets: encrypt with KMS; rotate keys regularly

## Testing strategy

- Unit tests: evaluator logic, hashing determinism, condition operators
- Integration tests: endpoints with Testcontainers (MySQL) and an embedded Redis
- E2E tests: local stack that boots API, worker, Redis, and runs SDK to assert parity
- Property tests: randomized rules / users to assert deterministic behavior
- Load testing: k6 or Locust to validate p95/p99 under cache hits and misses

## Observability & SLOs

Metrics to collect:
- Evaluation latency (p50/p95/p99)
- Redis cache hit ratio
- Events published / consumed and consumer lag
- Active SSE/WebSocket connections
- API error rates and 5xx counts

Tracing and logs:
- Distributed traces (OpenTelemetry / Jaeger) across API → event bus → workers
- Structured logs for admin changes and errors

Dashboards & alerts:
- Grafana dashboards for latency, cache hit rate, and consumer lag
- Alerts for high error rate, low cache hit ratio, or long consumer lag

## Deployment & infra notes

- Containerize services and use Kubernetes for staging/prod
- Use managed Redis and MySQL when possible
- Use Kafka (or managed alternative) for event-driven consistency
- CI/CD: PR → unit tests → build image → integration tests → staging → manual approval → prod

Production hardening checklist:
- TLS everywhere (API, SSE)
- API key rotation and expiry
- Backups and read replicas for MySQL
- Rate-limiting and circuit breakers where appropriate

## Roadmap (minimal milestones)

MVP
- Flag CRUD, simple evaluation endpoint, Redis snapshot endpoint, basic admin UI, Java SDK bootstrap

v1
- Target rules, deterministic bucketing, SSE updates, audit logs, basic analytics

v2
- Segments, multi-environment SDK keys, advanced operators (regex/contains), monitoring and rate limits

v3
- Multi-tenant isolation, KMS-based encryption, advanced analytics and cohorts, additional SDKs

## Example snippets

Evaluation controller (Spring Boot skeleton):

```java
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EvaluationController {
    private final FlagService flagService;
    private final EventProducer eventProducer;

    @PostMapping("/evaluate")
    public ResponseEntity<EvalResponse> evaluate(@RequestBody EvalRequest req) {
        EvalResult result = flagService.evaluate(req.getFlagKey(), req.getEnv(), req.getUser());
        eventProducer.publishImpression(result);
        return ResponseEntity.ok(EvalResponse.from(result));
    }
}
```

Redis update consumer (pseudo):

```java
@KafkaListener(topics = "flag-updates")
public void onFlagUpdate(FlagUpdateEvent e) {
    String redisKey = "flags:" + e.getEnv();
    redisTemplate.opsForHash().put(redisKey, e.getFlagKey(), e.getSerializedPayload());
    ssePublisher.publish(e);
}
```

## Next steps (pick one)

1. Generate service interfaces and DTOs for the Spring Boot project (controllers, DTOs, repository interfaces).
2. Create Flyway DB migrations and initial seed data for environments + a sample flag.
3. Draft Next.js admin UI page schemas and a minimal rule-builder component.

If you tell me which to prioritize, I will generate code and tests for that artifact.

---
Requirements coverage: This file documents architecture, data model, evaluation algorithm, API surface, caching, SDK behaviour, testing, observability, security, and deployment. Implementation artifacts (code/migrations/UI) are offered as next steps.