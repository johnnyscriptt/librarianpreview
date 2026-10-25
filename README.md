# Librarian Preview

Shows a librarian's trades on the lectern it claims, so you can reroll without opening the trade menu. A Fabric mod for Minecraft 26.3.

The full player-facing description is in
[`upload-pack/copy/description-modrinth.md`](upload-pack/copy/description-modrinth.md).

## Keys

| Key | Does |
| --- | --- |
| | |

All rebindable in Options, Controls, under **Librarian Preview**.

## Multiplayer

<Client only, or: one jar, both sides, and what works on a server without the mod.>

## Building

Requires JDK 25.

```
.\gradlew.bat test                 unit tests
.\gradlew.bat build                build\libs\librarianpreview-<version>.jar
.\gradlew.bat runClientGameTest    in-game tests, store screenshots and the icon render
```

## Architecture

- `core/`: pure Java with no Minecraft imports, covered by unit tests.
- `src/main`: common entrypoint, payloads, server handlers.
- `src/client`: keys, screens, HUD and rendering.
- `src/gametest`: store-shot and icon-render client gametests (not in the jar).

## License

MIT, see [LICENSE](LICENSE).
