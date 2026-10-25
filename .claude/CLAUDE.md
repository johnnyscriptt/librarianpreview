# Librarian Preview (Fabric mod)

Built and published with the `minecraft-mod-studio` skill; its references hold the brand,
store and upload conventions. This file only records what is specific to this mod.

## Platform and stack
- Windows (PowerShell) build machine. Minecraft 26.3, Fabric Loader 0.19.5, Fabric API
  0.161.0+26.3, Loom 1.18-SNAPSHOT, Gradle 9.7.1, Java 25, Mojang mappings (no remap step).
- JUnit 5 for `core/`; Fabric client gametests in `src/gametest`.

## Commands (run from this folder; the parent folder is not a Gradle project)
- `.\gradlew.bat test`, `.\gradlew.bat build`, `.\gradlew.bat runClientGameTest`.
- Deploy for testing: quit Minecraft, remove the old jar, copy
  `build\libs\librarianpreview-<ver>.jar` into `%APPDATA%\.minecraft\mods\`.

## Conventions
- TDD: a failing test is seen before implementation. Pure logic lives in `core/` with no
  Minecraft imports; `src/test` only compiles against `src/main`.
- Accent colour `#B57CFF`: in-game highlight, caption bar, icon glow.
- Default keys: <record them here, and check references/scaffold.md for clashes>.
- Store text: no em dashes. `.ps1` string literals stay ASCII.

## Domain notes
- <rules that are easy to get wrong>

## Upload
- `upload-pack/UPLOAD-GUIDE.md` has every store field. Stores are logged in on the Windows
  Chrome. Modrinth: no AI images, in-game-render icon. CurseForge: ChatGPT icon.
