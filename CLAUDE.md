# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

`qywx-wecom-spring-boot-starter` (groupId `org.cy`, artifact `qywx-wecom-spring-boot-starter`) — a standalone Spring Boot 3.5 auto-configuration starter that wraps WxJava (`weixin-java-cp`) to query WeCom (企业微信) contacts, OA approvals, and Smart HR rosters.

Note: this directory is an independent Maven module / Git repo. The outer `YuAnXm/CLAUDE.md` describes a different (ESOP2) project and does not apply here. The artifact is published to GitHub Packages (`maven.pkg.github.com/cheny-dd/qywx`).

## Common commands

```powershell
# Compile only
mvn clean compile

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=WxApprovalQueryUtilTest

# Run a single test method
mvn test -Dtest=WxApprovalQueryUtilTest#methodName

# Build jar (skip tests)
mvn clean install -DskipTests

# Publish to GitHub Packages (requires server creds for id `github`)
mvn deploy
```

Toolchain: Java 21, Maven, Spring Boot 3.5.1, Spring Framework 6.2.10. Lombok is used as an annotation processor (configured in `maven-compiler-plugin`).

## Architecture

This is a starter library, not an application — there is no `main` method or runnable Spring Boot app. Consumers depend on the jar; auto-configuration wires beans into their context.

### Auto-configuration entry point

`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` registers `org.cy.qywx.config.QywxWecomAutoConfiguration`, which conditionally creates beans only when properties are present:

- `wx.cp.corp-id` + `wx.cp.corp-secret` + `wx.cp.agent-id` → primary `WxCpService` (contact / approval scope)
- `wx.cp.corp-id` + `wx.cp.hr.secret` → secondary `WxCpService` named `qywxHrCpService` (Smart HR scope, **uses its own independent secret** — must NOT reuse the contact secret)

All beans are gated with `@ConditionalOnMissingBean`, so callers can override any of them.

### Two `WxCpService` instances

A WeCom corp issues a separate secret per app. Contact + Approval share one app's secret; "Smart HR / 智慧人事" issues a different secret. The starter therefore creates two `WxCpService` beans and routes:

- `WxContactQueryUtil`, `WxApprovalQueryUtil`, `WxApiClient` → primary `WxCpService`
- `WxHrRosterQueryUtil` → `@Qualifier("qywxHrCpService")`
- `WxCheckinQueryUtil` → primary `WxCpService` (checkin endpoints live on `WxCpOaService`: `getCheckinData`, `getCheckinDayData`, `getCheckinMonthData`, `getCheckinScheduleList`, `getCropCheckinOption` — note the "Crop" typo is WxJava's)

When adding new utilities, decide which app's API they hit and inject the matching service. If you accidentally call HR endpoints (`/cgi-bin/hr/*`) with the contact secret, WeCom returns `errcode=48002 / 60011`.

### Configuration model (`WxCpProperties`, prefix `wx.cp`)

Three nested groups, each with its own concurrency knobs:

- top-level: `corpId`, `corpSecret`, `agentId`
- `wx.cp.approval.*`: `segmentDays` (default 29 — WeCom approval API caps a single query at ≤31 days), `pageSize`, `maxRetryAttempts`, `retryBackoffMillis`, `requestsPerSecond`, `executorThreads`
- `wx.cp.hr.*`: `secret`, `agentId`, retry/throttle knobs, `executorThreads`

Two fixed thread pools are created with `destroyMethod="shutdown"`:
- `qywxApprovalQueryExecutor` (bean name constant `APPROVAL_EXECUTOR_BEAN_NAME`)
- `qywxHrRosterExecutor` (bean name constant `HR_EXECUTOR_BEAN_NAME`)
- `qywxCheckinExecutor` (bean name constant `CHECKIN_EXECUTOR_BEAN_NAME`)

### Cross-cutting patterns to preserve when editing

- **Date-range segmentation**: WeCom approval API limits queries to ~31 days. `WxDateRangeUtils` slices wider ranges into `segmentDays`-sized chunks; queries fan out via `CompletableFuture` on the relevant executor.
- **Throttling**: rate limiting is implemented inline with `LockSupport.parkNanos` keyed off `requestsPerSecond` (set to `0` to disable). When adding new endpoints, reuse this pattern instead of pulling in a new dependency.
- **Retries**: each query util implements its own bounded exponential-ish retry (`maxRetryAttempts` × `retryBackoffMillis`). Failures past the cap are collected into `*FetchFailure` records (e.g. `WxApprovalDetailFetchFailure`, `WxHrRosterFetchFailure`) and returned alongside successes in result records (`WxApprovalDetailQueryResult`, `WxHrRosterResult`) — callers get partial results rather than an exception.
- **VO conversion**: WxJava beans are converted to local `org.cy.qywx.vo.*` VOs via static converters (`WxContactConverter`, `WxApprovalConverter`). Don't leak `me.chanjar.weixin.*` types out of the `util` package.
- **Department enrichment**: `WxContactQueryUtil` post-processes user lists to fill `departments` and `mainDepartmentName` from a single department-list fetch. Preserve this batching when adding new user-returning methods — don't fetch departments per user.
- **User batching for ≤100/call APIs**: `WxCheckinQueryUtil` partitions userIds into batches of `userBatchSize` and crosses with date segments (cartesian product → parallel `CompletableFuture`s). When adding new endpoints with similar API limits, reuse the same `partitionUsers` + `segmentDates` helpers instead of re-implementing.

### Generic HTTP path (`WxApiClient`)

WxJava doesn't cover every WeCom endpoint (notably HR). `WxApiClient` provides a thin `HttpClient`-based GET/POST that auto-appends `access_token` from the injected `WxCpService` and validates `errcode` in the JSON response. Use it for any new endpoint not exposed by WxJava rather than hand-rolling token plumbing.

## Dependency overrides

`dependencyManagement` in `pom.xml` pins Spring Framework, Logback, Jackson, and AssertJ above Spring Boot 3.5.1's defaults to patch CVEs (CVE-2025-41242, CVE-2025-41249, CVE-2025-11226, CVE-2026-1225, GHSA-72hv-8253-57qq, WS-2026-0003, CVE-2026-24400). When bumping `spring-boot.version`, re-check whether these overrides are still required or can be dropped.

## Testing

Tests live under `src/test/java/org/cy/qywx/`. Patterns in use:

- Pure unit tests for converters and date utilities (`WxContactConverterTest`, `WxApprovalConverterTest`, `WxDateRangeUtilsTest`).
- Mockito-based tests for query utils (`WxApprovalQueryUtilTest`, `WxHrRosterQueryUtilTest`, `WxApiClientTest`) — they mock `WxCpService` / `HttpClient` rather than hitting the network.
- `QywxWecomAutoConfigurationTest` exercises the auto-config with `ApplicationContextRunner` to verify conditional bean creation. Mirror this style when adding new beans to the auto-config.
