# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Gradle plugin that integrates [ShellCheck](https://www.shellcheck.net/) (shell script static analysis) into Gradle builds. Published as `com.felipefzdz.gradle.shellcheck`. Supports two modes: Docker (default, using `koalaman/shellcheck-alpine`) and local binary (with optional auto-installation via 14 package managers).

## Build Commands

```bash
./gradlew check                # Build + run all tests (including functional tests)
./gradlew shellcheck:test      # Unit tests only
./gradlew functionalTest       # Functional tests only (uses Gradle TestKit, 4 parallel forks)
./gradlew publishPlugins       # Publish to Gradle Plugin Portal (runs functionalTest first)
```

Run a single test class:
```bash
./gradlew functionalTest --tests "com.felipefzdz.gradle.shellcheck.ShellcheckDockerPluginFuncTest"
```

Run a single test method:
```bash
./gradlew functionalTest --tests "*.ShellcheckDockerPluginFuncTest.can use configuration cache"
```

## Architecture

All source is in the `shellcheck/` subproject under `com.felipefzdz.gradle.shellcheck`.

**Core flow:** `ShellcheckPlugin` (registers extension + task) → `Shellcheck` task (cacheable, extends `ConventionTask`) → `ShellcheckInvoker` (static orchestrator that runs shellcheck, merges XML reports, generates HTML via XSLT, calculates violation summary).

Key classes:
- **ShellcheckPlugin.java** — Plugin entry point; applies `ReportingBasePlugin`, creates extension, registers task with convention mapping.
- **Shellcheck.java** — `@CacheableTask` implementing `VerificationTask`. Delegates execution to `ShellcheckInvoker.invoke(this)`.
- **ShellcheckExtension.java** — User DSL; extends `CodeQualityExtension`. Defaults: `useDocker=true`, `severity="style"`, `shellcheckVersion="v0.7.1"`.
- **ShellcheckInvoker.java** — Largest class (~300 lines). Handles directory scanning (`find | xargs`), individual file processing, report generation (checkstyle XML → HTML via XSLT, TTY text output), and violation counting.
- **Shell.java** — Process execution wrapper; scrubs environment to only PATH and HOME (Docker Mac compatibility).
- **ShellcheckInstaller.java** — Auto-installs shellcheck via specified package manager if binary not found.

Reports (HTML/XML/TXT) follow Gradle's reporting conventions under `build/reports/shellcheck/`.

## Testing

Tests are **functional/integration tests** using Gradle TestKit + Spock Framework (Groovy), in the `functionalTest` source set.

- **BaseInfraTest.groovy** — Abstract base; sets up temp project dirs, generates `build.gradle.kts`, copies test resources, provides `GradleRunner` builders.
- **BaseShellcheckPluginFuncTest.groovy** — 13 shared test cases (violations, ignoreFailures, caching, severity, reports, additionalArguments). Abstract; subclasses provide `useDocker`/`shellcheckBinary`.
- **ShellcheckDockerPluginFuncTest** — Docker-mode tests (requires Docker). Adds tests for image pull errors, version config, configuration cache, env var preservation.
- **ShellcheckBinaryPluginFuncTest** — Local binary tests. Skipped unless `SHELLCHECK_PATH` env var is set.
- **ShellcheckInstallerPluginFuncTest** — macOS-only; tests brew auto-installation.
- **ShellcheckCrossVersionTest** — Tests against Gradle 7.0, 7.6.3, 8.10.1, 9.0, 9.4.0. Uses Docker mode to avoid architecture mismatches on Apple Silicon (see Gotchas).

Test resources in `src/functionalTest/resources/`: `with_violations/`, `without_violations/`, `another_without_violations/`, `no_shell_scripts/`.

## Key Design Decisions

- Directory sources use `find | xargs shellcheck` for performance; `sourceFiles` runs individual invocations (supports non-standard extensions).
- Shell file extensions recognized: `.sh`, `.bash`, `.ksh`, `.bashrc`, `.bash_profile`, `.bash_login`, `.bash_logout`.
- Environment scrubbing in `Shell.java` preserves only PATH and HOME — this is intentional for Docker on Mac compatibility.
- XML reports from multiple invocations are merged by combining `<file>` elements under a single `<checkstyle>` root.
- Java 8 target compatibility.

## Gotchas

- **`withDebug(true)` bypasses JDK selection.** Debug mode runs Gradle in-process (same JVM), which ignores `org.gradle.java.home` in `gradle.properties`. Cross-version tests must NOT use debug mode so the forked daemon picks up the correct JDK.
- **Apple Silicon + x86_64 JDKs.** Older Gradle versions (< 9) need JDK 11/17, which may only be available as x86_64 (Rosetta). An x86_64 Gradle daemon can intermittently fail to exec arm64 native binaries like shellcheck. Use Docker mode in tests to avoid this architecture mismatch.
