# Remnant development

Remnant is a Kotlin/Cloche project targeting Minecraft 1.21.1 on Fabric and NeoForge.

## Requirements

- Java 21
- Gradle through the checked-in wrapper
- IntelliJ IDEA for the smoothest multi-loader workflow

## Common tasks

```bash
./gradlew clean build
```

MixinMCP's Gradle companion is included in the project for dependency-source indexing:

```bash
./gradlew genDependencySources
```

The first run can take a while because dependencies without published source jars are decompiled and cached. Later runs reuse that cache.

## IntelliJ + MixinMCP

For Minecraft-aware MCP tooling, install [MixinMCP](https://github.com/muon-rw/MixinMCP) in IntelliJ IDEA 2026.2 or newer and enable IntelliJ's built-in MCP Server under **Settings → Tools → MCP Server**.

The Gradle side of MixinMCP is already applied by this repository. The project also uses a 4 GB Gradle heap and keeps Gradle parallel execution disabled because large Vineflower decompilation jobs can otherwise become memory-heavy.

MixinMCP 1.5.0 automatically follows the standard `compileClasspath`. Cloche also creates loader-specific target classpaths, so a target-only third-party jar without published sources may need to be added explicitly until MixinMCP gains first-class Cloche discovery:

```bash
./gradlew genDependencySources --jar path/to/mod.jar
```

For a persistent local jar, use MixinMCP's `extraJars` configuration instead of committing decompiled output.

## One working copy

Keep IntelliJ, Codex/ChatGPT desktop, Git, and Gradle pointed at the same local repository. Do not maintain a second mirrored source folder or file-watcher copy. External edits can be picked up by IntelliJ with a VFS refresh or Gradle sync, and MixinMCP exposes project/classpath state directly through the IDE MCP server.

## Before pushing

Run:

```bash
./gradlew clean build --stacktrace
```

If build scripts or dependencies changed, also run:

```bash
./gradlew genDependencySources
```

Do not commit `.idea`, `.gradle`, `build`, run directories, crash reports, or MixinMCP's generated cache state.
