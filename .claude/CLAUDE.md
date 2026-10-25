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
- Default keys: H toggles the lectern panels (category "Librarian Preview").
- Store text: no em dashes. `.ps1` string literals stay ASCII.

## Domain notes
- Client+server: offers only exist on the server. `server/LecternScanner` runs every 20
  ticks, finds librarians whose `JOB_SITE` memory points at a loaded lectern, calls
  `getOffers()` (rolls level-1 trades like a first right-click; does not lock the
  profession) and sends deltas via `LecternTradesPayload`.
- Prices are base prices (`getBaseCostA`): reputation and Hero of the Village discounts are
  per player and only applied when the trade screen opens.
- Panel is drawn from a HEAD inject on `LecternRenderer.submit` (vanilla returns early
  without a book). Frame: rotate by `-(yRot + 90)`, tilt -22.5 about x through the block
  centre, then a left-handed GUI frame like sign text.
- Panel background quads are translucent and depth-sorted: never stack overlapping quads in
  one batch (they z-fight or sort wrong); arrows go in `collector.order(1)`.
- Sheet writing has one fixed size (`PanelLayout.WIDTH` fills the writing area) and starts
  at the top; more rows than fit scroll by whole rows. `MouseHandlerMixin` claims the wheel
  only when aiming at a lectern whose sheet can scroll, accumulating fractional steps.
- `LecternTradesGameTest` binds NoAI librarians to lecterns and screenshots the result.

## Upload
- `upload-pack/UPLOAD-GUIDE.md` has every store field. Stores are logged in on the Windows
  Chrome. Modrinth: no AI images, in-game-render icon. CurseForge: ChatGPT icon.
