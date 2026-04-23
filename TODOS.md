# TODOS

## Reliability

### Actuator Health/Readiness For Hardware State

**What:** Add Spring Boot Actuator health/readiness integration for the hardware connection state.

**Why:** `/api/v1/connection` is enough for simple clients, but Actuator lets monitoring tools distinguish "app alive" from "hardware unavailable."

**Context:** This was considered during the Aircontrol connection supervisor review and explicitly deferred from the core reliability pass to keep the first PR focused. Start from the future `ConnectionSnapshot` and `ConnectionState` model, then expose a custom health contributor that reports stale/disconnected hardware without hiding the fact that the web service is still alive.

**Effort:** M
**Priority:** P2
**Depends on:** Connection supervisor and stable connection state model.

### Background Polling Runtime

**What:** Add configurable background polling for selected registers so dashboards always have fresh cache values.

**Why:** The connection supervisor pass makes reads fast and honest, but it does not proactively keep every important register fresh unless something requests it.

**Context:** This was Approach C in the approved design doc. It was intentionally deferred because the root fix is connection supervision and cache-first API behavior. Revisit after the supervisor, fake serial tests, and register metadata indexing are in place. The design needs register selection, polling cadence, backoff, and device-load limits.

**Effort:** XL
**Priority:** P2
**Depends on:** Connection supervisor, fake serial tests, and register metadata indexing.

## Distribution

### Repeatable Release Packaging

**What:** Add a repeatable release pipeline for the executable jar, and later Docker/Home Assistant packaging if useful.

**Why:** The reliability pass makes the service better, but users still need a clear way to install the fixed version without manual release mistakes.

**Context:** The current plan keeps the existing Maven jar and systemd-style installation. The repo already has a `Dockerfile`, but no complete release workflow was reviewed. Start after Java 21 build hygiene and Maven wrapper repair are done.

**Effort:** L
**Priority:** P2
**Depends on:** Java 21 build hygiene and Maven wrapper repair.

## Completed
