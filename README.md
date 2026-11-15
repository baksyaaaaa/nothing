# PortalFlight — Fabric 1.21.11

Source project for Minecraft Java 1.21.11, Fabric, Java 21.

## Controls
- V: speed boost (client-side, may be rejected by multiplayer servers)
- B: boat vertical movement (Space up, Shift down; may be rejected by multiplayer servers)
- N: locate nearest loaded end portal frame within 24 horizontal / 16 vertical blocks, showing coordinates in the action bar

**Important:** This is an untested source project, not a compiled mod JAR. Portal locator displays coordinates; it does **not** draw an ESP overlay through walls. Multiplayer servers can reject movement changes. This mod does not bypass anti-cheat.

## Build
1. Install JDK 21 and Gradle 8.14+.
2. Open a terminal in this folder.
3. Run `gradle build`.
4. If compilation succeeds, find the remapped mod JAR in `build/libs/` (not the `-sources.jar`).
5. Install Fabric Loader and Fabric API for Minecraft 1.21.11 and place the JAR in `.minecraft/mods`.

The source uses Mojang mappings. Fabric/MC API signatures may need adjustment because this project has not been compiled against the Minecraft dependencies in this environment.
