# Changelog

## 2.0.0-beta.5

### Deployment
- Reworked releases around `Kira-NT/mc-publish` v3.3.1 for Modrinth, CurseForge, and GitHub Releases.
- Publish Fabric and NeoForge as distinct platform versions while creating only one GitHub release/tag.
- Build once, then fan out into independent Modrinth, CurseForge, and GitHub jobs so failed destinations can be retried without duplicating successful uploads.
- Add targeted manual recovery options for each loader/platform destination.
- Validate release tags against `mod_version` and verify loader jars before upload.
- Fail early when Modrinth or CurseForge credentials are missing.
- Pin the current GitHub Actions releases and mc-publish to immutable commit SHAs.
- Add Dependabot maintenance for Gradle and GitHub Actions.

### Build
- Centralize project version/group in `gradle.properties`.
- Keep the existing, known project dependency baseline for the release source rather than forcing untested loader/runtime upgrades.
- Document newer Kotlin/Fabric updates as follow-up candidates; keep Kotlin for Forge 5.11.0 and NeoForge 21.1.26 pending real launch testing because newer KFF/NeoForge combinations have unresolved 1.21.1 language-provider reports.
- Enable the Gradle build cache and configuration cache and use a Java 21 toolchain.
- Verify the Gradle 9.5.1 distribution with its official SHA-256 checksum; regenerate the checked-in wrapper JAR separately before release.
- Remove unused repositories, buildscript dependencies, compiler flags, and the unused `maven-publish` plugin.

### Correctness and cleanup
- Fix duplicate ledger registration so duplicate ids fail instead of returning an unregistered key.
- Expose the registered-key `BiMap` as an unmodifiable view.
- Make offline cache loading and per-key serialization more resilient to malformed or failing data.
- Keep cache internals private and use a stable player-data directory when an `OfflinePlayer` is closed.
- Fix username-change handling in the UUID/name `BiMap` and make offline name lookups case-insensitive.
- Fix command registration conflicts around `/remnant clear`.
- Make `get` and `list` correctly support live online players as well as cached offline players.
- Correct `/remnant players` counts and improve command failure reporting.
- Fix NeoForge event registration so integrated servers receive login/logout/command events without duplicate subscriber paths.
- Refresh documentation to match the current Remnant API, loaders, commands, and BML license.
