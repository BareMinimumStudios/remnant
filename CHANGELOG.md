# Changelog

Notable changes to Remnant are documented here, newest release first.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

## [3.0.0] - 2026-10-07

Stable release for Minecraft 1.21.1 on Fabric and NeoForge. Remnant continues Offline Player Cache (OPC).

### Added

- Separate Fabric and NeoForge publishing jobs for Modrinth and CurseForge, plus a single GitHub release containing both loader JARs.
- Manual recovery options for each loader and destination, so one failed upload can be retried without repeating successful uploads.
- Release validation for version tags, loader JARs, and missing marketplace credentials.
- A refreshed Remnant logo and banner based on the original OPC artwork, with a 256×256 icon and a 1920×640 banner optimized for web use.
- GitHub, Modrinth, and CurseForge descriptions, with the Bare Minimum Studios BisectHosting banner and sponsor code.
- MixinMCP 1.5.0 development tooling and an IntelliJ/Gradle setup guide.
- Dependabot configuration for Gradle and GitHub Actions maintenance.

### Changed

- Public versions use `3.0.0+1.21.1-fabric` and `3.0.0+1.21.1-neoforge`; the internal mod version remains `3.0.0` and the GitHub tag is `v3.0.0+1.21.1`.
- Publishing builds both loaders once, then uploads to each destination independently with stable Release status.
- GitHub release authentication uses the automatic `GITHUB_TOKEN` with permission to write repository contents.
- Publishing stages verified runtime JARs under consistent filenames and uses only the current release's changelog section as release notes.
- Project version and Maven group are configured in `gradle.properties`.
- Builds use Java 21, Gradle build and configuration caches, and a checksum-verified Gradle distribution.
- Gradle parallel execution is disabled for the MixinMCP/Vineflower development workflow to reduce decompilation memory pressure.
- Cached UUID and name collections now return snapshots instead of mutable backing-map views; the registered-key map is exposed as an unmodifiable view.
- Offline cache loading and per-key serialization handle malformed or failing data more safely.
- Documentation reflects the current API, commands, dependencies, project links, and BML v1.0 license. Maven examples use Modrinth project ID `oLPaySSb`.
- GitHub Actions and mc-publish references remain pinned to commit SHAs.

### Removed

- Unused repositories, buildscript dependencies, compiler flags, and the unused `maven-publish` plugin.

### Fixed

- Build-tool incompatibilities by aligning Gradle 9.2.1, Cloche 0.18.10, Kotlin 2.2.21, and matching Fabric Language Kotlin, with Kotlin 2.2 language/API output.
- Fabric dependency transforms running before their required mapping archive was generated.
- Duplicate ledger registrations returning an unregistered key instead of rejecting the duplicate ID.
- Offline player persistence using an unstable player-data directory when a player entry is closed.
- Username changes leaving inconsistent UUID/name mappings, and case-sensitive offline name lookups.
- Command registration conflicts involving `/remnant clear`.
- `get` and `list` failing to handle live online players alongside cached offline players.
- Incorrect `/remnant players` counts and unclear command failure reporting.
- NeoForge integrated servers missing login, logout, or command events, with duplicate subscriber paths removed.

[Unreleased]: https://github.com/BareMinimumStudios/remnant/compare/v3.0.0+1.21.1...HEAD
[3.0.0]: https://github.com/BareMinimumStudios/remnant/releases/tag/v3.0.0+1.21.1
