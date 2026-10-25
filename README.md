# Librarian Preview

Shows a librarian's trades on the lectern it claims, so you can reroll without opening the trade menu. A Fabric mod for Minecraft 26.3.

The full player-facing description is in
[`upload-pack/copy/description-modrinth.md`](upload-pack/copy/description-modrinth.md).

## Keys

| Key | Does |
| --- | --- |
| H | Show or hide the trade sheets |
| U | Open the settings (range, hunted enchantments) |

All rebindable in Options, Controls, under **Librarian Preview**.

## Multiplayer

Offers only exist on the server, so the server half reads each claimed librarian's trades and
sends them to nearby players. One jar, both sides. On a server without the mod nothing is shown.

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
